package cn.pocket.toolbox.ui.bodycast

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.pocket.toolbox.bodycast.BodyCastPrefs
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.data.MiCloudSource
import cn.pocket.toolbox.data.micloud.MiCloudApi
import cn.pocket.toolbox.data.micloud.MiCloudException
import cn.pocket.toolbox.data.micloud.MiSnapshot
import cn.pocket.toolbox.data.micloud.MiTokenVault
import cn.pocket.toolbox.glass.GlassRow
import cn.pocket.toolbox.ui.MiLoginActivity
import cn.pocket.toolbox.ui.skin.SkinTokens
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 小米账号（云同步）——设置 → 数据与同步里的那条**可选**通道。
 *
 * 这一段文案是这一页最重要的东西：用户得先知道「点了会发生什么」，再决定点不点。
 * 所以内容第一张卡只讲四件事：
 * ① 默认不联网；② 只有你连了账号，应用才会去小米服务器取你自己账号里的运动健康数据；
 * ③ 登录在小米官方页面完成，BodyCast 不接触密码；④ 令牌加密存在本机，随时可以断开删除。
 *
 * 连上之后这里显示的是**快照**：设备把数据同步到小米云，应用读回来的是那一刻的样子。
 * 界面如实写上次同步时间；超过 24 小时的快照不再参与今天的判定（和手动填写 / 导入同一条规矩）。
 *
 * 22.48.0：抽成 [MiCloudPanel]，**设置页的二级页与启动时的「数据连接指导」共用同一份**——
 * 两处必须逐字一致，所以只有这一个实现（页面形态见 [MiCloudPage]）。
 */
