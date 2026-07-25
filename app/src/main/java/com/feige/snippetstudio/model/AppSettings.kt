package com.feige.snippetstudio.model

/**
 * [AppSettings] 是应用的全局配置与偏好设置数据模型 (Immutable Data Class)。
 *
 * 教学解析：
 * 本类在 DataStore Preferences 中以 JSON 或键值对形式持久化。
 * 使用 Kotlin 默认参数构建不可变实体，更新设置时建议配合 `.copy(...)` 函数触发 StateFlow 响应式变更。
 *
 * @param lang 界面语言配置 ("zh": 简体中文, "ja": 日语, "en": 英文)
 * @param theme 主题样式 ("system": 跟随系统, "light": 浅色, "dark": 深色)
 * @param editorFontSp 代码编辑器文本字号大小 (单位: sp)
 * @param isWordWrap 编辑器超过边界时是否开启自动软换行
 * @param encoding 文本文件保存/读取编码 (默认 "UTF-8")
 * @param lineEnding 换行符类型 ("LF": Unix 风格 `\n`, "CRLF": Windows 风格 `\r\n`)
 * @param showLineNumbers 编辑器左侧是否渲染代码行号栏
 * @param highlightCurrentLine 是否在编辑器中高亮显示当前光标焦点行
 * @param tabSize 键盘点击 Tab 键输入的缩进空格数量 (默认 4 个空格)
 * @param autoPairBrackets 输入左括号 `{`, `(`, `[` 或引号时是否自动补全配对右符号
 * @param repoPath 本地物理代码仓库目录在界面上的友好显示名称
 * @param repoTreeUri 通过 Android SAF (Storage Access Framework) 持久化授权的文件夹 Tree Uri 字符串
 * @param gitUrl Git 远程仓库 HTTP(S) 地址 (如 GitHub/Gitee)
 * @param gitBranch 目标 Git 关联分支名称 (默认 "main")
 * @param gitPat Git 个人访问令牌 (Personal Access Token / PAT)
 * @param gitConnected Git 本地与远程物理仓库是否已通过连通性鉴权与拉取测试
 * @param lastSyncTime 最近一次 Git 成功完成双向同步的时间戳 (毫秒)
 * @param autoSyncEnabled 是否在代码片段增删改时自动触发异步 Git 物理提交
 * @param cardClickAction 首页/列表页点击代码卡片时的默认响应动作 ("detail": 查看详情, "editor": 直接打开全屏编辑器)
 * @param useBoilerplate 新建代码片段时是否自动注入对应类型的内置样板代码 (默认 true)
 * @param customTags 用户在全局设置中预设或自定义积累的便签标签列表
 */
data class AppSettings(
    val lang: String = "zh",
    val theme: String = "system",
    val editorFontSp: Float = 13.5f,
    val isWordWrap: Boolean = true,
    val encoding: String = "UTF-8",
    val lineEnding: String = "LF",
    val showLineNumbers: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val tabSize: Int = 4,
    val autoPairBrackets: Boolean = true,
    val repoPath: String = "Internal App Storage",
    val repoTreeUri: String = "",
    val gitUrl: String = "",
    val gitBranch: String = "main",
    val gitPat: String = "",
    val gitConnected: Boolean = false,
    val lastSyncTime: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val cardClickAction: String = "detail",
    val useBoilerplate: Boolean = true,
    val customTags: List<String> = emptyList()
)


