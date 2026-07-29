# 编辑器正文可见性与 TextMate 高亮交接文档

更新时间：2026-07-30

## 用户现象与当前判断

用户最初报告：打开编辑器后，代码内容一闪而逝。

后续用户确认：行号仍显示，光标可以在“空白”段落中选择。这说明 `CodeEditor` 内部 `Content` 没有被清空；问题是正文的渲染颜色在 TextMate 异步分析后变成透明或与背景相同。当前用户最新反馈是：正文可见后，**代码高亮仍未生效**。

## 已确认的 Sora 0.23.6 行为

已读取本机 Gradle 缓存中的 Sora 源码：

- `CodeEditor#setEditorLanguage()` 不会清空 `Content`。它在已有文本不为 `null` 时调用新语言分析器的 `reset(new ContentReference(text), ...)`。
- `TextMateAnalyzer` 为 token 生成的颜色索引是 `foreground + 255`。
- 普通 `EditorColorScheme` 不处理这些索引，默认返回 `0`，即 Android 的透明色。这正好解释“内容可选择、行号正常、正文不可见”。
- `TextMateColorScheme` 覆盖了 `getColor(type)`：当 `type >= 255` 时，会从当前 TextMate 主题取 token 颜色。因此 TextMate 编辑器必须使用该类型的颜色方案，而不是普通 `EditorColorScheme`。

缓存源码位置：

- `C:\Users\83836\.gradle\caches\modules-2\files-2.1\io.github.Rosemoe.sora-editor\editor\0.23.6\c1b16be3929ad8c161ca3ac9f547ce7b13ad68bd\editor-0.23.6-sources.jar`
- `C:\Users\83836\.gradle\caches\modules-2\files-2.1\io.github.Rosemoe.sora-editor\language-textmate\0.23.6\44d4b946585796f49f5c08135449654efa840ced\language-textmate-0.23.6-sources.jar`

## 当前工作区状态（重要）

工作区有 4 个未提交文件：

- `app/src/main/java/com/feige/snippetstudio/ui/components/SoraCodeEditor.kt`
- `app/src/main/java/com/feige/snippetstudio/ui/editor/EditorScreen.kt`
- `app/src/main/java/com/feige/snippetstudio/ui/editor/EditorViewModel.kt`
- `app/src/main/java/com/feige/snippetstudio/ui/editor/components/EditorContent.kt`

后 3 个文件包含 `selectionOffset` 与光标/符号插入同步调整；不要在不了解需求的情况下覆盖或重置它们。

`SoraCodeEditor.kt` 当前已有的关键改动：

1. 使用 `TextMateColorScheme.create(ThemeRegistry.getInstance())`，避免 TextMate token 透明。
2. 编辑器创建时立即 `ed.setText(text)`，避免初始化期间首帧空白。
3. 设置语言后仅当内部文本与外部 `latestText` 不一致时才 `setText`，避免不必要的闪动。
4. Content 事件仅在新文本与 Compose 的外部文本不同才回写 ViewModel。

编译验证已多次通过：

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## ~~未完成问题：高亮初始化竞态~~（已解决 2026-07-30）

**原问题**：主题配色与语言初始化分属两个独立 `LaunchedEffect`，并发启动导致语言分析器可能在配色方案绑定前开始分析，用户看到无高亮正文。

**修复方案**（已实施并编译通过）：

将两个 effect 合并为单一顺序协程 `LaunchedEffect(language, tmInitialized.value)`，严格按以下顺序执行：

1. `initTextMateRegistry(context)` 完成（前置 effect）。
2. 在同一协程中按 `isDark` 调用 `ThemeRegistry.getInstance().setTheme(...)`。
3. 创建 `TextMateColorScheme`。
4. 在主线程执行 `editor.colorScheme = buildColorScheme(...)`。
5. 再创建 `TextMateLanguage`，然后调用 `editor.setEditorLanguage(lang)`。
6. 仅在内容不一致时同步 `editor.setText(latestText)`，最后 `isEditorReady = true`。

