package cn.pocket.toolbox.data

import android.content.Context
import cn.pocket.toolbox.bodycast.BodySignals
import cn.pocket.toolbox.bodycast.SignalSource

/**
 * 健康数据源。
 *
 * 页面只认这个接口，不认任何厂商 SDK。
 * 现在只有 Health Connect 一个真实实现，将来要接小米 / 华为的私有通道，
 * 也只需要再加一个实现类，页面一行都不用改。
 */
interface HealthSource {

    val id: String

    /** 界面上展示的名字 */
    val label: String

    /** 这台设备上能不能用（没装 Health Connect、系统版本不够都会返回 false）。 */
    suspend fun isAvailable(context: Context): Boolean

    /** 是否已经拿到读取授权。 */
    suspend fun hasPermission(context: Context): Boolean

    /**
     * 读一次数据。
     *
     * 实现方必须遵守：**读不到的字段留 null，不要填 0**。
     * null 会一路传到界面上变成「暂时没读到」，填 0 会变成一句假的结论。
     */
    suspend fun read(context: Context): BodySignals
}

/** 手动录入。永远可用，且永远优先。 */
object ManualSource : HealthSource {
    override val id = "manual"
    override val label = "手动校准"

    override suspend fun isAvailable(context: Context) = true
    override suspend fun hasPermission(context: Context) = true

    /** 手动源没有可读的东西，信号由用户在界面上直接给。 */
    override suspend fun read(context: Context) = BodySignals(source = SignalSource.MANUAL)
}

/** 演示数据。没有设备、没有授权时用它把完整流程跑通，界面上会明确标注来源。 */
object DemoSource : HealthSource {
    override val id = "demo"
    override val label = "演示数据"

    override suspend fun isAvailable(context: Context) = true
    override suspend fun hasPermission(context: Context) = true

    override suspend fun read(context: Context) = BodySignals.demo()
}
