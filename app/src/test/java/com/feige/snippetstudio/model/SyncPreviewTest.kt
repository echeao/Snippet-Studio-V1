package com.feige.snippetstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SyncPreviewTest] 针对 [SyncPreview] 及同步状态模型的单元测试。
 *
 * 覆盖测试场景：
 * 1. 未决冲突状态判定 (hasUnresolvedConflicts)
 * 2. 变更数量与出入站方向统计 (incomingCount, outgoingCount, totalCount)
 * 3. 冲突解决策略状态流转
 */
class SyncPreviewTest {

    @Test
    fun `test SyncPreview conflict detection when pending`() {
        val conflict = SyncConflict(
            fileName = "button.html",
            folder = "components",
            localContent = "<button>Local</button>",
            remoteContent = "<button>Remote</button>",
            resolution = ConflictResolution.PENDING
        )

        val preview = SyncPreview(
            changes = emptyList(),
            conflicts = listOf(conflict),
            direction = SyncDirection.INCOMING
        )

        assertTrue(preview.hasUnresolvedConflicts)
        assertEquals(1, preview.totalCount)
    }

    @Test
    fun `test SyncPreview when all conflicts resolved`() {
        val conflict = SyncConflict(
            fileName = "button.html",
            folder = "components",
            localContent = "<button>Local</button>",
            remoteContent = "<button>Remote</button>",
            resolution = ConflictResolution.KEEP_LOCAL
        )

        val preview = SyncPreview(
            changes = emptyList(),
            conflicts = listOf(conflict),
            direction = SyncDirection.INCOMING
        )

        assertFalse(preview.hasUnresolvedConflicts)
    }

    @Test
    fun `test SyncPreview count calculations for incoming and outgoing changes`() {
        val change1 = SyncChangeItem(
            fileName = "a.js",
            folder = "",
            changeType = SyncChangeType.ADDED,
            direction = SyncDirection.INCOMING
        )
        val change2 = SyncChangeItem(
            fileName = "b.md",
            folder = "docs",
            changeType = SyncChangeType.UPDATED,
            direction = SyncDirection.INCOMING
        )

        val preview = SyncPreview(
            changes = listOf(change1, change2),
            conflicts = emptyList(),
            direction = SyncDirection.INCOMING
        )

        assertEquals(2, preview.incomingCount)
        assertEquals(0, preview.outgoingCount)
        assertEquals(2, preview.totalCount)
        assertFalse(preview.hasUnresolvedConflicts)
    }
}
