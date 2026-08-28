# Notification send scheduler — design

## Context

`VictoriaLogsErrorAlertScheduler` currently sends Slack alerts directly, via
`SlackAlertService`, which reads a Slack bot token/channel from a single global
`Setting` row (`group_name = notification`, `key_name = slack_error_alert`).
It never persists a `Notification` row, and it can only ever send to Slack.

The reference project `fm-api` (`D:\ITZ_PRO\fm-api\fm-api`) already solved
per-channel notification delivery with a two-scheduler split:

- A producer scheduler (`ServiceScheduler`) only creates `Notification` rows,
  each linked to a `NotificationGroup`.
- A consumer scheduler (`NotificationScheduler`) polls for pending
  `Notification` rows, resolves the Slack/Telegram token+channel from the
  row's own `NotificationGroup.channelSetting` (falling back to a global
  default `Setting` only when the group's own value is blank), sends, then
  deletes the row.

This project already has most of the fm-api-side building blocks ported
(`NotificationGroup.channelSetting`/`type`, `NotificationService.parseChannelSetting`,
`SlackService`, `TelegramSendService`) but `VictoriaLogsErrorAlertScheduler`
was never wired up to use them. This spec brings the alert-sending path in
line with fm-api's split, with two intentional differences confirmed by the
project owner:

- No global default `Setting` fallback — `NotificationGroup.channelSetting`
  is the only source of the token/channel. If it's missing, sending for that
  channel just fails/logs, same as it does today when a group has no config.
- Only two channel types exist (`telegram = 0`, `slack = 1`); there is no
  `disable` type to special-case.

## Goals

- `VictoriaLogsErrorAlertScheduler` stops sending anything itself; it only
  persists a `Notification` row for the active `NotificationGroup`.
- A new `NotificationScheduler` sends pending `Notification` rows to
  Slack/Telegram based on each row's own group, then deletes the rows it
  sent — mirroring fm-api's `NotificationScheduler` exactly, including its
  "one notification per channel type per tick" (`findFirst`, not `findAll`)
  behavior and its `fixedDelayString = "60000", zone = "UTC"` cadence.
- Dead code created by the old direct-Slack path (`SlackAlertService` and its
  two now-unused `BaseConstant` keys) is removed.

## Non-goals

- No change to `NotificationController`'s list/get/read/delete endpoints or
  to the `Notification` state (`SENT`/`READ`) semantics for rows that are
  created through other paths.
- No global default `Setting` fallback (explicitly out of scope, per project
  owner).
- No change to how `VictoriaLogsErrorAlertScheduler` decides which apps/
  queries breached their threshold — only how the result is delivered.

## Design

### 1. `NotificationRepository`

Add:

```java
Notification findFirstByNotificationGroupType(Integer type);
```

### 2. `VictoriaLogsErrorAlertScheduler`

- Remove the `SlackAlertService` dependency.
- Keep all existing breach-detection logic (`buildQuery`, `queryBreachesByApp`,
  `statsAlias`) unchanged.
- Where it currently builds `title` + `lines` and calls
  `slackAlertService.sendMessage(title, lines)`, instead join them into one
  plain-text message (`title` on its own line, then each line of `lines`),
  and persist it:

```java
Notification notification = new Notification();
notification.setMessage(message);
notification.setState(BaseConstant.NOTIFICATION_STATE_SENT);
notification.setNotificationGroup(activeGroup);
notificationRepository.save(notification);
```

### 3. `NotificationScheduler` (new — `log.monitor.api.scheduler`)

Mirrors fm-api's `NotificationScheduler` 1:1, minus the default-`Setting`
fallback and the `disable` branch:

```java
@Scheduled(fixedDelayString = "60000", zone = "UTC")
public void sendPendingNotifications() {
    Notification telegramNotification =
        notificationRepository.findFirstByNotificationGroupType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
    Notification slackNotification =
        notificationRepository.findFirstByNotificationGroupType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_SLACK);

    List<Notification> notifications = new ArrayList<>();
    if (telegramNotification != null) notifications.add(telegramNotification);
    if (slackNotification != null) notifications.add(slackNotification);
    if (notifications.isEmpty()) return;

    for (Notification notification : notifications) {
        NotificationGroup group = notification.getNotificationGroup();
        if (group == null) continue;
        SettingNotificationChannelDto setting =
            notificationService.parseChannelSetting(group.getChannelSetting());
        sendMessage(group.getType(), setting.getToken(), setting.getChannel(), notification.getMessage());
    }
    notificationRepository.deleteAll(notifications);
}

private void sendMessage(Integer type, String token, String channel, String message) {
    if (Objects.equals(type, BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM)) {
        telegramSendService.sendMessage(token, channel, message);
    } else if (Objects.equals(type, BaseConstant.NOTIFICATION_CHANNEL_TYPE_SLACK)) {
        slackService.sendMessage(token, channel, message);
    } else {
        log.error("Can not send message slack or telegram");
    }
}
```

### 4. Remove dead code

- Delete `SlackAlertService.java`.
- Remove `BaseConstant.SETTING_GROUP_NOTIFICATION` and
  `BaseConstant.SETTING_KEY_SLACK_ERROR_ALERT` (only consumer was
  `SlackAlertService`).

## Testing

- Unit test for the new `NotificationScheduler.sendPendingNotifications()`:
  routes to `TelegramSendService`/`SlackService` correctly per group type,
  deletes only the notifications it processed, no-ops when nothing pending.
- Update/verify `VictoriaLogsErrorAlertScheduler`'s existing test coverage
  (if any) for the new save-a-`Notification` behavior instead of the removed
  direct-Slack call.
