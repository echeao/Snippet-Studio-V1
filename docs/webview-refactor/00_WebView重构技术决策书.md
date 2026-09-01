# Snippet Studio V1 — WebView 重构技术决策书

| 项目 | 内容 |
|------|------|
| 产品名称 | Snippet Studio |
| 重构版本 | V2.0（WebView UI 重构） |
| 文档版本 | 1.0 |
| 编写日期 | 2026-08-08 |
| 文档性质 | 技术决策书（重构任务最高优先级约束文件） |

> ⚠️ 本文档中所有决策均为**已拍板的硬性约束**，执行重构的 AI 不得擅自变更。
> 若发现决策存在技术不可行点，必须先上报说明，不得自行替换方案。

---

## 1. 重构目标与范围

### 1.1 目标
将 Snippet Studio 的 **UI 层**从 Jetpack Compose 全量迁移为 **WebView + React** 实现，
做到**视觉还原 + 功能等价**；**数据层、Git 层、文件系统层保持 Kotlin 原生实现不变**。

### 1.2 范围内（必须重做）
- 全部页面的 UI 渲染（首页 / 文件页 / 编辑器 / 详情 / 设置 / 子页面 / Git 历史 / 临时预览）
- 全部对话框、BottomSheet、Snackbar、悬浮控件等交互组件
- 主题系统（6 种配色 × 明暗模式）在 Web 端的等价实现
- 多语言（zh / en / ja）在 Web 端的等价实现
- 语法高亮编辑器（Sora Editor → CodeMirror 6）
- HTML / JS / Markdown 预览在 Web 端的实现

### 1.3 范围外（严禁改动）
- Room 数据库（表结构、DAO、迁移脚本）
- DataStore 设置存储（键名与格式）
- JGit 相关类：`GitManager`、`SyncEngine` 的核心逻辑
- SAF / 内部存储文件操作：`LocalFileManager`
- `SnippetRepository` 业务编排逻辑
- 应用包名 `com.feige.snippetstudio`、数据库文件名、DataStore 文件名
- `assets/templates/` 样板模板的内容

### 1.4 验收总原则
重构后应用必须通过 `docs/delivery/01_产品功能规格说明书.md` 中全部功能条目的逐项验收，
且老用户覆盖安装后**数据零丢失、设置零丢失**。

---

## 2. 总体架构决策

### 2.1 架构形态：Android 壳 + 单 WebView + JSBridge

```
┌─────────────────────────────────────────────┐
│                Android 壳 (Kotlin)            │
│  MainActivity：单 Activity，承载 WebView      │
│  BridgeService：@JavascriptInterface 网关     │
│  ┌───────────────────────────────────────┐  │
│  │  原生保留层（不重构）                    │  │
│  │  SnippetRepository / GitManager /      │  │
│  │  SyncEngine / LocalFileManager /       │  │
│  │  Room / DataStore / Exporter /         │  │
│  │  SharedFileHandler                     │  │
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │  WebView                               │  │
│  │  React SPA（本地 assets 加载）          │  │
│  │  UI 渲染 / 路由 / 编辑器 / 预览         │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

**决策依据：**
- JGit 无成熟 JS 等价实现（isomorphic-git 不支持本项目所需的完整 pull/push/状态对比能力），Git 必须留在原生侧；
- SAF（Storage Access Framework）仅能通过 Android API 访问，必须留在原生侧；
- Room/DataStore 保留可保证老用户无缝升级与数据可回退。

### 2.2 数据流方向
- **JS → Native**：通过 `window.SnippetStudio.invoke(json)` 发起请求（请求-响应模式）；
- **Native → JS**：通过 `webView.evaluateJavascript("window.onNativeEvent(...)")` 推送事件（订阅模式、进度通知、分享进入等）；
- 详细协议见 `01_JSBridge接口契约文档.md`。

---

## 3. 前端技术栈决策

| 维度 | 决策 | 说明 |
|------|------|------|
| 框架 | **React 18 + TypeScript** | 函数组件 + Hooks，禁止 class 组件 |
| 构建 | **Vite 5+** | 产物输出到 `app/src/main/assets/web/` |
| 路由 | **React Router（HashRouter）** | WebView 本地 file:// 协议下必须用 Hash 路由 |
| 状态管理 | **Zustand** | 轻量；禁止引入 Redux |
| 样式方案 | **CSS Variables 设计令牌 + CSS Modules（或原生 CSS）** | 禁止引入大型组件库（antd / MUI），保证与原 Compose 视觉一致 |
| 代码编辑器 | **CodeMirror 6** | 替换 Sora Editor；需支持行号、自动换行、括号配对、Tab 缩进、当前行高亮、字号配置 |
| Markdown 渲染 | **markdown-it + highlight.js** | 替换原生 `MarkdownRenderer` |
| HTML/JS 预览 | **sandboxed iframe（srcDoc）** | 在 Web 端自行实现，不需要原生参与 |
| i18n | 自建轻量 JSON 词条方案 | 词条来源：`res/values*/strings.xml`，转为 `locales/{zh,en,ja}.json` |
| 图标 | SVG sprite | 从 `res/drawable/ic_*.xml`（VectorDrawable）转换为 SVG |

### 3.1 编辑器能力对照（Sora Editor → CodeMirror 6）

| 原能力（SoraCodeEditor.kt） | CodeMirror 6 对应方案 |
|---|---|
| 语法高亮 | `@codemirror/lang-*` 语言包（html/javascript/markdown/python/java/css/json） |
| 行号 | `lineNumbers()` |
| 自动换行 | `EditorView.lineWrapping` |
| 括号配对 | `closeBrackets()` |
| Tab 缩进（可配置宽度） | `indentUnit` + `EditorState.tabSize` |
| 当前行高亮 | `highlightActiveLine()` |
| 字号/字体族设置 | `EditorView.theme` 动态注入 |
| 明暗主题 | 自定义 theme 映射到设计令牌色板（`codeBg/codeText`） |
| 符号快捷输入栏（SymbolBar） | Web 端自绘工具条，调用 `view.dispatch` 插入 |

---

## 4. 原生壳工程决策

### 4.1 WebView 配置硬性要求
- `javaScriptEnabled = true`
- `domStorageEnabled = true`
- 拦截 `file://` 本地资源加载，禁止任何外部网络请求加载 UI 资源
- 禁用缩放（`setSupportZoom(false)`），由 Web 端控制 viewport
- `WebViewClient` 拦截所有外链跳转（`shouldOverrideUrlLoading` 返回 true，外链用系统浏览器打开）
- 处理返回键：优先由 Web 端路由消费（通过 Bridge 查询/通知），再退出应用

