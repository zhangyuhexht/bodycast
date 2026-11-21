package cn.pocket.toolbox.data.micloud

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 小米云通道的解析与加密回归（22.35.0）。
 *
 * 这条通道没法在单测里走真网络，但**纯逻辑必须锁死**：
 * 载荷两种形态、内联兜底、时间戳单位、睡眠段位求和、RC4 对称性、
 * 会话序列化、区域/主机/cookie 的拼法。任何一条被改坏，
 * 真机上表现都是「连不上」——这里先拦住。
 */
class MiCloudParsingTest {

    // ---- valueObject：三种载荷形态 ----

    @Test
    fun valueObjectParsesJsonStringValue() {
        val item = JSONObject()
            .put("key", "steps")
            .put("time", 1728000000L)
            .put("value", JSONObject().put("steps", 42).toString())
        val body = MiCloudApi.valueObject(item)
        assertNotNull(body)
        assertEquals(42, body!!.optInt("steps"))
        assertEquals(1728000000L, body.optLong("time"))
        assertEquals("steps", body.optString("key"))
    }

    @Test
    fun valueObjectParsesNestedObjectValue() {
        val item = JSONObject()
            .put("value", JSONObject().put("spo2", 97).put("time", 100L))
        val body = MiCloudApi.valueObject(item)
        assertEquals(97, body!!.optInt("spo2"))
    }

    @Test
    fun valueObjectFallsBackToInlinePayload() {
        // 部分端点把字段直接摊在条目上（没有 value 包装）——22.35.0 兜底。
        val item = JSONObject().put("steps", 88).put("time", 200L)
        val body = MiCloudApi.valueObject(item)
        assertNotNull(body)
        assertEquals(88, body!!.optInt("steps"))
    }

    @Test
    fun valueObjectSkipsUnknownShape() {
        assertNull(MiCloudApi.valueObject(JSONObject().put("foo", "bar")))
        assertNull(MiCloudApi.valueObject(null))
    }

    // ---- 时间戳单位 ----

    @Test
    fun secOfNormalizesMillis() {
        assertEquals(1728000000L, MiCloudApi.secOf(1728000000000L))
        assertEquals(1728000000L, MiCloudApi.secOf(1728000000L))
        assertEquals(0L, MiCloudApi.secOf(0L))
    }

    // ---- 睡眠段位 ----

    @Test
    fun sleepStageSecondsSumsAsleepAndDeep() {
        val stages = JSONArray()
            .put(JSONObject().put("state", 2).put("start_time", 0L).put("end_time", 100L))   // 深睡
            .put(JSONObject().put("state", 3).put("start_time", 100L).put("end_time", 300L)) // 浅睡
            .put(JSONObject().put("state", 4).put("start_time", 300L).put("end_time", 400L)) // REM
            .put(JSONObject().put("state", 5).put("start_time", 400L).put("end_time", 500L)) // 清醒（不算）
        val (asleep, deep) = MiCloudApi.sleepStageSeconds(stages)
        assertEquals(400L, asleep)
        assertEquals(100L, deep)
    }

    @Test
    fun sleepStageSecondsToleratesMillisAndBadRanges() {
        val stages = JSONArray()
            .put(JSONObject().put("state", 2).put("start_time", 1_728_000_001_000L).put("end_time", 1_728_000_061_000L)) // 毫秒
            .put(JSONObject().put("state", 3).put("start_time", 10L).put("end_time", 10L))        // 零长
        val (asleep, deep) = MiCloudApi.sleepStageSeconds(stages)
        assertEquals(60L, asleep)
        assertEquals(60L, deep)
    }


    // ---- 压力值解析（22.37.0）----

    @Test
    fun stressValueOfAcceptsFourFieldNames() {
        assertEquals(55, MiCloudApi.stressValueOf(JSONObject().put("value", 55)))
        assertEquals(40, MiCloudApi.stressValueOf(JSONObject().put("score", 40)))
        assertEquals(30, MiCloudApi.stressValueOf(JSONObject().put("stress", 30)))
        assertEquals(20, MiCloudApi.stressValueOf(JSONObject().put("pressure", 20)))
    }

