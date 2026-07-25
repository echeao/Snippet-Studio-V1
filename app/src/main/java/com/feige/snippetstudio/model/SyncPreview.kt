package com.feige.snippetstudio.model

/**
 * 同步变更类型枚举。
 */
enum class SyncChangeType {
    /** 新增文件 */
    ADDED,
    /** 内容更新 */
    UPDATED,
    /** 文件已删除 */
    DELETED
}

/**
 * 同步数据流向枚举。
 */
enum class SyncDirection {
    /** 远端 → 本地 (Pull 方向) */
    INCOMING,
    /** 本地 → 远端 (Push 方向) */
    OUTGOING
}

/**
 * 同步变更条目：描述单个文件在同步操作中的预期变动。
 *
 * @param fileName 文件名
 * @param folder 所属文件夹相对路径
 * @param changeType 变更类型（新增/更新/删除）
 * @param direction 数据流向（入站/出站）
 * @param localContent 本地版本内容（可选，用于对比展示）
 * @param remoteContent 远端版本内容（可选，用于对比展示）
 */
data class SyncChangeItem(
    val fileName: String,
    val folder: String,
    val changeType: SyncChangeType,
    val direction: SyncDirection,
    val localContent: String? = null,
    val remoteContent: String? = null
)

/**
 * 冲突解决策略枚举。
 */
enum class ConflictResolution {
    /** 尚未解决 */
    PENDING,
    /** 保留本地版本 */
    KEEP_LOCAL,
    /** 保留远端版本 */
    KEEP_REMOTE,
    /** 两者都保留（远端版本重命名保存） */
    KEEP_BOTH
}

/**
 * 同步冲突条目：描述本地与远端同一文件内容不一致时的冲突信息。
 *
 * @param fileName 冲突文件名
 * @param folder 所属文件夹相对路径
 * @param localContent 本地版本内容
 * @param remoteContent 远端版本内容
 * @param resolution 用户选择的解决策略
 */
data class SyncConflict(
    val fileName: String,
    val folder: String,
    val localContent: String,
    val remoteContent: String,
    val resolution: ConflictResolution = ConflictResolution.PENDING
)

/**
 * 同步预览汇总：在执行同步前展示给用户确认的变更清单。
 *
 * @param changes 预期变更列表
 * @param conflicts 检测到的冲突列表
 * @param direction 本次同步的数据流向
 */
data class SyncPreview(
    val changes: List<SyncChangeItem>,
    val conflicts: List<SyncConflict>,
    val direction: SyncDirection
) {
    /** 是否存在未解决的冲突 */
    val hasUnresolvedConflicts get() = conflicts.any { it.resolution == ConflictResolution.PENDING }

    /** 入站（拉取）变更数量 */
    val incomingCount get() = changes.count { it.direction == SyncDirection.INCOMING }

    /** 出站（推送）变更数量 */
    val outgoingCount get() = changes.count { it.direction == SyncDirection.OUTGOING }

    /** 总变更数量（含冲突） */
    val totalCount get() = changes.size + conflicts.size
}
