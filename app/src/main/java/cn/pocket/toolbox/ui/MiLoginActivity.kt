package cn.pocket.toolbox.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.pocket.toolbox.data.micloud.MiCloudApi
import cn.pocket.toolbox.i18n.AppLanguage
import cn.pocket.toolbox.ui.bodycast.CastCard
import cn.pocket.toolbox.ui.bodycast.CastHint
import cn.pocket.toolbox.ui.bodycast.CastIconButton
import cn.pocket.toolbox.ui.bodycast.CastPageHeader
import cn.pocket.toolbox.ui.bodycast.CastPrimaryButton
import cn.pocket.toolbox.ui.bodycast.CastSecondaryButton
import cn.pocket.toolbox.ui.bodycast.CastSectionHeader
import cn.pocket.toolbox.ui.bodycast.Text
import cn.pocket.toolbox.ui.skin.SkinTokens
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 小米账号登录（**在小米官方页面上完成**）。
 *
 * 三条硬规矩：
 * 1. **BodyCast 不接触密码**。这里是小米自己的登录页（`account.xiaomi.com`）：
 *    账号、密码、验证码、二次验证都发生在小米那边。本应用没有自己的登录框，
 *    也不读、不存、不转发密码——它只等页面跳到 STS 回调地址之后，从 Cookie 里取令牌；
 * 2. **只取令牌**。取到的东西交给设置页，由那边用 Android Keystore 加密后存本机；
 *    这个 Activity 自己不落盘任何东西，也不把任何令牌写进日志；
 * 3. **随时可以退出**。返回键 = 取消，什么都不会保存。
 *
 * 结果通过 [Activity.setResult] 回给设置页（见 [MiLoginActivity] 的常量）。
 */
class MiLoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_ID = "mi_device_id"
        const val EXTRA_FINAL_URL = "mi_final_url"
        const val EXTRA_ACCOUNT_COOKIES = "mi_account_cookies"
        const val EXTRA_HEALTH_COOKIES = "mi_health_cookies"

        fun intent(context: Context, deviceId: String): Intent =
            Intent(context, MiLoginActivity::class.java).putExtra(EXTRA_DEVICE_ID, deviceId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguage.initializeFromPrefs(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID).orEmpty()
            .ifBlank { MiCloudApi.newDeviceId() }
        setContent {
            MiLoginScreen(
                deviceId = deviceId,
                onCancel = { finishWithCancel() },
                onDone = { url, accountCookies, healthCookies ->
                    setResult(
                        Activity.RESULT_OK,
                        Intent()
                            .putExtra(EXTRA_DEVICE_ID, deviceId)
                            .putExtra(EXTRA_FINAL_URL, url)
                            .putExtra(EXTRA_ACCOUNT_COOKIES, accountCookies)
                            .putExtra(EXTRA_HEALTH_COOKIES, healthCookies)
                    )
                    finish()
                }
            )
        }
    }

    private fun finishWithCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}

