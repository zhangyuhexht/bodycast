package cn.pocket.toolbox.data

import android.content.Context
import cn.pocket.toolbox.bodycast.BodyCastPrefs
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.bodycast.BodySignals
import cn.pocket.toolbox.bodycast.EntryValidity
import cn.pocket.toolbox.bodycast.SignalSource
import cn.pocket.toolbox.data.micloud.MiCloudApi
import cn.pocket.toolbox.data.micloud.MiCloudException
import cn.pocket.toolbox.data.micloud.MiSession
import cn.pocket.toolbox.data.micloud.MiSnapshot
import cn.pocket.toolbox.data.micloud.MiTokenVault

/**
 * 小米账号（云同步）——可选通道，**默认关闭**。
 *
 * **移植/改编自 [ilyasaftr/better-mi-fitness-sync](https://github.com/ilyasaftr/better-mi-fitness-sync)
 * 的 `miclient` 模块与它的数据层（MIT License，Copyright (c) 2026 Ilyasa Fathur Rahman）。**
 * 原项目把这套客户端接到 Health Connect；这里换成接进 BodyCast 的 [HealthSource]，
 * 六类读数映射到 [BodySignals]，读不到的字段留 null。
 *
 * ## 它解决什么问题
 * 厂商 App（小米运动健康）不往系统健康服务写数据时，「能跳转、拿不到数」。
 * 这条路直接问用户**自己的**小米账号：设备同步到云上的那份快照，读回来给本应用用。
 *
 * ## 三条边界
 * 1. **默认不联网**。没连过账号时，这个应用一个字节都不往外发。联网只发生在：
 *    用户主动点「连接小米账号」（官方登录页）、以及连上之后主动点「立即同步」；
 * 2. **不接触密码**。登录在小米官方页面（WebView）里完成，应用只从 Cookie 里取令牌；
 * 3. **只读**。所有请求都是「查询」，请求体里只有要哪一类、哪个时间段，
 *    不向小米写任何数据、不上传本机的任何东西。
 *
 * ## 已知情况（如实写在代码里，也如实显示在界面上）
 * - 这是**非官方接口**：小米随时可能改或限制，届时这条路会失效，界面会如实报错；
 * - 数据是「设备同步到云」的快照，**不是实时的**：界面上必须显示上次同步时间，
 *   超过 24 小时的快照不再参与今天的判定（[EntryValidity]）；
 * - 区域决定数据主机（`cn` → `hlth.io.mi.com`，其它 → `<区域>.hlth.io.mi.com`）；
 *   区域取自登录回跳地址里的国家码，读不到就按新加坡兜。
 */
object MiCloudSource : HealthSource {

    override val id = "micloud"

    override val label = "小米账号（云同步）"

    /** 连过账号（本机有加密令牌）就算可用。 */
    override suspend fun isAvailable(context: Context): Boolean = sessionOf(context) != null

    /** 这里没有「系统授权」这一说：用户主动连接账号就是授权本身。 */
    override suspend fun hasPermission(context: Context): Boolean = sessionOf(context) != null

    /**
     * 读的是**上一次同步存下来的快照**，不联网。
     *
     * 联网只发生在用户主动点「立即同步」（见 [sync]）——「默认不联网」这条
     * 不能因为「读的时候顺手拉一下」而破功。
     */
    override suspend fun read(context: Context): BodySignals {
        val prefs = BodyCastStore.loadPrefs(context)
        val values = prefs.miCloudEntries
        if (values.isEmpty) return BodySignals(source = SignalSource.NONE)
        return BodySignals(
            source = SignalSource.MI_CLOUD,
            updatedAt = prefs.miCloudSyncedAt,
            restingHeartRate = values.restingHeartRate,
            hrv = values.hrv,
            sleepMinutes = values.sleepMinutes,
            deepSleepRatio = values.deepSleepRatio,
            steps = values.steps,
            bloodOxygen = values.bloodOxygen,
            activityMinutes = values.activityMinutes
        )
    }

    // ---------------------------------------------------------------------
    // 令牌
    // ---------------------------------------------------------------------

    /** 本机存的加密令牌 → 会话。解不开（密钥被系统重置等）返回 null。 */
    fun sessionOf(context: Context): MiSession? = sessionOf(BodyCastStore.loadPrefs(context))

    fun sessionOf(prefs: BodyCastPrefs): MiSession? {
        val sealed = prefs.miCloudToken
        if (sealed.isBlank()) return null
        val raw = MiTokenVault.open(sealed) ?: return null
        return MiSession.fromJson(raw)
    }

    /**
     * 同步：拿这份会话去小米云读六类读数。
     *
     * **不落盘**——存下来由 [BodyCastStore] 负责（和手动填写 / 导入同一条纪律：
     * 本机只留聚合值 + 一个时间戳）。失败抛 [MiCloudException]，由界面转成人话。
     */
    suspend fun sync(session: MiSession): MiSnapshot = MiCloudApi.fetchSnapshot(session)

    /**
     * 令牌过期时的自救：用 passToken 换一份新会话。
     * 换到了就返回新会话（调用方负责加密存回本机），换不到抛 [MiCloudException.Auth]。
     */
    suspend fun renew(session: MiSession): MiSession = MiCloudApi.refreshSession(session)

    /**
     * 同步时的续期策略（22.27.0 抽成纯逻辑，方便用假令牌走单测）。
     *
     * 顺序固定：先正常调一次 → 被拒（Auth）才用 passToken **自动续期一次** → 再调一次。
     * 续期本身失败、或续期后仍被拒，都如实把 [MiCloudException.Auth] 抛给界面，
     * 由界面提示「重新连接小米账号」——**绝不静默吞掉，也绝不反复重试**。
     * `renew` / `call` 由调用方注入：界面那侧负责把续期到的新会话加密存回本机。
     */
    suspend fun <T> withRenewalOnce(
        session: MiSession,
        renew: suspend (MiSession) -> MiSession,
        call: suspend (MiSession) -> T
    ): T = try {
        call(session)
    } catch (e: MiCloudException.Auth) {
        call(renew(session))
    }

    /** 界面上的错误码 → 中文源串（再经 `BodyCastText` 翻成当前语言）。 */
    fun messageOf(error: Throwable): String = when (error) {
        is MiCloudException.Auth -> "小米拒绝了这份会话（可能已过期），请重新连接小米账号。"
        is MiCloudException.Network -> "连不上小米的服务器：请检查网络后重试。"
        is MiCloudException.RateLimited -> "小米云限流了，过一会儿再试。"
        is MiCloudException.Undecryptable -> "小米云的响应读不懂（接口可能变了，或会话已失效）。"
        // 登录收尾的失败原因本身也是登记过的整句（不拼接、不塞变量）。
        is MiCloudException.IncompleteLogin -> error.message ?: "登录没有完成。"
        is MiCloudException.Server -> "小米云返回了错误，稍后再试；一直失败的话这条路可能已经不适用了。"
        else -> "同步失败：这次没成功，稍后再试。"
    }
}
