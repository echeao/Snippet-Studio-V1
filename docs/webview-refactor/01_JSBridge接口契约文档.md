# Snippet Studio V1 — JSBridge 接口契约文档

| 项目 | 内容 |
|------|------|
| 文档版本 | 1.0 |
| 编写日期 | 2026-08-08 |
| 文档性质 | JSBridge 接口契约（原生 ↔ Web 双向通信的唯一标准） |

> 原生侧实现文件：`bridge/JsBridge.kt`（新增）
> Web 侧封装文件：`web-src/src/bridge/index.ts`（新增）
> 本文档中标注 🆕 的接口表示原生侧当前无对应方法，需要**新增薄封装**（不得改动业务层内部逻辑）。

---

## 1. 通信协议

### 1.1 JS → Native（请求-响应模式）

原生侧通过 `addJavascriptInterface` 注入对象 `SnippetStudioNative`，暴露唯一入口：

```kotlin
@JavascriptInterface
fun invoke(payload: String): Unit  // payload 为 JSON 字符串
```

**请求报文格式：**

```json
{
  "callId": "c_1723100000_001",
  "method": "snippet.get",
  "params": { "id": "abc123" }
}
```

**响应回传：** 原生侧处理完成后调用：

```javascript
window.__bridgeResolve(callId, resultJsonString)   // 成功
window.__bridgeReject(callId, errorJsonString)     // 失败
```

**错误报文格式：**

```json
{ "code": 1002, "message": "片段不存在" }
```

**Web 侧封装要求：** 提供 `bridge.call<T>(method, params): Promise<T>`，内部维护 callId 映射与超时（默认 30s，Git 网络类接口 120s）。

### 1.2 Native → JS（事件推送模式）

原生侧通过 `webView.evaluateJavascript` 调用：

```javascript
window.onNativeEvent(eventName, payloadJsonString)
```

Web 侧提供 `bridge.on(event, handler)` / `bridge.off(event, handler)` 订阅机制。

### 1.3 通用约定

| 约定 | 说明 |
|---|---|
| 字段命名 | 全部 camelCase，与 Kotlin 数据类字段一致 |
| 时间戳 | 毫秒 Long |
| 枚举传输 | 一律传字符串 code（如 `"html"`、`"INCOMING"`） |
| 大文本 | 单次 JSON 载荷建议 ≤ 1MB；超限场景见 §8 临时缓存协议 |
| 线程 | 原生侧所有 invoke 处理在 IO 协程执行，禁止阻塞 JS 线程 |

### 1.4 错误码表

| code | 含义 |
|---|---|
| 0 | 成功（不进入 reject） |
| 1001 | 参数错误 |
| 1002 | 目标资源不存在 |
| 2001 | 文件读写失败 |
| 3001 | Git 操作失败（message 携带具体原因） |
| 3002 | Git 鉴权失败（PAT 无效） |
| 3003 | 网络不可用 |
| 4001 | SAF 授权失效，需重新选择目录 |
| 9999 | 未知错误 |

---

## 2. 片段域 `snippet.*`

对应原生类：`SnippetRepository`（全部已有实现）。

| 方法 | 入参 | 返回 | 原生对应 |
|---|---|---|---|
| `snippet.observe` | `{}` | 订阅事件（见 §7） | `observeActive()` |
| `snippet.list` | `{ filter?: "active"\|"starred"\|"trashed"\|"all", type?: string, folder?: string, query?: string }` | `Snippet[]` | `observeActive/Starred/Trashed` 快照化 🆕 |
| `snippet.get` | `{ id: string }` | `Snippet \| null` | `getById()` |
| `snippet.create` | `{ type: string, title?: string, useBoilerplate?: boolean, sharedText?: string }` | `Snippet`（新建结果） | `create()` |
| `snippet.save` | `{ snippet: Snippet }` | `{ ok: true }` | `saveOrUpdate()` |
| `snippet.rename` | `{ id, newTitle, newFileName }` | `{ ok: true }` | `updateRename()` |
| `snippet.moveFolder` | `{ id, newFolder }` | `{ ok: true }` | `updateFolder()` |
| `snippet.toggleStar` | `{ id, currentStarred }` | `{ ok: true }` | `toggleStar()` |
| `snippet.updateTags` | `{ id, tags: string[] }` | `{ ok: true }` | `saveOrUpdate(copy)` 🆕薄封装 |
| `snippet.trash` | `{ id }` | `{ ok: true }` | `trash()` |
| `snippet.restore` | `{ id }` | `{ ok: true }` | `restore()` |
| `snippet.purge` | `{ id }` | `{ ok: true }` | `purge()` |
| `snippet.purgeExpired` | `{ days?: number }` | `{ removed: number }` | `purgeExpired()` |
| `snippet.stats` | `{}` | `{ activeCount, starredCount, trashedCount }` | `activeCount()` 等 🆕聚合 |
| `snippet.template` | `{ type: string }` | `{ content: string }` | `SnippetTemplateManager` |

