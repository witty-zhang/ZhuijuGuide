# 追剧指南

Android 影视客户端：自带播放器 + 可插拔数据源，推荐、搜索、详情、播放全程在 APP 内完成，不跳第三方网站。

## 现在的样子

1. 打开 APP 是「推荐」海报墙，内容来自所有已接入的数据源
2. 顶部搜索框跨所有数据源搜索，输入即搜（350ms 防抖）
3. 点海报进详情页：海报、简介、以及可播放的地址列表
4. 点播放 → Media3 原生播放器播放，不进入网页
5. 海报右上角心形收藏，收藏页可回看

## 订阅自己的源

设置 → 订阅源，粘贴 RSS / 播客的 feed 地址即可：

- APP 会读取 feed 里 `<item>` 的 `<enclosure>`、`media:content` 等媒体地址
- 解析标题、简介（自动去 HTML）、封面（`itunes:image` / `media:thumbnail`）、发布日期
- 音频和视频都支持，详情页会标出「音频 / 视频」，点开就是原生播放
- 订阅保存在本地，可随时在设置里移除；订阅内容和内置源一起出现在推荐和搜索里
- feed 有 5 分钟内存缓存，单个 feed 上限 16 MB


## 架构

```
数据源（SourceProvider）
   ├─ featured()  首页推荐
   ├─ search()    统一搜索
   └─ episodes()  取可播放地址
        ↓
   SourceRegistry（注册/聚合）
        ↓
   GuideViewModel（推荐、搜索、详情、收藏）
        ↓
   Compose UI（推荐页 / 详情页 / 播放器 / 收藏）
```

新增一个内容来源只需要：

1. 实现 `SourceProvider` 接口（`app/src/main/java/com/lzzhang/zhuijuguide/data/sources/`）
2. 在 `GuideViewModel.Factory` 里把实例加进 `SourceRegistry(listOf(...))`

页面、搜索、播放、收藏都不需要改。

## 内置示例源

- **NASA 影像库**：公共领域的官方航天影像，使用 `images-api.nasa.gov` 搜索接口和 `images-assets.nasa.gov` 的 MP4 直链。
- **RSS 订阅源**：用户在设置里添加的 feed，用 `RssFeedSource` 解析，不内置任何站点爬虫。

## 构建

环境要求：JDK 17–21、Android SDK Platform 35、Build Tools 35.0.0。

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 边界

- 应用只播放数据源返回的地址，不抓取、不解析、不代理第三方影视站内容。
- 添加数据源前请确认你有权访问和使用该内容。
- 站点导航与可用性检测（0.4.0 的版本）仍保留在代码里，当前版本未接入主流程。