### 4.2 资源加载
- 入口：`file:///android_asset/web/index.html`
- 前端构建产物（含 hash 文件名）整体复制到 `assets/web/`
- 构建集成：Gradle task 在 `preBuild` 前执行 `npm run build` 并拷贝产物（允许手动拷贝，但必须在文档中说明步骤）

### 4.3 原生壳新增代码边界
只允许新增/修改以下原生文件：
- `MainActivity.kt`：改为承载 WebView（保留分享 Intent 处理、生命周期同步逻辑）
- 新增 `bridge/JsBridge.kt`：JS 网关（接口清单见契约文档）
- 新增 `bridge/BridgeEventBus.kt`：Native → JS 事件推送
- 删除：`ui/` 目录下全部 Compose 页面与组件（在阶段 6 统一删除，前期保留作为对照）

---

## 5. 兼容性与性能要求

| 项 | 要求 |
|---|---|
| 最低 Android 版本 | API 30（Android 11），与原应用一致 |
| WebView 最低要求 | 系统 WebView 90+；低于该版本时提示用户升级 WebView |
| 首屏可交互时间 | 冷启动 ≤ 1.5s（中端机型） |
| 列表滚动 | 200 条片段列表滚动无明显掉帧（考虑虚拟滚动 react-window） |
| 包体增量 | 前端产物 gzip 后 ≤ 2MB |
| 横竖屏 | 均支持，布局自适应 |

---

## 6. 工程目录规划

```
Snippet-Studio-V1/
├── app/src/main/
│   ├── assets/web/            # 前端构建产物（由 web-src 构建生成，git 可忽略源）
│   └── java/com/feige/snippetstudio/
│       ├── bridge/            # 【新增】JSBridge 网关
│       ├── data/              # 【保留不动】
│       ├── util/              # 【保留不动】（部分工具类被 Bridge 调用）
│       └── MainActivity.kt    # 【改造】WebView 容器
├── web-src/                   # 【新增】React 前端源码
│   ├── src/
│   │   ├── pages/             # 页面对应原 ui/ 各包
│   │   ├── components/        # 对应原 ui/components/
│   │   ├── bridge/            # JSBridge 客户端封装
│   │   ├── store/             # Zustand 状态
│   │   ├── theme/             # 设计令牌与主题切换
│   │   ├── locales/           # zh/en/ja 词条
│   │   └── styles/
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
└── docs/webview-refactor/     # 本套交接文档
```

---

## 7. 风险清单与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| WebView 内核差异导致样式不一致 | 视觉还原失败 | 锁定样式基线为 Chrome 90+ 支持特性；不用 :has 等新选择器 |
| JSBridge 同步调用阻塞 UI | 卡顿 | 所有 Bridge 调用必须异步；原生侧耗时操作已在 IO 调度器 |
| 大文件内容跨 Bridge 传输 | 内存抖动 | 单片段 content 上限 1MB，超限走原生临时缓存 + 引用 ID 传递（参考原 `TempPreviewCache` 设计） |
| 输入法遮挡编辑器 | 体验差 | Web 端监听 `visualViewport` 调整布局 |
| 老版本数据库迁移 | 数据丢失 | 严禁改动 Room 结构；覆盖安装回归测试为强制验收项 |

---

## 8. 决策变更记录

| 日期 | 变更 | 决策人 |
|------|------|--------|
| 2026-08-08 | 初版：确认"保留原生数据/Git 层，UI 重构至 WebView + React" | 用户 |