**Snippet JSON 结构：**

```json
{
  "id": "uuid",
  "type": "html",
  "title": "登录页",
  "fileName": "login.html",
  "content": "<html>...</html>",
  "tags": ["UI", "demo"],
  "starred": false,
  "createdAt": 1722000000000,
  "updatedAt": 1722000000000,
  "sizeBytes": 1024,
  "folder": "frontend/pages",
  "trashed": false,
  "trashedAt": null,
  "displayTitle": "登录页"
}
```

---

## 3. 文件夹域 `folder.*`

对应原生类：`SnippetRepository` / `FolderDao`。

| 方法 | 入参 | 返回 | 原生对应 |
|---|---|---|---|
| `folder.list` | `{}` | `FolderEntity[]` | `observeFolders()` 快照化 🆕 |
| `folder.create` | `{ path: string }` | `{ ok: true }` | `createFolder()` |
| `folder.rename` | `{ oldPath, newPath }` | `{ ok: true }` | FilesViewModel 现有逻辑下沉 🆕 |
| `folder.delete` | `{ path }` | `{ ok: true }` | FilesViewModel 现有逻辑下沉 🆕 |

**FolderEntity JSON：** `{ "path": "utils/string", "parentPath": "utils", "createdAt": 1722000000000 }`

> ⚠️ folder.rename 必须级联更新该目录下所有片段的 `folder` 字段并清理物理残留，
> 实现时参考 `updateFolder()` 的残留清理逻辑。

---

## 4. Git 域 `git.*`

对应原生类：`GitManager` + `SyncEngine`。所有接口均为耗时操作，Web 侧必须展示 loading。

| 方法 | 入参 | 返回 | 原生对应 |
|---|---|---|---|
| `git.testConnection` | `{ url, branch, pat }` | `{ connected: boolean }` | `GitManager.testConnection()` |
| `git.initOrClone` | `{ url, branch, pat }` | `{ ok: true }` | `GitManager.initOrClone()` |
| `git.previewPull` | `{}` | `SyncPreview` | `SyncEngine.previewPull()` |
| `git.previewPush` | `{}` | `SyncPreview` | `SyncEngine.previewPush()` |
| `git.executePull` | `{ resolutions?: ConflictResolution[] }` | `{ applied: number, deletedPaths: string[] }` | `SyncEngine.executePull()` |
| `git.executePush` | `{}` | `{ pushed: number }` | `SyncEngine.executePush()` |
| `git.fileHistory` | `{ relPath: string }` | `GitCommitInfo[]` | HistoryViewModel 现有逻辑下沉 🆕 |
| `git.fileDiff` | `{ relPath, commitId? }` | `DiffLine[]` | HistoryViewModel 现有逻辑下沉 🆕 |

**SyncPreview JSON：**

```json
{
  "changes": [
    {
      "fileName": "a.js",
      "folder": "utils",
      "changeType": "ADDED",
      "direction": "INCOMING",
      "localContent": null,
      "remoteContent": "console.log(1)"
    }
  ],
  "conflicts": [
    {
      "fileName": "b.md",
      "folder": "",
      "localContent": "本地版本",
      "remoteContent": "远端版本",
      "resolution": "PENDING"
    }
  ],
  "direction": "INCOMING"
}
```

**ConflictResolution 取值：** `PENDING / KEEP_LOCAL / KEEP_REMOTE / KEEP_BOTH`

**GitCommitInfo JSON：** `{ "commitId": "...", "shortId": "a1b2c3d", "message": "...", "author": "...", "timestamp": 1722000000000 }`

**DiffLine JSON：** `{ "type": "ADD|DELETE|CONTEXT", "content": "...", "oldLineNum": 3, "newLineNum": null }`

---

## 5. 设置域 `settings.*`

对应原生类：`SettingsRepository` / `SettingsDataStore`。

| 方法 | 入参 | 返回 | 原生对应 |
|---|---|---|---|
| `settings.get` | `{}` | `AppSettings` | 现有读取 Flow 快照 🆕 |
| `settings.update` | `{ patch: Partial<AppSettings> }` | `AppSettings`（更新后全量） | 现有更新方法 🆕薄封装 |

**AppSettings 字段全集（字段含义见 `model/AppSettings.kt` 注释）：**

