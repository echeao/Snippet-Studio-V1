# 十项需求可行性分析与实施方案

## 总体评估

所有 10 项需求在现有架构下均可实现，无需引入新的第三方库（除需求 10 可选引入模糊搜索库外）。项目采用 Jetpack Compose + MVVM + Navigation Compose + JGit 架构，扩展性良好。

---

## 需求 1: 分享文件临时预览（不保存）

**可行性**: 高 | **难度**: 中 | **优先级**: 高

**现状分析**:
- 当前 `MainActivity.handleShareIntent()` 接收文件后，走 `SharePanel`（编辑后保存）或静默保存两条路径，没有"仅预览不保存"的选项。
- `SharedFileHandler` 已能解析文件名、内容、推断类型。

**实施方案**:
1. 新增路由 `Screen.FilePreview`：`"preview_temp?fileName={fileName}&type={type}"`，通过 Navigation 参数传递内容（或内存缓存）。
2. 新增 `TempFilePreviewScreen.kt`：只读展示，复用 `CodeEditor`（设 `readOnly=true` 参数）或直接用 `SelectionContainer + Text` 展示高亮代码。
3. 在 `MainActivity` 的 `SharePanel` 中增加"仅预览"按钮；或在 `shareAction` 设置中新增 `"preview"` 模式。
4. 退出时不做任何持久化操作。

**涉及文件**:
- `ui/nav/Screen.kt` — 新增路由
- `ui/nav/AppNavGraph.kt` — 注册路由
- 新建 `ui/preview/TempFilePreviewScreen.kt`
- `MainActivity.kt` — 分享意图消费逻辑调整
- `ui/components/SharePanel.kt` — 增加"仅预览"入口

---

## 需求 2: Git 变更文件显示与 Diff 查看

**可行性**: 高 | **难度**: 中高 | **优先级**: 高

**现状分析**:
- `GitManager` 已有 JGit 集成（clone/push/pull/fetch/log），但缺少 `git.status()` 和 `git.diff()` 的封装。
- `SyncEngine` 已实现 Pull/Push 预览对比逻辑，`SyncPreviewSheet` 已有变更列表 UI。
- `model/SyncPreview.kt` 中 `SyncChangeItem` 已定义 ADDED/UPDATED/DELETED 类型。

**实施方案**:
1. `GitManager` 新增方法：
   - `getLocalStatus(): Result<GitStatusInfo>` — 调用 `git.status().call()` 获取 modified/added/untracked/deleted 文件列表。
   - `getFileDiff(filePath: String): Result<List<DiffLine>>` — 调用 JGit `DiffFormatter` 生成行级 diff。
2. 新增数据模型 `GitStatusInfo`（changedFiles, untrackedFiles, addedFiles, deletedFiles）。
3. 在 `SubPageScreen` 的 `"git"` 子页面中，连接成功后新增"本地变更"区块：
   - 显示变更文件列表（带状态图标：M/A/D）
   - 点击文件展开行级 Diff 视图（绿色新增行/红色删除行）
4. 可选：新增独立路由 `"gitstatus"` 作为全屏变更查看页。

**涉及文件**:
- `data/git/GitManager.kt` — 新增 status/diff 方法
- `model/` — 新增 `GitStatusInfo.kt` 数据模型
- `ui/subpage/SubPageScreen.kt` — Git 页面增加变更区块
- `ui/subpage/SubPageViewModel.kt` — 增加 loadLocalStatus 逻辑
- 可选新建 `ui/components/DiffViewer.kt` — 行级 Diff 渲染组件

---

## 需求 3: 编辑页底部状态栏与手势横条重叠

**可行性**: 高 | **难度**: 低 | **优先级**: 高（体验缺陷修复）

**现状分析**:
- `EditorScreen.kt` 第 496-534 行，`bottomBar` 的 `Surface` 固定高度 36.dp，未添加 `navigationBarsPadding()` 或 `WindowInsets.navigationBars` 适配。
- 项目已启用 `enableEdgeToEdge()`，手势导航条会覆盖在内容之上。

