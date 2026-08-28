package log.monitor.api.scheduler;

import log.monitor.api.constant.BaseConstant;
import log.monitor.api.dto.setting.SettingNotificationChannelDto;
import log.monitor.api.model.Notification;
import log.monitor.api.model.NotificationGroup;
import log.monitor.api.repository.NotificationRepository;
import log.monitor.api.service.NotificationService;
import log.monitor.api.service.SlackService;
import log.monitor.api.service.TelegramSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class NotificationScheduler {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TelegramSendService telegramSendService;

    @Autowired
    private SlackService slackService;

    @Scheduled(fixedDelayString = "60000", zone = "UTC")
    public void sendPendingNotifications() {
        Notification telegramNotification = notificationRepository
                .findFirstByNotificationGroupType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_TELEGRAM);
        Notification slackNotification = notificationRepository
                .findFirstByNotificationGroupType(BaseConstant.NOTIFICATION_CHANNEL_TYPE_SLACK);

        List<Notification> notifications = new ArrayList<>();
        if (telegramNotification != null) notifications.add(telegramNotification);
        if (slackNotification != null) notifications.add(slackNotification);
        if (notifications.isEmpty()) return;

        for (Notification notification : notifications) {
            NotificationGroup group = notification.getNotificationGroup();
            if (group == null) continue;
            SettingNotificationChannelDto setting = notificationService.parseChannelSetting(group.getChannelSetting());
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
}
