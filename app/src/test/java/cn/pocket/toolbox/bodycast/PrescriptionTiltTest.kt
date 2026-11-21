package cn.pocket.toolbox.bodycast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 处方按运动量倾斜（22.49.0）。
 *
 * 这里锁的是**排序行为**：拆组之后组内乱序、但倾斜组永远排在前面——
 * 之前只在排序前拆开是不够的，`pickWithinBudget` 会整体洗牌把倾斜冲掉。
 */
class PrescriptionTiltTest {

    private val engine = LocalRuleEngine()

    private fun sample(category: ActionCategory, id: String, minutes: Int = 3) =
        MicroAction(
            id = id,
            sourceTitle = id,
            category = category,
            minutes = minutes,
            slots = setOf(Slot.MORNING, Slot.NOON, Slot.EVENING, Slot.NIGHT),
            sourceGuide = listOf("做一下"),
            forKinds = WeatherKind.entries.toSet()
        )

    private val pool = listOf(
        sample(ActionCategory.MOVE, "move_a"),
        sample(ActionCategory.MOVE, "move_b"),
        sample(ActionCategory.BREATHE, "breath_a"),
        sample(ActionCategory.REST, "rest_a"),
        sample(ActionCategory.EAT, "eat_a")
    )

    @Test
    fun activeTiltPutsGentleCategoriesFirst() {
        val ordered = engine.orderByTilt(pool, ActivityTilt.ACTIVE, seed = 42L)
        assertEquals(pool.size, ordered.size)
        // 前三位必须是「放松/呼吸/光照」这一组里的（池子里有 2 个：breath_a、rest_a）。
        assertTrue(ordered.take(2).all { it.category in setOf(ActionCategory.REST, ActionCategory.BREATHE) })
        // 活动类被压到最后。
        assertTrue(ordered.drop(2).all { it.category == ActionCategory.MOVE || it.category == ActionCategory.EAT })
    }

    @Test
    fun sedentaryTiltPutsMovingFirst() {
        val ordered = engine.orderByTilt(pool, ActivityTilt.SEDENTARY, seed = 42L)
        assertTrue(ordered.take(2).all { it.category == ActionCategory.MOVE })
    }

    @Test
    fun neutralTiltKeepsSeededOrder() {
        val a = engine.orderByTilt(pool, ActivityTilt.NEUTRAL, seed = 7L)
        val b = engine.orderByTilt(pool, ActivityTilt.NEUTRAL, seed = 7L)
        assertEquals(a.map { it.id }, b.map { it.id })
    }

    @Test
    fun sameSeedSameOrderSoSubscriptionStaysStable() {
        // 同一天同一时段刷新不该换一批：同种子必须同结果。
        assertEquals(
            engine.orderByTilt(pool, ActivityTilt.ACTIVE, 1234L).map { it.id },
            engine.orderByTilt(pool, ActivityTilt.ACTIVE, 1234L).map { it.id }
        )
    }
}
