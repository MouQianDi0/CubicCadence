# Changelog

## 2026-08-24 01:00:19 - 修复问题（远程歌单连接与自定义字体缺字方框）

- **变更概述**：复核开发运行配置已指向可用的远程 `api-enhanced` 服务；修复自定义 TTF 资源路径被 Minecraft 重复添加 `font/` 后无法加载的问题，并对自定义字体不包含的字符实施显式原版字体回退，避免中文歌词显示方框。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/font/CustomLyricFontManager.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingHudRenderer.java`
  - `src/test/java/com/cubiccadence/client/font/CustomLyricFontManagerTest.java`
  - `src/test/java/com/cubiccadence/client/ui/hud/NowPlayingHudRendererTest.java`
  - `run/resourcepacks/cubic-cadence-custom-font/assets/cubic-cadence/font/custom.json`（本地生成资源包）
  - `CHANGELOG.md`
- **变更内容**：
  - 将字体 Provider 的文件标识从会被解析为 `font/font/custom.ttf` 的 `cubic-cadence:font/custom.ttf` 修正为 `cubic-cadence:custom.ttf`；同步修复当前开发运行目录中已生成的字体包，使现有字体可在下次资源加载时直接生效；
  - 在 TTF 校验阶段保留 Java 字体对象并缓存其真实字形覆盖范围；歌词文本按 Unicode 码点合并为连续字体段，自定义字体支持的字符继续使用自定义字体，不支持的中文、标点、表情或其他字符明确改用 Minecraft 原版字体；
  - 保留字体 JSON 中的 Unicode Provider 引用作为第二层回退；字体粗细、颜色、换行和歌词模式布局继续作用于混合字体组件，不修改普通 HUD 偏好；
  - 字体包测试不仅解码 JSON，还按 Minecraft 26.2 的 `font/` 自动前缀规则解析并检查真实资源文件；新增英文、自定义字体缺失的中文和 Unicode 代理对分段测试；
  - 已确认 `run/config/cubic-cadence.json` 当前为 `https://cub.cubiccadence.top/`，远程首页和 `/user/playlist` 均返回 HTTP 200，因此无需再次改写该配置。
- **风险**：低至中风险。混合字体的字宽和基线可能存在轻微差异，但宽度、截断与换行均基于最终组件重新计算；极少数字体可能声明某字符可用但自身字形就是方框，这属于字体文件内容，无法由缺字检测识别。资源热重载和实际歌词观感仍需游戏内人工验收。
- **验证结果**：字体管理器与 HUD 渲染器定向测试通过；首次全量测试因 Windows 临时目录清理发生一次 `TempDirDeletionException`，对应 `MusicLibraryManagerTest` 单独复跑通过，随后 `\.\gradlew.bat build --no-daemon` 完整构建成功；19 个测试套件共 85 个测试全部通过，中英文语言 JSON 均可解析、178 个键完全一致，`git diff --check` 无补丁格式错误。真实 TTF 热加载、混合中英文歌词观感与远程账号歌单仍需游戏内人工验收。

## 2026-08-24 00:18:31 - 新增功能（透明双语歌词模式与自定义 TTF 字体）

- **变更概述**：新增独立“歌词模式”。开启后不改写普通 HUD 偏好，而是在渲染层隐藏封面、歌名、作者、进度、背景和边框，只显示播放器式纵向双语歌词；同时增加原版/统一字符/自定义字体与常规/粗体选择，并支持在游戏内通过系统文件窗口选择本机 TTF、即时重载。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/config/HudLyricFont.java`
  - `src/client/java/com/cubiccadence/client/config/HudLyricWeight.java`
  - `src/client/java/com/cubiccadence/client/config/HudSettings.java`
  - `src/client/java/com/cubiccadence/client/config/ModConfig.java`
  - `src/client/java/com/cubiccadence/client/font/CustomLyricFontManager.java`
  - `src/main/java/com/cubiccadence/model/SyncedLyrics.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingSnapshot.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/PlayerNowPlayingSource.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingHudRenderer.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingHudElement.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/HudSettingsScreen.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - `src/test/java/com/cubiccadence/client/config/HudSettingsTest.java`
  - `src/test/java/com/cubiccadence/client/font/CustomLyricFontManagerTest.java`
  - `src/test/java/com/cubiccadence/model/SyncedLyricsTest.java`
  - `README.md`、`README.en.md`、`docs/design.md`、`CHANGELOG.md`
- **变更内容**：
  - `NowPlayingSnapshot` 从两段纯文本扩展为完整不可变 `LyricLine` 列表和当前行索引，保留 LRC 解析阶段已对齐的翻译；歌词模式最多展示前后五组原文/翻译，当前组完整高亮，其余组保持同一 RGB、仅降低透明度和相对字号，视口不足时优先裁减距离当前句最远的行；
  - 歌词模式在渲染入口独立分支并强制透明，不修改封面、标题、进度或普通背景配置；关闭后立即恢复原 HUD。旧配置缺少新增字段时歌词模式默认关闭、字体为原版、字重为常规；
  - HUD 设置新增“歌词”标签页、模式开关、普通 HUD 歌词开关、字体和粗细切换、系统 TTF 选择按钮、加载状态及五组双语实时预览；歌词字号和 RGB 继续复用现有尺寸/外观控件；
  - `CustomLyricFontManager` 使用 Minecraft 已附带的 LWJGL Tiny File Dialog，在后台选择和校验单个 `.ttf`；限制 64 MiB，检查 SFNT 文件头并执行 Java TrueType 解析，只把文件名和字体内容写入固定的本地专用资源包，不记录完整来源路径；
  - 自定义资源包只定义 `cubic-cadence:custom` 并追加 Unicode 缺字回退，不覆盖菜单、聊天或其他 HUD 字体；`pack.mcmeta` 按当前 26.2 的资源包 major/minor 同时写入 `pack_format`、`min_format` 和 `max_format`；资源包刷新、启用和重载失败时恢复旧字体文件及旧资源包选择，取消或无效文件不改变当前字体；
  - 新增配置默认/回退、歌词二分索引、无效 TTF 拒绝、字体包定义、Minecraft 原生 Provider/资源包元数据解码、旧字体回滚及来源文件名测试，并同步中英文说明和设计文档。
- **风险**：中等风险，主要集中在客户端字体资源重载和透明歌词布局。任意 TTF 的字面高度、覆盖字符及本身字重不同，可能造成换行或视觉差异；缺字会回退 Unicode，过高布局会按视口裁减。自动化测试不能证明操作系统文件窗口、真实 TTF 热重载及游戏内不同 GUI 缩放的最终观感，仍需人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，18 个测试套件共 83 个测试全部通过；中英文语言 JSON 均可解析且 178 个键完全一致；`git diff --check` 只有 Windows LF→CRLF 提示、没有补丁格式错误。操作系统文件窗口、真实 TTF 热重载和不同 GUI 缩放下的视觉效果仍需在游戏内人工验收。

## 2026-08-21 23:49:53 - 优化代码（HUD 预览与游戏窗口保持相同比例）

- **变更概述**：根据实际使用反馈，将 HUD 设置页左侧预览屏幕改为与当前游戏窗口严格保持相同宽高比，避免铺满高窄区域后造成画面和 HUD 位置观感失真。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/ui/screen/HudSettingsScreen.java`
- **变更内容**：
  - 在扩展后的完整左侧区域内，以当前游戏窗口 `width : height` 计算可容纳的最大预览矩形；使用统一缩放系数，禁止横向或纵向独立拉伸；
  - 预览边框只包围实际游戏画面，无法匹配比例的剩余区域不再绘制为空预览容器；
  - HUD 继续以完整游戏窗口作为逻辑视口，再与游戏画面使用同一个缩放系数绘制，确保九宫格锚点、偏移方向、相对尺寸和实际游戏一致。
- **风险**：低风险，仅调整设置页预览尺寸与坐标换算；当左侧可用区域和游戏窗口比例不一致时会自然出现少量上下或左右留白，这是保持比例所必需的。真实 HUD 渲染逻辑和配置数据未改动。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，全部测试通过。

## 2026-08-21 23:43:31 - 新增功能（HUD 全量自定义与满幅实时预览）

- **变更概述**：为正在播放 HUD 增加内容显示、整体尺寸、标题大小、歌词大小、歌词颜色、背景和屏幕位置自定义；设置页重做为左侧实时预览、右侧分组调节，并根据实际截图移除居中限宽和内部小预览框，让预览画布铺满左侧剩余空间，仅保留屏幕边距及与控制区的间隔。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/config/HudPosition.java`
  - `src/client/java/com/cubiccadence/client/config/HudSettings.java`
  - `src/client/java/com/cubiccadence/client/config/ModConfig.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingHudRenderer.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingHudElement.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/HudSettingsScreen.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - `src/test/java/com/cubiccadence/client/config/HudSettingsTest.java`
- **变更内容**：
  - 新增 50%–200% HUD 整体、标题和歌词缩放，RGB 歌词颜色、背景开关，以及九宫格锚点 + X/Y 像素偏移；所有数值在配置边界统一限制范围，旧配置缺少新字段时继续使用与原 HUD 一致的默认值；
  - 抽取真实 HUD 与设置预览共用的渲染器，统一文字截断、缩放后尺寸测量、当前/下一句歌词配色、封面占位、边界限制和锚点坐标，避免预览与实际效果使用两套算法；
  - 设置页右侧按“显示 / 尺寸 / 外观 / 位置”四个标签分组，提供恢复默认和完成按钮；编辑期间只维护临时设置，关闭页面时一次性保存，避免拖动滑块时高频写配置文件；
  - 左侧预览从屏幕左侧边距延伸至右侧控制区，底部延伸至屏幕边距；预览场景直接填满画布，HUD 以可读尺寸实时响应开关、缩放、颜色、锚点和偏移，不再缩在大空框内；
  - 新增默认值、非法缩放/颜色/偏移归一化、九宫格定位及越界限制测试，并补齐中英文界面文案。
- **风险**：中低风险，改动集中在客户端 HUD 配置、渲染和设置界面；预览画布会随可用区域改变宽高比，因此用于直观展示位置和大小，不承诺与所有游戏窗口比例逐像素一致。真实 HUD 和预览共用同一渲染器，位置和尺寸逻辑保持一致；不同 GUI 缩放、超宽/窄窗口下的最终观感仍需游戏内人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，全部测试通过（含新增 HUD 配置与定位测试）；中英文语言 JSON 已完成解析校验。

## 2026-08-21 23:01:51 - 修复问题（在线音频超时后按原位置无缝续播）

- **变更概述**：修复在线音乐播放过程中网络响应停止传输后，播放器等待 10 秒便直接进入 `ERROR` 并显示“播放失败，请查看日志”的问题。根因是原有 HTTP 请求超时只约束建连/响应阶段，已经建立的响应体读取可能长期阻塞；PCM 消费端只能填充短暂静音，超过饥饿阈值后直接失败，且没有能够保留 MP3 解码状态和原始字节位置的重连机制。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/ResumableHttpInputStream.java`
  - `src/client/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStream.java`
  - `src/test/java/com/cubiccadence/client/playback/ResumableHttpInputStreamTest.java`
  - `src/test/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStreamTest.java`
- **变更内容**：
  - 新增 `ResumableHttpInputStream`，精确累计已经交给 MP3 解码器的原始字节数；连接异常或响应体停滞时使用 `Range: bytes=<offset>-` 从下一未交付字节重连，保持同一个 JavaSound 解码器、OpenAL 流和播放时钟，不重新播放歌曲；
  - 续传请求严格要求 HTTP `206` 且 `Content-Range` 起点与累计字节位置完全一致；CDN 返回整文件 `200`、缺失/错误范围或三次重连均失败时安全终止，避免把重复或错位的 MP3 数据拼入当前流；
  - 无 PCM 数据的饥饿阈值由 10 秒延长为 15 秒，首次连接之外最多自动重连 3 次；请求响应超时由 20 秒延长为 30 秒；恢复期间维持 `BUFFERING`，数据重新到达后继续原曲位置；
  - 网络响应体关闭仍投递到既有清理线程，避免在 Minecraft 声音线程执行阻塞 I/O；PCM 队列继续保持 `16 × 64 KiB = 1 MiB` 硬上限，不缓存或重新下载整首歌曲；
  - 新增断线后精确字节续接、主动停滞重连、三次上限、CDN 忽略 Range、`Content-Range` 错位及确认参数等测试。