@Composable
fun MiCloudPanel(backdrop: Backdrop, store: BodyCastStore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overlay = LocalCastOverlay.current
    val scope = rememberCoroutineScope()
    val prefs = store.prefs

    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    /** 登录回来之后的收尾：取会话 → 加密存本机 → 立刻同步一次。 */
    fun completeLogin(deviceId: String, url: String, accountCookies: String, healthCookies: String) {
        scope.launch {
            busy = true
            error = null
            note = "正在取回会话…"
            try {
                val session = MiCloudApi.completeWebLogin(deviceId, url, accountCookies, healthCookies)
                val sealed = MiTokenVault.seal(session.toJson())
                    ?: throw MiCloudException.IncompleteLogin("本机没能加密保存令牌（系统密钥库不可用）。")
                store.saveMiCloudSession(sealed, session)
                note = "已连接，正在同步一次…"
                val snapshot = MiCloudSource.sync(session)
                store.saveMiCloudSnapshot(snapshot)
                note = "已连接，读到 ${snapshot.readKeys.size} 类读数。"
            } catch (e: Exception) {
                error = MiCloudSource.messageOf(e)
                note = null
            }
            busy = false
        }
    }

    val loginLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) {
            note = "已取消，没有连接任何账号。"
            return@rememberLauncherForActivityResult
        }
        completeLogin(
            deviceId = data.getStringExtra(MiLoginActivity.EXTRA_DEVICE_ID).orEmpty(),
            url = data.getStringExtra(MiLoginActivity.EXTRA_FINAL_URL).orEmpty(),
            accountCookies = data.getStringExtra(MiLoginActivity.EXTRA_ACCOUNT_COOKIES).orEmpty(),
            healthCookies = data.getStringExtra(MiLoginActivity.EXTRA_HEALTH_COOKIES).orEmpty()
        )
    }

    /**
     * 同步一次。令牌过期时**自动续期一次**再试——passToken 本来就是干这个用的；
     * 续期也失败才如实报错，让用户重新连接（策略在 [MiCloudSource.withRenewalOnce]，
     * 单测用假令牌锁住「只续期一次、失败才叫重新登录」）。
     */
    fun syncNow() {
        if (busy) return
        val session = MiCloudSource.sessionOf(prefs)
        if (session == null) {
            error = "本机没有可用的令牌，请重新连接小米账号。"
            return
        }
        scope.launch {
            busy = true
            error = null
            note = "正在同步…"
            try {
                val fresh = withContext(Dispatchers.IO) {
                    MiCloudSource.withRenewalOnce(
                        session = session,
                        renew = { s ->
                            val renewed = MiCloudSource.renew(s)
                            // 续期到的新会话立刻加密存回本机，下次不用再续。
                            MiTokenVault.seal(renewed.toJson())?.let { store.saveMiCloudSession(it, renewed) }
                            renewed
                        },
                        call = { s -> MiCloudSource.sync(s) }
                    )
                }
                store.saveMiCloudSnapshot(fresh)
                note = when {
                    fresh.readKeys.isEmpty() ->
                        "同步完成，但六类都没读到——账号里可能还没有设备同步上来的数据。"
                    fresh.failedReads > 0 ->
                        "同步完成，读到 ${fresh.readKeys.size} 类；另有 ${fresh.failedReads} 类这次没能读到（网络原因），下次同步会再试。"
                    else -> "同步完成，读到 ${fresh.readKeys.size} 类读数。"
                }
            } catch (e: Exception) {
                error = MiCloudSource.messageOf(e)
                note = null
            }
            busy = false
        }
    }

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)
    ) {
        // ---- 这一段先把话说清楚（22.49.0：正文收进标题旁的 ⓘ）----
        // 用户：「解释加太多了，旁边一个小 i，点一下才弹出来」。卡里只留标题、
        // 状态与按钮；五条说明连同那句免责声明整块搬进 ⓘ 浮层——内容一条不少。
        CastCard(backdrop = backdrop) {
            CastSectionHeader(
                title = "点之前先看清楚",
                trailing = {
                    CastInfoDot(
                        title = "点之前先看清楚",
                        lines = listOf(
                            "默认不联网。本应用在没有连接小米账号之前，不会向任何服务器发出请求。",
                            "只有你主动连接小米账号之后，应用才会访问小米的服务器，把你自己账号里的运动健康数据取回来。",
                            "登录在小米官方页面（account.xiaomi.com）完成：密码、验证码都只输在小米那里，BodyCast 不接触你的密码。",
                            "令牌用系统密钥库（Android Keystore）加密后存在本机，明文不落盘；随时可以「断开并删除令牌」。",
                            "取回来的是快照——设备同步到小米云的那一刻的样子，不是实时读数。所以这里会显示上次同步时间。",
                            "说明：这是小米的非官方接口，小米随时可能改动它。应用只读你自己账号里的数据，不向小米写任何东西、也不上传本机的任何内容。"
                        ),
                        backdrop = backdrop
                    )
                }
            )
            CastHint("连上之后应用才会去小米取你自己账号里的数据；登录在小米官方页面完成。")
        }

        // ---- 未连接：一个按钮，一句话说明点了会去哪 ----
        if (!prefs.miCloudConnected) {
            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "还没有连接")
                CastHint("点下面的按钮会打开小米官方的登录页；登录完成后回到这里，应用立刻同步一次。")
                Spacer(Modifier.height(6.dp))
                CastPrimaryButton(
                    text = if (busy) "正在连接…" else "连接小米账号",
                    icon = "shield",
                    enabled = !busy,
                    loading = busy,
                    onClick = {
                        // 设备标识先生成并记下来：同一条手机每次登录用同一个，小米那边不会
                        // 每次都当成新设备（换设备标识容易触发二次验证）。
                        val deviceId = prefs.miCloudDeviceId.ifBlank { MiCloudApi.newDeviceId() }
                        if (deviceId != prefs.miCloudDeviceId) {
                            store.updatePrefs { it.copy(miCloudDeviceId = deviceId) }
                        }
                        error = null
                        note = null
                        loginLauncher.launch(MiLoginActivity.intent(context, deviceId))
                    },
                    backdrop = backdrop
                )
                note?.let { CastHint(it) }
                error?.let { CastHint(it) }
                CastHint("不想连了？随时回到这一页点「断开并删除令牌」，本机不会留下任何令牌。")
            }
        }

        // ---- 已连接：账号、上次同步、六类各自读到没有、两个操作 ----
        if (prefs.miCloudConnected) {
            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "已连接")
                GlassRow(
                    backdrop = backdrop,
                    title = "小米账号",
                    subtitle = "令牌加密存在本机；断开即删除。",
                    value = if (prefs.miCloudAccount.isBlank()) "已连接" else "账号：${prefs.miCloudAccount}",
                    chevron = false,
                    onClick = null,
                    surface = false,
                    shape = RoundedCornerShape(SkinTokens.secondaryRadius)
                )
                GlassRow(
                    backdrop = backdrop,
                    title = "数据区域",
                    subtitle = "数据区域：" + MiCloudApi.healthHost(prefs.miCloudRegion.ifBlank { "sg" }),
                    value = MiCloudApi.regionLabel(prefs.miCloudRegion.ifBlank { "sg" }),
                    chevron = false,
                    onClick = null,
                    surface = false,
                    shape = RoundedCornerShape(SkinTokens.secondaryRadius)
                )
                GlassRow(
                    backdrop = backdrop,
                    title = "上次同步",
                    subtitle = when {
                        prefs.miCloudSyncedAt <= 0L -> "还没有同步过。"
                        prefs.miCloudFresh -> "快照 24 小时内有效，参与今天的判定。"
                        else -> "已过期（超过 24 小时），不参与今天的判定。"
                    },
                    value = if (prefs.miCloudSyncedAt <= 0L) "还没有同步过" else entryStamp(prefs.miCloudSyncedAt),
                    chevron = false,
                    onClick = null,
                    surface = false,
                    shape = RoundedCornerShape(SkinTokens.secondaryRadius)
                )
            }

            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "六类读数读到了没有")
                val read = prefs.miCloudReadKeys.toSet()
                MiReadRow(backdrop, "静息心率", MiSnapshot.KEY_RESTING_HEART_RATE in read)
                MiReadRow(backdrop, "心率变异性 HRV", MiSnapshot.KEY_HRV in read)
                MiReadRow(backdrop, "睡眠时长与深睡", MiSnapshot.KEY_SLEEP in read)
                MiReadRow(backdrop, "步数", MiSnapshot.KEY_STEPS in read)
                MiReadRow(backdrop, "血氧", MiSnapshot.KEY_BLOOD_OXYGEN in read)
                MiReadRow(backdrop, "运动时长", MiSnapshot.KEY_ACTIVITY in read)
                CastHint("「暂时没读到」= 小米那边没有这一类数据（可能是设备不支持，或者还没同步上云）。这一项留空，不填 0。")
                if (prefs.miCloudFresh) {
                    CastHint("读到的项已经并进首页读数：手动填的读数优先级更高，手动校准永远最高。")
                }
            }

            CastPrimaryButton(
                text = if (busy) "正在同步…" else "立即同步",
                icon = "swap",
                enabled = !busy,
                // 同步进行中用转圈（玻璃皮肤只靠文字，观感不变）。
                loading = busy,
                onClick = { syncNow() },
                backdrop = backdrop
            )
            CastHint("同步只在你点这一下（或刚连接完的那一次）发生；应用不会在后台自己联网。")

            CastSecondaryButton(
                text = "断开并删除令牌",
                icon = "close",
                enabled = !busy,
                onClick = {
                    overlay.show(
                        title = "断开并删除令牌",
                        subtitle = "断开之后，本机不再有这份令牌，云端读数也一并清掉。"
                    ) {
                        CastHint("密钥由系统密钥库保管，删除之后旧的密文再也解不开。")
                        CastHint("想再用的时候，重新连接一次就好。")
                        Spacer(Modifier.height(8.dp))
                        CastPrimaryButton(
                            text = "断开并删除",
                            onClick = {
                                store.disconnectMiCloud()
                                overlay.dismiss()
                                note = "已断开并删除令牌。"
                                error = null
                            },
                            backdrop = backdrop
                        )
                    }
                },
                backdrop = backdrop
            )
        }

        // 错误与状态各占一行，**不静默吞掉**：网络不通、令牌过期、区域不对都如实说。
        error?.let { message ->
            CastCard(backdrop = backdrop) {
                CastSectionHeader(title = "这一步没成功")
                CastHint(message)
                CastHint("网络不通就检查网络；令牌过期就重新连接；接口变了的话这条路会整体失效——那时它会一直报错，不会假装成功。")
            }
        }
        Column {
            note?.let { CastHint(it) }
            CastHint("说明：这是小米的非官方接口，小米随时可能改动它。应用只读你自己账号里的数据，不向小米写任何东西、也不上传本机的任何内容。")
        }
    }
}

