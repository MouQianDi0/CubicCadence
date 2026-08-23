# Cubic Cadence（方律）

> 在 Minecraft 中登录并播放网易云音乐的 Fabric 客户端模组。

简体中文 | [English](README.en.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-7F6DF2)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-blue.svg)](LICENSE)

Cubic Cadence（方律）是一款面向 Minecraft 26.2 的 Fabric 客户端模组。它让你不必切出游戏，即可扫码登录网易云音乐、浏览自己的歌单，并在游戏内完成在线流式播放或本地 MP3 播放，音乐通过 OpenAL 输出、与原版游戏声音并行，互不冲突。

## 特别声明

本模组通过与第三方后端服务 [api-enhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced) 通信，获取网易云音乐数据。

- **面向玩家**：作者已在服务器上部署好 `api-enhanced` 后端（`https://api.cubiccadence.top/`），该地址已作为模组内置默认值，安装后即可直接使用，无需自行搭建任何服务。
- **面向开发者**：如需自行开发、调试或部署，需要自行部署 `api-enhanced` 后端，并在 `config/cubic-cadence.json` 中通过 `apiEnhancedBaseUrl` 覆盖为你的服务地址。

第三方库地址：<https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced>

## 功能特性

- **扫码登录**：在游戏内生成登录二维码，使用网易云音乐 App 扫码授权；会话凭证经 Windows DPAPI 加密后保存到本地。
- **音乐库与歌单**：浏览“我创建的歌单”“我收藏的歌单”与“红心歌曲”，支持分页、按需加载与缓存优先的同步。
- **可播放性提示**：明确区分可播放、版权受限、会员不足、地区受限、音质不可用等状态。
- **在线流式播放**：边下边播，PCM 块队列上限 1 MiB，不缓存整首歌曲；支持断流保活与自动切歌保护。
- **下一首预加载**：播放当前歌曲时后台预打开下一首的音频流并预取歌词，自然切歌或手动下一首命中时直接复用，减少切换停顿。
- **本地 MP3 播放**：内置 JavaSound MP3 解码器，无需原生依赖即可解码为 PCM。
- **播放控制**：播放、暂停、停止、上一首、下一首、进度与音量调节。
- **播放模式**：顺序播放、单曲循环、列表循环、随机播放。
- **同步歌词 HUD**：普通模式在紧凑面板中显示当前与下一句；歌词模式隐藏封面、标题、进度、背景和边框，以纵向双语上下文突出当前歌词。
- **歌词字体定制**：歌词可切换原版/统一字符字体和常规/粗体，也可在游戏内选择本机 TTF；自定义字体通过隔离的本地资源包加载，缺字回退到 Unicode 字体。
- **可配置 HUD**：可整体关闭 HUD，也可分别选择封面、歌名、作者、进度、歌词、大小、颜色、九宫格位置及偏移；配置在重启游戏后保留。
- **音质选择**：标准、较高、极高；无损流式解码暂不支持。
- **独立音量**：可分别调节方律音量与原版音乐音量，并可禁用原版背景音乐。
- **封面与缓存**：远程封面解码与本地缓存。

## 技术栈

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.158.0+26.2 |
| Fabric Loom | 1.17-SNAPSHOT |
| Java | JDK 25 |
| Gradle | 9.7.0 |
| Mod ID | `cubic-cadence` |

## 快速开始

### 环境要求

- JDK 25
- Gradle（使用仓库自带的 Gradle Wrapper）

### 构建

```powershell
.\gradlew.bat build --no-daemon
```

构建产物位于 `build/libs/`。

### 运行

```powershell
.\gradlew.bat runClient --no-daemon
```

IDE 的配置与运行方式请参考 [Fabric 官方文档](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) 中与你所用 IDE 对应的章节。

## 网易云数据源

网易云音乐未向个人开发者开放多用户 Open API（个人账号仅限 `ncm-cli`），因此本项目使用第三方逆向库 `NeteaseCloudMusicApiEnhanced/api-enhanced` 作为数据源。

该库依赖网易云的非公开接口，并非官方授权。将其用于公开发布存在账号、服务条款与法律合规风险，接口也可能在不另行通知的情况下失效。这是个人开发者约束下的有意取舍，并非官方网易云集成。

## 配置

模组直接与 `api-enhanced` 服务通信，服务地址通过 `config/cubic-cadence.json` 中的 `apiEnhancedBaseUrl` 配置。

- **面向玩家**：模组内置了作者部署的服务器地址，安装后开箱即用，无需任何配置。

```json
{
  "apiEnhancedBaseUrl": "https://api.cubiccadence.top/"
}
```

- **面向开发者**：如需连接自托管的 `api-enhanced` 服务，请覆盖为你的服务地址，例如：

```json
{
  "apiEnhancedBaseUrl": "http://localhost:3000"
}
```

扫码登录后返回的网易云 Cookie 使用 Windows DPAPI 加密，保存在 `config/cubic-cadence/auth-session.dpapi`。

## 界面预览


### 音乐库主页

### ![音乐库主页](docs\screenshots\music-library.jpg)

### 扫码登录

![扫码登录](docs\screenshots\login-qr.jpg)

### 歌单详情

![歌单详情](docs\screenshots\playlist-detail.png)

### 设置界面

![设置界面](docs\screenshots\settings.png)

## 目录结构

```text
src/main/java/com/cubiccadence/      # 数据模型、Provider 接口与跨层契约
src/client/java/com/cubiccadence/    # 客户端实现（认证、播放、UI、缓存、同步）
src/client/resources/                # 客户端资源（语言文件、Mixin 配置）
docs/                                # 设计文档与开发文档
```

## 许可

本项目基于 [CC0-1.0](LICENSE) 许可发布，可自由学习与复用。