- **风险**：中等风险，改动位于在线 MP3 网络输入层。无缝续播依赖 CDN 支持标准 HTTP Range；若 CDN 不支持或临时 URL 已失效，播放器会在三次安全重连失败后保留明确错误，不会从头播放冒充续播。自动化测试覆盖字节连续性和失败边界，但真实 CDN 的 Range 响应、听感连续性及网络中断恢复仍需在游戏内使用真实账号人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，73 个测试全部通过；首次全量测试曾遇到一个 Windows `@TempDir` 删除竞态，随后完整构建复跑通过，确认与本次播放修改无关。

## 2026-08-21 17:46:10 - 修复问题（ESC 暂停后进度条空跑并最终播放失败）

- **变更概述**：修复按 ESC 打开游戏菜单/设置后，音乐被原版暂停但 HUD 进度条继续推进到 100% 并最终弹出“播放失败”的问题。根因是原版暂停只走 `SoundEngine.pauseAllExcept(...)` 停掉 OpenAL 通道，不会经过本 Mod 的 `AudioEngine.pause()`，导致播放挂钟与流式生产线程全程无感知：挂钟继续外推使进度条空跑，生产线程持续填满有界队列后阻塞、网络连接长时间空闲被 CDN 掐断，流进入 `FAILED` 后被误判为播放失败。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStream.java`
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `src/test/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStreamTest.java`
- **变更内容**：
  - `JavaSoundStreamingAudioStream` 新增 `pause()`/`resume()`，基于锁 + 条件变量让生产线程在游戏暂停期间挂起、停止解码与读取网络，恢复后继续；`close()` 唤醒被挂起的生产线程，避免暂停中关闭造成死锁；
  - `AudioEngine` 新增游戏暂停感知：`tick()` 内同步 `Minecraft.getInstance().isPaused()`，暂停沿记录 `gamePausedAtNanos` 并 `stream.pause()`，恢复沿把暂停时长计入 `totalPausedNanos` 并 `stream.resume()`；游戏暂停期间 `tick()` 提前返回，跳过“通道失活→ERROR/ENDED”与缓冲/饥饿判定，避免误报失败；`getPositionMs()` 在游戏暂停时冻结挂钟，进度条立即停止；`resetClock()` 重置游戏暂停态，避免切歌/停止后残留；
  - 新增 `pauseStopsTheProducerAndResumeContinuesIt`、`closeWhilePausedDoesNotDeadlock` 两个流测试，用计数输入流 + 消费者线程精确验证暂停时停止读输入、恢复后继续读，以及暂停中 `close()` 不死锁。
- **风险**：中低风险，改动集中在流对象状态机与 `AudioEngine` 的暂停联动。已回归：正常播放、手动暂停/恢复、流 starving 自动缓冲、seek 后暂停均不经过新增游戏暂停分支；`gamePausedAtNanos` 与手动暂停的 `pausedAtNanos` 相互独立、按各自窗口累计，不重复计数。生产线程暂停期间网络连接仍会空闲，超长暂停（超过 CDN 空闲超时）理论上仍可能触发流失败，但普通时长的 ESC 暂停不再复现；该极端场景留作后续可选的“恢复时按当前位置重建流”加固。真实进度/歌词同步与失败边界仍需游戏内使用真实账号人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，全部测试通过（含新增 2 个流测试）。

## 2026-08-21 17:17:42 - 修复问题（暂停后进度条继续推进）

- **变更概述**：修复点击暂停后声音已停但 HUD 进度条仍继续外推到 100% 的问题。根因是暂停只在 `PLAYING` 状态下真正冻结挂钟，`BUFFERING`（流 starving 自动缓冲）阶段点暂停只改了状态标签、没有停下底层时钟，且 `PlayerController.tick()` 会把 `PAUSED` 回写覆盖成 `BUFFERING`/`PLAYING`，导致进度条在无声状态下空跑。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `src/client/java/com/cubiccadence/client/playback/PlayerController.java`
  - `src/test/java/com/cubiccadence/client/playback/PlayerControllerTest.java`
- **变更内容**：
  - `AudioEngine.pause()` 门槛从“仅 `PLAYING`”放宽到“`PLAYING` 或 `BUFFERING`”，并在已存在 `pausedAtNanos`（流 starving 自动暂停已记下冻结点）时不覆盖该值；先把 `state` 置为 `PAUSED`，通道存在时才执行 `Channel::pause`，保留 `tick()` 每帧重发 `pause` 的兜底逻辑；
  - `AudioEngine.resume()` 去掉 `handle == null` 的提前返回，使“时钟已停但通道暂未映射”的场景也能正确解冻并把暂停时长计入 `totalPausedNanos`；
  - `PlayerController.tick()` 在 `state == PAUSED` 时不跟随引擎状态回写，避免 `PAUSED` 被 `BUFFERING`/`PLAYING` 覆盖，直到 `resume()`、停止、出错或结束才离开 `PAUSED`；
  - 新增 `PlayerControllerTest.pausedStateIsNotClobberedByBufferingEngineState` 用例，验证 `PAUSED` 不被 `BUFFERING` 引擎态覆盖的不变量。
- **风险**：改动集中在客户端播放路径，属低风险；正常 `PLAYING` 暂停/恢复、流 starving 自动缓冲与恢复、`seek` 后暂停的分支均未改动。`RESOLVING` 阶段及通道安装前阶段的暂停当前 UI 不可达（暂停按钮在 `BUFFERING`/`RESOLVING` 时为 inactive），本次未做“待暂停”加固，保留为后续可选加固项。真实声音/进度/歌词同步仍需游戏内使用真实账号人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，全部测试通过（含新增用例）。

## 2026-08-21 16:34:17 - 新增功能（下一首音频与歌词预加载）

- **变更概述**：当前歌曲播放期间，后台提前解析并预打开下一首的在线音频流，同时预取下一首歌词；自然切歌或手动下一首命中预加载时直接复用，减少切歌停顿，实现接近无缝衔接。预加载是纯增强，失败时静默回退到原有实时加载，不改动既有失败、重试、迟到响应语义。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/PlaybackQueue.java`
  - `src/client/java/com/cubiccadence/client/playback/PlaybackEngine.java`
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `src/client/java/com/cubiccadence/client/playback/PlayerController.java`
  - `src/client/java/com/cubiccadence/client/lyrics/LyricsManager.java`
  - `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - `src/test/java/com/cubiccadence/client/playback/PlayerControllerTest.java`
  - `src/test/java/com/cubiccadence/client/playback/PlaybackQueueTest.java`
  - `src/test/java/com/cubiccadence/client/lyrics/LyricsManagerTest.java`
  - `docs/design.md`、`README.md`、`README.en.md`、`CHANGELOG.md`
- **变更内容**：
  - `PlaybackQueue` 新增 `peekNextAfterEnd()`，以非破坏方式返回自然结束后要播放的下一首；洗牌回绕重排无法不消耗随机数预测时返回空，回退惰性加载；
  - `PlaybackEngine` 接口新增 `preload(source)` / `cancelPreload()`，`AudioEngine` 复用 `JavaSoundStreamingAudioStream.open()` 提前建连、解码并预缓冲下一首流；命中就绪流时 `play` 直接 attach，否则退回实时打开；
  - `PlayerController` 在进入 `PLAYING` 后通过 `peekNextAfterEnd()` 定位下一首并后台解析 + 预加载；自然结束与手动下一首命中时复用已解析源，跳过重复解析；手动切歌、队列到底、停止、退出登录时清理过期预加载；
  - `LyricsManager` 新增 `preload(track)`，仅填充缓存不干扰当前歌词；切到该曲时 `tick` 命中缓存直接展示，避免二次请求；
  - 预加载仅对在线 http/https 流生效；本地音源仍走整曲解码与缓存复用。
- **风险**：预加载期间额外占用一条 CDN 连接和最多 1 MiB PCM 缓冲（沿用现有上限），并占用流线程池第二线程；预加载尚未完成即切歌会退回实时打开，极端竞态下可能短暂多开一个流，下一轮预加载或清理时自动回收。洗牌回绕那一拍不做预加载。真实连续切歌无停顿仍需游戏内真实账号人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功；新增队列 peek、自然结束预加载复用、手动下一首复用、歌词预加载等用例全部通过。

## 2026-08-21 15:23:38 - 新增功能（同步歌词与可复用正在播放 HUD）

- **变更概述**：新增按播放时间逐行同步的歌词能力，以及平台无关的游戏内正在播放 HUD；HUD 按确认稿在左侧显示封面和歌曲信息，底部同一行左侧高亮当前歌词、右侧弱化下一行，并允许整体关闭或分别选择显示内容。
- **修改文件**：
  - `src/main/java/com/cubiccadence/model/LyricLine.java`、`SyncedLyrics.java`、`PlaybackSource.java`
  - `src/main/java/com/cubiccadence/lyrics/LrcParser.java`
  - `src/main/java/com/cubiccadence/provider/MusicProvider.java`
  - `src/client/java/com/cubiccadence/client/lyrics/LyricsLoadState.java`、`LyricsManager.java`
  - `src/client/java/com/cubiccadence/client/ui/hud/NowPlayingSource.java`、`NowPlayingSnapshot.java`、`PlayerNowPlayingSource.java`、`NowPlayingHudElement.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`、`NeteaseMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/playback/PlayerController.java`
  - `src/client/java/com/cubiccadence/client/config/ModConfig.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`、`MusicSettingsScreen.java`、`HudSettingsScreen.java`
  - `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`、`en_us.json`
  - `src/test/java/com/cubiccadence/lyrics/LrcParserTest.java`
  - `src/test/java/com/cubiccadence/client/lyrics/LyricsManagerTest.java`
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseApiClientTest.java`
  - `src/test/java/com/cubiccadence/client/playback/PlayerControllerTest.java`
  - `README.md`、`README.en.md`、`docs/design.md`、`CHANGELOG.md`
- **变更内容**：
  - 扩展 `MusicProvider` 的统一歌词契约，网易云实现按需调用 api-enhanced `/lyric/new`；普通歌词和翻译按时间戳对齐，支持 LRC 偏移、多时间戳、不同小数精度和异常行降级；
  - 新增最多缓存 64 首的内存歌词管理器，以请求代次阻止快速切歌后的迟到响应污染当前歌词；歌词不批量同步、不持久化、不影响播放失败边界；
  - 为试听播放源保留原曲时间轴起点，歌词使用“本地播放进度 + 试听起点”高亮，HUD 进度继续显示试听片段自身进度；
  - 新增 `NowPlayingSource -> NowPlayingSnapshot -> NowPlayingHudElement` 平台无关工具边界，HUD 不依赖网易云 JSON，后续平台可提供相同快照直接复用；
  - 使用 Fabric 26.2 `HudElementRegistry` 注册左上角紧凑面板；打开其他界面或 F3 时隐藏，跟随 F1 原版 HUD 隐藏条件；
  - 新增 HUD 设置子页，支持总开关以及封面、歌名、作者、进度、歌词五项独立配置并持久化；退出登录改由 `PlayerController.stop()` 同步清理 HUD 状态。
