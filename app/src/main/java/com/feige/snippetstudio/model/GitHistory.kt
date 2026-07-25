package com.feige.snippetstudio.model

/**
 * [GitCommitInfo] 表示一条 Git 提交记录信息。
 *
 * @param commitId 完整的 commit SHA-1 哈希
 * @param shortId 缩短的 commit ID（前 7 位）
 * @param message 提交说明消息
 * @param author 提交作者名称
 * @param timestamp 提交时间戳（毫秒）
 */
data class GitCommitInfo(
    val commitId: String,
    val shortId: String,
    val message: String,
    val author: String,
    val timestamp: Long
)

/**
 * [DiffLine] 表示 Diff 对比视图中的单行内容。
 *
 * @param type 行类型（新增/删除/上下文）
 * @param content 行文本内容
 * @param oldLineNum 在旧版本中的行号（删除行和上下文行有值）
 * @param newLineNum 在新版本中的行号（新增行和上下文行有值）
 */
data class DiffLine(
    val type: DiffType,
    val content: String,
    val oldLineNum: Int? = null,
    val newLineNum: Int? = null
)

/**
 * [DiffType] Diff 行类型枚举。
 */
enum class DiffType {
    /** 新增行（绿色） */
    ADD,
    /** 删除行（红色） */
    DELETE,
    /** 上下文行（无变化） */
    CONTEXT
}
