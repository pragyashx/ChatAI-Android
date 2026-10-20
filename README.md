# Chiti Code

Chiti Code is an Android AI programming assistant and code-verification lab. It uses OpenRouter for streaming chat and AI-generated test cases, Judge0 for sandboxed code execution, and Room for local history.

## Features

- Coding-focused AI chat experience
- Streaming responses through OpenRouter
- Conversation history with new, selected, and deleted chats
- Automatic conversation titles
- Local message persistence with Room
- Markdown, code, tables, links, and image rendering
- Code playground with standard input and execution output
- Python, Java, C, C++, JavaScript, and Kotlin execution
- AI-generated normal, boundary, and edge test cases
- Verified/Failed results based on real sandbox execution
- First failing counterexample with expected and actual output
- Recent code-attempt history stored locally
- Shared limit of 10 AI operations per calendar day
- 8,000-character limit for chat messages and playground source code
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
- Judge0 CE API
- DataStore Preferences
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

Judge0 CE does not require an API key. The public service is appropriate for a classroom demonstration but does not provide a production availability guarantee.

## Verification Lab

1. Open the app and tap the code icon in the chat header.
2. Select a programming language.
3. Describe what the program should do.
4. Enter source code and optional standard input.
5. Tap **Run once** to execute the code without using the AI allowance.
6. Tap **Generate & verify** to create five AI test cases and execute them in Judge0.
7. Review the verification badge, per-test results, and first failing counterexample.

Chat requests and AI test generation share a per-device allowance of 10 operations per calendar day. The allowance resets automatically according to the device's local date. Direct Judge0 sandbox runs do not consume it.

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

Run the local quality checks with:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

The generated APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

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
