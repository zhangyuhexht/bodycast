package cn.pocket.toolbox.data.micloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.random.Random

/**
 * 小米账号的一次会话。
 *
 * **移植/改编自 [ilyasaftr/better-mi-fitness-sync](https://github.com/ilyasaftr/better-mi-fitness-sync)
 * 的 `miclient` 模块（MIT License，Copyright (c) 2026 Ilyasa Fathur Rahman）。**
 * 字段含义与原实现一致：
 * - [passToken] 长期有效（passport 层），用来续期；
 * - [serviceToken] 与 [ssecurity] 是短期的会话凭据；
 * - [cUserId] 是健康云用的加密用户 id（APK `encryptedUserId`）。
 *
 * 这份结构**只在加密后落盘**（见 [MiTokenVault]）。
 */
data class MiSession(
    val userId: String,
    val ssecurity: String,
    val serviceToken: String,
    val passToken: String,
    val deviceId: String,
    /** cn / sg / us / de / ru / i2 */
    val region: String,
    val cUserId: String = "",
    /** 界面上显示的账号线索：邮箱前缀 + 域名，**不是**完整账号。 */
    val accountHint: String = ""
) {
    fun toJson(): String = JSONObject().apply {
        put("userId", userId)
        put("ssecurity", ssecurity)
        put("serviceToken", serviceToken)
        put("passToken", passToken)
        put("deviceId", deviceId)
        put("region", region)
        put("cUserId", cUserId)
        put("accountHint", accountHint)
    }.toString()

    companion object {
        fun fromJson(raw: String): MiSession? = runCatching {
            val o = JSONObject(raw)
            MiSession(
                userId = o.optString("userId"),
                ssecurity = o.optString("ssecurity"),
                serviceToken = o.optString("serviceToken"),
                passToken = o.optString("passToken"),
                deviceId = o.optString("deviceId"),
                region = o.optString("region").ifBlank { "sg" },
                cUserId = o.optString("cUserId"),
                accountHint = o.optString("accountHint")
            )
        }.getOrNull()

        /** 邮箱 / 手机号都只留线索：`abc***@qq.com`、手机号只留后四位。 */
        fun hintOf(account: String): String {
            val trimmed = account.trim()
            if (trimmed.isEmpty()) return ""
            val at = trimmed.indexOf('@')
            return if (at > 0) {
                val name = trimmed.substring(0, at)
                val head = if (name.length <= 3) name else name.substring(0, 3)
                "$head***${trimmed.substring(at)}"
            } else if (trimmed.length >= 4) {
                "***" + trimmed.takeLast(4)
            } else {
                trimmed
            }
        }
    }
}

/** 一次同步从云端读到的六类读数。**读不到的字段是 null，绝不填 0。** */
data class MiSnapshot(
    val restingHeartRate: Int? = null,
    val hrv: Int? = null,
    val sleepMinutes: Int? = null,
    val deepSleepRatio: Float? = null,
    val steps: Int? = null,
    /** 压力值（0-100）。小米云端有「压力」类数据时填，没有就留空。 */
    val stressLevel: Int? = null,
    val bloodOxygen: Int? = null,
    val activityMinutes: Int? = null,
    /**
     * 昨夜睡眠的**真实起止时间**（epoch 毫秒；0 = 云端没给出有效窗口）。
     * 只有时长没有窗口时留 0——写入 Health Connect 时不编造时间。
     */
    val sleepStartAt: Long = 0L,
    val sleepEndAt: Long = 0L,
    /** 今日运动记录的**真实起止时间**（epoch 毫秒；0 = 没拿到窗口）。 */
    val exerciseStartAt: Long = 0L,
    val exerciseEndAt: Long = 0L,
    /** 六类里读到了哪几类（界面按这个如实显示「读到 / 暂时没读到」）。 */
    val readKeys: Set<String> = emptySet(),
    /**
     * 这次有几类**因为请求失败**没读到（网络不通、服务端报错之类）。
     *
     * 和「账号里本来就没有这一类数据」是两件事：前者下次同步会再试，后者不会自己出现。
     * 界面分开说，不把网络问题说成「你没有数据」。
     */
    val failedReads: Int = 0
) {
    companion object {
        const val KEY_RESTING_HEART_RATE = "resting_heart_rate"
        const val KEY_HRV = "hrv"
        const val KEY_SLEEP = "sleep"
        const val KEY_STEPS = "steps"
    const val KEY_STRESS = "stress"
        const val KEY_BLOOD_OXYGEN = "blood_oxygen"
        const val KEY_ACTIVITY = "activity"
    }
}

/** 小米云这一路会遇到的失败。每一种都有对应的界面文案，不静默吞掉。 */
sealed class MiCloudException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** 令牌过期 / 被服务端拒绝：修法是重新连接一次。 */
    class Auth(message: String, cause: Throwable? = null) : MiCloudException(message, cause)

    /** 网络不通 / 超时 / DNS 失败。 */
    class Network(message: String, cause: Throwable? = null) : MiCloudException(message, cause)

    /** 被限流（HTTP 429）。 */
    class RateLimited(message: String) : MiCloudException(message)

    /** 服务端返回了别的东西（含业务码非 0）。 */
    class Server(val code: Int, message: String) : MiCloudException(message)

    /** 响应解不开或不是 JSON。 */
    class Undecryptable(message: String, cause: Throwable? = null) : MiCloudException(message, cause)

    /** 登录回来的东西不完整（缺 passToken / ssecurity / serviceToken）。 */
    class IncompleteLogin(message: String) : MiCloudException(message)
}

