package cn.pocket.toolbox.bodycast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「上次读取」的时长纪律（22.27.0），时间戳全部注入——不用真等 30 分钟。
 *
 * 两条规则：回前台 5 分钟内不自动重读；超过 30 分钟提示「数据可能过期」。
 */
class FetchFreshnessTest {

    private val now = 1_000_000_000_000L

    @Test
    fun autoRefreshOnlyAfterFiveMinutes() {
        assertFalse(FetchFreshness.shouldAutoRefresh(now - 4 * 60 * 1000L, now))
        assertTrue(FetchFreshness.shouldAutoRefresh(now - 5 * 60 * 1000L, now))
    }

    @Test
    fun neverFetchedCountsAsShouldRefresh() {
        assertTrue(FetchFreshness.shouldAutoRefresh(0L, now))
    }

    @Test
    fun staleHintOnlyAfterThirtyMinutes() {
        assertFalse(FetchFreshness.isStale(now - 29 * 60 * 1000L, now))
        assertTrue(FetchFreshness.isStale(now - 30 * 60 * 1000L, now))
    }

    @Test
    fun neverFetchedIsNotStaleButShowsNotReadYet() {
        // 没读过不算「过期」——那是另一句话：「还没有读取过」。
        assertFalse(FetchFreshness.isStale(0L, now))
    }
}
