package cn.pocket.toolbox.data.micloud

import java.security.MessageDigest
import java.util.Base64
import kotlin.random.Random

/**
 * 小米云接口的签名与加密。
 *
 * **移植/改编自 [ilyasaftr/better-mi-fitness-sync](https://github.com/ilyasaftr/better-mi-fitness-sync)
 * 的 `miclient` 模块（MIT License，Copyright (c) 2026 Ilyasa Fathur Rahman）。**
 * 原实现是 KMP 纯 Kotlin 版；这里改用 `java.security.MessageDigest`（同一族标准摘要），
 * RC4 与签名拼接逐行照搬，行为不变。
 *
 * 协议（逆向自小米运动健康 APK 与 python-miio）：
 * ```
 * nonce         = 8 随机字节 + 大端(unix 秒 / 60)  → base64
 * signedNonce   = base64( SHA-256( decode(ssecurity) + decode(nonce) ) )
 * rc4Key        = decode(signedNonce)
 *
 * rc4_hash__    = base64( rc4( SHA-1("POST&path&data=明文&signedNonce") ) )
 * data          = base64( rc4( 明文 ) )
 * signature     = base64( SHA-1("POST&path&data=data&rc4_hash__=hash&signedNonce") )
 * ```
 * RC4 用小米变体：**丢弃前 1024 字节密钥流**。
 *
 * **这是非官方接口**：小米没有公开它，随时可能变更或限制；本应用只读用户自己账号的数据，
 * 不向小米上传任何东西。签名与加密在这里是「让服务端认这份请求」的手段，
 * 不是用来藏数据的——请求里只有查询条件（要哪一类、什么时间段）。
 */
object MiCrypto {

    fun sha1(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(input)

    fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)

    fun md5(input: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(input)

    /** 小米登录用的口令形态：`UPPER(hex(MD5(password)))`。本应用不用它（只走官方登录页）。 */
    fun md5UpperHex(input: ByteArray): String =
        md5(input).joinToString("") { it.toUByte().toString(16).padStart(2, '0') }.uppercase()

    fun base64Encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun base64Decode(encoded: String): ByteArray = Base64.getDecoder().decode(encoded)
}

/**
 * 带「跳过前 1024 字节密钥流」的 RC4（小米变体，用来缓解 RC4 头部偏差）。
 * 逐行照搬自原实现的 `SkippedRC4`。RC4 对称：加密与解密是同一个操作。
 */
internal class SkippedRC4(key: ByteArray) {

    private val state = IntArray(256)
    private var i = 0
    private var j = 0

    init {
        for (k in 0 until 256) state[k] = k
        var swap = 0
        for (k in 0 until 256) {
            swap = (swap + state[k] + (key[k % key.size].toInt() and 0xFF)) and 0xFF
            val temp = state[k]
            state[k] = state[swap]
            state[swap] = temp
        }
        repeat(1024) { nextKeystreamByte() }
    }

    fun process(input: ByteArray): ByteArray {
        val output = ByteArray(input.size)
        for (index in input.indices) {
            output[index] = (input[index].toInt() xor nextKeystreamByte()).toByte()
        }
        return output
    }

    private fun nextKeystreamByte(): Int {
        i = (i + 1) and 0xFF
        j = (j + state[i]) and 0xFF
        val temp = state[i]
        state[i] = state[j]
        state[j] = temp
        return state[(state[i] + state[j]) and 0xFF]
    }
}

/** 一次加密请求的四个表单字段 + 解密响应要用的 `signedNonce`。 */
data class MiEncryptedRequest(
    val nonce: String,
    val data: String,
    val rc4Hash: String,
    val signature: String,
    val signedNonce: String
)

/**
 * 构造加密请求、解密响应。照搬原实现的 `MiCloudSigner`。
 *
 * @param path 参与签名的路径（如 `/app/v1/data/get_latest_fitness_data`）。
 *   注意有的端点请求路径带前缀（`/healthapp/...`）而签名只用后半段，由调用方分开传。
 */
object MiCloudSigner {