- **风险**：`/lyric/new` 为第三方逆向接口，字段可能漂移；当前采用宽容解析和无歌词降级。第一版只实现逐行高亮，不解析逐字 `yrc`。真实字体宽度、GUI 缩放、HUD 图层冲突、试听时间轴和连续切歌体验仍需在游戏内使用真实账号人工验收。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功，58 项测试、0 失败；本次新增 LRC 解析、翻译对齐、无歌词、歌词缓存、迟到响应、试听时间轴和网易云响应映射测试均通过。线上 `https://cub.cubiccadence.top/lyric/new?id=1824020871` 返回 `code=200`，并确认存在普通歌词、逐字歌词和翻译字段；未输出歌词正文。开发客户端成功启动，Mod、Fabric HUD API、资源与 OpenAL 初始化正常；Windows 自动化无法激活 Minecraft 窗口，因此真实歌曲播放、设置点击和 HUD 视觉仍保留为人工验收项。

## 2026-08-21 14:19:04 - 修复问题（二维码授权状态长时间不更新）

- **变更概述**：修复网易云二维码扫码并确认后，Mod 仍可能等待接近 2 分钟才登录成功的问题；二维码登录相关请求现在会主动绕过 `api-enhanced` 的 2 分钟响应缓存。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseAuthClient.java`
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseAuthClientTest.java`（新增）
  - `CHANGELOG.md`
- **变更内容**：
  - 为 `/login/qr/key`、`/login/qr/create` 和每次 `/login/qr/check` 请求添加动态毫秒时间戳，避免相同 URL 持续命中首次返回的 `801` 或 `802` 缓存；
  - 使用线程安全的单调递增缓存标识，确保同一毫秒内生成的连续请求仍具有不同 URL；
  - 将二维码状态轮询间隔从 1 秒调整为 3 秒，与 `api-enhanced` 自带二维码登录页面一致，在及时获取状态的同时降低上游接口限流风险；
  - 新增进程内 HTTP 回归测试，验证 key、create、连续三次 check 请求的时间戳存在且互不相同，并覆盖 `801（等待扫码）→ 802（已扫码）→ 803（已授权）` 状态映射与 Cookie 会话生成。
- **风险**：二维码状态检查不再由服务端缓存吸收，会真实到达上游接口；通过 3 秒轮询间隔控制请求频率。未修改 Cookie 格式、凭据存储、认证状态机、界面或音乐库逻辑。
- **验证结果**：`.\gradlew.bat test --tests com.cubiccadence.client.provider.netease.NeteaseAuthClientTest --no-daemon` 与 `.\gradlew.bat test --no-daemon` 均构建成功；真实手机扫码体验仍需在游戏中进行人工验收，预期确认授权后约 0–3 秒加网络耗时完成登录。

## 2026-08-20 22:35:31 - 优化代码（歌单同步改为有界并发翻页）

- **变更概述**：将登录后歌单列表的同步从「串行逐页翻页」改为「首屏串行 + 剩余页有界并发」，在 `playlistCount` 总数已知时最多同时拉取 4 页，显著降低歌单较多时的首次加载等待。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/library/MusicLibraryManager.java`
  - `src/test/java/com/cubiccadence/client/library/MusicLibraryManagerTest.java`
  - `CHANGELOG.md`
- **变更内容**：
  - 新增固定 4 线程的守护线程池 `pageExecutor`（`MAX_CONCURRENT_PAGES = 4`），并在 `close()` 中关闭；
  - 重写 `fetchAllPlaylists`：先串行请求第 0 页拿到 `playlistCount` 总数，若总数已知则按页数一次性并发拉取剩余页（页 1 到 `totalPages - 1`），完成后按页号顺序合并；
  - 保留 `MAX_SYNC_PAGES` 安全上限与 fail-fast 语义：任一页失败整体刷新失败，退出登录/切账号时通过 `requestGeneration` 丢弃迟到响应；
  - 当 `playlistCount` 缺失（`total == null`）时退回原有的串行滑动窗口（靠 `more` 字段推进），最坏情况行为与改造前一致；
  - 并发阶段用 `AtomicInteger` 累加 `syncLoadedCount`，合并完成后统一校正，避免多线程读改写竞态；
  - 新增两个单元测试：`syncsManyPagesUsingReportedTotalConcurrently`（总数已知的 5 页并发合并与顺序校验）、`fallsBackToSequentialSlidingWindowWhenTotalMissing`（总数缺失的串行兜底）。
- **风险**：仅在客户端歌单元数据同步路径引入并发，不改动接口契约、缓存格式或 UI 语义；并发页数由后端返回的 `playlistCount` 决定，若该值异常偏小可能少拉少量歌单，但原逻辑同样依赖该值，属既有边界。
- **验证结果**：`.\gradlew.bat test --no-daemon` 构建成功，`MusicLibraryManagerTest` 7 个用例全部通过（含 2 个新增用例），无其他回归。

## 2026-08-20 19:34:55 - 新增功能（Cubic Cadence Mod 图标）

- **变更概述**：将原有纯文字占位图替换为原创的像素风 Mod 图标，以青色立体方块和金色音符共同表达“Cubic Cadence（方律）”的方块音乐主题。
- **修改文件**：
  - `src/main/resources/assets/cubic-cadence/icon.png`
  - `CHANGELOG.md`
- **变更内容**：
  - 使用内置图像生成能力创建原创图标，不包含文字、网易云音乐标志、Minecraft 官方方块或其他第三方品牌元素；
  - 将生成原稿高质量缩放为 128×128 的 32 位 ARGB PNG，并保留透明背景；
  - 保持 `fabric.mod.json` 中既有的 `assets/cubic-cadence/icon.png` 资源引用不变。
- **风险**：仅替换静态图片资源，不改变代码、配置或运行逻辑；主要风险为小尺寸辨识度和透明边缘显示，已通过 128×128 与 32×32 预览检查控制。
- **验证结果**：已确认正式文件为 128×128、`Format32bppArgb` 且四角透明；`.\gradlew.bat processResources --no-daemon` 执行成功，构建输出与源图标的 SHA-256 完全一致。

## 2026-08-20 19:09:44 - 优化代码（后端代理切换为服务器地址）

- **变更概述**：将模组内置的后端代理地址从空值（文档示例为本地 `http://localhost:3000`）切换为作者部署的 Cloudflare 服务器地址 `https://api.cubiccadence.top/`，实现玩家开箱即用。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/config/ModConfig.java`
  - `README.md`
  - `README.en.md`
  - `CHANGELOG.md`
- **变更内容**：
  - `ModConfig` 中 `apiEnhancedBaseUrl` 默认值由 `""` 改为 `https://api.cubiccadence.top/`，全新安装（无 `config/cubic-cadence.json`）默认直连服务器；
  - 中文 `README.md` 与英文 `README.en.md` 的「特别声明 / Important Notice」章节补充服务器地址，并说明该地址已作为模组内置默认值；
  - 「配置 / Configuration」章节由单一本地示例改为面向玩家（内置地址、开箱即用）与面向开发者（自托管覆盖）两种情况，分别给出对应 JSON 示例。
- **风险**：仅常量默认值与文档变更，无接口、构建或资源逻辑改动；已存在且显式配置了 `localhost` 的旧配置文件不会被本次默认值覆盖（属开发者本机配置的预期行为）。
- **验证结果**：`.\gradlew.bat build --no-daemon` 构建成功（`:compileClientJava` 重新编译、`:test` 通过）；另已通过只读 HTTP 探测确认 `https://api.cubiccadence.top/` 与 `/login/qr/key` 均返回 200。

## 2026-08-20 18:58:19 - 优化代码（README 补充 api-enhanced 后端特别声明）

- **变更概述**：在两个 README 中新增「特别声明 / Important Notice」章节，说明模组通过 `api-enhanced` 后端通信，区分玩家与开发者的使用/部署方式，并贴出第三方库地址。
- **修改文件**：
  - `README.md`
  - `README.en.md`
  - `CHANGELOG.md`
- **变更内容**：
  - 中文版 `README.md` 新增「特别声明」章节：说明后端为 `api-enhanced`，玩家可直接使用（后端已由作者部署在服务器），开发者需自行部署并在 `config/cubic-cadence.json` 配置 `apiEnhancedBaseUrl`，并附第三方库地址 `https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced`；
  - 英文版 `README.en.md` 同步新增对应的 `Important Notice` 章节；
  - 章节置于项目简介之后、功能特性之前，两个语言版本内容一一对应。
- **风险**：仅文档新增章节，无代码、资源或构建配置变更。
- **验证结果**：未触发构建；`README.md` 与 `README.en.md` 均为纯 Markdown 文档，无代码回归面。

## 2026-08-20 18:49:51 - 优化代码（README 拆分为中文默认版与英文版两个文件）

- **变更概述**：将单一 README 按语言拆分为两个独立文件，默认入口 `README.md` 使用中文，英文版单独存放在 `README.en.md`，并在两个文件顶部互设语言切换链接。
- **修改文件**：
  - `README.md`
  - `README.en.md`（新增）
  - `README.zh-CN.md`（删除）
  - `CHANGELOG.md`
- **变更内容**：
  - `README.md` 改为纯中文，作为仓库默认展示入口，顶部提供 `English` 链接指向 `README.en.md`；
  - 新增 `README.en.md` 纯英文版，内容与中文版一一对应，顶部提供 `简体中文` 链接指向 `README.md`；
  - 移除此前临时使用的 `README.zh-CN.md`；
  - 两个版本均保留项目简介、功能特性、技术栈、快速开始、网易云数据源与合规说明、配置、界面预览（截图占位）、目录结构、许可等章节。
- **风险**：仅文档文件拆分与链接调整，无代码、资源或构建配置变更。
- **验证结果**：未触发构建；`README.md` 与 `README.en.md` 均为纯 Markdown 文档，无代码回归面。

## 2026-08-20 18:12:54 - 优化代码（重写项目 README 为双语并新增截图占位目录）

- **变更概述**：将原精简 README 重写为中文/英文双语项目介绍，并新增界面截图占位目录，方便后续插入界面素材。
- **修改文件**：
  - `README.md`
  - `docs/screenshots/README.md`（新增）
  - `CHANGELOG.md`
- **变更内容**：
  - `README.md` 改为中文为主、英文并列的双语结构，补充项目简介、功能特性、技术栈与版本基线、快速开始（构建/运行）、网易云数据源与合规说明、配置说明、目录结构、许可等章节；
  - 新增「界面预览 / Screenshots」章节，预置音乐库主页、扫码登录、歌单详情、设置界面四个插图占位槽，均指向 `docs/screenshots/`；
  - 新增 `docs/screenshots/README.md`，约定截图文件名、替换方式与推荐分辨率/格式；
  - 保留原有 Setup、NetEase 数据源、License 说明，未改动任何代码、资源或构建配置。
- **风险**：仅文档与目录占位变更，无功能影响。
- **验证结果**：未触发构建；`README.md` 与 `docs/screenshots/README.md` 为纯 Markdown 文档，无代码回归面。

## 2026-08-20 17:59:31 - 修复问题（播放失败缺少可追踪日志）