/** 登录页界面：一句说明 + 小米自己的页面 + 一个「我已经登录完了」的兜底按钮。 */
@Composable
private fun MiLoginScreen(
    deviceId: String,
    onCancel: () -> Unit,
    onDone: (url: String, accountCookies: String, healthCookies: String) -> Unit
) {
    /*
     * 环境层：**只有底色进这一层**，玻璃元素在外面采样它。
     *
     * 这一层与 MainActivity 是同一套结构的简化版，原因也一样：`layerBackdrop`
     * 会把所挂节点的整棵子树录成渲染节点，玻璃元素若也在其中就会成环，
     * 渲染线程无限递归、栈溢出、直接 SIGSEGV（编译期看不出来）。
     */
    val backdrop = rememberLayerBackdrop()
    // 当前地址的主机名：一是让用户随时看清「现在在谁家的页面上」，
    // 二是登录跳回来时由它判断。
    var host by remember { mutableStateOf("account.xiaomi.com") }
    var status by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    // 页面没打开（断网、DNS 失败、被拦、HTTP 报错）时给一句人话 + 「重试」，
    // 而不是留一块黑框让用户猜（真机反馈：那个框是黑的）。
    var loadError by remember { mutableStateOf(false) }
    // WebView 本体的普通引用（不是状态）：只给「重试」重新 loadUrl 用。
    val webViewRef = remember { arrayOfNulls<WebView>(1) }

    fun finishOnce(url: String) {
        if (finished) return
        val account = runCatching { CookieManager.getInstance().getCookie(MiCloudApi.ACCOUNT_ORIGIN) }
            .getOrNull().orEmpty()
        val health = runCatching { CookieManager.getInstance().getCookie(MiCloudApi.STS_ORIGIN) }
            .getOrNull().orEmpty()
        finished = true
        onDone(url, account, health)
    }

    BackHandler { onCancel() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(SkinTokens.bg)
        )

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(
                    PaddingValues(
                        start = SkinTokens.pagePadding,
                        end = SkinTokens.pagePadding,
                        top = 8.dp,
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 20.dp
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(SkinTokens.cardGap)
        ) {
            CastPageHeader(
                title = "连接小米账号",
                subtitle = "在你的小米官方页面里登录。",
                trailing = {
                    CastIconButton(
                        icon = "back",
                        label = "取消",
                        onClick = onCancel,
                        backdrop = backdrop,
                        tint = SkinTokens.muted
                    )
                }
            )

            // 22.41.0：说明压成一行——整卡换成一句话，把高度还给登录页
            //（用户反馈「登录小米页面太矮」）。「当前页面」挪到网页下方小字。
            Column {
                CastHint("登录在小米官方页面完成：账号、密码只输在小米那里，BodyCast 只取一个访问令牌。")
                status?.let { CastHint(it) }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        /*
                         * 22.34.0：黑块 / 白块的根因定位到了——WebView 的
                         * **CSS 视口高度被解析成 0**（`html{height:100%}` 连
                         * `100vh` 都算出 0px，表单被 `overflow:auto` 整个裁掉；
                         * DevTools 里 DOM 完整、就是不出画）。
                         * 两个嫌疑一起拔掉：
                         * 1. 不再用 createConfigurationContext 造 WebView——
                         *    非 Activity 上下文会让 WebView 拿到错误的视口度量；
                         * 2. useWideViewPort + loadWithOverviewMode，按真浏览器
                         *    的方式认 meta viewport，百分比高度链才解析得对。
                         */
                        WebView(context).apply {
                            webViewRef[0] = this
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            /*
                             * 22.44.0：安全键盘弹不出来（点密码框没反应、只能粘贴）的修复。
                             * WebView 自己只发「隐式」输入法请求，部分 ROM（尤其
                             * HyperOS 的安全键盘）不接；再加上 Compose 的 AndroidView
                             * 宿主里焦点容易被外层拿走。两手一起补：
                             * 1. 触摸按下就保焦（focusable + requestFocus）；
                             * 2. 页面里跑一个焦点探针：document.activeElement 落在
                             *    输入框（含跨源 iframe——此时 activeElement 是 iframe
                             *    本身）就通过 JS 桥**显式** showSoftInput，离开时收起。
                             */
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setOnTouchListener { v, event ->
                                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                    v.isFocusable = true
                                    v.isFocusableInTouchMode = true
                                    if (!v.hasFocus()) v.requestFocus()
                                }
                                false
                            }
                            addJavascriptInterface(ImeBridge(this), "BodyCastIme")
                            // 22.34.0：远程调试口只在 debuggable 包开启（诊断表单渲染用）。
                            if ((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                                WebView.setWebContentsDebuggingEnabled(true)
                            }
                            // 先把底色定成白：页面还没画出来 / 没打开时，
                            // 这里是一块干净的「网页位」，不是一块黑框。
                            setBackgroundColor(android.graphics.Color.WHITE)
                            /*
                             * 22.34.0：撤掉软件层，回到默认硬件层。
                             * 22.26.0 软件层是冲着「表单黑块」去的，但实测它反而
                             * 让跨源 iframe 的表单整个不绘制（黑块的真根因是上面的
                             * 深色 scheme，已用浅色配置上下文解决）。硬件层下
                             * iframe 合成正常。
                             */
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            /*
                             * UA 去掉 "; wv" 标记、按 Chrome 移动版报。
                             * 小米登录页对 WebView UA 有降级逻辑，某些表单资源
                             * 只发给「完整浏览器」。这里只是 UA 字符串，不涉及
                             * 任何权限或数据。
                             */
                            settings.userAgentString = settings.userAgentString
                                ?.replace("; wv)", ")")
                                ?.replace("Version/4.0", "Chrome/120.0.0.0 Mobile Safari/537.36")
                            /*
                             * 关掉系统「深色强制」。本应用整体是深色主题，
                             * 系统会在 WebView 上做算法反色（force dark / 算法暗化）；
                             * 小米登录页这类复杂 CSS 一反色就整页黑掉——
                             * 真机反馈「那个框是黑的」的第一嫌疑。
                             * 登录页保持小米自己的浅色原样。
                             */
                            if (Build.VERSION.SDK_INT >= 29) {
                                isForceDarkAllowed = false
                            }
                            if (Build.VERSION.SDK_INT >= 33) {
                                settings.isAlgorithmicDarkeningAllowed = false
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): Boolean {
                                    val url = request.url.toString()
                                    host = request.url.host ?: host
                                    if (isStsCallback(url)) {
                                        status = "登录完成，正在取回会话…"
                                        finishOnce(url)
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    loadError = false
                                    // 键盘稳健性：进了页面就把焦点给 WebView，
                                    // 点输入框（尤其密码框）时系统/安全键盘才肯起。
                                    view.isFocusable = true
                                    view.isFocusableInTouchMode = true
                                    view.requestFocus()
                                    url ?: return
                                    host = hostOf(url) ?: host
                                    if (isStsCallback(url)) {
                                        status = "登录完成，正在取回会话…"
                                        finishOnce(url)
                                    }
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    // 页面画完了就撤错误卡：先报过错、后来又加载成功的情况
                                    //（超时后自动恢复 / 点了「重试」）不能把提示留在表单上面。
                                    loadError = false
                                    // 22.34.0：视口高度补丁。这个 WebView 里 CSS 的
                                    // `100%` / `100vh` 一律解出 0px（JS 的 innerHeight
                                    // 却是 421），小米登录页整条高度链塌成 0，表单被
                                    // `overflow:auto` 裁没——这就是「那个框是黑的」的
                                    // 终极根因。像素高度能正常解析，所以加载完把
                                    // html/body 的高度钉成 innerHeight 像素值，
                                    // resize 时跟着改。
                                    view.evaluateJavascript(VIEWPORT_HEIGHT_SHIM, null)
                                    // 22.44.0：焦点探针——输入框（含 iframe）拿到焦点时
                                    // 显式唤起输入法（HyperOS 安全键盘不接隐式请求）。
                                    view.evaluateJavascript(IME_PROBE_JS, null)
                                    url ?: return
                                    host = hostOf(url) ?: host
                                    // 兜底：有的机型上 STS 授权是「页面不动、只下发 Cookie」，
                                    // 那就按 Cookie 判断登录是否已经完成。
                                    if (hasServiceToken() && hasLoginCookies()) {
                                        status = "登录完成，正在取回会话…"
                                        finishOnce(url)
                                    }
                                }

                                // 打不开主页面（断网、DNS 失败、超时）→ 显示错误卡，不留黑框。
                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError
                                ) {
                                    if (request.isForMainFrame) loadError = true
                                }

                                // 服务端直接报错（4xx / 5xx）也算打不开。
                                override fun onReceivedHttpError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    errorResponse: WebResourceResponse
                                ) {
                                    if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                                        loadError = true
                                    }
                                }
                            }
                            loadUrl(MiCloudApi.loginUrl(deviceId))
                        }
                    },
                    // 离开页面就把 WebView 收干净，别把渲染进程留到后台。
                    onRelease = { webView ->
                        webViewRef[0] = null
                        webView.stopLoading()
                        webView.destroy()
                    }
                )

                if (loadError) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(SkinTokens.bg)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        CastHint("打不开小米登录页")
                        CastHint("可能是网络不通，或者页面被拦住了。检查网络之后点「重试」。")
                        Spacer(Modifier.height(8.dp))
                        CastSecondaryButton(
                            text = "重试",
                            icon = "reset",
                            onClick = {
                                loadError = false
                                status = "正在重新打开…"
                                webViewRef[0]?.loadUrl(MiCloudApi.loginUrl(deviceId))
                            },
                            backdrop = backdrop
                        )
                    }
                }
            }

            // 万一跳转没被识别到（自定义 ROM 的 WebView 行为不同），给用户一个按钮。
            CastPrimaryButton(
                text = "我已经登录完了",
                icon = "check",
                onClick = {
                    status = "正在确认登录结果…"
                    finishOnce("")
                },
                backdrop = backdrop
            )
            CastHint("登录只在小米的页面上完成；BodyCast 不会看到你的密码，也不会把它存下来。")
        }
    }
}

