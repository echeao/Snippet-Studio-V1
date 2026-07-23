package com.feige.snippetstudio.model

data class AppSettings(
    val lang: String = "zh", // zh, ja, en
    val theme: String = "system", // light, dark, system
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
    val gitConnected: Boolean = false
)