**实施方案**:
在 `bottomBar` 的 `Surface` 上添加底部安全区内边距：
```kotlin
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        .height(36.dp)
        .border(1.dp, tc.line),
    color = tc.surface2
) { ... }
```
或在外层包裹 `Modifier.navigationBarsPadding()`。

**涉及文件**:
- `ui/editor/EditorScreen.kt` — bottomBar 区域（约第 498 行）

---

## 需求 4: 编辑区/预览区双指缩放

**可行性**: 中高 | **难度**: 中 | **优先级**: 中

**现状分析**:
- 编辑器已有 A-/A+ 按钮调整字号（11-22sp 范围），但无手势缩放。
- `CodeEditor` 使用 `BasicTextField` + `verticalScroll`，直接在 Compose 层做 transform 缩放会影响文本交互。
- `RunPreview` 中 WebView 默认未启用缩放。

**实施方案**:

**方案 A（推荐 - 编辑器）**: 双指捏合调整 fontSp（而非视觉缩放）
- 在 `CodeEditor` 外层添加 `Modifier.pointerInput` 检测 `detectTransformGestures`
- 将缩放比例映射为 fontSp 增减（限制 11-22sp）
- 优点：不影响文本选择/光标定位，体验自然

**方案 B（预览区 WebView）**:
- HTML/JS/Markdown 预览的 WebView 启用 `settings.setSupportZoom(true)` + `settings.builtInZoomControls = true` + `settings.displayZoomControls = false`
- Prompt 预览（纯 Text）可用 `graphicsLayer(scaleX, scaleY)` + `pointerInput` 实现

**涉及文件**:
- `ui/components/CodeEditor.kt` — 添加手势检测层
- `ui/components/RunPreview.kt` — WebView 启用缩放
- `ui/editor/EditorViewModel.kt` — 可选：暴露连续 fontSp 调整方法

---

## 需求 5: 文件类型标识冗余优化

**可行性**: 高 | **难度**: 低 | **优先级**: 中

**现状分析**:
当前编辑页有 3 处类型标识：
1. 顶部控制条的 `SegmentedControl`（Code/Preview 切换）— 非类型标识，保留
2. 底部状态栏右侧的 `type.displayName` Badge（可点击切换类型）— 与菜单重复
3. 菜单中"切换语言类型"选项 — 功能入口

**实施方案**:
- 保留底部状态栏的类型 Badge 作为唯一可见标识 + 快捷切换入口（点击弹出类型选择对话框）
- 从 DropdownMenu 中移除"切换语言类型"菜单项（或保留为辅助入口但降低视觉权重）
- 可选：在顶部控制条右侧仅保留 A-/A+ 和全屏按钮，使布局更简洁

**涉及文件**:
- `ui/editor/EditorScreen.kt` — 调整菜单项与底部栏逻辑

---

## 需求 6: 新建文件类型扩展与自由编辑模式

**可行性**: 中高 | **难度**: 中高 | **优先级**: 高

**现状分析**:
- `SnippetType` 枚举仅 4 种：HTML, JS, MARKDOWN, PROMPT
- 新建时必须选择类型，通过 `Screen.Editor.new(type)` 路由传入
- `SyntaxLanguageDetector` 已能根据文件名推断语法语言
- `SharedFileHandler.SUPPORTED_EXTENSIONS` 已定义了 40+ 种扩展名白名单

**实施方案（分阶段）**:

**阶段 1 - 扩展 SnippetType**:
- 将 `SnippetType` 从 4 种扩展为更通用的分类，或新增 `GENERAL("general", "通用文本", ".txt")` 类型
- 首页/文件页新增第 5 个"自由文本"快捷入口

**阶段 2 - 自由编辑 + 自动检测**:
- 新增"快速编辑"入口，直接进入编辑器（默认 GENERAL 类型，空内容）
- 利用现有 `SyntaxLanguageDetector` + `MainActivity.detectShareType()` 的逻辑，在用户输入内容后自动推断语言类型
- 在 `EditorViewModel.onTextFieldValueChange()` 中加入自动检测逻辑（仅当类型为 GENERAL 时触发）