/**
 * 视口高度补丁：见 [MiLoginScreen] 里 onPageFinished 的说明。
 * 只动 html/body 的高度两个内联样式，不碰页面任何业务 DOM。
 */
private const val VIEWPORT_HEIGHT_SHIM =
    "(function(){function f(){var h=Math.round(window.innerHeight);" +
        "if(h>0){document.documentElement.style.height=h+'px';" +
        "document.body.style.height=h+'px';}}" +
        "f();window.addEventListener('resize',f);})()"

/**
 * 输入法焦点探针（22.44.0）：每 250ms 看一眼 `document.activeElement`，
 * 落在输入类元素上就叫原生端**显式**弹输入法，离开就收起。
 *
 * 为什么需要它：WebView 对输入框只发「隐式」输入法请求，部分 ROM
 *（HyperOS 的安全键盘最典型）不接，表现为点密码框没反应、只能粘贴。
 * 密码框可能在**跨源 iframe** 里（脚本伸不进去），这时主文档看到的
 * activeElement 是 iframe 元素本身——一并算作「正在输入」，
 * 探针对小米登录页的结构同样有效。
 */
private const val IME_PROBE_JS =
    "(function(){if(window.__bcImeProbe)return;window.__bcImeProbe=true;" +
        "var last=null;setInterval(function(){var a=document.activeElement;" +
        "var t=!!a&&(a.tagName==='INPUT'||a.tagName==='TEXTAREA'||a.tagName==='SELECT'||" +
        "a.isContentEditable===true||a.tagName==='IFRAME'||a.tagName==='FRAME');" +
        "if(t!==last){last=t;try{BodyCastIme.ime(t);}catch(e){}}},250);})()"