- **变更概述**：播放失败时 UI 提示“请查看日志”，但失败路径没有写任何日志，导致无法定位具体原因。为解析播放地址、登录态和权限限制等失败分支补齐结构化日志。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/PlayerController.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`
  - `CHANGELOG.md`
- **变更内容**：
  - `PlayerController.fail()` 统一记录最终失败定位键、当前曲目 id 与标题；
  - `PlayerController.playSelected()` 在“未登录/会话不可用”和“解析播放地址失败”两个分支分别记录曲目、音质及底层异常；
  - `NeteaseApiClient.resolvePlaybackSource()` 在 api-enhanced 返回异常时记录 trackId、quality 与异常栈，且不改变原异步结果传播；
  - 未修改任何播放、权限、解析或队列控制逻辑，仅新增诊断输出。
- **风险**：仅新增日志、无控制流变更，理论上零功能回归；失败时会产生额外 warn 日志（属预期诊断输出）。
- **验证结果**：`.\gradlew.bat compileJava compileClientJava compileTestJava test --no-daemon` 全部通过，47 项测试零失败；`zh_cn.json`、`en_us.json` 键集合与既有逻辑不变。真实“播放失败”场景仍需在 Windows 游戏实例内复现，确认日志能定位到具体错误键与底层异常。

## 2026-08-20 17:45:42 - 修复问题（本机与 GitHub 跨平台选择 Gradle JDK 25）

- **变更概述**：修正仅删除本机 JDK 路径会导致 `JAVA_HOME` 为 JDK 17 的本机终端无法构建的问题，改用 Gradle 9.7 Daemon JVM Criteria 跨平台声明 Java 25 要求。
- **修改文件**：
  - `gradle.properties`
  - `gradle/gradle-daemon-jvm.properties`（新增）
  - `CHANGELOG.md`
- **变更内容**：
  - 保持 `gradle.properties` 不包含 `C:/Program Files/...` 等机器专属 `org.gradle.java.home`，并注明 Daemon JDK 由独立条件文件选择；
  - 新增 `gradle/gradle-daemon-jvm.properties`，以 `toolchainVersion=25` 要求任意厂商的 JDK 25；本机会自动发现已安装的 Temurin/Oracle JDK 25，GitHub Actions 会使用 `actions/setup-java` 提供的 Microsoft JDK 25；
  - 不更改系统级 `JAVA_HOME`，避免影响仍需 JDK 17 的其他项目。
- **风险**：没有安装 JDK 25 且未配置 JDK 自动供应源的其他机器会明确构建失败；当前本机已检测到两个 JDK 25，GitHub 工作流也已安装 JDK 25，因此当前环境不受影响。
- **验证结果**：保持本机 `JAVA_HOME` 为 Eclipse Temurin JDK 17，直接执行 `.\gradlew.bat --version` 显示 Launcher JVM 17、Daemon JVM 来自 `gradle/gradle-daemon-jvm.properties` 且兼容 Java 25；随后执行 `.\gradlew.bat build --no-daemon` 成功（`BUILD SUCCESSFUL`，9 个任务中 2 个执行、7 个为最新状态）。

## 2026-08-20 17:28:28 - 修复问题（在线播放提前切歌与自动切歌卡顿）

- **变更概述**：修复在线音乐在结尾前几十秒被误判结束并自动跳到下一首，以及切歌时客户端渲染线程同步关闭网络/解码资源导致游戏短时卡死的问题。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStream.java`
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `src/test/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStreamTest.java`（新增）
  - `src/test/java/com/cubiccadence/client/playback/PlayerControllerTest.java`
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - 将在线流生命周期明确拆分为 `RUNNING / EOF / FAILED / CANCELLED`，不再把声音线程一次取数超时当作真正 EOF；短时断流改为有限静音保活并展示缓冲状态，数据恢复后继续播放，连续断流超过 10 秒转为错误；
  - 按 PCM 格式累计真实解码时长，并与 `PlaybackSource.playableDurationMs` 比较；早于声明时长超过 `max(5 秒, 2%)` 的 EOF 视为流异常，不触发自动下一首；试听源继续以平台返回的实际可播放时长为准；
  - 停止、自然结束、失败和切歌时只同步取消生产任务、清空队列和更新状态，JavaSound/HTTP 底层资源交给最多两个专用守护清理线程关闭，避免渲染 tick 等待网络锁或解码锁；
  - 保持 PCM 队列最多 16 个 64 KiB 块（硬上限 1 MiB），静音保活不进入该队列，OpenAL 继续使用原版四段流缓冲，不缓存整首歌曲；
  - 增加暂时断流恢复、真正 EOF、持久失败、提前 EOF、非阻塞关闭、内存上限以及播放错误不自动切歌测试。
- **风险**：断流容忍窗口固定为 10 秒，极差网络下会先播放静音再报告失败；播放源声明时长若平台返回错误，可能被提前结束保护判为异常；自动测试无法建立真实 Minecraft OpenAL 上下文，仍需 Windows 游戏实例长时间播放验证。
- **验证结果**：`\.\gradlew.bat compileJava compileClientJava compileTestJava test` 全部通过，47 项测试零失败；`zh_cn.json`、`en_us.json` 均解析成功，各 112 个键且键集合一致；`git diff --check` 无空白错误，仅有工作区既有 LF→CRLF 提示。真实网络抖动恢复、歌曲完整播放至结尾、自动下一首无卡顿及长时间内存表现仍需 Windows 游戏实例人工复测。

## 2026-08-20 16:47:45 - 修复问题（在线播放 OpenAL 流式缓冲无法入队）

- **变更概述**：修复在线播放已成功获取和解码 MP3、但 OpenAL 持续报告 `Creating buffer: Invalid operation` 且没有声音的问题。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - 在 Minecraft 声音线程中停止在线播放 Source 后，显式执行 `AL_BUFFER = 0` 解除静音引导静态缓冲，再挂载原版 `AudioStream` 流式队列，避免同一 Source 同时处于静态缓冲与队列缓冲模式；
  - 静态引导缓冲改为在声音线程、且确认解绑后释放，避免跨线程调用 OpenAL 或删除仍绑定的缓冲；
  - 流式队列挂载后读取 `AL_BUFFERS_QUEUED` 验证实际入队数量，只有至少一个缓冲成功入队才切换为 `PLAYING`；解绑或入队失败时立即停止声道、关闭网络流并进入单次错误状态，防止声音线程无限重试和持续刷屏；
  - 保持网易云播放地址解析、后台 HTTP/MP3 解码、Cookie/URL 日志保护、权限限制和本地 WAV/MP3 播放路径不变。
- **风险**：修复依赖 Minecraft 26.2 当前 `Channel` 的 OpenAL Source 实现和声音线程执行顺序；自动测试没有真实 OpenAL 上下文，必须重新启动 Windows 游戏实例确认出声、错误日志消失以及停止/切歌资源释放。
- **验证结果**：`.\gradlew.bat compileJava compileClientJava compileTestJava test` 全部通过，39 项测试零失败；`zh_cn.json`、`en_us.json` 均解析成功，各 112 个键且键集合一致；`git diff --check` 无空白错误，仅有工作区既有 LF→CRLF 提示。真实 OpenAL 播放、错误日志消失、暂停/切歌和资源释放仍需重新启动游戏实例人工复测。

## 2026-08-20 16:22:39 - 新增功能（第三阶段在线播放、队列与播放模式）