**阶段 3 - 退出时命名提示**:
- 在 `handleBackWithCleanup()` 中，若为新建且标题未修改，弹出命名对话框
- 用户可输入文件名（含扩展名），系统自动匹配类型
- 不输入则默认 `untitled.txt`

**阶段 4 - 手动选择语言后自动应用扩展名**:
- 在 `setSnippetType()` 中，同步更新 fileName 的扩展名

**涉及文件**:
- `model/Snippet.kt` — SnippetType 枚举扩展
- `ui/home/HomeScreen.kt` — 新增"自由编辑"入口卡片
- `ui/editor/EditorViewModel.kt` — 自动检测 + 退出命名逻辑
- `ui/editor/EditorScreen.kt` — 退出时命名对话框
- `util/SyntaxLanguageDetector.kt` — 增强内容检测能力

---

## 需求 7: 键盘弹出后内容被遮挡

**可行性**: 高 | **难度**: 低中 | **优先级**: 高（体验缺陷修复）

**现状分析**:
- `EditorScreen` 的 Scaffold 未显式处理 IME insets
- `CodeEditor` 使用 `verticalScroll`，但键盘弹出后可视区域缩小，底部内容可能被遮挡
- 项目已启用 `enableEdgeToEdge()`，需要手动处理 IME 内边距

**实施方案**:
1. 在 `EditorScreen` 的 `editorContent` 容器上添加 `Modifier.imePadding()`（Compose Material3 提供）
2. 或在 Scaffold 层级设置 `contentWindowInsets = WindowInsets.ime`
3. 确保 `CodeEditor` 的 `verticalScroll` 区域在键盘弹出后仍能滚动到最后一行
4. 全屏模式下同样需要处理（虽然全屏通常隐藏导航栏，但键盘仍需适配）

**涉及文件**:
- `ui/editor/EditorScreen.kt` — editorContent 容器添加 imePadding
- `ui/components/CodeEditor.kt` — 可选：底部增加额外 padding 确保最后一行可见

---

## 需求 8: 编辑区字体自定义

**可行性**: 中 | **难度**: 中 | **优先级**: 低

**现状分析**:
- `CodeEditor.kt` 硬编码 `FontFamily.Monospace`
- `AppSettings` 中无字体相关字段
- Android 系统自带等宽字体有限（monospace, sans-serif, serif）

**实施方案**:
1. `AppSettings` 新增 `editorFontFamily: String = "monospace"` 字段
2. 预置字体选项：
   - `monospace`（系统等宽，默认）
   - `sans-serif`（无衬线）
   - `serif`（衬线）
   - 可选：打包 1-2 个编程字体（如 JetBrains Mono）到 `res/font/`，但会增加 APK 体积（约 200-500KB/字体）
3. `CodeEditor` 接收 `fontFamily: FontFamily` 参数，替代硬编码
4. 在 `EditorSettingsContent` 设置面板中增加字体选择 UI（FilterChip 或下拉）
5. `SettingsDataStore` 持久化字体选择

**涉及文件**:
- `model/AppSettings.kt` — 新增字段
- `data/local/SettingsDataStore.kt` — 持久化
- `ui/components/CodeEditor.kt` — 参数化字体
- `ui/editor/EditorScreen.kt` — 设置面板增加字体选项
- `ui/editor/EditorViewModel.kt` — 传递字体设置
- 可选：`res/font/` — 打包自定义字体文件

---

## 需求 9: 列表页上滑隐藏搜索栏

**可行性**: 高 | **难度**: 中 | **优先级**: 中

**现状分析**:
- `HomeScreen` 中 SearchBar 位于 `Column` 内、`LazyColumn` 之前，始终可见
- `FilesScreen` 结构类似
- 项目已有 `NestedScrollConnection` 使用经验（EditorScreen 全屏模式）

**实施方案**:

**方案 A（推荐 - 简洁）**: 将 SearchBar 放入 LazyColumn 的 `item {}` 中作为首项
- 搜索栏随列表自然滚动消失
- 缺点：滚动回顶部才能再次搜索

