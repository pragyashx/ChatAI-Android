# Chiti Code

Chiti Code is an Android AI assistant designed for programming and software-development conversations. It uses OpenRouter to provide streaming responses from an AI model and stores conversations locally for easy access later.

## Features

- Coding-focused AI chat experience
- Streaming responses through OpenRouter
- Conversation history with new, selected, and deleted chats
- Automatic conversation titles
- Local message persistence with Room
- Markdown, code, tables, links, and image rendering
- Jetpack Compose and Material 3 user interface
- Support for Android 8.0 and newer

## Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK 34
- Room Database
- OkHttp and Server-Sent Events
- Kotlin Coroutines
- OpenRouter API
- Gradle 8.5

## Requirements

- JDK 17
- Android SDK 34
- Android Studio with Android SDK and emulator/device support
- An OpenRouter API key

## Setup

1. Clone the repository and open it in Android Studio.
2. Create or update the root `local.properties` file with your OpenRouter API key:

   ```properties
   OPENROUTER_API_KEY=your_api_key_here
   ```

3. Sync the project with Gradle.

Do not commit `local.properties` or expose your API key in source control.

## Build and Run

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

`installDebug` requires a connected Android device or running emulator.

## Project Details

- Application ID: `com.chatai.app`
- Current version: `1.4.0`
- Minimum Android version: API 26 (Android 8.0)
- Target Android version: API 34

## Contributors

- [Pragya Sharma](https://github.com/pragyashx)
- [Tanisha Jha](https://github.com/tanishajha7)

## License

No project license has been declared yet.
