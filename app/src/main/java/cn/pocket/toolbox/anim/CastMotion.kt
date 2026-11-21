package cn.pocket.toolbox.anim

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf

/**
 * 全应用统一的动画节奏（22.28.0 · 动画批）。
 *
 * 用户定调：「动画可以优雅一点，但是该快的地方就是要快一点」。
 * 于是把散在各处的 tween/spring 参数收敛到这一处：
 *
 * - **时长分级**：快 [FAST] 120ms（按压/箭头/退场/换 tab 落定）、
 *   中 [MEDIUM] 220ms（浮层开合、卡片展开、二级页转场）、
 *   慢 [SLOW] 320ms（完成卡这类要"稳"的入场、进度填充）；
 * - **曲线统一**：M3 emphasized 一族——[Emphasized]（通用）、
 *   [Decelerate]（进/落定：快进缓出）、[Accelerate]（退/收起：快出）。
 *   需要"弹"的地方（指示器落位、跟手回弹）用同一组弹簧（见 [indicatorSpec] / [settleSpec]）；
 * - **开关**：[enabled] 由设置里的「动画效果」（`prefs.animationEnabled`）
 *   与系统「减少动画」（ANIMATOR_DURATION_SCALE == 0）共同决定，
 *   关掉时所有经由本文件的时长归零（硬切）、弹簧换成 snap。
 *   注意 tween/spring 本身还受系统动画时长缩放（MotionDurationScale）影响，
 *   系统调 0 时 Compose 自己就会把补间压成瞬时——两层保险；
 *   循环类动画（呼吸/游走）不受缩放影响，必须走 [enabled] 门（调用点自己判）。
 *
 * 只动动画参数，不动任何绘制——玻璃皮肤静态观感零变化。
 */
object CastMotion {

    /** 快档：按压反馈、箭头旋转、退场、换 tab 落定。 */
    const val FAST = 120

    /** 中档：浮层开合、卡片展开/收起、二级页转场、数值过渡。 */
    const val MEDIUM = 220

    /** 慢档：完成态入场、进度条填充等需要"稳一下"的动作。 */
    const val SLOW = 320

    /** M3 emphasized：快进快出，中段稳。通用曲线。 */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** M3 emphasized decelerate：进门/落定用——第一下就动起来，尾部轻轻停住。 */
    val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** M3 emphasized accelerate：退场/收起用——起步即走，不拖尾。 */
    val Accelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /**
     * 动画总开关。MainActivity 在合成期把 `prefs.animationEnabled`
     * 与系统「减少动画」合算后写进来；读它会订阅变化。
     */
    val enabled = mutableStateOf(true)

    /** 按开关换算时长：关掉 = 0ms（动作直接到位，不播）。 */
    fun ms(base: Int): Int = if (enabled.value) base else 0

    /** 统一补间：默认 emphasized 曲线。 */
    fun <T> tween(base: Int, easing: Easing = Emphasized): FiniteAnimationSpec<T> =
        tween(durationMillis = ms(base), easing = easing)

    /** 进场/落定补间（emphasized decelerate）。 */
    fun <T> enter(base: Int = MEDIUM): FiniteAnimationSpec<T> = tween(base, Decelerate)

    /** 退场/收起补间（emphasized accelerate）。 */
    fun <T> exit(base: Int = FAST): FiniteAnimationSpec<T> = tween(base, Accelerate)

    /**
     * 落定弹簧：轻微阻尼、快速回位。跟手拖拽松手后的回弹、
     * 浮层下拉取消后的回位共用这一组。
     */
    fun <T> settleSpec(): FiniteAnimationSpec<T> =
        if (enabled.value) spring(dampingRatio = 0.85f, stiffness = 500f) else snap()

    /**
     * 底栏指示器位移：两套皮肤同一组物理参数。
     * 与玻璃底栏滑块（`DampedDragAnimation` 的 valueAnimationSpec：
     * dampingRatio 1、stiffness 1000）对齐——切 tab 时两边的"滑过去"是同一个节奏。
     */
    fun <T> indicatorSpec(): FiniteAnimationSpec<T> =
        if (enabled.value) spring(dampingRatio = 1f, stiffness = 1000f) else snap()
}
