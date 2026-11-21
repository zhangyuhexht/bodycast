package cn.pocket.toolbox.data

import cn.pocket.toolbox.data.micloud.MiCloudException
import cn.pocket.toolbox.data.micloud.MiSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 小米云令牌过期时的续期策略（22.27.0）。
 *
 * 用假令牌 / 假调用走一遍代码路径（不联网）：先正常调 → 被拒才用 passToken
 * 自动续期一次 → 再调；续期失败或续期后仍被拒**才**提示重新登录，
 * 中间绝不静默吞错、也绝不反复重试。
 */
class MiCloudRenewalTest {

    private val session = MiSession(
        userId = "12345",
        deviceId = "wb_test",
        serviceToken = "stale-service-token",
        passToken = "pass-token",
        ssecurity = "c2VjdXJpdHk=",
        region = "cn",
        accountHint = "138****0000"
    )

    @Test
    fun happyPathDoesNotRenew() {
        var renewals = 0
        val out = runBlocking {
            MiCloudSource.withRenewalOnce(
                session = session,
                renew = { renewals += 1; it },
                call = { "snapshot" }
            )
        }
        assertEquals("snapshot", out)
        assertEquals(0, renewals)
    }

    @Test
    fun expiredTokenIsRenewedOnceThenSucceeds() {
        var renewals = 0
        var calls = 0
        val out = runBlocking {
            MiCloudSource.withRenewalOnce(
                session = session,
                renew = { s ->
                    renewals += 1
                    s.copy(serviceToken = "fresh-service-token")
                },
                call = { s ->
                    calls += 1
                    if (s.serviceToken == "stale-service-token") {
                        throw MiCloudException.Auth("expired")
                    }
                    "snapshot-from-renewed"
                }
            )
        }
        assertEquals("snapshot-from-renewed", out)
        assertEquals(1, renewals)
        assertEquals(2, calls)
    }

    @Test
    fun renewFailureBubblesAuthSoUiAsksRelogin() {
        var renewals = 0
        val result = runCatching {
            runBlocking {
                MiCloudSource.withRenewalOnce(
                    session = session,
                    renew = { renewals += 1; throw MiCloudException.Auth("passToken dead") },
                    call = { throw MiCloudException.Auth("expired") }
                )
            }
        }
        assertTrue(result.exceptionOrNull() is MiCloudException.Auth)
        assertEquals(1, renewals)
        // 界面拿到 Auth 后走 messageOf：说的是「重新连接小米账号」，不是假装成功。
        val message = MiCloudSource.messageOf(result.exceptionOrNull()!!)
        assertTrue(message, message.contains("重新连接小米账号"))
    }

    @Test
    fun renewedTokenStillRejectedAsksReloginNotRetryLoop() {
        var renewals = 0
        var calls = 0
        val result = runCatching {
            runBlocking {
                MiCloudSource.withRenewalOnce(
                    session = session,
                    renew = { renewals += 1; it.copy(serviceToken = "fresh") },
                    call = { calls += 1; throw MiCloudException.Auth("still expired") }
                )
            }
        }
        assertTrue(result.exceptionOrNull() is MiCloudException.Auth)
        // 只续期一次、只重试一次：不会陷入重试循环。
        assertEquals(1, renewals)
        assertEquals(2, calls)
    }

    @Test
    fun nonAuthErrorsAreNotRetried() {
        var renewals = 0
        var calls = 0
        val result = runCatching {
            runBlocking {
                MiCloudSource.withRenewalOnce(
                    session = session,
                    renew = { renewals += 1; it },
                    call = { calls += 1; throw MiCloudException.Network("offline") }
                )
            }
        }
        assertTrue(result.exceptionOrNull() is MiCloudException.Network)
        assertEquals(0, renewals)
        assertEquals(1, calls)
    }
}