**方案 B（更优体验）**: 使用 `NestedScrollConnection` + `AnimatedVisibility`
- 检测滚动方向：上滑 → 隐藏搜索栏（slideOutVertically）；下滑 → 显示
- 类似 Material Design 的 `ScrollBehavior` 效果
- HomeScreen 和 FilesScreen 均需实现

**方案 C（Material3 原生）**: 使用 `TopAppBarScrollBehavior`
- 将 SearchBar 集成到 TopAppBar 的 `collapsed`/`expanded` 状态中
- 利用 `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`

推荐方案 B，兼顾体验与实现复杂度。

**涉及文件**:
- `ui/home/HomeScreen.kt` — 搜索栏隐显动画
- `ui/files/FilesScreen.kt` — 同上
- 可选：抽取公共 `CollapsibleSearchBar` 组件

---

## 需求 10: 搜索栏模糊关键词匹配

**可行性**: 高 | **难度**: 低中 | **优先级**: 中

**现状分析**:
- 当前搜索逻辑（HomeViewModel 第 80-84 行，FilesViewModel 第 120-126 行）：
  ```kotlin
  it.title.contains(query, ignoreCase = true) ||
  it.content.contains(query, ignoreCase = true) ||
  it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
  ```
- 仅支持连续子串匹配，不支持分词、拼音、容错

**实施方案（渐进式）**:

**Level 1 - 多关键词分词匹配（推荐首选）**:
- 将查询按空格拆分为多个关键词
- 每个关键词独立匹配 title/content/tags
- 所有关键词均命中才算匹配（AND 逻辑）
- 示例：输入 "html div" 可匹配含 "html" 和 "div" 的片段

**Level 2 - 子序列模糊匹配**:
- 实现轻量级子序列匹配算法（字符按顺序出现即可，不要求连续）
- 示例：输入 "hml" 可匹配 "html"
- 无需第三方库，约 20 行 Kotlin 代码

**Level 3 - 可选引入第三方库**:
- 如 `com.github.androidquery` 或自实现 Levenshtein 距离容错
- 考虑性能：片段数量通常 < 1000，内存计算完全可行

**涉及文件**:
- 新建 `util/FuzzySearchUtil.kt` — 模糊匹配工具类
- `ui/home/HomeViewModel.kt` — 替换过滤逻辑
- `ui/files/FilesViewModel.kt` — 替换过滤逻辑

---

## 优先级与工作量排序

| 优先级 | 需求 | 预估工作量 | 理由 |
|--------|------|-----------|------|
| P0 | 3 - 底部状态栏重叠 | 0.5h | 明显 UI 缺陷，一行代码修复 |
| P0 | 7 - 键盘遮挡 | 1-2h | 影响基本编辑体验 |
| P1 | 1 - 分享文件临时预览 | 3-4h | 核心功能缺口 |
| P1 | 6 - 新建文件类型扩展 | 4-6h | 影响日常使用效率 |
| P1 | 2 - Git 变更文件显示 | 5-7h | Git 工作流核心需求 |
| P2 | 5 - 类型标识优化 | 1h | 简单 UI 调整 |
| P2 | 10 - 模糊搜索 | 2-3h | 提升搜索体验 |
| P2 | 9 - 上滑隐藏搜索栏 | 2-3h | 体验优化 |
| P3 | 4 - 双指缩放 | 3-4h | 锦上添花 |
| P3 | 8 - 字体自定义 | 3-4h | 个性化需求 |

---

## 技术风险点

1. **需求 4（双指缩放）**: 在 `BasicTextField` 上叠加手势检测可能与文本选择/光标拖拽冲突，需要仔细处理手势优先级（建议：仅在双指时触发缩放，单指保持原有编辑行为）。
2. **需求 6（类型扩展）**: `SnippetType` 枚举扩展会影响数据库已有数据的兼容性，需确保 `fromCode()` 的 fallback 逻辑健壮；`SyntaxHighlighter` 对新类型的高亮支持需同步跟进。
3. **需求 2（Git Diff）**: JGit 的 `DiffFormatter` 在 Android 上的性能需关注，大文件 diff 应在 IO 线程执行并设置超时。
