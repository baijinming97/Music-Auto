# Music Auto

[English](README_en.md) · [下载 1.4.8 测试版](https://github.com/baijinming97/Music-Auto/releases/tag/v1.4.8) · [上游 AAMediaMate](https://github.com/gululu1235/AAMediaMate)

Music Auto 是基于 **gululu1235/AAMediaMate** 的社区分叉，改进 Spotify 与 Android Auto 的桥接、连接后续播，以及长歌词的两行显示。Spotify 负责实际播放，Music Auto 提供车机上的媒体控制和可选歌词。

**当前为 1.4.8 预发布版。** 自动续播和两行歌词已有实车验证；自动选中 Music Auto 的结果仍受手机、车机和 Android Auto 影响。此前出现过回到原生 Spotify，最近一次用户反馈启动即显示蓝色 MA、歌词和声音正常，尚不能保证所有设备或每次连接都成功。

本分叉由 baijinming97 维护，保留上游 Apache-2.0 许可证与项目归属；它不是上游官方发行版，也不隶属于 Spotify 或 Google。

## 本分叉的改动

- 默认锁定 Spotify 来源，即使启用了“忽略原生 Android Auto 应用”，也不会把 Spotify 一并过滤掉。可关闭锁定以恢复其他播放器桥接。
- 音乐信息与播放控制使用同一个媒体会话，排除两份桥接应用作为来源，避免循环桥接。
- Spotify 尚未建立会话时，车机列表仍提供启动入口，通过公开的媒体按键接收器尝试续播。
- 默认在 AA 连接、桥接就绪后续播一次；已经播放时跳过，手动暂停后不反复自动播放。可在设置中关闭。
- 通知监听服务与车机浏览服务共享会话，减少浏览服务重建导致的会话丢失。
- 长歌词优先分成上下两行，超长句停留后按歌词时间翻页，不做水平跑马灯；可关闭此选项。
- 精确检查当前安装的通知监听授权，并在手机主界面显示版本号。

详见 [CHANGELOG](CHANGELOG.md) 和 [测试记录](docs/VALIDATION.md)。

## 安装和开始使用

1. 从本仓库 [Releases](https://github.com/baijinming97/Music-Auto/releases) 下载 `Music-Auto-1.4.8.apk`。需要 Android 10+ 和可连接的 Android Auto 车机。
2. 手机保留 Spotify，登录后先确认能正常播放。Music Auto 不提供 Spotify 账号、音乐文件或订阅。
3. 安装并打开 Music Auto，为 **Music Auto 这一份安装**授权通知访问。默认开启 Spotify 来源锁定和 AA 连接后续播。
4. 如果 AA 中没有 Music Auto，在手机 AA 设置中点击版本号 10 次启用开发者模式，再在开发者设置中启用“未知来源”，然后重新连接。
5. 在车机中选择 Music Auto。需要歌词时，在手机上阅读并确认歌词提示，启用歌词并配置可用的歌词源。仅在停车或作为乘客时查看歌词。
6. 如果手机限制后台运行，可为 Music Auto 设置后台无限制及自动启动；各品牌的入口可能不同。

本次 APK 与已实车测试的本地 1.4.8 文件相同，是 **可调试的 debug 构建**，包名 `com.gululu.aamediamate.dev`，versionCode `22`，使用独立的 Android Debug 签名。原版 AAMediaMate 可并存。它不包含本地歌词库、备份、账号、API Key 或设备日志。

同签名的 Music Auto 升级可保留数据。若已有其他来源、同包名但不同签名的开发版，先在旧应用内导出备份；不要在未备份时卸载。不同签名不能直接覆盖安装，自行编译的 debug APK 也可能使用不同签名。

## 歌词与备份

延续上游的 LRCLib、Spotify via RapidAPI 和 LrcApi 支持。LRCLib 无需 API Key；其他服务如需 Key、地址或 Token，请使用自己的配置。歌词可用性取决于歌曲及服务。

两行布局按约 12 个汉字一行估算，已在一套 800 像素宽车机上验证。其他屏幕、字体或语言仍可能截断，可在显示设置中关闭“两行”选项。当前句后半段优先占用第二行，可能暂时替代下一句预览或歌曲信息。

已有 AAMediaMate 用户可通过应用内导出、导入功能迁移歌词和设置。公开反馈不要附带含 API Key 的配置、完整备份或未经检查的设备日志。

## 已知限制

- **锁定 Spotify 来源不等于锁定 AA 显示的播放器。** 1.4.8 没有永久夺取音频焦点，也不禁止 Spotify 获取焦点。自动切回原生 Spotify 的问题仍需更多重复启动和设备验证。
- 全屏地图底栏由 AA 控制，文字可能折叠成播放按钮。本应用不能保证在该栏持续显示歌词。
- 蓝牙自动续播、AA 自带自动播放及后台省电策略会影响启动顺序。关闭 AA 自动播放并不能单独保证选中 MA；Spotify 的“自动播放相似内容”也不是车机连接续播开关。
- 无声 PCM 实验只在需显式启用的设备测试代码中，**发布 APK 不运行该实验**。它曾改变系统媒体按键目标，但没有证明能解决 AA 界面自动接管。

## 从源码构建

需要 JDK 17、Android SDK Platform 36 和仓库自带的 Gradle Wrapper。设置 `ANDROID_HOME` 或本机 `local.properties` 的 SDK 路径后运行：

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug --max-workers=1
```

Windows 使用 `gradlew.bat`。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。源码不含签名私钥；CI 只构建和验证，不自动签发可覆盖现有安装的发布包。

本次分享对应 `debug` 变体；继承的 `release` 变体仍使用上游基础包名，不能视为本 APK 的直接升级路径。设备测试需单独启用并在停车时执行，见 [测试说明](docs/VALIDATION.md)。

## 贡献、归属与许可

欢迎在 [本分叉 Issues](https://github.com/baijinming97/Music-Auto/issues) 反馈相关版本、显示模式、预期行为和实际行为，请先删除敏感信息。

感谢 [gululu1235/AAMediaMate](https://github.com/gululu1235/AAMediaMate) 及原项目贡献者。原项目的桥接、歌词、备份和设置功能构成了本分叉的基础，上游历史保留在 Git 中。

沿用 [Apache License 2.0](LICENSE)，归属及修改说明见 [NOTICE](NOTICE)。本分叉是在 ChatGPT/Codex 辅助下实现和测试的。
