package cn.pocket.toolbox.data.micloud

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 小米令牌的本机加密箱子。
 *
 * 三条纪律：
 * 1. **令牌只在加密后落盘**。密钥由 Android Keystore 生成并保管，**私钥部分不出安全硬件**
 *    （有 TEE / StrongBox 的机器上就在里面），应用只能请求它做加解密，读不出密钥本身；
 * 2. **断开连接时连密钥一起删**。只删密文的话，留在存储里的那串字节仍然可解——
 *    用户点「断开并删除令牌」，得到的结果必须是「这东西没了」；
 * 3. **不打印**。这里任何一条路径都不把明文写进日志、异常消息或界面文案。
 *
 * 算法是 AES-256/GCM（自带完整性校验：密文被改动过就解不开，返回 null），
 * 每次加密随机 12 字节 IV，与密文拼在一起后做 Base64。
 */
object MiTokenVault {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "bodycast.micloud.token.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    /** 加密。Keystore 不可用（极少数定制 ROM）时返回 null，调用方按「没能保存」处理。 */
    fun seal(plain: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain.toByteArray())
        Base64.getEncoder().encodeToString(iv + sealed)
    }.getOrNull()

    /** 解密。解不开（密钥被系统重置、密文被改动过）返回 null。 */
    fun open(sealed: String): String? = runCatching {
        val raw = Base64.getDecoder().decode(sealed)
        if (raw.size <= IV_BYTES) return null
        val iv = raw.copyOfRange(0, IV_BYTES)
        val body = raw.copyOfRange(IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(body).decodeToString()
    }.getOrNull()

    /** 断开连接：把密钥删掉。删过之后旧的密文再也解不开。 */
    fun deleteKey() {
        runCatching {
            val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // 不要求解锁屏幕才能解密：同步可能在锁屏后发生，而令牌本身不是用户口令。
                // 它只是「这个账号的一次会话」，随时可在设置里断开删除。
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    /**
     * 自检用：随便走一遍「存 → 读 → 删」，回答三个问题——密文是不是明文、能不能读回来、
     * 删掉之后还在不在。**只回报布尔与长度，不回报内容**。
     */
    data class RoundTrip(
        val cipherTextDiffersFromPlain: Boolean,
        val decryptedMatches: Boolean,
        val removedAfterDelete: Boolean,
        val keyUsable: Boolean
    )

    fun selfTest(): RoundTrip {
        val probe = "probe-" + System.currentTimeMillis()
        val sealed = seal(probe) ?: return RoundTrip(false, false, false, false)
        val opened = open(sealed)
        deleteKey()
        val afterDelete = open(sealed)
        return RoundTrip(
            cipherTextDiffersFromPlain = sealed != probe,
            decryptedMatches = opened == probe,
            removedAfterDelete = afterDelete == null,
            keyUsable = true
        )
    }
}