/**
 * JS → 原生的输入法桥。`ime()` 在 WebView 的 JS 线程回调，
 * 一律 post 回 UI 线程再动输入法。
 */
private class ImeBridge(view: WebView) {
    private val webView = java.lang.ref.WeakReference(view)

    @android.webkit.JavascriptInterface
    fun ime(show: Boolean) {
        val v = webView.get() ?: return
        v.post {
            val imm = v.context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                ?: return@post
            if (show) {
                v.isFocusable = true
                v.isFocusableInTouchMode = true
                if (!v.hasFocus()) v.requestFocus()
                imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        }
    }
}

/** 是不是跳到了健康云的 STS 授权地址（登录成功的信号）。 */
private fun isStsCallback(url: String): Boolean {
    val host = hostOf(url) ?: return false
    return host == "sts-hlth.io.mi.com" || host.endsWith(".sts-hlth.io.mi.com")
}

private fun hostOf(url: String): String? =
    runCatching { android.net.Uri.parse(url).host }.getOrNull()

/** 健康云那边有没有下发会话令牌（不读值，只问有没有）。 */
private fun hasServiceToken(): Boolean {
    val cookie = cookieOf(MiCloudApi.STS_ORIGIN) + cookieOf(MiCloudApi.ACCOUNT_ORIGIN)
    return cookie.contains("serviceToken=")
}

/** 账号域有没有登录 Cookie（同样只问有没有）。 */
private fun hasLoginCookies(): Boolean {
    val cookie = cookieOf(MiCloudApi.ACCOUNT_ORIGIN)
    return cookie.contains("userId=") && cookie.contains("passToken=")
}

private fun cookieOf(origin: String): String =
    runCatching { CookieManager.getInstance().getCookie(origin) }.getOrNull().orEmpty()