    fun buildEncryptedRequest(path: String, ssecurity: String, plaintext: String): MiEncryptedRequest {
        val nonce = generateNonce()
        val signedNonce = computeSignedNonce(ssecurity, nonce)

        val preHashInput = "POST&$path&data=$plaintext&$signedNonce"
        val rc4HashPlain = MiCrypto.base64Encode(MiCrypto.sha1(preHashInput.toByteArray()))
        val rc4HashEncrypted = MiCrypto.base64Encode(
            rc4(signedNonce, rc4HashPlain.toByteArray())
        )

        val encryptedData = MiCrypto.base64Encode(rc4(signedNonce, plaintext.toByteArray()))

        val signatureInput = "POST&$path&data=$encryptedData&rc4_hash__=$rc4HashEncrypted&$signedNonce"
        val signature = MiCrypto.base64Encode(MiCrypto.sha1(signatureInput.toByteArray()))

        return MiEncryptedRequest(
            nonce = nonce,
            data = encryptedData,
            rc4Hash = rc4HashEncrypted,
            signature = signature,
            signedNonce = signedNonce
        )
    }

    /** 用请求里的 `signedNonce` 解出响应体。 */
    fun decryptResponse(signedNonce: String, responseBase64: String): ByteArray =
        rc4(signedNonce, MiCrypto.base64Decode(responseBase64))

    /** 8 随机字节 + 大端(unix 秒 / 60)，共 12 字节。 */
    fun generateNonce(nowSeconds: Long = System.currentTimeMillis() / 1000): String {
        val buffer = ByteArray(12)
        Random.nextBytes(buffer, 0, 8)
        val minutesSinceEpoch = (nowSeconds / 60).toInt()
        buffer[8] = (minutesSinceEpoch shr 24).toByte()
        buffer[9] = (minutesSinceEpoch shr 16).toByte()
        buffer[10] = (minutesSinceEpoch shr 8).toByte()
        buffer[11] = minutesSinceEpoch.toByte()
        return MiCrypto.base64Encode(buffer)
    }

    fun computeSignedNonce(ssecurityBase64: String, nonceBase64: String): String {
        val ssecurity = MiCrypto.base64Decode(ssecurityBase64)
        val nonce = MiCrypto.base64Decode(nonceBase64)
        return MiCrypto.base64Encode(MiCrypto.sha256(ssecurity + nonce))
    }

    private fun rc4(signedNonceBase64: String, plaintext: ByteArray): ByteArray =
        SkippedRC4(MiCrypto.base64Decode(signedNonceBase64)).process(plaintext)
}

/**
 * 小米 passport 的 STS 客户端签名（对应 APK 的 `XMPassport.getClientSign`）。
 * 只有「用 passToken 续期」那条路会用到；WebView 登录拿到的 STS 授权是服务端自己签的。
 */
object MiPassportSts {

    /** `base64(SHA1("nonce=<nonce>&" + ssecurity))`。 */
    fun clientSign(nonce: String, ssecurity: String): String {
        require(nonce.isNotEmpty()) { "nonce required for clientSign" }
        require(ssecurity.isNotEmpty()) { "ssecurity required for clientSign" }
        return MiCrypto.base64Encode(MiCrypto.sha1("nonce=$nonce&$ssecurity".toByteArray()))
    }

    /** 把 `clientSign` 与 `_userIdNeedEncrypt` 拼到 STS 授权地址后面。 */
    fun signedLocationUrl(location: String, nonce: String, ssecurity: String): String {
        val sign = java.net.URLEncoder.encode(clientSign(nonce, ssecurity), "UTF-8")
        val sep = if (location.contains('?')) '&' else '?'
        return "$location${sep}clientSign=$sign&_userIdNeedEncrypt=true"
    }

    /** `UPPER(hex(MD5(passToken)))`，与响应头 `re-pass-token` 对照用。 */
    fun passTokenMd5Upper(passToken: String): String =
        MiCrypto.md5UpperHex(passToken.toByteArray())
}