- **变更概述**：接入网易云临时播放源解析和基于 Minecraft 原版 `AudioStream`/OpenAL 声道的 MP3 流式缓冲；歌单详情页支持点击播放、全局控制和四种播放模式，并明确展示试听及权限限制。
- **修改文件**：
  - `src/main/java/com/cubiccadence/model/PlaybackAccess.java`（新增）
  - `src/main/java/com/cubiccadence/model/PlaybackSource.java`
  - `src/main/java/com/cubiccadence/provider/MusicProvider.java`
  - `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `src/client/java/com/cubiccadence/client/playback/JavaSoundStreamingAudioStream.java`（新增）
  - `src/client/java/com/cubiccadence/client/playback/PlaybackEngine.java`（新增）
  - `src/client/java/com/cubiccadence/client/playback/PlaybackQueue.java`
  - `src/client/java/com/cubiccadence/client/playback/PlayerController.java`
  - `src/client/java/com/cubiccadence/client/provider/UnavailableMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicSettingsScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/PlaylistDetailScreen.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - `src/test/java/com/cubiccadence/client/auth/AuthManagerTest.java`
  - `src/test/java/com/cubiccadence/client/library/MusicLibraryManagerTest.java`
  - `src/test/java/com/cubiccadence/client/library/PlaylistDetailManagerTest.java`
  - `src/test/java/com/cubiccadence/client/playback/PlaybackQueueTest.java`（新增）
  - `src/test/java/com/cubiccadence/client/playback/PlayerControllerTest.java`（新增）
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseApiClientTest.java`
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - `MusicProvider.resolvePlaybackSource` 显式接收当前 `AuthSession`；网易云实现调用 `/song/url/v1`，按低/标准/高映射 `standard/higher/exhigh`，不发送 `unblock`，不调用解灰接口，不记录 Cookie 或完整播放地址；HTTP 媒体地址升级为 HTTPS，播放源仅保存在内存并按 `expi` 标记过期时间；
  - `PlaybackSource` 增加不可变请求头、完整/试听访问类型和实际可播放时长；平台返回 `freeTrialInfo` 时只播放原响应提供的试听内容并在界面标注，不伪装为完整歌曲；无地址、非 MP3 或无损格式均 fail-closed；
  - 新增后台 HTTP/JavaSound MP3 解码流，使用至多 16 个 64 KiB PCM 块进行有界预缓冲；Minecraft 原版声音线程只从队列消费 PCM，并通过原生四段 `AudioStream` OpenAL 缓冲播放，停止、切歌、自然结束和关闭客户端时释放网络流与声道资源；
  - `PlayerController` 统一持有当前歌曲、临时来源、异步代次和播放状态；登录失效立即停止，快速切歌会丢弃迟到结果，明确受限歌曲不会请求播放源，解析失败按当前队列有限跳过；
  - `PlaybackQueue` 支持从点击歌曲开始的当前详情页队列、受限歌曲跳过、顺序播放、单曲循环、列表循环和带历史顺序的随机播放；详情页增加点击播放、当前项高亮、上一首/播放暂停/下一首/模式切换和试听/缓冲状态，主页控制栏继续控制同一个全局播放器；
  - 设置页增加在线播放音质循环选择。本阶段仅支持 MP3 的低/标准/高音质；无损不会静默降级，在线任意位置 seek 保持禁用并留待后续流式 seek 阶段。
- **风险**：`api-enhanced` 与网易云媒体 CDN 均非稳定官方契约，字段、地址有效期、重定向和 MP3 编码可能变化；JavaSound SPI 与 Minecraft 声道队列的真实长时间表现需 Windows 游戏实例确认；当前队列仅包含歌单详情已加载的当前 50 首；无损 FLAC 和在线任意位置 seek 不在本阶段范围。
- **验证结果**：`.\gradlew.bat compileJava compileClientJava compileTestJava test` 全部通过，39 项测试零失败；`zh_cn.json`、`en_us.json` 均解析成功，各 112 个键且键集合完全一致；`git diff --check` 无空白错误，仅有工作区既有 LF→CRLF 提示。JDK 25 仍输出既有 JNA native-access 与 LWJGL Unsafe 未来兼容性警告，不影响通过。真实在线播放、约四秒预缓冲、权限/试听状态、连续切歌、资源释放和四种模式仍需启动游戏人工确认。

## 2026-08-20 15:43:10 - 新增功能（第二阶段歌单详情按需加载与歌曲权限展示）

- **变更概述**：主页歌单卡片增加悬停和点击交互；只有玩家打开歌单详情后才分页请求歌曲，详情页展示歌曲封面、名称、歌手、专辑、时长及可播放、不可播放、版权受限、会员不足、地区受限等状态。
- **修改文件**：
  - `src/main/java/com/cubiccadence/model/Availability.java`
  - `src/main/java/com/cubiccadence/model/MusicErrorCode.java`
  - `src/main/java/com/cubiccadence/model/Track.java`
  - `src/main/java/com/cubiccadence/provider/MusicProvider.java`
  - `src/main/java/com/cubiccadence/provider/PlaylistPage.java`
  - `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - `src/client/java/com/cubiccadence/client/library/PlaylistDetailManager.java`（新增）
  - `src/client/java/com/cubiccadence/client/provider/UnavailableMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/PlaylistDetailScreen.java`（新增）
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - `src/test/java/com/cubiccadence/client/auth/AuthManagerTest.java`
  - `src/test/java/com/cubiccadence/client/library/MusicLibraryManagerTest.java`
  - `src/test/java/com/cubiccadence/client/library/PlaylistDetailManagerTest.java`（新增）
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseApiClientTest.java`
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - `MusicProvider.getPlaylistTracks` 显式接收 `AuthSession`，Provider 不持有 Cookie；网易云实现调用 `/playlist/track/all?id=<id>&limit=50&offset=<page*50>`，请求继续在后台线程完成，Cookie 和完整请求地址不写日志；
  - 将接口 `songs` 与 `privileges` 按歌曲 ID 合并为不可变 `Track`，映射歌手、专辑、封面和时长；权限按地区提示、灰色版权状态、当前播放音质和 `fee` 保守判定，无法可靠识别时显示未知，不绕过任何会员、版权、地区或音质限制；
  - 新增独立 `PlaylistDetailManager`：启动和歌单摘要同步不请求歌曲，点击歌单后才加载第一页；支持每页 50 首、上一页、下一页、失败重试、同一歌单当前页内存复用，以及退出/换页后的迟到响应隔离；
  - 新增 `PlaylistDetailScreen`，采用原版 `ObjectSelectionList` 滚动展示歌曲资料与彩色权限标签；主页歌单卡片增加绿色悬停边框和左键命中，返回按钮、ESC 或音乐库快捷键可返回主页；当前阶段明确不触发在线播放；
  - 新增歌曲字段/六类权限映射测试，以及“点击前零请求”、按需分页、失败重试、退出后忽略迟到响应测试；同步中英文文案和设计说明。
- **风险**：`api-enhanced` 为第三方逆向实现，`songs`、`privileges`、`pl/plLevel`、`st`、`toast` 和 `fee` 字段可能变化；状态映射采用保守策略，播放阶段仍须以实际播放源响应复核；每页 50 张封面可能增加 GPU/磁盘缓存压力，但只有可见行触发下载且离开页面会释放动态纹理；不同 GUI 缩放下的滚动列表、长歌曲信息和真实权限状态需游戏内人工确认。
- **验证结果**：在会话级显式选择 JDK 25 后，`.\gradlew.bat compileJava compileClientJava compileTestJava test` 全部通过；`zh_cn.json`、`en_us.json` 均解析成功，各 86 个键且键集合完全一致；`git diff --check` 无空白错误，仅有工作区既有 LF→CRLF 提示。JDK 25 测试仍输出既有 JNA native-access 与 LWJGL Unsafe 未来兼容性警告，不影响通过。需人工确认卡片点击、滚动列表、封面、分页、失败重试、返回路径以及各种真实权益状态。

## 2026-08-20 15:36:11 - 修复问题（GitHub Actions 无法启动 Gradle 构建）

- **变更概述**：移除仓库中绑定本机 Windows JDK 安装目录的 Gradle 配置，避免 Ubuntu GitHub Actions 将该路径误作 Java Home 并在编译前失败。
- **修改文件**：
  - `gradle.properties`
  - `CHANGELOG.md`
- **变更内容**：
  - 删除 `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot` 及其本机路径说明；
  - GitHub Actions 继续通过 `actions/setup-java` 提供 Microsoft JDK 25，本机开发环境改由 IDE Gradle JVM、`JAVA_HOME` 或用户级 Gradle 配置选择 JDK，不再把机器专属绝对路径提交到仓库。
- **风险**：未配置 Java 25 的本机终端将无法直接构建，需要先设置会话级或系统级 `JAVA_HOME`，但不会影响已经配置 `actions/setup-java` 的 GitHub Actions。
- **验证结果**：使用会话级 JDK 25 执行 `.\gradlew.bat build --no-daemon`，构建及测试全部通过（`BUILD SUCCESSFUL`，9 个任务中 5 个执行、4 个为最新状态）；仅保留既有的 JNA native-access、LWJGL Unsafe 未来兼容性警告，以及 `tritonus-share:0.3.7.4` 非标准 SemVer 提示。

## 2026-08-20 15:08:32 - 新增功能（第一阶段音乐库全量同步、缓存优先启动与远程图片稳定加载）

- **变更概述**：实现第一阶段音乐库同步闭环：同步用户创建、收藏及网易云 `specialType=5` 的红心歌单，支持首次同步进度、手动刷新、缓存先显示再后台刷新和每页 8 项本地分页；同时修复合法网易云 PNG 已下载但在原图片解码链路持续失败、头像与封面只能显示占位的问题。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/library/LibrarySnapshot.java`（新增）
  - `src/client/java/com/cubiccadence/client/library/LibraryCacheStore.java`（新增）
  - `src/client/java/com/cubiccadence/client/library/MusicLibraryManager.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/texture/RemoteTextureCache.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - `src/test/java/com/cubiccadence/client/library/LibraryCacheStoreTest.java`（新增）
  - `src/test/java/com/cubiccadence/client/library/MusicLibraryManagerTest.java`
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseApiClientTest.java`
  - `src/test/java/com/cubiccadence/client/ui/texture/RemoteTextureCacheTest.java`
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - `MusicLibraryManager` 在恢复登录后异步读取资料库缓存并立即构建首页，同时后台重新获取账号资料，以 `limit=50` 连续请求全部歌单摘要；同步完成后替换缓存，网络失败且已有缓存时继续显示旧数据并给出刷新警告；
  - 首页仍保持 4×2、每页 8 项，但上一页/下一页改为内存分页，不再逐页发起网络请求；新增手动刷新按钮、首次同步已获取数/总数、后台刷新、上次同步时间以及创建/收藏/红心分类文案；
  - `LibraryCacheStore` 在专用后台单线程中原子读写 `config/cubic-cadence/cache/library.json`，设 2 MiB、10000 个歌单的安全上限；只保存公开资料和歌单摘要，不保存 Cookie、歌曲明细或完整播放地址；显式退出按队列顺序清除资料库缓存；
  - `NeteaseApiClient` 优先将 `specialType=5` 映射为 `SPECIAL` 红心集合，其余按创建者 ID 区分创建与收藏，不绕过平台会员、版权、地区或音质限制；
  - `RemoteTextureCache` 改用 `ImageIO -> BufferedImage -> NativeImage -> DynamicTexture` 图片链路，继续后台下载、渲染线程注册/释放；成功图片原子写入最多 512 项磁盘缓存，网络/服务端错误有限重试，解码等确定性失败不重复请求；退出清缓存时用代次与文件锁避免迟到下载重新写回。
  - 测试覆盖全量分批同步、本地八项分页、缓存先显示、后台失败降级、退出删除缓存、资料缓存读写、红心歌单映射和远程图片 URL 安全规则；同步更新中英文文案与设计说明。
- **风险**：`api-enhanced` 属第三方逆向服务，`/user/playlist` 的 `more`、`playlistCount`、`specialType` 字段可能变化；单账号缓存依赖显式退出清理，异常强制终止时会保留到下次后台校验；大量歌单会增加顺序分页请求，但已限制最多 200 页/10000 项；Java ImageIO、AWT 到 NativeImage 的颜色和真实 GPU 动态纹理仍需在 Windows 游戏实例中确认。
- **验证结果**：`.\gradlew.bat compileJava compileClientJava compileTestJava test` 全部通过；`zh_cn.json`、`en_us.json` 均通过 PowerShell JSON 解析；`git diff --check` 无空白错误，仅有工作区既有 LF→CRLF 提示。JDK 25 测试仍输出 JNA native-access 与 LWJGL Unsafe 的未来兼容性警告，不影响通过。需人工确认首次/缓存启动提示、手动刷新、红心/创建/收藏标签、头像与封面实际渲染、翻页复用、刷新失败降级及退出后缓存目录清理。

## 2026-08-20 14:10:02 - 修复问题（主页设置分层、歌单大封面居中及账号远程资料修复）

- **变更概述**：依据游戏内截图重新整理音乐库主页，将音量相关控件迁入独立设置页并在右下角增加齿轮入口；将每页 8 个歌单封面放大到最大 176×176 并连同分页整体居中；修复远程头像/封面首次失败后不再恢复的问题，并通过额外资料接口补全用户等级和会员状态。
- **修改文件**：
  - `src/main/java/com/cubiccadence/model/UserProfile.java`
  - `src/main/java/com/cubiccadence/model/MembershipTier.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicSettingsScreen.java`（新增）
  - `src/client/java/com/cubiccadence/client/ui/texture/RemoteTextureCache.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseApiClientTest.java`
  - `src/test/java/com/cubiccadence/client/ui/texture/RemoteTextureCacheTest.java`（新增）
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - 新增 `MusicSettingsScreen`，集中承载方律音量、原版音乐音量和禁用原版背景音乐；设置即时生效并沿用既有持久化，主页右下角以齿轮按钮进入，完成或 ESC 返回原主页；
  - `MusicLibraryScreen` 移除底部设置行，仅保留播放进度和媒体控制；歌单保持 4 列 × 2 行、每页 8 项，封面最大尺寸由 88 提升到 176，依据窗口宽高自适应收缩；网格在账号栏与播放器之间整体居中，分页紧贴网格底部；
  - 账号资料先读取 `/user/account` 的身份信息，再以同一认证会话异步请求 `/user/level` 和 `/vip/info` 补充等级与会员；明确无权益显示非会员，补充接口失败显示未知且不使登录失败；Cookie 和完整请求地址不写日志；
  - `RemoteTextureCache` 修正远程纹理首次失败后永久停留占位的问题：允许每轮最多 3 次、间隔 1.5 秒的重试，最终失败 30 秒后可重新发起一轮；区分网络、HTTP、大小、解码和注册阶段，日志仅记录 CDN 主机及失败阶段；继续限制网易云图片域、响应大小、尺寸和像素数，并在渲染线程注册/释放纹理；
  - 增加资料补充、黑胶 VIP、明确非会员、接口不可用降级，以及 CDN URL HTTPS 升级和伪造域名拒绝测试；同步中英文文案与设计说明。
- **风险**：`api-enhanced` 为第三方逆向服务，`/user/level`、`/vip/info` 字段可能变化；远程纹理重试会产生少量额外 CDN 请求但已限制次数和周期；176×176 为逻辑最大值，小窗口会自动缩小；齿轮字符及 GPU 动态纹理需在真实游戏实例中确认字体和渲染表现。
- **验证结果**：`\.\gradlew.bat compileJava compileClientJava compileTestJava test` 全部通过；`zh_cn.json`、`en_us.json` 均通过 PowerShell JSON 解析；`git diff --check` 无空白错误，仅输出工作区既有 LF→CRLF 提示；JDK 25 运行 JNA 测试时仍有既有 native-access 未来兼容性警告，不影响测试通过。

## 2026-08-20 12:46:30 - 新增功能（账号资料、真实歌单封面与主页内嵌分页）

