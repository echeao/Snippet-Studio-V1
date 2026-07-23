package com.feige.snippetstudio.model

data class AppSettings(
    val lang: String = "zh", // zh, ja, en
    val theme: String = "system", // light, dark, system
    val editorFontSp: Float = 13.5f,
    val repoPath: String = "Internal App Storage",
    val gitUrl: String = "",
    val gitBranch: String = "main",
    val gitPat: String = "",
    val gitConnected: Boolean = false
)
