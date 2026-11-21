package cn.pocket.toolbox.bodycast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 多源合并优先级（22.27.0）。
 *
 * 锁住产品铁律的读数层：**手动填读数 > 小米云 > 文件导入 > 设备（HC）**，
 * 且只按字段补——高优先级没填的字段继续往下找，一路都没有就是 null
 * （null ≠ 0，绝不用 0 补齐）。手动校准（改结论）不参与这一层。
 */
class SignalMergeTest {

    private val device = BodySignals(
        source = SignalSource.HEALTH_CONNECT,
        updatedAt = 1_000L,
        restingHeartRate = 60,
        hrv = 40,
        steps = 3000,
        bloodOxygen = 96,
        activityMinutes = 10
    )

    @Test
    fun manualEntryWinsOverCloudImportAndDevice() {
        val merged = SignalMerge.merge(
            base = device,
            manual = EntryValues(restingHeartRate = 70, steps = 5000),
            manualAt = 5_000L,
            cloud = EntryValues(restingHeartRate = 65, steps = 4000, hrv = 55),
            cloudAt = 4_000L,
            imported = EntryValues(restingHeartRate = 62, steps = 3500),
            importedAt = 3_000L
        )
        assertEquals(70, merged.restingHeartRate)
        assertEquals(5000, merged.steps)
        // 手动没填的字段继续往下：小米云的 hrv 顶上，而不是设备的。
        assertEquals(55, merged.hrv)
        // 一路都没填的字段保持 null（血氧在 manual/cloud 都没有，用设备的）。
        assertEquals(96, merged.bloodOxygen)
        assertEquals(SignalSource.MANUAL_ENTRY, merged.source)
    }

    @Test
    fun cloudWinsOverImportAndDevice() {
        val merged = SignalMerge.merge(
            base = device,
            cloud = EntryValues(restingHeartRate = 65),
            cloudAt = 4_000L,
            imported = EntryValues(restingHeartRate = 62),
            importedAt = 3_000L
        )
        assertEquals(65, merged.restingHeartRate)
        assertEquals(SignalSource.MI_CLOUD, merged.source)
        // 手动/云端没填的步数落到导入，再没有才是设备。
        assertEquals(3000, merged.steps)
    }

    @Test
    fun importWinsOverDevice() {
        val merged = SignalMerge.merge(
            base = device,
            imported = EntryValues(steps = 8000),
            importedAt = 3_000L
        )
        assertEquals(8000, merged.steps)
        assertEquals(SignalSource.IMPORT, merged.source)
    }

    @Test
    fun perFieldFillKeepsOtherSources() {
        // 手动只填了步数：心率仍然是设备读到的，两边都没有的才是 null。
        val merged = SignalMerge.merge(
            base = device.copy(hrv = null),
            manual = EntryValues(steps = 100),
            manualAt = 5_000L
        )
        assertEquals(100, merged.steps)
        assertEquals(60, merged.restingHeartRate)
        assertNull(merged.hrv)
    }

    @Test
    fun nullStaysNullAndNeverBecomesZero() {
        val merged = SignalMerge.merge(
            base = BodySignals(source = SignalSource.HEALTH_CONNECT),
            manual = EntryValues(steps = 0),
            manualAt = 5_000L
        )
        // 显式填的 0 是 0（步数可以真的是 0），没填的仍然是 null。
        assertEquals(0, merged.steps)
        assertNull(merged.restingHeartRate)
        assertNull(merged.bloodOxygen)
    }

    @Test
    fun nothingToMergeReturnsBaseUnchanged() {
        val merged = SignalMerge.merge(base = device)
        assertEquals(device, merged)
    }

    @Test
    fun demoSignalsAreNeverOverlaid() {
        val demo = BodySignals.demo()
        val merged = SignalMerge.merge(
            base = demo,
            manual = EntryValues(steps = 12345),
            manualAt = 5_000L
        )
        assertEquals(demo, merged)
        assertEquals(SignalSource.DEMO, merged.source)
    }

    @Test
    fun stressLevelOnlyComesFromImport() {
        // 压力值只有文件导入会给（Health Connect 里没有这一项），
        // 手动填写和小米云都不产生它——不许从别的路补出来。
        val merged = SignalMerge.merge(
            base = device,
            manual = EntryValues(steps = 1),
            manualAt = 5_000L,
            cloud = EntryValues(steps = 2),
            cloudAt = 4_000L,
            imported = EntryValues(stressLevel = 44),
            importedAt = 3_000L
        )
        assertEquals(44, merged.stressLevel)
    }

    @Test
    fun estimatedRestingHeartRateClearsWhenOverridden() {
        val base = device.copy(restingHeartRateEstimated = true)
        val keepEstimate = SignalMerge.merge(base = base)
        assertTrue(keepEstimate.restingHeartRateEstimated)

        val overridden = SignalMerge.merge(
            base = base,
            manual = EntryValues(restingHeartRate = 61),
            manualAt = 5_000L
        )
        assertFalse(overridden.restingHeartRateEstimated)
    }

    @Test
    fun updatedAtIsTheLatestParticipatingSource() {
        val merged = SignalMerge.merge(
            base = device.copy(updatedAt = 1_000L),
            manual = EntryValues(steps = 1),
            manualAt = 9_000L,
            imported = EntryValues(steps = 2),
            importedAt = 7_000L
        )
        assertEquals(9_000L, merged.updatedAt)
    }
}