- **变更概述**：按确认的主页线框重新排版音乐库；登录成功或恢复会话后异步获取网易云账号资料和用户歌单，展示头像、昵称、完整 UID、等级及会员状态；歌单封面采用 4 列 × 2 行、每页 8 项的主页内嵌分页。
- **修改文件**：
  - `src/main/java/com/cubiccadence/model/UserProfile.java`
  - `src/main/java/com/cubiccadence/model/MembershipTier.java`（新增）
  - `src/main/java/com/cubiccadence/provider/MusicProvider.java`
  - `src/main/java/com/cubiccadence/provider/PlaylistSummaryPage.java`（新增）
  - `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - `src/client/java/com/cubiccadence/client/library/MusicLibraryManager.java`（新增）
  - `src/client/java/com/cubiccadence/client/provider/UnavailableMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseApiClient.java`
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseMusicProvider.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/texture/RemoteTextureCache.java`（新增）
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`、`en_us.json`
  - `src/test/java/com/cubiccadence/client/auth/AuthManagerTest.java`
  - `src/test/java/com/cubiccadence/client/library/MusicLibraryManagerTest.java`（新增）
  - `src/test/java/com/cubiccadence/client/provider/netease/NeteaseApiClientTest.java`（新增）
  - `docs/design.md`
  - `CHANGELOG.md`
- **变更内容**：
  - `UserProfile` 新增等级和会员枚举；`vipType` 按 `0/10/11` 映射为非会员/音乐包/黑胶 VIP，未知值安全降级为非会员；
  - `MusicProvider` 的资料和歌单查询显式接收 `AuthSession`，Provider 不保存 Cookie；新增 `PlaylistSummaryPage` 分页契约；
  - `NeteaseApiClient` 实现 `/user/account` 与 `/user/playlist` 异步 GET、响应大小和 JSON 结构校验、资料/歌单领域映射；歌单按 `limit=8`、`offset=page*8` 请求，Cookie 和完整请求地址均不写日志；
  - `MusicLibraryManager` 在 `SIGNED_IN` 后加载资料和第一页歌单，支持上一页、下一页与失败重试；用请求代次隔离退出登录后的迟到响应，资料或图片失败不会改变认证状态；
  - `RemoteTextureCache` 直接使用接口返回的头像/封面 URL，由 Mod 客户端后台下载、渲染线程注册 `DynamicTexture`；仅允许网易云图片域 HTTPS，限制 4 MiB、4096 边长和 16777216 像素，翻页、关闭、退出时释放纹理；
  - `MusicLibraryScreen` 改为左上账号横条、中部 4×2 自适应大封面与内嵌分页、底部播放控制；保留播放/暂停、停止、格式切换、进度、方律音量、原版音乐音量及禁用原版背景音乐；补充加载、空数据、失败和占位状态；
  - 新增资料/VIP/歌单映射测试及登录恢复、每页 8 项、退出后迟到响应隔离测试；同步中英文语言与设计文档。
- **风险**：`api-enhanced` 为第三方逆向服务，字段或接口可能变更；头像和封面依赖网易云 CDN HTTPS 可用性，失败时仅显示占位；不同 GUI 缩放下的封面大小、长昵称/UID、真实扫码资料、翻页图片释放和退出清理仍需运行游戏实例人工确认。
- **验证**：`compileJava/compileClientJava/compileTestJava/test` 全部通过；中英文语言 JSON 解析通过；`git diff --check` 无空白错误。JDK 25 运行 JNA 测试时仍输出既有 native-access 未来兼容性警告，不影响本次构建成功。

## 2026-08-20 00:37:42 - 修复问题（暂停失效、二维码返回主页入口与退出二次确认）

- **变更概述**：修复本地音乐暂停后返回游戏仍继续播放的问题；为二维码登录页新增「返回主页」按钮；退出登录增加二次确认。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/LoginQrScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`、`en_us.json`
- **变更内容**：
  - `AudioEngine.tick()`：当 `state == PAUSED` 且当前 channel 存在时，每帧重新执行 `handle.execute(Channel::pause)`。`Channel.pause()` 内部只在 `AL_PLAYING` 时真正暂停、已是 `PAUSED` 时为 no-op，因此能在一帧内纠正因异步竞态或 SoundEngine 全局恢复导致的「状态显示暂停但底层仍在播放」，同时不影响正常恢复；
  - `LoginQrScreen`：新增「返回主页」按钮（`button.cubic-cadence.back`），与「重新获取二维码」并排，点击复用 `returnToLibrary()`；关闭（ESC）仍走 `returnToLibrary()`，实现等待授权时随时回主页继续等待；
  - `MusicLibraryScreen`：`SIGNED_IN` 时点击登录按钮改为弹出原版 `ConfirmScreen` 二次确认，确认后才 `audioEngine.stop()` 并 `logout()`；确认或取消后均回到音乐库主页；
  - 语言文件：新增 `button.cubic-cadence.back` 与 `confirm.cubic-cadence.logout_title/message/confirm/cancel`。
- **风险**：`tick()` 在 `PAUSED` 期间每帧提交一个异步 `pause` Consumer，开销极小；`Channel.pause()` 的 `PLAYING` 检查保证不会误伤正常恢复。返回主页与 ESC 共用 `returnToLibrary()`，靠 `navigated` 防重复跳转。
- **验证**：`compileJava/compileClientJava/compileTestJava/test` 全部通过；中英文语言 JSON 校验通过。暂停与退出确认的真实游戏内交互需运行游戏实例人工确认。

## 2026-08-19 23:43:18 - 修复问题（登录体验优化：等待反馈、速度与返回主页）

- **变更概述**：优化网易云二维码登录体验。将扫码后轮询间隔从 2 秒降到 1 秒、缩短请求/连接超时；暴露「已扫码待确认」状态并在主页/二维码页显示；二维码垂直居中并加生成动画；移除不能独立完成登录的「在浏览器打开」按钮；授权成功或关闭二维码页时返回「音乐库」主页继续等待。
- **修改文件**：
  - `src/client/java/com/cubiccadence/client/provider/netease/NeteaseAuthClient.java`
  - `src/client/java/com/cubiccadence/client/auth/AuthManager.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/LoginQrScreen.java`
  - `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`、`en_us.json`
  - `src/test/java/com/cubiccadence/client/auth/AuthManagerTest.java`
- **变更内容**：
  - `NeteaseAuthClient`：`POLL_INTERVAL_MS` 2s→1s（对齐 `AuthManager` 的 `MIN_POLL_INTERVAL_MS`），`REQUEST_TIMEOUT` 15s→10s，`connectTimeout` 10s→5s，扫码后更快感知结果、服务不可达时更快暴露错误；
  - `AuthManager`：新增 `volatile lastStatus` 与 `getLastStatus()`，在 `beginLogin`/授权成功/拒绝/过期/错误时重置为 `PENDING`，轮询返回 `SCANNED` 时记录并保持 `AUTHORIZING`，供 UI 区分「等待扫码」与「已扫码待确认」；
  - `LoginQrScreen`：移除「在浏览器打开」按钮（`authorizationUrl` 与二维码内容相同，无法独立登录）；二维码由固定 `QR_TOP=54` 改为垂直居中；二维码生成中按 tick 渲染省略号动画；新增 `SCANNED` 黄色提示；授权成功与手动关闭均通过 `returnToLibrary()` 返回 `MusicLibraryScreen`，并用 `navigated` 防重复跳转；
  - `MusicLibraryScreen`：`AUTHORIZING` 时顶部显示等待原因（需手机 App 扫码确认），登录按钮由置灰改为「查看二维码」可重新打开二维码页，`SCANNED` 时同步显示「已扫码待确认」；仅 `REFRESHING` 状态保持按钮置灰；
  - 语言文件：移除 `button.cubic-cadence.open_browser` 与 `button.cubic-cadence.authorizing`，新增 `button.cubic-cadence.view_qr`、`auth.cubic-cadence.authorizing_home`、`auth.cubic-cadence.scanned`；
  - 测试：新增 `exposesScannedStatusWithoutCompletingAuthorization` 用例，验证 `SCANNED` 保持 `AUTHORIZING` 且 `getLastStatus()` 正确暴露。
- **风险**：轮询降到 1 秒会轻微增加自建 `api-enhanced` 服务的轮询频率（二维码有效期 5 分钟，可接受）；超时下调可能使网络抖动时的单次慢请求更快判定失败，但会通过错误文案与「重新获取二维码」按钮可恢复。
- **验证**：`compileJava/compileClientJava/compileTestJava/test` 全部通过；中英文语言 JSON 校验通过。

## 2026-08-19 22:43:46 - 重构（客户端直连 api-enhanced，废弃 Spring Boot 后端）

- **变更概述**：按已确认的方案 A，废弃 Spring Boot 后端与官方适配器，Mod 直连自建的 `api-enhanced` 服务；鉴权模型从 accessToken/refreshToken 改为网易云 Cookie，二维码登录改为 api-enhanced 的 `/login/qr/key`、`/login/qr/create`、`/login/qr/check` 三步。
- **修改文件**：
  - 删除 `backend/` 整个目录、`docs/auth-gateway-openapi.yaml`、`docs/demo.zip`；
  - 修改 `src/main/java/com/cubiccadence/auth/AuthSession.java`（accessToken/refreshToken → cookie）；
  - 重写 `src/client/java/com/cubiccadence/client/provider/netease/NeteaseAuthClient.java`（直连 api-enhanced 二维码登录、刷新、退出）；
  - 修改 `src/client/java/com/cubiccadence/client/config/ModConfig.java`（authGatewayBaseUrl → apiEnhancedBaseUrl）、`CubicCadenceClient.java`、`NeteaseMusicProvider.java`、`UnavailableMusicProvider.java`；
  - 修改 `src/test/.../AuthManagerTest.java`、`WindowsDpapiTokenStoreTest.java` 适配 Cookie 模型；
  - 修改 `README.md`、`docs/design.md`、`docs/minecraft-fabric-music-mod-development-document.md`、`docs/netease-cloud-music-api-integration-requirements.md`；
  - 修改 `CHANGELOG.md`。
- **变更内容**：
  - `AuthSession` 只保留 `providerId`、`cookie`、`expiresAtEpochMs`；`expiresAtEpochMs` 在登录/刷新成功时填保守的 7 天默认值用于触发定时刷新；
  - `NeteaseAuthClient` 直连 api-enhanced：`/login/qr/key` 取 unikey、`/login/qr/create` 取二维码 URL、`/login/qr/check` 轮询（`code` 801 等待 / 802 已扫码 / 803 成功并返回 Cookie / 800 过期）、`/login/refresh` 用 Cookie 续期、`/logout` 退出；
  - 客户端通过 `apiEnhancedBaseUrl` 配置 api-enhanced 服务地址，不再持有开发者私钥或官方令牌；
  - 保留 `NeteaseApiClient` 为空实现，业务端点（搜索/歌单/播放）留待阶段 4。
- **风险**：api-enhanced 为逆向第三方服务，需自行部署并常驻运行；接口可能随上游变化。Cookie 无权威过期时间，7 天为保守默认值，实际以 api-enhanced 返回为准。
- **验证**：客户端 `compileJava/compileClientJava/compileTestJava/test` 通过；后端因已废弃不再参与构建。

## 2026-08-19 21:08:39 - 变更决策（网易云数据源改为第三方逆向库）

- **变更概述**：因网易云官方对个人开发者不支持多用户 Open API（仅开放 `ncm-cli`，厂商接入需联系商务），将数据源从官方多用户 API 改为第三方逆向库 `NeteaseCloudMusicApiEnhanced/api-enhanced`，并同步更新相关文档。
- **修改文件**：
  - `docs/netease-cloud-music-api-integration-requirements.md`
  - `docs/minecraft-fabric-music-mod-development-document.md`
  - `docs/design.md`
  - `README.md`
  - `CHANGELOG.md`
- **变更内容**：
  - 在接口需求文档中移除「正式编码前不得根据非官方逆向接口补全端点」「不会使用 Binaryify、NeteaseCloudMusicApi Enhanced 或其他逆向接口」的旧边界，改为明确记录采用 `api-enhanced` 作为数据来源；
  - 新增风险提示：该库逆向网易云未公开私有接口，非官方授权，公开发布存在账号封禁、服务条款与法律合规风险，接口无稳定性承诺、可能随时失效；
  - 同步更新开发文档、设计文档和 README 中的接入方式与阶段路线描述；
  - 本次仅改文档，代码从官方适配器切换到 `api-enhanced` 的落地工作尚未执行，后续单独规划。
- **风险与边界**：本变更属团队在「个人开发者无法接入官方多用户 API」前提下的知情决策，不应对外描述为网易云官方认可的实现；公开发布前需重新评估合规风险。

## 2026-08-19 20:15:01 - 新增功能（网易云官方适配器与游戏内登录二维码）

