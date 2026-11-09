Music Auto 是基于 [gululu1235/AAMediaMate](https://github.com/gululu1235/AAMediaMate) 的社区分叉，本次分享已实车测试的 1.4.8，保留 Apache-2.0 许可和上游归属。

主要改进：Spotify 来源锁定、连接 AA 后续播、媒体会话保留、长歌词两行与延迟翻页，以及通知权限识别。

**这是预发布测试版，APK 是可调试的 debug 构建。** 包名 `com.gululu.aamediamate.dev`，versionCode `22`，Android 10+。签名与上游官方版不同，迁移前请导出歌词和设置备份。

139 个本地单元测试通过，lint 无错误。两行歌词已有实车确认；最近一次反馈启动即为蓝色 MA、歌词和声音正常，但此前重连仍出现原生 Spotify，不能保证所有设备或每次启动都自动选中 MA。全屏地图底栏折叠仍由 AA 控制。无声 PCM 实验不在发布 APK 的正常运行路径中。

下载 `Music-Auto-1.4.8.apk`，说明见 [README](https://github.com/baijinming97/Music-Auto/blob/main/README.md) 和 [测试记录](https://github.com/baijinming97/Music-Auto/blob/main/docs/VALIDATION.md)。仅在停车或作为乘客时查看歌词。

APK SHA-256:

```text
dc81056f0b2f790a27a1df50bb0f37fb5e39a1216536d08ac649d796d68e5c95
```

English: Community prerelease of the Spotify-focused AAMediaMate fork, with source locking, AA auto-resume, retained sessions, and two-row lyrics. The APK is the previously car-tested **debuggable debug build**, package `com.gululu.aamediamate.dev`, version 1.4.8 (22), Android 10+. 139 local unit tests passed and lint reported no errors. Automatic MA selection remains device-dependent despite the latest successful report; persistent Maps-strip lyrics are not guaranteed. No production silent-PCM or focus-blocking behavior is included. Back up data before migration and view lyrics only while parked or as a passenger.