/**
 * 小米云 API 客户端（只读）。
 *
 * **这是非官方接口**：小米没有公开它，随时可能变更或限制。客户端只做两件事——
 * 用账号令牌去问「我这个账号里的运动健康数据是什么」，把结果读回来。
 * **不上传任何东西**：请求体里只有查询条件（要哪一类、哪个时间段）。
 *
 * 传输层用 `HttpsURLConnection`（不引第三方 HTTP 库），JSON 用 `org.json`。
 * 所有调用都在 [Dispatchers.IO] 上跑，连接与读取各 15 秒上限，失败抛 [MiCloudException]。
 */
object MiCloudApi {

    const val SID = "miothealth"
    const val STS_CALLBACK = "https://sts-hlth.io.mi.com/healthapp/sts"
    const val ACCOUNT_ORIGIN = "https://account.xiaomi.com"
    const val STS_ORIGIN = "https://sts-hlth.io.mi.com"

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_PAGES = 12

    /**
     * 自检专用的主机名：`.invalid` 是 RFC 2606 保留的顶级域，**永远不会解析**。
     * 拿它验证「连不上时报什么、崩不崩」，不接触任何真实服务。
     */
    const val SELF_CHECK_HOST = "bodycast-selfcheck.invalid"

    /** 官方 App 的 User-Agent（原实现照抄的形态）；服务端按它决定返回哪一套字段。 */
    private const val USER_AGENT = "APP/com.xiaomi.miwatch.pro APPV/3.49.1 " +
        "iosPassportSDK/4.2.64 iOS/18.7.8 MK/aVBob25lMTQsMw== " +
        "DEVT/aVBob25l DEVS/aU9T BRA/QXBwbGU= L/en_US miHSTS"

    private const val AUTH_KEY = "rwelJuWBFJxmbMKD"

    private val AUTH_BUSINESS_CODES = setOf(3, 401, 403, 1002, 10017, 70016)

    // ---------------------------------------------------------------------
    // 设备标识 / 登录地址 / 区域
    // ---------------------------------------------------------------------

    /** 登录用的设备标识：`wb_` + 32 位十六进制。生成一次就固定下来（原实现同款）。 */
    fun newDeviceId(): String = "wb_" + buildString(32) {
        repeat(32) { append("0123456789abcdef"[Random.nextInt(16)]) }
    }

    /**
     * 小米官方登录页（**用户只在这里输密码**）。
     *
     * `sid=miothealth` 是要授权的服务；`callback` 是登录成功后跳到的地方，
     * 也就是健康云的 STS 端点——它把 `serviceToken` 以 Cookie 形式发下来。
     */
    fun loginUrl(deviceId: String, locale: String = "zh_CN"): String {
        val sid = URLEncoder.encode(SID, "UTF-8")
        val callback = URLEncoder.encode(STS_CALLBACK, "UTF-8")
        val d = URLEncoder.encode(deviceId, "UTF-8")
        return "$ACCOUNT_ORIGIN/pass/serviceLogin?sid=$sid&callback=$callback&_locale=$locale&d=$d"
    }