- **变更概述**：按网易云开放平台官方 demo 与 API 文档，实现真实后端认证适配器（RSA-SHA256 签名、匿名登录、二维码获取、状态轮询、令牌刷新），并在游戏内新增独立登录二维码弹窗，玩家无需离开游戏即可扫码授权。
- **修改文件**：
  - 后端新增 `NeteaseSigner.java`、`NeteaseOpenApiClient.java`、`ConfiguredNeteaseOfficialAuthAdapter.java`、`NeteaseAuthAdapterConfiguration.java` 及 `NeteaseSignerTest.java`；
  - 后端修改 `NeteaseOfficialAuthAdapter.java`、`UnconfiguredNeteaseOfficialAuthAdapter.java`、`AuthGatewayService.java`、`AuthGatewayController.java`、`RefreshSessionRequest.java`、`application.yml`、`.env.example`、`README.md`；
  - 客户端新增 `LoginQrScreen.java`；修改 `build.gradle`、`NeteaseAuthClient.java`、`MusicLibraryScreen.java` 及中英文语言文件；
  - 修改 `docs/auth-gateway-openapi.yaml`。
- **变更内容**：
  - 按官方 demo 实现 RSA-SHA256 签名：除 `sign` 外的参数按 key 字典序拼接，用 PKCS#8 私钥做 `SHA256withRSA` 签名并 Base64，作为 `sign`；GET 请求中 `device`、`bizContent`、`sign` 额外做一次 URL 编码；
  - 实现四个官方端点：匿名登录 `/openapi/music/basic/oauth2/login/anonymous`、获取二维码 `/openapi/music/basic/user/oauth2/qrcodekey/get/v2`、轮询 `/openapi/music/basic/oauth2/device/login/qrcode/get`、刷新 `/openapi/music/basic/user/oauth2/token/refresh/v2`；
  - 轮询状态 `800/801/802/803/804` 映射到 `EXPIRED/PENDING/SCANNED/AUTHORIZED/EXPIRED`，并保留 `1406/1407/1408` 令牌异常语义说明；
  - 仅当 `NCM_APP_ID`、`NCM_APP_SECRET`、`NCM_PRIVATE_KEY` 全部注入时才启用真实适配器，否则仍 fail-closed（`ready=false`、返回 HTTP 503）；
  - `refresh` 契约新增 `accessToken` 字段，因为网易云刷新接口需要旧 accessToken 参与；
  - 客户端引入 ZXing core，新增 `LoginQrScreen` 独立弹窗：游戏内渲染授权二维码、实时显示授权状态、授权成功自动关闭，并保留“在浏览器打开”和“重新获取二维码”按钮；
  - 登录动作由跳转系统浏览器改为打开游戏内二维码弹窗。
- **验证**：后端 `test` 通过（含签名“签名→验签”自检、PEM 私钥解析、字典序拼接）；客户端 `compileJava/compileClientJava/test` 通过。真实扫码联调需真实凭据，本轮未执行。

## 2026-08-19 18:55:00 - 新增功能（阶段 3 登录客户端与公开发布后端边界）

- **变更概述**：实现 Mod 侧无密码浏览器授权状态机、Windows DPAPI 会话持久化、自动刷新与退出清理，并新增独立认证网关工程和 OpenAPI 契约；由于当前没有可审查的网易云官方服务端接入文档，网关的官方适配器保持 fail-closed，未伪造私有接口或把 `privateKey` 放进 Mod。
- **修改文件**：
  - 修改 `.gitignore`、`README.md`、`build.gradle`、`docs/design.md`、`docs/minecraft-fabric-music-mod-development-document.md`、`docs/netease-cloud-music-api-integration-requirements.md`；新增 `docs/auth-gateway-openapi.yaml`；
  - 修改 `src/main/java/com/cubiccadence/auth/AuthSession.java`、`src/main/java/com/cubiccadence/provider/MusicProvider.java`；新增 `AuthorizationChallenge.java`、`AuthorizationResult.java`、`AuthorizationStatus.java`；
  - 修改 `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`、`auth/AuthManager.java`、`config/ModConfig.java`、`provider/netease/NeteaseAuthClient.java`、`provider/netease/NeteaseMusicProvider.java`、`ui/screen/MusicLibraryScreen.java` 及中英文语言文件；新增 `auth/WindowsDpapiTokenStore.java`、`provider/UnavailableMusicProvider.java`；
  - 新增 `src/test/` 下 AuthManager 与 DPAPI 令牌存储测试；新增独立 `backend/` Spring Boot 认证网关工程、测试、容器配置与部署说明；修改 `CHANGELOG.md`。
- **变更内容**：
  - `AuthManager` 实现 `SIGNED_OUT/AUTHORIZING/SIGNED_IN/REFRESHING/EXPIRED/ERROR` 状态流转、授权轮询、启动恢复、到期前五分钟自动刷新、并发操作失效保护及退出本地清理；登录界面显示状态并使用系统浏览器打开 HTTPS 授权页，用户不向 Mod 输入或提供网易云密码；
  - Mod 仅保存项目网关签发的短期访问令牌和轮换刷新令牌，并使用当前 Windows 用户的 DPAPI 加密后原子写入配置目录；网关地址配置不含密钥，非本机 HTTP 地址被拒绝，响应大小、超时和重定向均受限制；
  - `backend/` 定义授权创建/轮询、会话刷新/撤销和 readiness 边界，`appId/privateKey` 仅允许由后端环境变量注入；未配置经审查的官方适配器时所有认证入口返回不可用，防止公开版本误用逆向私有协议；
  - 验证根工程与后端测试通过，覆盖登录持久化、有效会话恢复、临期刷新、远端撤销失败时的本地退出以及 DPAPI 密文不含明文令牌。

## 2026-08-19 17:38:25 - 新增功能（配置持久化与音量/格式记忆）

- **变更概述**：将方律音量和本地测试音频格式选项持久化到 `config/cubic-cadence.json`，使退出游戏重进后音量保持不变，退出音乐菜单再进入后格式选项保持上次选择。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/config/ModConfig.java`
  - 修改 `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - `ModConfig` 由内存单例扩展为 JSON 持久化配置，使用 Gson 读写 Fabric 配置目录下的 `cubic-cadence.json`，字段包含 `volume`、`hudEnabled`、`audioQuality` 与 `lastTestTrackIndex`；读取时对缺失文件、JSON 损坏、非法枚举值做兜底并回退默认值，不影响游戏启动；
  - 音量与格式选项在变更时立即保存，并在客户端退出时兜底保存；`MusicLibraryScreen` 构造时从配置恢复格式选项，切换格式时写回配置，退出菜单再进入仍保持上次选择；

## 2026-08-19 17:12:37 - 优化代码（移除 M4A/AAC 音频支持）

- **变更概述**：按需求移除用不到的 M4A（含 MP4/AAC）音频支持，仅保留 WAV 与 MP3 解码能力，精简依赖与测试资源。

* **修改文件**：
  - 修改 `build.gradle`
  - 修改 `src/client/java/com/cubiccadence/client/playback/JavaSoundAudioDecoder.java`
  - 修改 `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 删除 `src/client/resources/assets/cubic-cadence/audio/test-audio.m4a`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
* **变更内容**：
  - 从 `build.gradle` 移除 `javasound-aac` 与 `javasound-resloader` 的 implementation 与 include，仅保留 `javasound-mp3` 与 `tritonus-share`；
  - `JavaSoundAudioDecoder` 删除 AAC/MP4 解码分支、AAC 格式魔数识别、AAC 字段与字节序翻转逻辑，`supports()` 仅保留 wav/wave/mpeg/mp3；
  - 移除 `CubicCadenceClient.LOCAL_TEST_AUDIO_M4A` 常量与 M4A 测试资源，音乐界面测试格式切换仅保留 WAV/MP3 两项；
  - 同步更新 `docs/design.md` 的格式说明。

## 2026-08-19 14:59:51 - 修复问题（切换音频格式后仍播放旧格式）

- **变更概述**：修复切换 WAV/MP3/M4A 测试音轨后，实际播放内容仍为首次加载格式的问题；每次播放均用当前解码出的 PCM 重建 OpenAL 缓冲，并在替换或关闭时释放旧缓冲。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - `AudioEngine.installAndPlay(...)` 原逻辑仅在 `soundBuffer` 为空或失效时创建缓冲，导致首次加载后永久复用同一个 OpenAL 缓冲，切换格式后播放内容不变；改为每次都用当前 `decoded` 重新创建 `SoundBuffer`；
  - 替换旧缓冲前调用 `SoundBuffer.discardAlBuffer()` 释放其 OpenAL buffer，避免每次切换累积 OpenAL 缓冲泄漏；
  - `AudioEngine.close()` 同步补充 `discardAlBuffer()`，关闭引擎时释放仍持有的 OpenAL 缓冲，消除退出时的既有泄漏隐患。

## 2026-08-19 14:38:31 - 新增功能（MP3 与 AAC/MP4 音频解码支持）

- **变更概述**：为本地音乐播放链路新增 MP3 与 AAC（含 MP4/M4A 容器）解码能力，使模组除 WAV 外也能播放 MP3 与 MP4/M4A 音频文件；本地测试入口增加 WAV/MP3/M4A 三种格式切换。
- **修改文件**：
  - 修改 `build.gradle`
  - 新增 `src/client/java/com/cubiccadence/client/playback/JavaSoundAudioDecoder.java`
  - 删除 `src/client/java/com/cubiccadence/client/playback/WaveAudioDecoder.java`
  - 修改 `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 新增 `src/client/resources/assets/cubic-cadence/audio/test-audio.mp3`
  - 新增 `src/client/resources/assets/cubic-cadence/audio/test-audio.m4a`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - 引入 `com.tianscar.javasound:javasound-mp3:1.9.8`（LGPL 2.0）与 `com.tianscar.javasound:javasound-aac:0.9.8`（Apache 2.0）及传递依赖 `tritonus-share`、`javasound-resloader`，通过 Loom `include` 以嵌套 jar 形式打进 mod 包；
  - 新增 `JavaSoundAudioDecoder`，按文件魔数识别 WAV/MP3/MP4(AAC)，显式调用对应 JavaSound SPI provider 类完成解码，绕开 Fabric 嵌套 jar 下 `ServiceLoader` 无法稳定发现 SPI 的问题；
  - MP3 经 `MpegFormatConversionProvider` 解码为 16-bit 小端 PCM；AAC/MP4 经 JAAD 解码为 16-bit 大端 PCM 后统一翻转为小端，最终均归一化为 `PCM_SIGNED` 立体声/单声道格式交给 OpenAL；
  - 删除仅支持 WAV 的 `WaveAudioDecoder`，`AudioEngine` 改为使用通用解码器，原有 WAV 播放与进度/seek 逻辑不受影响；
  - 本地测试音乐界面增加格式切换按钮，可在 WAV、MP3、M4A 之间循环切换并播放对应测试资源，便于直接在游戏内验收三种格式。

## 2026-08-19 13:56:51 - 优化代码（加长经验条与媒体图标按钮）

- **变更概述**：加长经验条进度控件，将纯时间数值放到条体正下方居中，并把播放/暂停切换和停止按钮的可见文字替换为常见媒体图标。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - 经验条宽度由原版 182 像素加长为 220 像素，与原版音乐音量和方律音量控件同宽；灰色背景、绿色进度填充、像素拖动点及松开鼠标后单次 seek 的交互保持不变；
  - 经验条整体上移，纯时间数值以 `0:01 / 0:03` 的格式显示在条体正下方居中，并与播放按钮保留 6 像素间距；
  - 播放或继续使用 `▶`，播放中暂停使用 `Ⅱ`，停止使用 `■`，加载中使用 `…`；两个媒体按钮改为 40 像素宽并在进度区域下方居中；
  - 图标按钮通过 Minecraft `Tooltip` 保留“播放、暂停、继续、加载中、停止”的本地化文字说明，避免图标化后丢失操作语义。

## 2026-08-19 13:42:05 - 优化代码（进度拖动延迟提交与右侧时间布局）

