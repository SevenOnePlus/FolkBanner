# FolkBanner

A simple wallpaper download application for Android.

## Features

- Browse random wallpapers from multiple sources
- Download wallpapers to gallery without storage permissions
- Support for multiple languages (English, Simplified Chinese, Traditional Chinese, Japanese)
- Dark theme support
- Clean and intuitive Material Design interface

## Download

Download the latest APK from [Releases](https://github.com/matsuzaka-yuki/FolkBanner/releases).

## Building from Source

### Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 34

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/matsuzaka-yuki/FolkBanner.git
   ```

2. Open the project in Android Studio

3. Sync project with Gradle files

4. Build the project:
   ```bash
   ./gradlew assembleRelease
   ```

## Architecture

- **Language**: Kotlin
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Network**: Retrofit + OkHttp
- **Image Loading**: Coil
- **UI**: Material Design 3

## Permissions

This app requires minimal permissions:
- `INTERNET` - For downloading wallpapers from online sources

No storage permissions are required! Wallpapers are saved using the MediaStore API.

## API Sources

Wallpapers are fetched from various online sources. See `api.php` and `apis.txt` for configuration.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

[Add license information here]

## Author

**Matsuzaka Yuki**

- GitHub: [@matsuzaka-yuki](https://github.com/matsuzaka-yuki)