/**
 * 设置页里的二级页形态（22.48.0 起内容全在 [MiCloudPanel] 里）。
 *
 * 页面只负责页头与返回；内容一字不差地和「数据连接指导」里的那一块共用。
 */
@Composable
fun MiCloudPage(backdrop: Backdrop, store: BodyCastStore, onBack: () -> Unit) {
    CastPage {
        item {
            CastPageHeader(
                title = "小米账号（云同步）",
                subtitle = "可选通道。连上之后，应用才会去小米取你自己账号里的数据。",
                trailing = {
                    CastIconButton(
                        icon = "back",
                        label = "返回",
                        onClick = onBack,
                        backdrop = backdrop,
                        tint = SkinTokens.muted
                    )
                }
            )
        }
        item {
            MiCloudPanel(backdrop = backdrop, store = store)
        }
    }
}

/** 六类里某一类读到了没有。三态写成文字，不靠颜色。 */
@Composable
private fun MiReadRow(backdrop: Backdrop, label: String, read: Boolean) {
    GlassRow(
        backdrop = backdrop,
        title = label,
        subtitle = null,
        value = if (read) "读到了" else "暂时没读到",
        chevron = false,
        onClick = null,
        surface = false,
        shape = RoundedCornerShape(SkinTokens.secondaryRadius)
    )
}

/** 设置页那一行右侧的状态字。 */
fun miCloudStatus(prefs: BodyCastPrefs): String = when {
    !prefs.miCloudConnected -> "未登录"
    prefs.miCloudSyncedAt <= 0L -> "已连接：" + prefs.miCloudAccount.ifBlank { "小米账号" } + " · 还没有同步过"
    prefs.miCloudFresh -> "已连接：" + prefs.miCloudAccount.ifBlank { "小米账号" } + " · " + entryStamp(prefs.miCloudSyncedAt)
    else -> "已连接：" + prefs.miCloudAccount.ifBlank { "小米账号" } + " · 已过期"
}