- **变更概述**：将音乐进度拖动改为“拖动期间仅预览、释放鼠标后单次跳转”，避免连续 OpenAL seek 产生电音；同时将纯时间数值移动到经验条右侧。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - 鼠标按下和移动期间只更新经验条绿色填充、像素拖动点与目标时间预览，不调用 `AudioEngine.seek(...)`，实际音频继续从原位置正常播放；
  - 鼠标释放时读取最终预览位置并只执行一次 seek，随后重新同步播放时钟和控件值；暂停状态下跳转后仍保持暂停；
  - 键盘方向键调整不属于连续鼠标拖动，继续保持每次按键提交一次跳转；
  - 时间由经验条上方移到右侧，与条体垂直对齐，仅显示 `0:01 / 0:03` 一类数值，不显示“进度”文字；经验条和时间按预留宽度作为整体居中。

## 2026-08-19 13:31:20 - 优化代码（经验条样式音乐进度控件）

- **变更概述**：将音乐进度控件由原版按钮式滑块改为 Minecraft HUD 经验条样式，复用原版灰色经验槽和绿色经验填充，并增加可拖动的像素菱形进度点。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - 直接使用 Minecraft 26.2 内置 `hud/experience_bar_background` 与 `hud/experience_bar_progress` sprite，不新增或复制贴图资源；
  - 保持原版经验条 `182×5` 的视觉尺寸，根据播放比例裁切绿色填充，并在条体上叠加带深色描边的 `7×9` 像素菱形拖动点；
  - 拖动点在悬停、聚焦或拖动时变为亮绿色，禁用时变为灰色；控件保留更高的透明交互区域，改善细经验条的点击和拖动命中；
  - 将时间文本独立显示在经验条上方，播放中和暂停中的拖动跳转、键盘控制和无障碍朗读继续复用已有进度逻辑。

## 2026-08-19 13:23:13 - 新增功能（可拖动音乐进度条）

- **变更概述**：为本地测试音乐增加 Minecraft 原版样式进度条，播放中和暂停中均可拖动跳转，并保持界面时间、内部播放时钟与 OpenAL 实际声源位置同步。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - 新增 `src/client/java/com/cubiccadence/client/mixin/ChannelAccessor.java`
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 修改 `src/client/resources/cubic-cadence.client.mixins.json`
  - 修改 `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - 修改 `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - `AudioEngine.seek(long)` 对跳转位置做边界限制，通过当前 `ChannelHandle` 的声音线程设置 OpenAL `AL_SEC_OFFSET`，暂停状态下跳转后仍保持暂停；
  - 新增 `ChannelAccessor`，仅暴露执行静态缓冲定位所需的 OpenAL source ID，并注册到客户端 Mixin 配置；
  - 音乐界面新增进度滑块，实时显示当前时间与总时长；播放或暂停时启用，未播放、加载中、结束或错误时禁用，拖动期间不会被每帧状态刷新覆盖；
  - 增加中英文进度条文本，并更新技术设计边界：当前静态 PCM/WAV 使用 OpenAL 定位，未来在线流式音频仍需由流式解码器单独实现 seek。

## 2026-08-19 12:24:10 - 新增功能（阶段 2 本地音频播放与原版音乐独立控制）

- **变更概述**：完成阶段 2 本地 WAV 音频播放验证，将方律音乐接入 Minecraft 26.2 现有 `SoundManager`/`SoundEngine`/OpenAL 输出链，并增加播放、暂停、继续、停止、状态/时长、方律独立音量、原版音乐音量及快速禁用原版背景音乐控件；方律音乐不接管 `MusicManager.currentMusic`，可与原版声音并行播放。
- **修改文件**：
  - 修改 `src/client/java/com/cubiccadence/client/CubicCadenceClient.java`
  - 修改 `src/client/java/com/cubiccadence/client/config/ModConfig.java`
  - 修改 `src/client/java/com/cubiccadence/client/playback/AudioDecoder.java`
  - 修改 `src/client/java/com/cubiccadence/client/playback/AudioEngine.java`
  - 新增 `src/client/java/com/cubiccadence/client/playback/DecodedAudio.java`
  - 新增 `src/client/java/com/cubiccadence/client/playback/WaveAudioDecoder.java`
  - 新增 `src/client/java/com/cubiccadence/client/playback/LocalMusicSoundInstance.java`
  - 新增 `src/client/java/com/cubiccadence/client/mixin/CheckboxAccessor.java`
  - 新增 `src/client/java/com/cubiccadence/client/mixin/SoundManagerAccessor.java`
  - 新增 `src/client/java/com/cubiccadence/client/mixin/SoundEngineAccessor.java`
  - 新增 `src/client/java/com/cubiccadence/client/mixin/SoundBufferLibraryAccessor.java`
  - 修改 `src/client/java/com/cubiccadence/client/ui/screen/MusicLibraryScreen.java`
  - 新增 `src/client/resources/cubic-cadence.client.mixins.json`
  - 新增 `src/client/resources/assets/cubic-cadence/audio/test-audio.wav`
  - 修改 `src/client/resources/assets/cubic-cadence/lang/zh_cn.json`
  - 修改 `src/client/resources/assets/cubic-cadence/lang/en_us.json`
  - 修改 `src/main/resources/fabric.mod.json`
  - 修改 `docs/design.md`
  - 修改 `CHANGELOG.md`
- **变更内容**：
  - 在受控单线程执行器中读取并将 WAV 解码为 16-bit 小端 PCM，通过 Mixin Accessor 注入原版 `SoundBufferLibrary`，由 Minecraft 自己的 Channel 与 OpenAL 设备输出；客户端退出时停止实例并关闭解码执行器；
  - `LocalMusicSoundInstance` 使用 `SoundSource.MASTER`、相对监听者和无距离衰减设置，最终音量为“Minecraft 主音量 × 方律音量”，不受原版 `MUSIC` 滑块影响；
  - 音乐界面提供播放/暂停/继续/停止按钮、播放状态和时长、方律独立音量滑块；关闭界面后音乐继续播放；
  - 增加“禁用原版背景音乐”复选框和原版音乐音量滑块：勾选只把原版 `MUSIC` 音量设为 0，不停止或替换 `MusicManager`；取消时恢复最近一次非零值，滑块与复选框双向同步并通过原版 `OptionInstance` 即时刷新；
  - 补充客户端 Mixin 配置、中英文文本和阶段 2 音频共存设计说明；当前本地测试音频约 3 秒，在线长音频和进度跳转留待流式解码阶段。

## 2026-08-19 04:15:25 - 新增功能（第一阶段基础框架重建）

- **变更概述**：工作区被还原至模板状态后，按 `docs/design.md` 第 9 节重新落地第一阶段 Fabric 项目骨架，并一并在落地时包含此前确认的三项 UI 修复，实现可编译、可运行、按 `M` 键打开/关闭音乐主界面的纯客户端 Mod，业务逻辑以 `TODO` 占位。
- **修改文件**：
  - 删除 `src/main/java/name/modid/` 与 `src/client/java/name/modid/` 整树（模板残留，含旧 main 入口与两个示例 Mixin）
  - 删除 `src/main/resources/cubic-cadence.mixins.json`、`src/client/resources/cubic-cadence.client.mixins.json`
  - 新增 `src/main/java/com/cubiccadence/model/` 下 10 个文件（5 个 record + 5 个 enum）
  - 新增 `src/main/java/com/cubiccadence/provider/` 下 6 个文件（MusicProvider 接口 + PageRequest/SearchPage/PlaylistPage record + SearchType/AudioQuality enum）
  - 新增 `src/main/java/com/cubiccadence/auth/` 下 2 个文件（AuthSession record + AuthState enum）
  - 新增 `src/client/java/com/cubiccadence/client/` 下 15 个文件（入口、config、auth、provider/netease、playback、sync、cache、ui/screen）
  - 修改 `gradle.properties`：`group` 由 `name.modid` 改为 `com.cubiccadence`
  - 修改 `src/main/resources/fabric.mod.json`：`environment` 改为 `client`，移除 `main` 入口与 Mixin 配置，`client` 入口改为 `com.cubiccadence.client.CubicCadenceClient`
- **变更内容**：
  - 采用 split source set + 纯客户端结构：`src/main` 承载 model/provider/auth 跨层契约，`src/client` 承载客户端实现；
  - 依据 Minecraft 26.2 反编译源码对齐 API：`Screen` 使用 `extractRenderState`/`extractBackground` 渲染、`isPauseScreen`/`onClose` 替代旧版 `shouldPause`/`close`，按键使用 `KeyMapping` + `KeyMappingHelper.registerKeyMapping`，界面切换使用 `Minecraft.setScreenAndShow`/`Gui.screen`；
  - `MusicLibraryScreen` 使用 `Screen` 默认背景（游戏画面模糊 + 半透明菜单遮罩），标题颜色使用 ARGB 不透明白 `0xFFFFFFFF`（`0xFFFFFF` 会因 alpha=0 被 `text()` 跳过），并在 `keyPressed(KeyEvent)` 中用 `openLibraryKey.matches(event)` 检测 `M` 键关闭界面；
  - 验证：`gradlew build` 成功，`compileJava` 与 `compileClientJava` 均通过，产物 `build/libs/cubic-cadence-1.0.0.jar` 包含客户端入口与全部契约类。

## 2026-08-19 03:11:16 - 新增功能（技术设计文档）

- **变更概述**：新增 `docs/design.md`，明确第一阶段 Fabric 项目骨架的包结构、领域模型、接口契约、模块职责、线程规则与 UI 素材方案。
- **修改文件**：
  - `docs/design.md`（新增）
  - `docs/minecraft-fabric-music-mod-development-document.md`（迁入）
  - `docs/netease-cloud-music-api-integration-requirements.md`（迁入）
  - `CHANGELOG.md`
- **变更内容**：
  - 将 `docs` 目录从工作区根目录迁入本工程；
  - 固定技术栈基线：Minecraft 26.2、Fabric Loader 0.19.3、Fabric API 0.158.0+26.2、Loom 1.17-SNAPSHOT、JDK 25、默认 Yarn 映射；
  - 采用 split source set + 纯客户端结构，`src/main` 承载 model/provider/auth 契约，`src/client` 承载客户端实现；
  - 逐类定义领域模型、Provider 接口、认证、播放、同步、缓存与 UI 类的字段和方法签名；
  - 确定 UI 直接引用 Minecraft 原版 `assets/minecraft/textures/gui/` 素材，不打包自定义贴图。

## 2026-08-19 01:17:31 - 修复问题

- **变更概述**：修复 IntelliJ IDEA 缺少 `Minecraft Client` / `Minecraft Server` 启动配置的问题。
- **修改文件**：
  - `.idea/gradle.xml`
  - `.idea/runConfigurations/Minecraft_Client.xml`（由 `ideaSyncTask` 生成）
  - `.idea/runConfigurations/Minecraft_Server.xml`（由 `ideaSyncTask` 生成）
- **变更内容**：
  - 将 `.idea/gradle.xml` 的 `gradleJvm` 从 `#GRADLE_JAVA_HOME`（继承系统 JDK 17）改为 `25`（IDEA 已注册的 JDK 25 SDK，指向 `D:/Java`），使 IDEA 的 Gradle 集成使用 JDK 25，满足 Fabric Loom 的 JVM 21+ 要求。
  - 运行 `gradlew ideaSyncTask` 生成 Loom 的 IDEA 运行配置，得到 `Minecraft Client`（模块 `cubic-cadence.client`）与 `Minecraft Server`（模块 `cubic-cadence.main`）两个启动配置。

## 2026-08-19 00:52:59 - 修复问题

- **变更概述**：修复 Gradle 构建失败（`fabric-loom` 要求 JVM 21+，但构建使用了 Java 17）。
- **修改文件**：
  - `gradle.properties`
- **变更内容**：
  - 在 `gradle.properties` 末尾新增 `org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot`，让 Gradle wrapper 使用已安装的 JDK 25 启动 daemon，满足 Fabric Loom 1.17-SNAPSHOT 的 JVM 21+ 运行时要求，同时保持系统级 `JAVA_HOME`（JDK 17）不变，避免影响其它项目。