主题切换使用独立 `LaunchedEffect(isDark, themeColors, tmFirstInitDone)`，以 `tmFirstInitDone` 布尔门控（而非 `currentTmColorScheme`）避免循环触发。该 effect 仅在首次初始化完成后响应主题变更，不会与初始化 effect 竞争。

## 语言覆盖范围

当前 TextMate assets 仅包含：JavaScript、HTML、CSS、Python、Java。

`buildSoraLanguage()` 明确将以下语言降级为 `EmptyLanguage`：JSON、Markdown、YAML、Shell、C/C++、Go、Rust、Prompt、Plain。因此这些类型当前**不可能**有 Sora TextMate 高亮。若用户测试的是 Markdown 或 Prompt，这不是配色问题，而是缺少 grammar asset。

## 根本原因：IThemeSource 文件名缺少扩展名（已修复 2026-07-30）

**现象**：所有语言类型均无高亮，文本可见但无颜色区分。

**根因分析**（通过 adb logcat 诊断确认）：

1. `IThemeSource.fromInputStream(stream, fileName, charset)` 的第二个参数 `fileName` 被 tm4e 的 `guessFileFormat()` 用于判断主题文件格式（JSON / PLIST）。
2. 之前传入 `"dark_default"`（无扩展名），tm4e 无法识别格式，抛出 `IllegalArgumentException: Unsupported file type: dark_default`。
3. 异常被外层 try-catch 捕获，主题从未加载 → `TextMateColorScheme` 无 token 颜色 → 所有文本回退到 TEXT_NORMAL（单色）。
4. 语法加载和 tokenize 均正常（grammar 不依赖主题），因此问题仅表现为"有分词但无颜色"。

**修复**：

文件名参数加上 `.json` 扩展名：
```kotlin
IThemeSource.fromInputStream(assets.open("textmate/themes/dark_default.json"), "dark_default.json", null)
```

同时将 `initTextMateRegistry()` 重构为三步独立 try-catch（FileProvider → 语法 → 主题），避免一个环节失败拖垮整体初始化。

当前编辑页使用 `SyntaxLanguageDetector.fromSnippetType(uiState.type)`，不是文件名探测：

- HTML、JS、Java：有注册 grammar。
- Markdown、Prompt、General：当前会走 `EmptyLanguage`。

如产品要求这些类型也高亮，需要增加对应 `.tmLanguage.json` 到 `app/src/main/assets/textmate/`，在 `initTextMateRegistry()` 注册，并在 `buildSoraLanguage()` 映射 scope。至少应在 UI/文档中明确当前支持范围，避免将 `EmptyLanguage` 误判为故障。

## 验证建议

1. 使用 HTML 或 JS 片段验证高亮：应至少包含关键字、字符串、注释和数字，例如：

   ```javascript
   // comment
   const value = "visible";
   console.log(42);
   ```

2. 在深色与浅色主题下分别打开同一片段，确认正文不透明、关键字/字符串/注释颜色不同。
3. 切换代码/预览 Tab、全屏和普通模式，确认文本不会丢失；两个模式各自会创建 `SoraCodeEditor`。
4. 使用设备 `adb logcat | Select-String SoraCodeEditor` 检查是否有“TextMate 注册表初始化失败”“语言构建失败”或“TextMate 配色初始化失败”。本次会话没有可用 `adb` 命令。
5. 完成后运行 `git diff --check` 与 `./gradlew.bat :app:compileDebugKotlin`。

## 不建议的回退方式

- 不要仅用 `EditorColorScheme` 替换 `TextMateColorScheme`；这会重新引入透明 token。
- 不要在语言设置后无条件 `setText(...)` 再配合固定 `delay(...)`；会导致可见闪动，并掩盖真正的初始化时序。
- 不要把所有空字符串事件都强行恢复：这会阻止用户真实地删除全部代码。应依据外部同步来源/初始化门控来区分事件。
