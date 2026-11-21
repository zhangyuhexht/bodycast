package cn.pocket.toolbox.ui.bodycast

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import com.kyant.backdrop.Backdrop
import cn.pocket.toolbox.bodycast.BodyCastStore
import cn.pocket.toolbox.data.HealthConnectBridge
import cn.pocket.toolbox.glass.GlassRow
import cn.pocket.toolbox.ui.skin.SkinTokens

/**
 * 数据连接指导（22.48.0）——**授权指南**。
 *
 * 为什么单独做这一页：小米云这条路已经能拿到数据了，但「怎么授权」这件事
 * 停在文档里、没进到用户手里。启动时的弹窗（[LocalCastOverlay] 的带勾选框对话框）
 * 点「开始指导」就进这一页，三步按顺序走完：
 *
 * ① 授权系统健康服务（Health Connect）——同一个授权框，但这里把「要开哪几项」
 *    说清楚，并且授权完回到本页会自动重查一次；
 * ② 连接小米账号（云同步）——**内嵌设置页那一整块**（[MiCloudPanel]），
 *    不是跳转过去：用户在这一页里就能登完、同步完；
 * ③ 预览已同步数据——**和开发者模式里的那张表一模一样**（[SyncedDataCard]）。
 *    「同步成功」四个字不算证据，数字摆出来才算。
 *
 * 页面本身只是一层壳：所有数字都来自同一个 [BodyCastStore] 与
 * [HealthConnectBridge.diagnose]，不在这里另存一份状态。
 */
@Composable
fun DataGuidePage(
    backdrop: Backdrop,
    store: BodyCastStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = store.prefs

    // 自检结论（异步；null = 正在检查）。
    var diagnosis by remember { mutableStateOf<HealthConnectBridge.Diagnosis?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var pausedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        pausedOnce = false
        refreshKey++
    }

    // 从系统页面回来（授权框、小米登录页）自动重跑一次：用户不该需要「手动刷新」。
    DisposableEffect(Unit) {
        val owner = context as? android.app.Activity
        val observer = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(
                source: androidx.lifecycle.LifecycleOwner,
                event: androidx.lifecycle.Lifecycle.Event
            ) {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> pausedOnce = true
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                        if (pausedOnce) {
                            pausedOnce = false
                            refreshKey++
                        }
                    else -> Unit
                }
            }
        }
        (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.addObserver(observer)
        onDispose {
            (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshKey) {
        val fresh = HealthConnectBridge.diagnose(context)
        diagnosis = fresh
        store.updateSignals(fresh.signals)
    }

    BackHandler { onBack() }

    val d = diagnosis
    val hcOn = d != null && d.grantState == HealthConnectBridge.StepState.OK
    val readCount = d?.readCount ?: 0
    val miOn = prefs.miCloudConnected

    CastPage {
        item {
            CastPageHeader(
                title = "连接运动数据",
                subtitle = "三步：授权健康服务、连接小米账号、确认数据真的进来了。",
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

        // ---- 总览：三步各自走到哪了（通 / 断写成字，不靠颜色）----
        item {
            CastCard(backdrop = backdrop) {
                CastSectionHeader(
                    title = "现在走到哪一步了",
                    // 22.49.0：解释收进 ⓘ（用户：「解释加太多了，旁边一个小 i，点一下才弹出来」）。
                    trailing = {
                        CastInfoDot(
                            title = "两条路怎么走",
                            lines = listOf(
                                "两条路是并行的，走通一条就够：系统健康服务（手环 → 厂商 App → 系统）或小米账号（直接从云端取）。两条都通的时候，谁的数据新用谁。",
                                "一条都不通也能用这个应用：首页会请你手动填一次读数，填完照样出天气。只是每天都要自己填。"
                            ),
                            backdrop = backdrop
                        )
                    }
                )
                StepLine(
                    backdrop = backdrop,
                    title = "① 系统健康服务",
                    value = when {
                        d == null -> "正在检查…"
                        hcOn && readCount > 0 -> "通，读到 $readCount 类"
                        hcOn -> "已授权"
                        else -> "还没有授权"
                    }
                )
                StepLine(
                    backdrop = backdrop,
                    title = "② 小米账号",
                    value = if (miOn) "已连接" else "还没有连接"
                )
                StepLine(
                    backdrop = backdrop,
                    title = "③ 数据读到了没有",
                    value = when {
                        readCount > 0 -> "读到 $readCount 类读数"
                        prefs.miCloudReadKeys.isNotEmpty() -> "读到 ${prefs.miCloudReadKeys.size} 类读数"
                        else -> "还没有数据"
                    }
                )
            }
        }

        // ---- ① 系统健康服务 ----
        item { CastGroupLabel("① 授权系统健康服务") }
        item {
            CastCard(backdrop = backdrop) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            d == null -> "正在检查这台手机的健康服务…"
                            readCount > 0 -> "已经通了：系统健康服务里读到了 $readCount 类数据，首页正在用它算今天的天气。"
                            hcOn -> "已经授权。数据要等厂商 App 写进来才会出现——手环同步过一次之后再看这里。"
                            else -> "还没有授权"
                        },
                        modifier = Modifier.weight(1f),
                        fontSize = SkinTokens.body,
                        lineHeight = 26.sp,
                        color = SkinTokens.ink
                    )
                    CastInfoDot(
                        title = "授权要做的几件事",
                        lines = listOf(
                            "在系统页面上一次把六类都打开：步数、睡眠、静息心率、心率变异性、血氧、运动记录。少开一项，就少一类数据。",
                            "安卓 14 及以上系统自带；安卓 13 及以下要在应用商店装一个 Health Connect。",
                            "系统只给「授权之后」产生的数据。想看更早的记录，要在那个页面上单独再开一档历史权限。",
                            "授权完回到这一页会自动重新检查一遍，不用手动刷新。",
                            "如果授权了还是没数据，多半是厂商 App 没有把数据写进系统——设置 → 数据与同步 → 授权向导里有手环配对的完整步骤。"
                        ),
                        backdrop = backdrop
                    )
                }
                Spacer(Modifier.height(12.dp))
                CastPrimaryButton(
                    text = if (hcOn) "再看一眼授权状态" else "去授权",
                    icon = "shield",
                    onClick = {
                        runCatching { permissionLauncher.launch(HealthConnectBridge.permissions) }
                    },
                    backdrop = backdrop
                )
            }
        }

        // ---- ② 小米账号（内嵌设置页那一整块，不是跳转）----
        item { CastGroupLabel("② 连接小米账号（云同步）") }
        item {
            MiCloudPanel(backdrop = backdrop, store = store)
        }

        // ---- ③ 预览已同步数据 ----
        item { CastGroupLabel("③ 预览已同步数据") }
        item {
            SyncedDataCard(
                backdrop = backdrop,
                store = store,
                infoLines = listOf(
                    "这张表就是应用当前手里的全部读数：「小米云 / 手动 / 导入」各占一列，取到就填值，没取到写「没读到」，不猜也不补 0。"
                )
            )
        }
    }
}

/** 总览里的行：左边名字，右边状态字（不靠颜色表达三态）。 */
@Composable
private fun StepLine(backdrop: Backdrop, title: String, value: String) {
    GlassRow(
        backdrop = backdrop,
        title = title,
        subtitle = null,
        value = value,
        chevron = false,
        onClick = null,
        surface = false,
        shape = RoundedCornerShape(SkinTokens.secondaryRadius)
    )
}