    @Test
    fun stressValueOfRejectsOutOfRangeAndMissing() {
        assertNull(MiCloudApi.stressValueOf(JSONObject().put("value", 300)))
        assertNull(MiCloudApi.stressValueOf(JSONObject().put("value", -1)))
        assertNull(MiCloudApi.stressValueOf(JSONObject()))
    }

    // ---- 加密与签名 ----

    @Test
    fun rc4RoundTripIsIdentity() {
        val signedNonce = MiCrypto.base64Encode(ByteArray(32) { it.toByte() })
        val plain = "hello 小米".toByteArray()
        val enc = MiCloudSigner.buildEncryptedRequest("/app/v1/data/x", "c2VjcmV0", "{}")
        // 解密用与加密同一个 signedNonce：RC4 对称，密文再过一次就是明文。
        val cipher = SkippedRC4(MiCrypto.base64Decode(signedNonce)).process(plain)
        val back = SkippedRC4(MiCrypto.base64Decode(signedNonce)).process(cipher)
        assertTrue(plain.contentEquals(back))
        // 构造出的四件套不为空、base64 可解。
        assertTrue(enc.nonce.isNotBlank() && enc.data.isNotBlank())
        assertTrue(MiCrypto.base64Decode(enc.signature).isNotEmpty())
    }

    @Test
    fun nonceIsTwelveBytesWithMinuteCounter() {
        val raw = MiCrypto.base64Decode(MiCloudSigner.generateNonce(nowSeconds = 1_728_000_000L))
        assertEquals(12, raw.size)
        val minutes = 1_728_000_000L / 60
        val tail = ((raw[8].toInt() and 0xFF) shl 24) or ((raw[9].toInt() and 0xFF) shl 16) or
            ((raw[10].toInt() and 0xFF) shl 8) or (raw[11].toInt() and 0xFF)
        assertEquals(minutes.toInt(), tail)
    }

    @Test
    fun clientSignMatchesFormula() {
        val sign = MiPassportSts.clientSign("n", "c2VjcmV0")
        val expect = MiCrypto.base64Encode(MiCrypto.sha1("nonce=n&c2VjcmV0".toByteArray()))
        assertEquals(expect, sign)
    }

    // ---- 会话与地址 ----

    @Test
    fun sessionJsonRoundTrip() {
        val s = MiSession(
            userId = "u1", ssecurity = "sec", serviceToken = "st", passToken = "pt",
            deviceId = "d1", region = "cn", cUserId = "c1", accountHint = "hint"
        )
        val back = MiSession.fromJson(s.toJson())
        assertNotNull(back)
        assertEquals("u1", back!!.userId)
        assertEquals("sec", back.ssecurity)
        assertEquals("st", back.serviceToken)
        assertEquals("pt", back.passToken)
        assertEquals("cn", back.region)
        assertEquals("c1", back.cUserId)
        assertEquals("hint", back.accountHint)
    }

    @Test
    fun regionAndHostMapping() {
        assertEquals("cn", MiCloudApi.resolveRegion("CN"))
        assertEquals("sg", MiCloudApi.resolveRegion(null))
        assertEquals("de", MiCloudApi.resolveRegion("DE"))
        assertEquals("hlth.io.mi.com", MiCloudApi.healthHost("cn"))
        assertEquals("sg.hlth.io.mi.com", MiCloudApi.healthHost("SG"))
    }

    @Test
    fun parseCookiesHandlesSemicolons() {
        val map = MiCloudApi.parseCookies("userId=1; passToken=abc; serviceToken=tok")
        assertEquals("1", map["userId"])
        assertEquals("abc", map["passToken"])
        assertEquals("tok", map["serviceToken"])
    }
}
