package com.douduoyu.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectOrderingTest {
    @Test
    fun orderedByMostRecent_doesNotForceDemoProjectToTheFront() {
        val demo = project(id = PatternEngine.DEMO_PROJECT_ID, updatedAt = 100L)
        val newerProject = project(id = "newer", updatedAt = 300L)
        val olderProject = project(id = "older", updatedAt = 50L)

        assertEquals(
            listOf("newer", PatternEngine.DEMO_PROJECT_ID, "older"),
            listOf(demo, olderProject, newerProject).orderedByMostRecent().map(BeadProject::id),
        )
    }

    @Test
    fun preservingUpdatedAtIfEquivalentTo_keepsExistingDemoTimestamp() {
        val existing = project(id = PatternEngine.DEMO_PROJECT_ID, updatedAt = 100L)
        val regenerated = existing.copy(updatedAt = 900L, sourceImagePath = "new-source.png")

        val reconciled = regenerated.preservingUpdatedAtIfEquivalentTo(existing)

        assertEquals(100L, reconciled.updatedAt)
        assertEquals("new-source.png", reconciled.sourceImagePath)
    }

    @Test
    fun preservingUpdatedAtIfEquivalentTo_keepsNewTimestampWhenContentChanged() {
        val existing = project(id = PatternEngine.DEMO_PROJECT_ID, updatedAt = 100L)
        val regenerated =
            existing.copy(
                title = "另一枚图标",
                updatedAt = 900L,
                sourceImagePath = "new-source.png",
            )

        assertEquals(900L, regenerated.preservingUpdatedAtIfEquivalentTo(existing).updatedAt)
    }

    private fun project(
        id: String,
        updatedAt: Long,
    ): BeadProject {
        return BeadProject(
            id = id,
            title = id,
            width = 1,
            height = 1,
            cells = listOf("coco:A01"),
            updatedAt = updatedAt,
        )
    }
}
