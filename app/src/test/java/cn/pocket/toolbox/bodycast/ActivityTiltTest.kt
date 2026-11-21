package cn.pocket.toolbox.bodycast

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 运动读数 → 处方倾向（22.49.0）。
 *
 * 锁住三件事：动够多的判据、久坐的判据、以及「读不到不等于久坐」。
 */
class ActivityTiltTest {

    @Test
    fun activeWhenStepsOrMinutesReachThreshold() {
        assertEquals(ActivityTilt.ACTIVE, ActivityTilt.of(8_000, null))
        assertEquals(ActivityTilt.ACTIVE, ActivityTilt.of(null, 30))
        assertEquals(ActivityTilt.ACTIVE, ActivityTilt.of(12_000, 48))
    }

    @Test
    fun sedentaryOnlyWhenStepsWereActuallyRead() {
        assertEquals(ActivityTilt.SEDENTARY, ActivityTilt.of(1_200, 5))
        // 没读到步数 → 不算久坐（没读到不等于没动过）。
        assertEquals(ActivityTilt.NEUTRAL, ActivityTilt.of(null, null))
        assertEquals(ActivityTilt.NEUTRAL, ActivityTilt.of(null, 10))
    }

    @Test
    fun inBetweenStaysNeutral() {
        assertEquals(ActivityTilt.NEUTRAL, ActivityTilt.of(4_500, 12))
        assertEquals(ActivityTilt.NEUTRAL, ActivityTilt.of(2_000, 0))
    }
}
