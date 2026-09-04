# Hướng dẫn tạo Channel thông báo cho Slack và Telegram

**(Dùng cho hệ thống Log Monitor API — cảnh báo lỗi qua VictoriaLogsErrorAlertScheduler / NotificationScheduler)**

Hệ thống hỗ trợ 2 kênh gửi thông báo, lưu trong bảng `notification_group`:

| type | Kênh | Ghi chú |
|:-----|:-----|:--------|
| **0** | Telegram | Gửi qua Telegram Bot API |
| **1** | Slack | Gửi qua Slack Web API (`chat.postMessage`) |

Cấu hình kết nối được lưu ở field `channelSetting` (JSON), dạng:

```json
{
  "type": 1,
  "channel": "#alerts",
  "token": "xoxb-xxxxxxxxxxxx-xxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxx",
  "username": "Log Monitor Bot"
}
```

- `type`: 0 = Telegram, 1 = Slack
- `channel`: với Slack là tên/ID channel (`#alerts` hoặc `C0123456789`); với Telegram là `chat_id`
- `token`: Bot Token (Slack Bot Token hoặc Telegram Bot Token)
- `username`: tên hiển thị (tuỳ chọn)

## Phần 1 - Tạo channel Slack

### Bước 1: Đăng nhập Slack và tạo Workspace

1. Truy cập [https://slack.com/intl/fr-vn/](https://slack.com/intl/fr-vn/) chọn **To Start up** để đăng nhập
2. Đăng nhập với Google/ Apple account
3. Tạo Work space mới
4. Nhập tên cho workspace
5. Nhập display name của mình
6. Thêm đồng nghiệp vào workspace (có thể chọn "Ignore this step" để bỏ qua bước này)

### Bước 2: Tạo Slack App (Bot)

1. Truy cập [https://api.slack.com/apps](https://api.slack.com/apps) → **Create New App**.
2. Chọn **Blank app** để cấu hình từ đầu (hoặc chọn **From a manifest** tạo từ Json/config)
3. Đặt tên app (VD: Log Monitor Bot), chọn Workspace muốn cài đặt → **Create App**.

### Bước 3: Cấp quyền (OAuth Scopes) cho Bot

1. Vào menu **OAuth & Permissions** (cột trái).
2. Kéo xuống **Scopes → Bot Token Scopes**, bấm **Add an OAuth Scope**, thêm tối thiểu:
   - `chat:write` — cho phép bot gửi tin nhắn
   - `channels:read` — đọc danh sách channel công khai (nếu cần lấy channel ID)
   - `groups:read` — đọc channel private (nếu channel là private)
3. Kéo lên đầu trang mục **OAuth Tokens**, bấm **Install to Workspace** → **Chọn work space** → **Allow**.
4. Sau khi cài đặt, ở mục **OAuth Tokens** copy **Bot User OAuth Token** (bắt đầu bằng `xoxb-...`). Đây là giá trị điền vào field `token`.

### Bước 4: Tạo channel Slack và thêm Bot vào channel

1. Trong Workspace, bấm dấu **+** cạnh mục "**Channel**" → **Create a channel**.
2. Đặt tên channel, VD: `alerts` hoặc `log-monitor-alerts`.
3. Chọn Public hoặc Private tuỳ nhu cầu → **Create**
4. Thêm Bot với tên vừa tạo
   *Hoặc: **Channel details → Member → Add people or Apps** → nhập tên bot vừa tạo.*

### Bước 5: Lấy Channel ID

1. Right click vào **tên channel** → **Channel details → View Channel Details**
2. Cuộn xuống cuối popup → copy **Channel ID** (dạng `C0123456789`).

### Bước 6: Test gửi tin nhắn (tuỳ chọn, để xác minh trước khi cấu hình hệ thống)

```bash
curl -X POST "https://slack.com/api/chat.postMessage" \
  -H "Authorization: Bearer xoxb-xxxxxxxxxxxx-xxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "C0123456789",
    "text": "Test message from Log Monitor"
  }'
```

**Với**:
- `channel`: Id của channel
- `Bearer ...`: Token của bot

Nếu thành công, response trả về `"ok": true`.

### Bước 7: Cấu hình vào hệ thống

Tạo/sửa NotificationGroup với:

```json
{
  "name": "New noti group test",
  "description": "New noti group test",
  "channelSetting": "{\"type\":1,\"channel\":\"CXXXXXXXXXX\",\"token\":\"xoxb-xxxxxxxxxxxxxxxxxxxx\"}"
}
```

## Phần 2 — Tạo channel/group Telegram

### Bước 1: Tạo Bot Telegram qua BotFather

1. Mở Telegram, tìm và chat với **@BotFather**.
2. Gõ lệnh `/newbot`.
3. Đặt tên hiển thị cho bot (VD: Log Monitor Bot).
4. Đặt username cho bot, phải kết thúc bằng "bot" (VD: `log_monitor_alert_bot`).
5. BotFather trả về **HTTP API Token**, dạng: `123456789:AAExxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   → Đây là giá trị điền vào field `token`.

### Bước 2: Tạo Channel Telegram để nhận thông báo

1. Mở Menu button
2. Bấm **New Channel**.
3. Đặt tên, mô tả → chọn Public hoặc Private → **Create**.

### Bước 3: Thêm Bot vào Group/Channel

1. Ở channel vào **View Channel info → Chọn Subscribers/Admin → Add users
2. Tìm theo username bot đã tạo (**VD: @log_monitor_alert_bot**) → thêm vào.
3. Bắt buộc cấp quyền **Admin** cho bot (bắt buộc có quyền "**Post Messages**" ở mục **Manage messages**) thì bot mới gửi được tin nhắn.

### Bước 4: Lấy Chat ID

**Cách 1 — Dùng API getUpdates:**

1. Gửi 1 tin nhắn bất kỳ vào group/channel đã thêm bot.
2. Mở trình duyệt, truy cập: `https://api.telegram.org/bot<TOKEN>/getUpdates`
3. Lấy id của channel: `my_chat_member -> chat -> id` — `{"id": -100xxxxxxxxxx, ...}` trong JSON trả về.
   - Group thường có `chat_id` âm (VD: `-123456789`).
   - Channel/Supergroup thường có dạng `-100xxxxxxxxxx`.

### Bước 5: Test gửi tin nhắn

```bash
curl -X POST "https://api.telegram.org/bot<BOT_TOKEN>/sendMessage" \
  -H "Content-Type: application/json" \
  -d '{
    "chat_id": "<CHAT_ID>",
    "text": "Test message from Log Monitor"
  }'
```

Nếu thành công, response trả về `"ok": true`.

### Bước 6: Cấu hình vào hệ thống

Tạo/sửa NotificationGroup với:

```json
{
  "name": "Team Alerts Telegram",
  "description": "Send error alerts via Telegram",
  "type": 0,
  "channelSetting": "{\"type\":0,\"token\":\"<TELEGRAM_BOT_TOKEN>\",\"channel\":\"<TELEGRAM_CHAT_ID>\",\"username\":\"<TELEGRAM_BOT_USERNAME>\"}"
}
```
