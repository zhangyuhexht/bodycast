package cn.pocket.toolbox.data.micloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * 小米云签名/加密的移植校验。
 *
 * 测试向量来自原项目 go 参考实现生成的固定值
 * （`ilyasaftr/better-mi-fitness-sync` → `miclient/src/commonTest/.../CryptoParityTest.kt`，
 * MIT License，Copyright (c) 2026 Ilyasa Fathur Rahman）。**向量原样照抄**：
 * 换实现可以，换向量就失去意义了——它证明的是「这份移植和原实现逐字节一致」。
 */
class MiCryptoTest {

    private val ssecurity = MiCrypto.base64Encode("test-ssecurity-16b".toByteArray())
    private val nonce = MiCrypto.base64Encode("12bytesnonce".toByteArray())

    @Test
    fun signedNonceMatchesReference() {
        assertEquals(
            "1pBrTAq6mHpeOnEOvny8EWXI5Qdep3FZlr5fllYk5K0=",
            MiCloudSigner.computeSignedNonce(ssecurity, nonce)
        )
    }

    @Test
    fun rc4WithKeystreamSkipMatchesReference() {
        val key = MiCrypto.base64Decode("1pBrTAq6mHpeOnEOvny8EWXI5Qdep3FZlr5fllYk5K0=")
        val encrypted = SkippedRC4(key).process("hello world".toByteArray())
        assertEquals("/qsdcUhDyVUee+M=", MiCrypto.base64Encode(encrypted))
    }

    @Test
    fun sha1MatchesReference() {
        assertEquals("SYg7NOWg9IIk3WIw9HHp3Bvb6vU=", MiCrypto.base64Encode(MiCrypto.sha1("test input".toByteArray())))
    }

    @Test
    fun sha256MatchesReference() {
        assertEquals(
            "nf5vFdGrc6+Jhzk5T9Iv1yoD2wGDRYLyS7Lhxmx6rq4=",
            MiCrypto.base64Encode(MiCrypto.sha256("test input".toByteArray()))
        )
    }

    @Test
    fun md5UpperHexMatchesReference() {
        assertEquals("5F4DCC3B5AA765D61D8327DEB882CF99", MiCrypto.md5UpperHex("password".toByteArray()))
    }

    @Test
    fun encryptThenDecryptIsIdentity() {
        val plaintext = """{"key":"heart_rate","limit":50}"""
        val request = MiCloudSigner.buildEncryptedRequest(
            path = "/app/v1/data/get_latest_fitness_data",
            ssecurity = ssecurity,
            plaintext = plaintext
        )
        assertEquals(plaintext, MiCloudSigner.decryptResponse(request.signedNonce, request.data).decodeToString())
    }

    @Test
    fun nonceIsTwelveBytes() {
        assertEquals(12, Base64.getDecoder().decode(MiCloudSigner.generateNonce()).size)
    }

    @Test
    fun clientSignShape() {
        val sign = MiPassportSts.clientSign("12345", "c2VjdXJpdHk=")
        assertTrue(sign.isNotEmpty())
        assertEquals(
            MiCrypto.base64Encode(MiCrypto.sha1("nonce=12345&c2VjdXJpdHk=".toByteArray())),
            sign
        )
    }
}
