# 追剧指南

一个面向 Android 的在线影视资源导航客户端，用 Kotlin、Jetpack Compose 和 Media3 构建。

## 已实现

- 内置 46 个在线影视站点入口，打开即列，首页固定「全部」「在线影视」「直播源」「M3U / M3U8」四个分类
- 点开站点在 APP 内的网页播放页里完成搜索、选集和播放，不再跳到系统浏览器
- 可用 / 受限 / 失效状态全部由 APP 在本机检测，不使用上游目录里的旧时间戳
- 打开 APP 不做任何检测，只有点刷新按钮（首页右上角或设置页）才发起
- 检测结果按站点缓存，下次打开先显示上一次结果
- 搜索、收藏和详情面板
- 直播源订阅：点一个源就把清单下载下来展开成频道，TXT（`频道名,地址` + `分组名,#genre#`）与 M3U / M3U8 两种写法都认，UTF-8 / GBK 自动识别
- 本地 M3U / M3U8 播放列表导入，频道统一归在「M3U / M3U8」分类下，走 Media3 原生播放
- 同一个 APK 支持 Android TV / 电视盒子，按 `uiMode` 自动切换为电视界面（网格布局 + 遥控器焦点 + 左侧导航 + 强制深色）
- 浅色/深色主题

## 可用性检测

点首页右上角的刷新按钮或设置页的「立即检测」才会开始检测；打开 APP 不发起任何请求。
检测对本机每个站点发一个带 `Range: bytes=0-2048` 的 GET 请求，6 秒超时、最多 6 个并发，
UA 取 WebView 默认值，和 APP 内实际打开站点时一致。判定规则：

| HTTP 状态码 | 结果 |
| --- | --- |
| 200–399 | 可用 |
| 401 / 403 / 429 / 451 | 受限（站点在，但这台设备访问不到内容） |
| 其他状态码、超时、DNS 失败、连接重置 | 失效 |

检测时首页标题栏实时显示进度，结束后显示「可用 X · 受限 Y · 失效 Z · 待检测 W」。
结果写在 `files/availability_cache.json`，只保存状态和检测时间。

## 直播源

「直播源」分类里放的是订阅地址，点一个源会下载清单并展开成频道列表，频道只放在内存里，
不进收藏也不落盘，点一条直接进播放页。

- TXT：一行一条 `频道名,地址`，`分组名,#genre#` 切换分组
- M3U / M3U8：`#EXTINF` + 地址
- 编码先按 UTF-8 严格解码，失败退回 GB18030；中文域名先转 punycode 再发请求

内置的三个地址：

| 名称 | 地址 | 点开后 |
| --- | --- | --- |
| VMA | `https://pan.vma.cc/down.php/f83b92a24d61858666f13c5db938a292.txt` | 下载 TXT 清单，展开约 4000 路频道 |
| 饭太硬 | `http://www.饭太硬.cc/tv` | 实测会 302 回站点首页，属于导航站，在应用内网页打开 |
| 肥猫 | `http://肥猫.net/tv` | 返回 TVBox 配置（spider 接口），应用内打开是空白页，需要在 TVBox 客户端里导入这个地址 |

## 电视端

同一个 APK 同时支持手机和电视，启动时按 `uiMode` 自动选择界面，不需要单独打包。

| 判断依据 | 说明 |
| --- | --- |
| `uiMode & UI_MODE_TYPE_MASK == UI_MODE_TYPE_TELEVISION` | 主判断，Android TV / Google TV 都会设置 |
| `FEATURE_LEANBACK` 且没有 `FEATURE_TOUCHSCREEN` | 少数盒子不设 uiMode 时兜底 |

电视端与手机端的差异：

- 清单里声明 `LEANBACK_LAUNCHER` 和 320×180 的 `banner`，否则电视桌面根本不显示入口
- 首页从单列列表换成 3 列网格，字号整体上调（正文 16sp 起，标题 30sp）
- 所有可点区域带焦点描边 + 1.05 倍放大，遥控器能看清光标落在哪
- 导航从底部改为左侧竖栏，避免和网格的焦点路径互相抢
- 详情从底部弹层改为居中对话框（弹层在电视上会被裁到屏幕外）
- 播放页全屏，不显示标题栏；返回键交给 `BackHandler`
- 强制深色配色，窗口背景同步改黑（`values-television/themes.xml`），避免冷启动闪白

装到电视盒子：

```bash
adb connect <盒子IP>:5555      # 先在盒子上打开「网络调试」/「ADB 调试」
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

也可以把 APK 拷到 U 盘，用电视上的文件管理器直接安装。

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