    /** 「Cookie: a=1; b=2」或 `Set-Cookie` 头 → 名字到值的映射。 */
    fun parseCookies(header: String?): Map<String, String> {
        if (header.isNullOrBlank()) return emptyMap()
        val out = linkedMapOf<String, String>()
        header.split(';').forEach { part ->
            val trimmed = part.trim()
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                val name = trimmed.substring(0, eq).trim()
                val value = trimmed.substring(eq + 1).trim().trim('"')
                if (name.isNotEmpty() && value.isNotEmpty()) out[name] = value
            }
        }
        return out
    }

    /**
     * 从 `Set-Cookie` 行里取某个 cookie 的值（严格按名字匹配，跳过删除用的空值）。
     * 原实现的 `setCookieValue` 同款：`Max-Age=0`、空值、`EXPIRED` 一律不算数。
     */
    fun setCookieValue(headers: List<String>, name: String): String? {
        val dead = setOf("", "-", "expired", "deleted", "null")
        for (header in headers) {
            val firstPair = header.substringBefore(';').trim()
            val eq = firstPair.indexOf('=')
            if (eq <= 0) continue
            if (!firstPair.substring(0, eq).trim().equals(name, ignoreCase = true)) continue
            if (Regex("max-age\\s*=\\s*0(\\D|$)", RegexOption.IGNORE_CASE).containsMatchIn(header)) continue
            val value = firstPair.substring(eq + 1).trim().trim('"')
            if (value.lowercase() in dead) continue
            return value
        }
        return null
    }

    /** 从登录后的地址里取设备标识（`d=…`）。 */
    fun deviceIdFromUrl(url: String): String? = queryValue(url, "d")

    /** 从登录后的地址里取区域提示（`p_ur=SG` 这类国家码）。 */
    fun regionFromUrl(url: String): String? = queryValue(url, "p_ur")

    /** 国家码 → 区域代号，与原实现的 `resolveRegion` 一致。 */
    fun resolveRegion(countryCode: String?): String = when (countryCode?.uppercase(Locale.ROOT)) {
        "CN" -> "cn"
        "IN" -> "i2"
        "RU" -> "ru"
        "US", "BR", "CA", "MX" -> "us"
        "DE", "GB", "FR", "IT", "ES", "NL", "PL", "SE", "NO", "DK", "FI", "AT", "CH", "BE" -> "de"
        else -> "sg"
    }

    /** 区域 → 数据主机。**国行账号走 `hlth.io.mi.com`，没有前缀**。 */
    fun healthHost(region: String): String =
        if (region.lowercase(Locale.ROOT) == "cn") "hlth.io.mi.com" else "${region.lowercase(Locale.ROOT)}.hlth.io.mi.com"

    /** 区域代号 → 界面上给人看的名字。 */
    fun regionLabel(region: String): String = when (region.lowercase(Locale.ROOT)) {
        "cn" -> "中国大陆"
        "sg" -> "新加坡"
        "us" -> "美国"
        "de" -> "欧洲"
        "ru" -> "俄罗斯"
        "i2" -> "印度"
        else -> region
    }

    private fun queryValue(url: String, key: String): String? = runCatching {
        val query = url.substringAfter('?', "").substringBefore('#')
        query.split('&').firstOrNull { it.substringBefore('=') == key }?.substringAfter('=', "")
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    // ---------------------------------------------------------------------
    // 登录收尾
    // ---------------------------------------------------------------------

    /**
     * 官方登录页回到 STS 地址之后，把会话凑齐。
     *
     * 输入是 WebView 那一刻的 Cookie（两个域的文本形式）与最终地址。这里**不碰密码**：
     * 密码从头到尾只在小米自己的页面里。缺 [MiSession.passToken] 就如实报错——
     * 没有它这份会话过期后无法续期，存下来只会给用户一个「某天忽然失效」的哑谜。
     */
    suspend fun completeWebLogin(
        deviceId: String,
        finalUrl: String,
        accountCookieHeader: String?,
        healthCookieHeader: String?
    ): MiSession = withContext(Dispatchers.IO) {
        val account = parseCookies(accountCookieHeader)
        val health = parseCookies(healthCookieHeader)

        val userId = account["userId"].orEmpty()
        val passToken = account["passToken"].orEmpty()
        val cUserId = health["cUserId"] ?: account["cUserId"] ?: ""
        val serviceToken = health["serviceToken"]
            ?: health["${SID}_serviceToken"]
            ?: account["serviceToken"]
            ?: account["${SID}_serviceToken"]
            ?: ""

        if (passToken.isBlank()) {
            throw MiCloudException.IncompleteLogin(
                "登录完成了，但没拿到可续期的凭据（passToken）。请在小米页面里完成登录后马上返回。"
            )
        }
        if (userId.isBlank()) {
            throw MiCloudException.IncompleteLogin("登录完成了，但没拿到账号标识（userId）。")
        }

        // 区域优先按登录地址里的国家码，其次按登录服务器给的区域，最后按新加坡兜底。
        val region = resolveRegion(regionFromUrl(finalUrl))

        val session = if (serviceToken.isNotBlank()) {
            // ssecurity 是加密请求的必需件；它由 passport 在带 passToken 的查询里给出。
            val harvest = harvestSsecurity(userId, passToken, deviceId)
            MiSession(
                userId = userId,
                ssecurity = harvest?.ssecurity.orEmpty(),
                serviceToken = serviceToken,
                passToken = harvest?.rotatedPassToken?.takeIf { it.isNotBlank() } ?: passToken,
                deviceId = deviceId,
                region = region,
                cUserId = cUserId
            ).also {
                if (it.ssecurity.isBlank()) {
                    throw MiCloudException.IncompleteLogin("登录完成了，但没能取得会话密钥（ssecurity）。")
                }
            }
        } else {
            // 没直接拿到 serviceToken：用 passToken 走一遍官方续期路径（这也是令牌过期后的修法）。
            refreshSession(
                MiSession(
                    userId = userId,
                    ssecurity = "",
                    serviceToken = "",
                    passToken = passToken,
                    deviceId = deviceId,
                    region = region,
                    cUserId = cUserId
                )
            )
        }
        attachAccountHint(session)
    }

    /**
     * 给会话补一个**只在本机显示**的账号线索。
     *
     * 说明白一件事：WebView 登录这条路**拿不到账号字符串**（邮箱 / 手机号压根不出现在
     * Cookie 或回调里，而我们不去扒登录页的内容）。所以线索按两档取：
     * ① 健康云里的昵称（官方接口 `get_miot_user_profile` 的 `name`，读不到不影响连接）；
     * ② 兜底用 userId 的掩码（只留后四位）。两者都不是邮箱，界面文案也照这个如实写。
     */
    private fun attachAccountHint(session: MiSession): MiSession {
        val nickname = runCatching { profileName(session) }.getOrNull()
        val hint = nickname?.takeIf { it.isNotBlank() } ?: MiSession.hintOf(session.userId)
        return session.copy(accountHint = hint)
    }

    /** 账号昵称（读不到就是 null，不影响连接与同步）。 */
    private fun profileName(session: MiSession): String? {
        val response = post(
            host = healthHost(session.region),
            session = session,
            path = "/healthapp/user/get_miot_user_profile",
            signPath = "/user/get_miot_user_profile",
            payload = "{}"
        )
        return response.optJSONObject("result")?.optString("name")?.takeIf { it.isNotBlank() }
    }

    /**
     * 用 passToken 换一份新的 serviceToken（必要时连 ssecurity 一起换）。
     * 对应原实现的 `refreshWithPassToken` + 官方 SDK 的 STS 签名。
     */
    suspend fun refreshSession(session: MiSession): MiSession = withContext(Dispatchers.IO) {
        val deviceId = session.deviceId.ifBlank { newDeviceId() }
        val cookie = buildString {
            append("userId=${session.userId}; passToken=${session.passToken}")
            if (deviceId.isNotBlank()) append("; deviceId=$deviceId")
        }
        val url = "$ACCOUNT_ORIGIN/pass/serviceLogin?sid=$SID&_json=true&d=${URLEncoder.encode(deviceId, "UTF-8")}"
        val response = get(url, cookie = cookie, followRedirects = false, userAgent = USER_AGENT)
        val setCookies = response.headers.all("Set-Cookie")
        val rotated = setCookieValue(setCookies, "passToken")
        val body = stripJsonPrefix(response.body)
        val obj = runCatching { JSONObject(body) }.getOrNull()
            ?: throw MiCloudException.Server(response.code, "小米账号没有按预期返回内容（HTTP ${response.code}）。")

        val code = obj.optInt("code", -1)
        if (code != 0) {
            val desc = obj.optString("desc").ifBlank { obj.optString("description") }
            throw if (code == 70016 || code in AUTH_BUSINESS_CODES) {
                MiCloudException.Auth(desc.ifBlank { "小米账号不再接受这份会话（代码 $code），需要重新连接。" })
            } else {
                MiCloudException.Server(code, desc.ifBlank { "小米账号返回了代码 $code。" })
            }
        }
        if (obj.optInt("securityStatus", 0) != 0) {
            throw MiCloudException.Auth("小米要求重新验证这次登录（securityStatus=${obj.optInt("securityStatus")}）。")
        }

        var ssecurity = obj.optString("ssecurity")
        var passToken = rotated ?: obj.optString("passToken").takeIf { it.isNotBlank() } ?: session.passToken
        if (ssecurity.isBlank()) {
            val harvest = harvestSsecurity(session.userId, passToken, deviceId)
            ssecurity = harvest?.ssecurity.orEmpty()
            harvest?.rotatedPassToken?.takeIf { it.isNotBlank() }?.let { passToken = it }
        }
        if (ssecurity.isBlank()) {
            throw MiCloudException.IncompleteLogin("续期时没拿到会话密钥（ssecurity）。")
        }

        val location = obj.optString("location")
        val nonce = obj.opt("nonce")?.toString().orEmpty().takeIf { it != "null" }.orEmpty()
        val serviceToken: String
        if (location.isNotBlank()) {
            val absolute = if (location.startsWith("http")) location else "$ACCOUNT_ORIGIN$location"
            val signed = if (nonce.isNotBlank()) MiPassportSts.signedLocationUrl(absolute, nonce, ssecurity) else absolute
            serviceToken = collectServiceToken(signed)
        } else {
            serviceToken = setCookieValue(setCookies, "serviceToken")
                ?: setCookieValue(setCookies, "${SID}_serviceToken")
                ?: session.serviceToken
        }
        if (serviceToken.isBlank()) {
            throw MiCloudException.Auth("续期没换来新的会话令牌，需要重新连接小米账号。")
        }

        val cUserId = setCookieValue(setCookies, "cUserId")
            ?: obj.optString("cUserId").takeIf { it.isNotBlank() }
            ?: session.cUserId

        session.copy(
            ssecurity = ssecurity,
            serviceToken = serviceToken,
            passToken = passToken,
            deviceId = deviceId,
            cUserId = cUserId
        )
    }

    private data class Harvest(val ssecurity: String, val rotatedPassToken: String?)

    /**
     * 取 `ssecurity`：带 userId + passToken 去问一次 passport，
     * 值在 `Extension-Pragma` 头或响应体的 JSON 里（两个位置原实现都遇到）。
     */
    private fun harvestSsecurity(userId: String, passToken: String, deviceId: String): Harvest? {
        if (userId.isBlank() || passToken.isBlank()) return null
        val url = "$ACCOUNT_ORIGIN/pass/serviceLogin?sid=$SID&_json=true&d=${URLEncoder.encode(deviceId, "UTF-8")}"
        val cookie = "userId=$userId; passToken=$passToken" + if (deviceId.isNotBlank()) "; deviceId=$deviceId" else ""
        val response = runCatching { get(url, cookie = cookie, followRedirects = false, userAgent = USER_AGENT) }
            .getOrNull() ?: return null
        val rotated = setCookieValue(response.headers.all("Set-Cookie"), "passToken")
        val pragma = response.headers["extension-pragma"] ?: response.headers["Extension-Pragma"]
        if (pragma != null) {
            val value = runCatching { JSONObject(pragma).optString("ssecurity") }.getOrNull()
            if (!value.isNullOrBlank()) return Harvest(value, rotated)
        }
        val body = runCatching { JSONObject(stripJsonPrefix(response.body)) }.getOrNull() ?: return null
        val value = body.optString("ssecurity")
        return if (value.isBlank()) null else Harvest(value, rotated)
    }

    /** 跟着 STS 授权地址走，从 `Set-Cookie` 里收集 `serviceToken`（最多 12 跳）。 */
    private fun collectServiceToken(startUrl: String): String {
        var current = startUrl
        repeat(12) {
            val response = get(current, cookie = null, followRedirects = false, userAgent = USER_AGENT)
            val setCookies = response.headers.all("Set-Cookie")
            val token = setCookieValue(setCookies, "${SID}_serviceToken")
                ?: setCookieValue(setCookies, "serviceToken")
                ?: response.headers["serviceToken"]
                ?: response.headers["${SID}_serviceToken"]
            if (!token.isNullOrBlank()) return token
            val location = response.headers["Location"]
            if (response.code !in 300..399 || location.isNullOrBlank()) return ""
            current = if (location.startsWith("http")) location else "$ACCOUNT_ORIGIN$location"
        }
        return ""
    }

    // ---------------------------------------------------------------------
    // 数据读取（六类）
    // ---------------------------------------------------------------------

    /**
     * 读一次这个账号的六类读数。
     *
     * 每一类都是**独立取、独立失败**：某一类读不到只是那一项留在 null，
     * 不影响其它几类——「读不到就说读不到」在这里就是不把整次同步判死。
     *
     * 但**失败不能静默**：
     * - 任何一类报「会话失效」→ 整次同步判失败（[MiCloudException.Auth]），界面提示重新连接；
     * - 六类全都没读上来，而且每一次都是请求失败 → 把第一个错误抛出去（网络不通要说网络不通），
     *   不能显示成「同步完成，但六类都没读到」；
     * - 只失败了一部分 → 返回读到的那几类，并把失败数量带在 [MiSnapshot.failedReads] 里。
     *
     * @param hostOverride 只给自检用：把请求指向一个指定的主机（默认按账号区域取）。
     */
    suspend fun fetchSnapshot(
        session: MiSession,
        nowMs: Long = System.currentTimeMillis(),
        hostOverride: String? = null
    ): MiSnapshot =
        withContext(Dispatchers.IO) {
            val host = hostOverride ?: healthHost(session.region)
            val zone = java.util.TimeZone.getDefault()
            val dayStart = startOfLocalDay(nowMs, zone)
            val nowSec = nowMs / 1000

            val result = MiSnapshotBuilder()
            val failures = mutableListOf<MiCloudException>()

            /** 跑一段读取；失败只记账，不让整次同步崩掉。 */
            fun <T> attempt(block: () -> List<T>): List<T> = try {
                block()
            } catch (e: MiCloudException) {
                failures += e
                emptyList()
            } catch (e: Exception) {
                failures += MiCloudException.Network(e.message ?: "请求失败", e)
                emptyList()
            }

            // ① 睡眠 + ② HRV（同一个数据里出：HRV 挂在睡眠记录上）
            val sleep = attempt {
                latest(host, session, "sleep,watch_night_sleep,watch_daytime_sleep")
            }
            // 一次可能返回好几晚：取**最近的一晚**（按醒来时间倒序），
            // 没有有效的就整类留空——不拿前天的睡眠冒充昨夜。
            val lastNight = sleep
                .filter { it.optLongOrNull("wake_up_time") != null || it.optLongOrNull("bedtime") != null }
                .sortedByDescending { it.optLongOrNull("wake_up_time") ?: it.optLongOrNull("bedtime") ?: 0L }
                .firstOrNull()
            lastNight?.let { s ->
                val bedtime = secOf(s.optLongOrNull("bedtime") ?: 0L)
                val wakeUp = secOf(s.optLongOrNull("wake_up_time") ?: 0L)
                // 真实起止窗口（22.42.0）：写入 Health Connect 的睡眠段要用，不编造时间。
                if (bedtime > 0 && wakeUp > bedtime) {
                    result.sleepStartAt = bedtime * 1000L
                    result.sleepEndAt = wakeUp * 1000L
                }
                val (asleepSeconds, deepSeconds) = sleepStageSeconds(s.optJSONArray("items"))
                val totalSeconds = if (asleepSeconds > 0) asleepSeconds else (wakeUp - bedtime).coerceAtLeast(0L)
                if (totalSeconds > 0) {
                    result.sleepMinutes = (totalSeconds / 60).toInt()
                    result.readKeys += MiSnapshot.KEY_SLEEP
                }
                if (deepSeconds > 0 && totalSeconds > 0) {
                    result.deepSleepRatio = (deepSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                }
                val hrv = s.optIntOrNull("avg_hrv") ?: s.optIntOrNull("avgHrv")
                if (hrv != null && hrv in 5..300) {
                    result.hrv = hrv
                    result.readKeys += MiSnapshot.KEY_HRV
                }
            }
            // HRV 补抓（22.37.0）：睡眠记录里没挂 avg_hrv 的手环/固件，
            // 单独有 hrv 类的数据——别名一起问，取最新一条合理值。
            if (result.hrv == null) {
                attempt { latest(host, session, "hrv,hrv_avg,avg_hrv,rmssd") }
                    .sortedByDescending { it.optLongOrNull("time") ?: it.optLongOrNull("date_time") ?: 0L }
                    .firstNotNullOfOrNull { item ->
                        (item.optIntOrNull("value") ?: item.optIntOrNull("hrv")
                            ?: item.optIntOrNull("avg_hrv") ?: item.optIntOrNull("rmssd"))
                            ?.takeIf { it in 5..300 }
                    }?.let {
                        result.hrv = it
                        result.readKeys += MiSnapshot.KEY_HRV
                    }
            }

            // ③ 静息心率（"latest" 端点，载荷里是 date_time 不是 time）
            attempt { latest(host, session, "resting_heart_rate") }
                .sortedByDescending { it.optLongOrNull("date_time") ?: it.optLongOrNull("time") ?: 0L }
                .firstNotNullOfOrNull { item ->
                    item.optIntOrNull("bpm")?.takeIf { it in 25..200 }
                }?.let {
                    result.restingHeartRate = it
                    result.readKeys += MiSnapshot.KEY_RESTING_HEART_RATE
                }

            // ④ 今日步数：小米记的是每分钟增量，这里把今天的加起来
            attempt { byTime(host, session, "steps", dayStart, nowSec) }.let { items ->
                var total = 0
                items.forEach { item -> item.optIntOrNull("steps")?.let { if (it > 0) total += it } }
                if (total > 0) {
                    result.steps = total
                    result.readKeys += MiSnapshot.KEY_STEPS
                }
            }

            // ④.5 压力：小米云端的「压力」类（各家手环都有，键名不统一，
            // 多带几个别名一起问，认到哪个算哪个）。取值范围 0-100，超范围不收。
            attempt { latest(host, session, "pressure,stress,stress_score,watch_stress,watch_pressure") }
                .sortedByDescending { it.optLongOrNull("time") ?: it.optLongOrNull("date_time") ?: 0L }
                .firstNotNullOfOrNull { item -> stressValueOf(item) }?.let {
                    result.stressLevel = it
                    result.readKeys += MiSnapshot.KEY_STRESS
                }

            // ⑤ 血氧：先看今天的记录，今天还没量就退一步看最近 24 小时里量过的那一次
            //（血氧是随手量的，不是每天都有；但超过 24 小时的一律不顶上来）。
            attempt { byTime(host, session, "spo2", dayStart, nowSec) }.let { items ->
                items.firstNotNullOfOrNull { item -> item.optIntOrNull("spo2")?.takeIf { it in 50..100 } }?.let {
                    result.bloodOxygen = it
                    result.readKeys += MiSnapshot.KEY_BLOOD_OXYGEN
                }
            }
            if (result.bloodOxygen == null) {
                attempt { latest(host, session, "spo2", limit = 5) }
                    .sortedByDescending { it.optLongOrNull("time") ?: it.optLongOrNull("date_time") ?: 0L }
                    .firstNotNullOfOrNull { item ->
                        val measuredAt = secOf(item.optLongOrNull("time") ?: item.optLongOrNull("date_time") ?: 0L)
                        val fresh = measuredAt <= 0L || nowSec - measuredAt <= 24 * 60 * 60
                        item.optIntOrNull("spo2")?.takeIf { it in 50..100 && fresh }
                    }?.let {
                        result.bloodOxygen = it
                        result.readKeys += MiSnapshot.KEY_BLOOD_OXYGEN
                    }
            }

            // ⑥ 今日运动时长：把今天的运动记录时长相加
            attempt { sportRecords(host, session, dayStart, nowSec) }.let { records ->
                var seconds = 0L
                var bestStart = 0L
                var bestEnd = 0L
                records.forEach { record ->
                    val startAt = secOf(record.optLongOrNull("start_time") ?: 0L)
                    val endAt = secOf(record.optLongOrNull("end_time") ?: 0L)
                    val duration = secOf(record.optLongOrNull("duration") ?: 0L)
                        .takeIf { it > 0 }
                        ?: (endAt - startAt)
                    if (duration > 0) seconds += duration
                    // 真实起止窗口（22.42.0）：写入 HC 的运动段要用；只有时长、
                    // 没有起止时间的记录不编窗口——拿「今天之内最晚结束」的那条。
                    if (startAt > 0 && endAt > startAt && endAt >= bestEnd) {
                        bestStart = startAt
                        bestEnd = endAt
                    }
                }
                if (bestStart > 0) {
                    result.exerciseStartAt = bestStart * 1000L
                    result.exerciseEndAt = bestEnd * 1000L
                }
                if (seconds > 0) {
                    result.activityMinutes = (seconds / 60).toInt()
                    result.readKeys += MiSnapshot.KEY_ACTIVITY
                }
            }

            // 会话失效是整次同步的失败——不管前面读到了什么，都要让用户去重新连接。
            failures.firstOrNull { it is MiCloudException.Auth }?.let { throw it }
            // 一类都没读到、而且每一次都是失败：抛第一个错误（网络不通就说网络不通），
            // 不能显示成「同步完成，但六类都没读到」。
            if (result.readKeys.isEmpty() && failures.isNotEmpty()) throw failures.first()

            return@withContext result.build(failedReads = failures.size)
        }

    private class MiSnapshotBuilder {
        var restingHeartRate: Int? = null
        var hrv: Int? = null
        var sleepMinutes: Int? = null
        var deepSleepRatio: Float? = null
        var steps: Int? = null
        var stressLevel: Int? = null
        var bloodOxygen: Int? = null
        var activityMinutes: Int? = null
        var sleepStartAt: Long = 0L
        var sleepEndAt: Long = 0L
        var exerciseStartAt: Long = 0L
        var exerciseEndAt: Long = 0L
        val readKeys = linkedSetOf<String>()

        fun build(failedReads: Int = 0) = MiSnapshot(
            restingHeartRate = restingHeartRate,
            hrv = hrv,
            sleepMinutes = sleepMinutes,
            deepSleepRatio = deepSleepRatio,
            steps = steps,
            stressLevel = stressLevel,
            bloodOxygen = bloodOxygen,
            activityMinutes = activityMinutes,
            sleepStartAt = sleepStartAt,
            sleepEndAt = sleepEndAt,
            exerciseStartAt = exerciseStartAt,
            exerciseEndAt = exerciseEndAt,
            readKeys = readKeys.toSet(),
            failedReads = failedReads
        )
    }

    /** `get_latest_fitness_data`：一次问多个 key，每个 key 返回最新几条。 */
    private fun latest(host: String, session: MiSession, keys: String, limit: Int = 30): List<JSONObject> {
        val params = JSONArray()
        keys.split(',').forEach { key ->
            params.put(JSONObject().put("key", key.trim()).put("limit", limit))
        }
        val response = post(
            host = host,
            session = session,
            path = "/app/v1/data/get_latest_fitness_data",
            payload = JSONObject().put("params", params).toString()
        )
        return dataList(response)
    }

    /** `get_fitness_data_by_time`：一段时间里的记录，翻页取全。 */
    private fun byTime(
        host: String,
        session: MiSession,
        key: String,
        fromSec: Long,
        toSec: Long
    ): List<JSONObject> {
        val all = mutableListOf<JSONObject>()
        var nextKey: String? = null
        var page = 0
        while (page < MAX_PAGES) {
            val payload = JSONObject().apply {
                put("key", key)
                put("start_time", fromSec)
                put("end_time", toSec)
                put("reverse", true)
                if (!nextKey.isNullOrEmpty()) put("next_key", nextKey)
            }
            val response = post(
                host = host,
                session = session,
                path = "/app/v1/data/get_fitness_data_by_time",
                payload = payload.toString()
            )
            val result = response.optJSONObject("result") ?: break
            all += dataList(response)
            page++
            val hasMore = result.optBoolean("has_more", false)
            val next = result.optString("next_key")
            if (!hasMore || next.isBlank()) break
            nextKey = next
        }
        return all
    }

    /** `get_sport_records_by_time`：运动记录（时长从这里算）。 */
    private fun sportRecords(host: String, session: MiSession, fromSec: Long, toSec: Long): List<JSONObject> {
        val all = mutableListOf<JSONObject>()
        var nextKey: String? = null
        var page = 0
        while (page < MAX_PAGES) {
            val payload = JSONObject().apply {
                put("start_time", fromSec)
                put("end_time", toSec)
                put("limit", 50)
                if (!nextKey.isNullOrEmpty()) put("next_key", nextKey)
            }
            val response = post(
                host = host,
                session = session,
                path = "/app/v1/data/get_sport_records_by_time",
                payload = payload.toString()
            )
            val result = response.optJSONObject("result") ?: break
            val records = result.optJSONArray("sport_records")
            if (records != null) {
                for (i in 0 until records.length()) {
                    valueObject(records.optJSONObject(i))?.let { all += it }
                }
            }
            page++
            val hasMore = result.optBoolean("has_more", false)
            val next = result.optString("next_key")
            if (!hasMore || next.isBlank()) break
            nextKey = next
        }
        return all
    }

    /**
     * 压力值解析：`value` / `score` / `stress` / `pressure` 四种字段名都认，
     * 0-100 之外的不收（不猜单位）。
     */
    internal fun stressValueOf(body: JSONObject): Int? {
        val raw = body.optIntOrNull("value")
            ?: body.optIntOrNull("score")
            ?: body.optIntOrNull("stress")
            ?: body.optIntOrNull("pressure")
        return raw?.takeIf { it in 0..100 }
    }

    /**
     * 时间戳归一到秒（22.35.0）：小米不同端点有秒也有毫秒，
     * `> 10^12`（约 2001 年 9 月之后的毫秒值）视为毫秒。
     * 不归一的话睡眠时长会算出天文数字、血氧新鲜度会判反。
     */
    internal fun secOf(raw: Long): Long =
        if (raw > 1_000_000_000_000L) raw / 1000L else raw

    /**
     * 睡眠段位求和：返回 (睡着秒数, 深睡秒数)。
     * Mi 的段位编号：2=深睡 3=浅睡 4=REM 5=清醒。清醒不算睡眠时长。
     */
    internal fun sleepStageSeconds(stages: JSONArray?): Pair<Long, Long> {
        var asleep = 0L
        var deep = 0L
        if (stages != null) {
            for (i in 0 until stages.length()) {
                val stage = stages.optJSONObject(i) ?: continue
                val state = stage.optInt("state", -1)
                val from = secOf(stage.optLong("start_time", 0L))
                val to = secOf(stage.optLong("end_time", 0L))
                if (to <= from) continue
                if (state in 2..4) asleep += (to - from)
                if (state == 2) deep += (to - from)
            }
        }
        return asleep to deep
    }

    private fun dataList(response: JSONObject): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        val list = response.optJSONObject("result")?.optJSONArray("data_list") ?: return out
        for (i in 0 until list.length()) {
            valueObject(list.optJSONObject(i))?.let { out += it }
        }
        return out
    }

    /**
     * 一条记录里的 `value` 有两种形态：JSON 字符串（绝大多数）或对象。
     * 两种都吃；`value` 整个没有时（部分端点把字段直接摊在条目上），
     * 把条目本身当载荷兜底——认不出才跳过，不猜。
     */
    internal fun valueObject(item: JSONObject?): JSONObject? {
        if (item == null) return null
        val value = item.opt("value")
        val body: JSONObject? = if (value is JSONObject) {
            value
        } else if (value is String) {
            runCatching { JSONObject(value) }.getOrNull()
        } else {
            // 内联载荷兜底（22.35.0）：条目自带字段就当它本身就是 value。
            if (item.has("steps") || item.has("spo2") || item.has("bpm") ||
                item.has("wake_up_time") || item.has("duration") || item.has("avg_hrv")
            ) {
                JSONObject(item.toString())
            } else {
                null
            }
        }
        if (body == null) return null
        // 外层的 time / key 补进来，有些载荷里只有一处有。
        if (!body.has("time") && item.optLongOrNull("time") != null) body.put("time", item.optLong("time"))
        if (!body.has("key") && item.optString("key").isNotBlank()) body.put("key", item.optString("key"))
        return body
    }

    // ---------------------------------------------------------------------
    // 传输层
    // ---------------------------------------------------------------------

    /**
     * 加密 POST：签名 + RC4 加密请求体，响应解密回明文 JSON。
     *
     * @param path 请求路径。
     * @param signPath 参与签名的路径：多数端点与 [path] 相同，少数（`/healthapp/...`）不同。
     */
    private fun post(
        host: String,
        session: MiSession,
        path: String,
        payload: String,
        signPath: String = path
    ): JSONObject {
        if (session.ssecurity.isBlank() || session.serviceToken.isBlank()) {
            throw MiCloudException.Auth("这份会话不完整，需要重新连接小米账号。")
        }
        val encrypted = MiCloudSigner.buildEncryptedRequest(signPath, session.ssecurity, payload)
        val form = listOf(
            "_nonce" to encrypted.nonce,
            "data" to encrypted.data,
            "rc4_hash__" to encrypted.rc4Hash,
            "signature" to encrypted.signature
        ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

        val response = try {
            request(
                url = "https://$host$path",
                method = "POST",
                body = form.toByteArray(),
                contentType = "application/x-www-form-urlencoded",
                cookie = cookieHeader(session),
                followRedirects = false
            )
        } catch (e: MiCloudException) {
            throw e
        } catch (e: IOException) {
            throw MiCloudException.Network(e.message ?: "网络不通", e)
        }

        when (response.code) {
            401, 403 -> throw MiCloudException.Auth("小米云拒绝了这次请求（HTTP ${response.code}）。")
            429 -> throw MiCloudException.RateLimited("小米云限流了（HTTP 429），过一会儿再试。")
            !in 200..299 -> throw MiCloudException.Server(
                response.code,
                "小米云返回 HTTP ${response.code}：${response.body.take(120)}"
            )
        }

        val decrypted = try {
            MiCloudSigner.decryptResponse(encrypted.signedNonce, response.body.trim()).decodeToString()
        } catch (e: Exception) {
            // 过期会话常见返回 HTML 登录页或一串解不开的东西。
            if (response.body.trim().startsWith("<") || response.body.contains("login", ignoreCase = true)) {
                throw MiCloudException.Auth("小米云返回了登录页，这份会话已经失效。", e)
            }
            throw MiCloudException.Undecryptable("小米云的响应解不开（会话可能已失效）。", e)
        }

        val json = runCatching { JSONObject(decrypted) }.getOrElse {
            throw MiCloudException.Undecryptable("小米云返回的不是 JSON。", it)
        }
        val code = json.optInt("code", 0)
        if (code != 0) {
            val message = json.optString("message").ifBlank { json.optString("msg") }
                .ifBlank { json.optString("description") }
            if (code in AUTH_BUSINESS_CODES || looksLikeAuth(message)) {
                throw MiCloudException.Auth(message.ifBlank { "小米云拒绝了这次请求（代码 $code）。" })
            }
        }
        return json
    }

    private fun looksLikeAuth(message: String): Boolean {
        if (message.isBlank()) return false
        val lower = message.lowercase(Locale.ROOT)
        return listOf("login", "token", "unauth", "expired", "session", "not auth").any { it in lower }
    }

    private fun cookieHeader(session: MiSession): String {
        val identity = if (session.cUserId.isNotBlank()) "cUserId=${session.cUserId}" else "userId=${session.userId}"
        return listOf(
            "serviceToken=${session.serviceToken}",
            identity,
            "PassportDeviceId=${session.deviceId}",
            "locale=en_US",
            "auth_key=$AUTH_KEY"
        ).joinToString("; ")
    }

    private class HttpResponse(
        val code: Int,
        val body: String,
        val headers: HeaderView
    )

    /** 响应头的小包装：大小写不敏感，`all()` 拿去查 `Set-Cookie`。 */
    private class HeaderView(private val fields: Map<String, List<String>>) {
        operator fun get(name: String): String? =
            fields.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

        fun all(name: String): List<String> =
            fields.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value.orEmpty()
    }

    private fun get(url: String, cookie: String?, followRedirects: Boolean, userAgent: String): HttpResponse =
        request(url, "GET", null, null, cookie, followRedirects, userAgent)

    private fun request(
        url: String,
        method: String,
        body: ByteArray?,
        contentType: String?,
        cookie: String?,
        followRedirects: Boolean,
        userAgent: String = USER_AGENT
    ): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = followRedirects
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Accept-Language", "en_US")
            if (!cookie.isNullOrBlank()) setRequestProperty("Cookie", cookie)
            if (contentType != null) setRequestProperty("Content-Type", contentType)
            if (body != null) doOutput = true
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return HttpResponse(code, text, HeaderView(connection.headerFields ?: emptyMap()))
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    // ---------------------------------------------------------------------
    // 小工具
    // ---------------------------------------------------------------------

    private fun stripJsonPrefix(body: String): String = body.removePrefix("&&&START&&&").trim()

    /** 设备本地的「今天零点」对应的 unix 秒。 */
    fun startOfLocalDay(nowMs: Long, zone: java.util.TimeZone): Long {
        val calendar = java.util.Calendar.getInstance(zone)
        calendar.timeInMillis = nowMs
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis / 1000
    }

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (isNull(name)) null else runCatching { getLong(name) }.getOrNull()
            ?: runCatching { optString(name).toLongOrNull() }.getOrNull()

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (isNull(name)) null else runCatching { getInt(name) }.getOrNull()
            ?: runCatching { optString(name).toDoubleOrNull()?.toInt() }.getOrNull()
}
