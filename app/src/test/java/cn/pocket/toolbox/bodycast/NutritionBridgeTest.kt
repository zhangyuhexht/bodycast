package cn.pocket.toolbox.bodycast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 运动读数 → 今天先补什么（22.48.0）。
 *
 * 锁住三件事：
 * 1. 动得够多（8000 步 / 30 分钟中高强度）时首选元素一律是镁，与天气无关；
 * 2. 读数缺失（null）时**不能**当成 0 之外的东西——退回天气口径；
 * 3. 理由句只在真的动得够多时给，而且带的是**真实数字**，不编抬头。
 */
class NutritionBridgeTest {

    @Test
    fun activeDayNeedsEitherStepsOrMinutes() {
        assertTrue(NutritionBridge.activeDay(8_000, null))
        assertTrue(NutritionBridge.activeDay(null, 30))
        assertFalse(NutritionBridge.activeDay(7_999, 29))
        assertFalse(NutritionBridge.activeDay(null, null))
    }

    @Test
    fun activeDayOverridesWeather() {
        // 恢复天本来给蛋白质；今天走了 12000 步，先给镁。
        assertEquals(
            Nutrient.MAGNESIUM,
            NutritionBridge.primary(WeatherKind.RECOVERING, 12_000, 0)
        )
        // 平衡天本来给纤维；中高强度 45 分钟，同样让位。
        assertEquals(
            Nutrient.MAGNESIUM,
            NutritionBridge.primary(WeatherKind.BALANCED, 0, 45)
        )
    }

    @Test
    fun quietDayFollowsWeather() {
        assertEquals(
            Nutrient.PROTEIN,
            NutritionBridge.primary(WeatherKind.RECOVERING, 3_000, 12)
        )
        // 没有运动数据时与只按天气算是同一个结论——这条同时守住了老口径。
        assertEquals(
            NutritionBridge.weatherPrimary(WeatherKind.PRESSURE),
            NutritionBridge.primary(WeatherKind.PRESSURE, null, null)
        )
    }

    @Test
    fun reasonOnlySpeaksWhenItHasNumbers() {
        assertTrue(NutritionBridge.reason(12_000, 45).contains("12000"))
        assertTrue(NutritionBridge.reason(9_001, null).contains("9001"))
        assertTrue(NutritionBridge.reason(null, 33).contains("33"))
        assertEquals("", NutritionBridge.reason(2_000, 5))
        assertEquals("", NutritionBridge.reason(null, null))
    }
}
