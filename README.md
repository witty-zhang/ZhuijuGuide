# 追剧指南

一个面向 Android 的在线影视资源导航客户端，用 Kotlin、Jetpack Compose 和 Media3 构建。

## 已实现

- 内置 46 个在线影视站点入口，启动后直接列出，不需要先选分类
- 点开站点在 APP 内的网页播放页里完成搜索、选集和播放，不再跳到系统浏览器
- 可用 / 受限 / 失效状态全部由 APP 在本机检测，不使用上游目录里的旧时间戳
- 检测结果按站点缓存，下次启动先显示上一次结果，再在后台重新检测
- 搜索、分类筛选、收藏和详情面板
- 本地 M3U 播放列表导入，直播频道走 Media3 原生播放
- 浅色/深色主题

## 可用性检测

每次启动会在本机对每个站点发一个带 `Range: bytes=0-2048` 的 GET 请求，6 秒超时、最多 6 个并发，
UA 取 WebView 默认值，和 APP 内实际打开站点时一致。判定规则：

| HTTP 状态码 | 结果 |
| --- | --- |
| 200–399 | 可用 |
| 401 / 403 / 429 / 451 | 受限（站点在，但这台设备访问不到内容） |
| 其他状态码、超时、DNS 失败、连接重置 | 失效 |

首页标题栏实时显示「可用 X · 受限 Y · 失效 Z · 待检测 W」，设置页可以手动「立即检测」。
结果写在 `files/availability_cache.json`，只保存状态和检测时间。

## 构建

环境要求：

- JDK 17 或 21（Gradle JVM 需为 17–22）
- Android SDK Platform 35
- Android Build Tools 35.0.0

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 边界

本项目只提供目录、状态检测和播放能力，不提供影视文件，也不抓取或解析第三方站点里的视频地址。
第三方资源需要单独判断合法性、隐私和安全性。发布前应补充隐私政策、投诉渠道、内容审核和资源来源说明。

产品方向参考：

- [laoma528/awesome-zhuiju-free](https://github.com/laoma528/awesome-zhuiju-free)，其目录内容采用 CC BY 4.0。
- 第三方链接、商标、海报、影视内容和直播流不属于该项目许可证授权范围。
