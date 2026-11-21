package cn.pocket.toolbox.ui

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import cn.pocket.toolbox.ui.bodycast.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import cn.pocket.toolbox.ui.skin.SkinTokens
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.ui.bodycast.CastBanner
import cn.pocket.toolbox.ui.bodycast.CastCard
import cn.pocket.toolbox.ui.bodycast.CastPageHeader
import cn.pocket.toolbox.ui.bodycast.CastSectionHeader

/**
 * 隐私说明页。
 *
 * Android 14 起，Health Connect 在弹出授权框之前会先向应用索取一段
 * 「为什么要读这些数据」的说明。这个 Activity 就是那段说明的落点。
 *
 * 内容上只说事实：数据从哪来、存在哪、去哪了。不写「我们非常重视您的隐私」
 * 这种空话——用户要的是能核对的边界，不是承诺。
 */
class PrivacyPolicyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguage.initializeFromPrefs(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { PrivacyPolicyScreen() }
    }
}

private val PARAGRAPHS = listOf(
    "数据从哪里来" to
        "只从系统的健康服务（Health Connect）读取。你的手环先把数据同步到小米运动健康或华为运动健康，" +
        "再由它们同步进 Health Connect，BodyCast 只做最后一步的读取。应用不直接连蓝牙，也不连手环。",

    "读了哪些数据" to
        "步数、睡眠、静息心率、心率变异性、血氧、运动记录，共六项，全部是**只读**。" +
        "应用不会写入任何数据，也不会修改系统里的健康记录。",

    "数据存在哪" to
        "只存在这台手机上。应用默认不联网、没有服务器、没有账号，平时也没有任何上传行为。" +
        "本机保存的只是「今天是什么天气」和「你手动勾选的感觉标签」，" +
        "不保存原始心率、睡眠、血氧数值。",

    "可选的一条联网通道：小米账号（云同步）" to
        "默认关闭。只有你自己在「设置 → 数据与同步 → 小米账号（云同步）」里主动连接之后，" +
        "应用才会访问小米的服务器，把你自己账号里的运动健康数据取回来。" +
        "· 连了什么域名：登录页是 account.xiaomi.com（小米官方账号页）；" +
        "数据请求发给 hlth.io.mi.com（中国大陆）或 <区域>.hlth.io.mi.com（其它区域）；" +
        "· 传了什么：只有查询条件——要哪一类读数、哪个时间段。应用不向小米写任何数据，" +
        "也不上传本机的任何内容；" +
        "· 密码呢：密码、验证码都只输在小米自己的登录页上。BodyCast 看不到、不保存、不转发。",

    "这条通道在本机存了什么" to
        "只存三样东西：① 小米回给应用的访问令牌（serviceToken / passToken 等）；" +
        "② 六类读数的聚合值；③ 上次同步时间。" +
        "令牌用 Android Keystore 生成的 AES 密钥加密之后才落盘，**明文令牌不落盘**，" +
        "应用日志与界面都不显示令牌内容。这条通道的请求只发生在你点「连接」与「立即同步」时，" +
        "后台不会自己联网。",

    "怎么断开这条通道" to
        "在同一个页面上点「断开并删除令牌」：密钥与令牌一起删掉，旧的密文从此解不开，" +
        "云端取回来的读数也一并清除。断开之后应用回到默认的「不联网」状态，其它功能不受影响。",

    "数据去哪了" to
        "哪也不去。没有第三方 SDK，没有广告，没有数据分析。" +
        "你在设置里点「删除所有数据」，本机记录会立即清空。",

    "你可以随时收回" to
        "在设置页点「撤销授权」，或者在系统的 Health Connect 里取消勾选，" +
        "应用就再也读不到数据了。收回之后应用仍然可用，只是天气会显示为「暂时没读到」。" +
        "小米账号那条通道则用「断开并删除令牌」收回。"
)

@Composable
private fun PrivacyPolicyScreen() {
    val backdrop = rememberLayerBackdrop()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        Modifier
            .fillMaxSize()
            .background(SkinTokens.bg)
    ) {
        /*
         * 环境采样层：**只挂背景**，页面里的玻璃卡片采样它。
         *
         * 22.28.0 根因修复：此前整棵滚动 Column 挂在 layerBackdrop(backdrop) 上，
         * 而 Column 里的 CastCard 又采样同一个 backdrop —— 渲染节点出现
         * 「录到自己」的环（README 坑 #1 / MainActivity 注释里的同一事故），
         * RenderThread 在 prepareTreeImpl 里无限递归，栈溢出即
         * "stack pointer is close to top of stack" SIGSEGV，页面一打开必崩。
         * 现在与 MainActivity 的两层结构同构：采样层只有底色，卡片们在外面。
         * 谷歌皮肤没有玻璃采样，同样只是不挂 layerBackdrop（结构不动）。
         */
        Box(
            Modifier
                .fillMaxSize()
                .then(Modifier.layerBackdrop(backdrop))
                .background(SkinTokens.bg)
        )

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start = SkinTokens.pagePadding,
                        end = SkinTokens.pagePadding,
                        top = 8.dp,
                        bottom = navBottom + 32.dp
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)
        ) {
            CastPageHeader(
                title = "数据边界",
                subtitle = "读什么、存哪、去哪。"
            )

            CastBanner(
                text = "这份说明只讲事实。如果哪一条你不认可，直接撤销授权就好，应用不会因此不可用。",
                backdrop = backdrop,
                icon = "shield"
            )

            PARAGRAPHS.forEach { (title, body) ->
                CastCard(backdrop = backdrop) {
                    CastSectionHeader(title = title)
                    Text(
                        text = body,
                        fontSize = SkinTokens.body,
                        lineHeight = 27.sp,
                        color = SkinTokens.ink
                    )
                }
            }

            CastCard(backdrop = backdrop) {
                Text(
                    text = "如有特殊健康状况，请咨询医生。BodyCast 只播报状态，不做诊断。",
                    fontSize = SkinTokens.caption,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = SkinTokens.muted
                )
            }

            Box(Modifier.fillMaxWidth())
        }
    }
}
