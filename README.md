# 追剧指南

一个面向 Android 的资源导航与播放客户端。首版使用 Kotlin、Jetpack Compose 和 Media3 构建。

## 已实现

- 内置合法示例资源与分类浏览
- 搜索、分类筛选、收藏和详情面板
- 自定义远程 JSON 数据源与本地缓存
- 启动时自动刷新远程数据，并每 12 小时后台更新
- 兼容 `resources`、`items`、`data`、`list` 等常见包装字段
- 本地 M3U/M3U8 播放列表导入
- HLS/MP4 等直接媒体地址播放
- 浅色/深色主题

当前内置 40 个资源入口，包括国内官方平台、国际免费服务、公共领域馆藏、新闻直播、IPTV 索引工具和开源播放器。

## 数据源格式

最小资源对象需要 `title` 和 `url`。应用也兼容 `name`、`link`、`homepage`、`description`、`status`、`risk`、`rating` 等常见字段。

```json
{
  "resources": [
    {
      "id": "example",
      "title": "示例资源",
      "description": "资源说明",
      "url": "https://example.com",
      "category": "官方电视",
      "status": "可访问",
      "risk": "低",
      "rating": 5,
      "last_checked": "2026-10-28",
      "tags": ["官方"]
    }
  ]
}
```

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

本项目只提供资源目录、状态展示和播放器能力，不提供影视文件。第三方资源需要单独判断合法性、隐私和安全性。发布前应补充隐私政策、投诉渠道、内容审核和资源来源说明。

产品方向参考：

- [laoma528/awesome-zhuiju-free](https://github.com/laoma528/awesome-zhuiju-free)，其目录内容采用 CC BY 4.0。
- 第三方链接、商标、海报、影视内容和直播流不属于该项目许可证授权范围。
