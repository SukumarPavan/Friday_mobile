# Friday Mobile

Friday Mobile is an Android virtual assistant app that helps you perform daily tasks using voice commands. It leverages speech recognition and text-to-speech to provide a hands-free, interactive experience.

## Features

- **Voice Command Recognition:** Perform actions using natural language.
- **Make Calls:** Call contacts by name.
- **Send Messages:** Send SMS to contacts.
- **Set Alarms & Timers:** Quickly set alarms and timers.
- **Open Apps:** Launch installed apps by name.
- **Take Notes:** Save quick notes to your device.
- **Accessibility Actions:** Navigate your device (home, back, recents, swipe, scroll) using voice.
- **Weather, Music, and More:** Get weather updates, play music, and more (customizable via webhook).

## Screenshots

🚧🚧🚧

## Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- Android device or emulator (API 21+)
- Internet connection for webhook features

### Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SukumarPavan/Friday_mobile.git
   cd Friday_mobile
   ```

2. **Open in Android Studio:**
   - Open the project folder in Android Studio.
   - Let Gradle sync and download dependencies.

3. **Build and Run:**
   - Connect your Android device or start an emulator.
   - Click "Run" in Android Studio.

### Permissions

The app requires the following permissions:
- Microphone (for speech recognition)
- Contacts (for calling and messaging)
- Phone (for making calls)
- SMS (for sending messages)
- Storage (for saving notes)

Grant these permissions when prompted.

## Usage

- Tap the microphone or use the voice trigger to start.
- Speak commands like:
  - "Call Me"
  - "Send message to You!"
  - "Set alarm for 7:00 AM"
  - "Open WhatsApp"
  - "Note Buy groceries"

## Customization

- Webhook integration can be customized in `WebhookUtil`.
- Add or modify supported commands in `FridayTasksActivity.kt`.

## Configuration: Webhook URL

To use your own n8n webhook, set the URL in your `local.properties` file (do not commit this file):

```
N8N_WEBHOOK_URL=https://your-n8n-instance.com/webhook/your-webhook-id
```

**Warning:** Never commit your real webhook URL or secrets to a public repository.

## Contributing

Contributions are welcome! Please open issues or submit pull requests for improvements.

## License

This project is licensed under the MIT License.

---

**Developed by Sukumar Pavan**

---

## GitHub Star Graph

[![Stargazers over time](https://starchart.cc/SukumarPavan/Friday_mobile.svg)](https://starchart.cc/SukumarPavan/Friday_mobile)
