# DawnShift

Are you a student on vacation whose sleep schedule has "accidentally" drifted to waking up at 2 PM? Do you have an 8 AM class starting in two weeks and a sense of impending doom? Or do you have a long flight soon and want to prevent jet lag by slowly shifting your sleep schedule before your flight?

**DawnShift** is the app for you. It helps you progressively shift your wake-up time back to societal norms (or a new time zone), saving you from the shock of a cold-turkey reset. It's not just an alarm; it's a rehabilitation program for your circadian rhythm.

Designed with a focus on smooth aesthetics (because if you have to wake up early, it should at least look good) and reliable wake-up features. Built with Jetpack Compose and Material Design 3.

## Features

*   **Shift your sleep schedule**: Set a current wake up and target wake up time, a number of days to shift and the app will calculate the daily shift for you and wake you up at the calculated time every day.

## Future Features

*   **Snooze Tracker**: Track how often and for how long you've been delaying the inevitable.
*   **Hard Mode**: Prevent snoozing and have the alarm stay on for 30 minutes.
*   **Friends Accountability**: Share your progress with friends and get shamed into waking up on time.
*.  **iPhone Support**: self explanatory.


## How It Works

1. Set your current wake time (e.g., 11:00 AM)
2. Set your target wake time (e.g., 8:00 AM)
3. Choose how many days to transition (e.g., 14 days)
4. Dawn Shift calculates a gradual schedule
5. Alarms are automatically set each day

## Installation

**Option 1: Download APK**
You can simply download the latest `.apk` file from the [Releases](../../releases) page on GitHub and install it on your phone.

**Option 2: F-Droid (Coming Soon)**
I plan to submit DawnShift to F-Droid soon. Stay tuned!


## Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material3)
*   **Build System**: Gradle (Kotlin DSL)

## Development

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/gionstegmann/DawnShift.git
    ```
2.  **Open in Android Studio**:
    *   File > Open > Select the `DawnShift` folder.
3.  **Build and Run**:
    *   Wait for Gradle sync to complete.
    *   Press the green "Run" button (Shift+F10) to deploy to an emulator or physical device.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License

[MIT](LICENSE)