```json
{
  "lang": "zh", "theme": "system", "colorTheme": "forest",
  "editorFontSp": 13.5, "isWordWrap": true, "encoding": "UTF-8", "lineEnding": "LF",
  "showLineNumbers": true, "highlightCurrentLine": true, "tabSize": 4, "autoPairBrackets": true,
  "repoPath": "Internal App Storage", "repoTreeUri": "",
  "gitUrl": "", "gitBranch": "main", "gitPat": "", "gitConnected": false,
  "lastSyncTime": 0, "autoSyncEnabled": true,
  "cardClickAction": "detail", "useBoilerplate": true,
  "customTags": [], "shareAction": "panel", "editorFontFamily": "monospace"
}
```

> ⚠️ `gitPat` 属敏感字段：Web 端仅允许在设置页密码框中呈现（mask），禁止打印到日志。
> ⚠️ `theme/colorTheme/lang/editor*` 变更后会触发 `settingsChanged` 事件，Web 端据此热切换。

---

## 6. 文件与应用域 `file.*` / `app.*`

对应原生类：`LocalFileManager`、`Exporter`、`SharedFileHandler`、`ClipboardDetector`、`LocaleHelper`。

| 方法 | 入参 | 返回 | 原生对应 |
|---|---|---|---|
| `file.pickWorkspace` | `{}` | `{ treeUri, displayName }` | 拉起 SAF 目录选择器 🆕交互封装 |
| `file.exportJson` | `{}` | `{ ok: true, message }` | `Exporter.exportJson()` |
| `file.exportZip` | `{}` | `{ ok: true, message }` | `Exporter.exportZip()` |
| `file.importShared` | `{}` | `SharedFile[]`（待处理分享文件队列） | `SharedFileHandler` 队列化 🆕 |
| `app.clipboardCheck` | `{}` | `{ clip?: { text, detectedType } }` | `ClipboardDetector` |
| `app.clipboardIgnore` | `{ text }` | `{ ok: true }` | HomeViewModel 逻辑下沉 🆕 |
| `app.toast` | `{ message, type?: "info"\|"success"\|"error" }` | — | Web 端 Snackbar 也可自绘，此接口二选一 |
| `app.openExternal` | `{ url }` | `{ ok: true }` | 系统浏览器打开 🆕 |
| `app.backHandled` | `{ consumed: boolean }` | — | Web 端告知原生返回键是否被消费 🆕 |
| `app.version` | `{}` | `{ versionName, versionCode }` | BuildConfig 🆕 |

---

## 7. Native → JS 事件清单

| 事件名 | 载荷 | 触发时机 |
|---|---|---|
| `dataChanged` | `{ scope: "snippet"\|"folder" }` | Room 数据变更（替代 Flow 观察，Web 端收到后重新 list） |
| `settingsChanged` | `AppSettings`（全量） | 设置被原生或 Web 修改后广播 |
| `syncProgress` | `{ phase: "pulling"\|"pushing"\|"committing", percent? }` | Git 同步过程中 |
| `syncFinished` | `{ ok: boolean, message }` | 同步结束（含 autoSync 后台提交） |
| `sharedReceived` | `{ fileName, type, contentRef }` | 系统分享 Intent 进入应用 |
| `backPressed` | `{}` | 用户按返回键，Web 端决策消费或放行 |
| `clipboardDetected` | `{ text, detectedType }` | 剪贴板检测到可导入内容 |

**订阅式数据流的实现策略：** Web 端不再直接订阅 Flow，采用"事件驱动刷新"——
原生侧在 Repository 写入完成后通过 `BridgeEventBus.emit("dataChanged")` 通知，
Web 端收到后调用对应 `list` 接口拉取最新数据。这与原 Compose 的 StateFlow 驱动等价。

---

## 8. 大文本临时缓存协议

用于分享导入超大文件、避免 JSON 载荷过大：

| 方法 | 说明 |
|---|---|
| `cache.put` → 返回 `{ ref: string }` | 原生侧把大文本存入内存缓存（对应原 `TempPreviewCache` 思路），仅传引用 |
| `cache.get` | `{ ref }` → `{ content: string }`，读取后由 Web 端决定是否释放 |

事件 `sharedReceived` 的 `contentRef` 即为该协议引用。

---

## 9. 原生侧实现注意事项

1. `JsBridge.kt` 中所有方法通过 `AppContainer` 获取已有单例，**禁止新建第二套数据层实例**；
2. JSON 序列化统一使用 `kotlinx.serialization` 或 `org.json`（项目现有依赖），禁止引入 Gson 新依赖；
3. 所有 `suspend` 方法通过 `CoroutineScope(Dispatchers.IO)` 桥接，完成后 `runOnUiThread` 回调 `evaluateJavascript`；
4. `evaluateJavascript` 传参必须做 JSON 字符串转义，防止引号注入；
5. WebView 未加载完成前收到 invoke 的兜底：排队等待 `onPageFinished`。
