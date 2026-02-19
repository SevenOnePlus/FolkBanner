# FolkBanner

一个简单的 Android 壁纸下载应用。

## 功能特点

- 从多个来源浏览随机壁纸
- 无需存储权限即可下载壁纸到相册
- 支持多语言（英语、简体中文、繁体中文、日语）
- 支持深色主题
- 简洁直观的 Material Design 界面

## 下载

从 [Releases](https://github.com/matsuzaka-yuki/FolkBanner/releases) 下载最新版本 APK。

## 从源码构建

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK 34

### 构建步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/matsuzaka-yuki/FolkBanner.git
   ```

2. 在 Android Studio 中打开项目

3. 同步 Gradle 文件

4. 构建项目：
   ```bash
   ./gradlew assembleRelease
   ```

## 技术架构

- **编程语言**: Kotlin
- **架构模式**: MVVM (Model-View-ViewModel)
- **网络库**: Retrofit + OkHttp
- **图片加载**: Coil
- **UI 框架**: Material Design 3

## 权限说明

本应用只需要最少的权限：
- `INTERNET` - 用于从在线源下载壁纸

无需存储权限！壁纸使用 MediaStore API 保存。

## API 数据源

壁纸从各种在线源获取。配置信息请查看 `api.php` 和 `apis.txt`。

## 参与贡献

欢迎贡献！请随时提交 Pull Request。

## 开源协议

[在此添加协议信息]

## 作者

**Matsuzaka Yuki**

- GitHub: [@matsuzaka-yuki](https://github.com/matsuzaka-yuki)
