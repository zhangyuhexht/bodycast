package cn.pocket.toolbox.bodycast

import cn.pocket.toolbox.i18n.BodyCastText
import cn.pocket.toolbox.i18n.DomainText

/** 微行动的分类。分类只用来做「换一张」和偏好过滤，不带优劣。 */
enum class ActionCategory(private val sourceLabel: String, val icon: String) {
    BREATHE("呼吸", "lung"),
    MOVE("活动", "walk"),
    EAT("饮食", "leaf"),
    REST("休息", "moon"),
    LIGHT("光照", "sun"),
    CONNECT("连接", "heart");

    val label: String get() = BodyCastText.text(sourceLabel)
}

/** 时段。处方会按当前时间落到对应时段。 */
enum class Slot(private val sourceLabel: String) {
    MORNING("早"),
    NOON("午"),
    EVENING("晚"),
    NIGHT("夜");

    val label: String get() = BodyCastText.text(sourceLabel)

    companion object {
        fun of(hour: Int): Slot = when (hour) {
            in 5..10 -> MORNING
            in 11..14 -> NOON
            in 15..18 -> EVENING
            else -> NIGHT
        }
    }
}

/**
 * 一个微行动。
 *
 * [minutes] 一律 ≤ 10。这不是限制想象力，是保证「现在就能做完」——
 * 任何需要提前准备的动作都会变成明天的负担。
 */
data class MicroAction(
    val id: String,
    private val sourceTitle: String,
    val category: ActionCategory,
    val minutes: Int,
    val slots: Set<Slot>,
    /** 分步引导，页面按行展示 */
    private val sourceGuide: List<String>,
    /** 哪些天气适合它 */
    val forKinds: Set<WeatherKind>
) {
    val title: String get() = DomainText.actionName(id, sourceTitle)
    val guide: List<String> get() = DomainText.actionGuide(id, sourceGuide)
}

/**
 * 动作库。
 *
 * 每条都满足三个条件：现在能做、十分钟内、不需要器械。
 * 不写「跑步 5 公里」这类动作——它们会把「做不完」变成常态。
 */
object ActionLibrary {

    private val ALL_WEATHER = WeatherKind.entries.toSet()

    val ALL: List<MicroAction> = listOf(
        // ---- 呼吸 ----
        MicroAction(
            "breath_456", "4-5-6 呼吸", ActionCategory.BREATHE, 4,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING, Slot.NIGHT),
            listOf(
                "找个能坐下的地方，背靠着椅背。",
                "吸气 4 秒，不用吸满。",
                "停 5 秒，肩膀放下来。",
                "呼气 6 秒，比吸气长一点。",
                "重复 6 轮，做完就结束，不用加量。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "breath_window", "窗边换气", ActionCategory.BREATHE, 3,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "走到窗边，把窗开一条缝。",
                "慢慢吸 5 次，每次都想把空气吸到肚子。",
                "再慢慢呼 5 次，肩膀跟着往下沉。"
            ),
            setOf(WeatherKind.PRESSURE, WeatherKind.SENSITIVE, WeatherKind.RECOVERING)
        ),
        MicroAction(
            "breath_long_exhale", "长呼气", ActionCategory.BREATHE, 2,
            ALL_WEATHER_SLOTS(),
            listOf(
                "鼻子吸气 3 秒。",
                "嘴微微张开，呼气 8 秒，像吹凉一碗汤。",
                "做 5 轮，中途想停就停。"
            ),
            ALL_WEATHER
        ),

        // ---- 活动 ----
        MicroAction(
            "move_stand", "站起来走 20 步", ActionCategory.MOVE, 2,
            ALL_WEATHER_SLOTS(),
            listOf(
                "从椅子上站起来。",
                "走到房间另一头再走回来，大概 20 步。",
                "回来的时候顺便把肩膀转两圈。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "move_neck", "转肩松颈", ActionCategory.MOVE, 3,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "坐直，双手自然放在腿上。",
                "肩膀往上耸到耳朵，停 2 秒，放下。",
                "重复 8 次，然后慢慢左右转头各 5 次。"
            ),
            setOf(WeatherKind.PRESSURE, WeatherKind.SENSITIVE)
        ),
        MicroAction(
            "move_walk_out", "出门走一小段", ActionCategory.MOVE, 10,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "不用换运动服，穿鞋出门就行。",
                "走 5 分钟，不追求速度。",
                "转身走回来，路上看看天。"
            ),
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED)
        ),
        MicroAction(
            "move_stretch_leg", "坐姿抬腿", ActionCategory.MOVE, 4,
            setOf(Slot.NOON, Slot.EVENING, Slot.NIGHT),
            listOf(
                "坐在椅子前三分之一处。",
                "一条腿伸直抬起，停 5 秒，放下。",
                "左右各 6 次，做的时候腰别塌。"
            ),
            setOf(WeatherKind.RECOVERING, WeatherKind.BALANCED)
        ),
        MicroAction(
            "move_desk_neck", "桌边肩颈解冻", ActionCategory.MOVE, 1,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "坐直，双脚踩实地面。",
                "肩膀往上耸，停一下，再放下来。",
                "慢慢左右转头，各转两圈就停。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "move_office_stretch", "办公室隐形拉伸", ActionCategory.MOVE, 2,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "坐着就能做，别人看不出来。",
                "双手在背后扣住，胸口往前打开。",
                "保持几个呼吸，然后松开。",
                "再来一次，肩膀跟着往下沉。"
            ),
            setOf(WeatherKind.PRESSURE, WeatherKind.SENSITIVE)
        ),
        MicroAction(
            "move_quiet_walk", "情绪降噪散步", ActionCategory.MOVE, 5,
            setOf(Slot.NOON, Slot.EVENING),
            listOf(
                "不用换衣服，出门就行。",
                "走得不快不慢，能正常说话的速度。",
                "把注意力放在脚底和呼吸上，不用想事。",
                "走五分钟，想停就停。"
            ),
            setOf(WeatherKind.SENSITIVE, WeatherKind.PRESSURE)
        ),

        // ---- 饮食 ----
        MicroAction(
            "eat_warm_water", "喝一杯温水", ActionCategory.EAT, 2,
            ALL_WEATHER_SLOTS(),
            listOf(
                "倒一杯温水，别太烫。",
                "分几口喝完，不要一口闷。",
                "喝的时候就专心喝，不用一边做别的事。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "eat_handful_nuts", "一小把坚果", ActionCategory.EAT, 3,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "抓一小把，大概盖住手心就够。",
                "慢慢嚼，别一边刷手机一边吃。",
                "嚼出味道再咽，吃完就停。"
            ),
            setOf(WeatherKind.PRESSURE, WeatherKind.RECOVERING, WeatherKind.SENSITIVE)
        ),
        MicroAction(
            "eat_fruit", "一份水果", ActionCategory.EAT, 3,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "挑手边最容易拿到的水果。",
                "慢慢吃，尝到味道就行。",
                "吃完就好，不用凑够「每天五种」。"
            ),
            setOf(WeatherKind.ENERGY_HIGH, WeatherKind.BALANCED)
        ),
        MicroAction(
            "eat_chew_slow", "这顿慢一点吃", ActionCategory.EAT, 8,
            setOf(Slot.NOON, Slot.EVENING),
            listOf(
                "把手机放到看不见的地方。",
                "每口多嚼几下再咽。",
                "吃到七分饱就停，剩下的留着下一顿。"
            ),
            setOf(WeatherKind.PRESSURE, WeatherKind.SENSITIVE)
        ),
        MicroAction(
            "eat_three_sips", "喝三口温水", ActionCategory.EAT, 1,
            ALL_WEATHER_SLOTS(),
            listOf(
                "倒一杯温水，放在手边。",
                "慢慢喝三口，每一口都停一下。",
                "剩下的想喝再喝。"
            ),
            ALL_WEATHER
        ),

        // ---- 休息 ----
        MicroAction(
            "rest_eyes", "让眼睛歇 20 秒", ActionCategory.REST, 1,
            ALL_WEATHER_SLOTS(),
            listOf(
                "把视线从屏幕上移开。",
                "看窗外最远的东西，20 秒。",
                "顺便眨几下眼睛。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "rest_close_eyes", "闭眼 3 分钟", ActionCategory.REST, 3,
            setOf(Slot.NOON, Slot.EVENING, Slot.NIGHT),
            listOf(
                "放下手机，靠着椅背。",
                "闭眼，什么都不用想。",
                "3 分钟后自然睁开，不用定闹钟。"
            ),
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE, WeatherKind.PRESSURE)
        ),
        MicroAction(
            "rest_screen_off", "睡前半小时关屏", ActionCategory.REST, 5,
            setOf(Slot.NIGHT),
            listOf(
                "把手机放到床头够不到的地方。",
                "去做一件不用屏幕的事：洗漱、叠衣服、发呆都行。",
                "如果又想去拿手机，先坐一会儿再决定。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "rest_flat_back", "平躺放松腰", ActionCategory.REST, 5,
            setOf(Slot.EVENING, Slot.NIGHT),
            listOf(
                "找一块平地或硬一点的床面躺下。",
                "膝盖弯起来，脚踩地。",
                "躺 5 分钟，感受腰贴到地面。"
            ),
            setOf(WeatherKind.RECOVERING, WeatherKind.PRESSURE)
        ),
        MicroAction(
            "rest_gaze_far", "窗边远眺 20 秒", ActionCategory.REST, 1,
            ALL_WEATHER_SLOTS(),
            listOf(
                "走到窗边，或者把头转向窗外的方向。",
                "找一个最远的东西看住，眼睛别用力。",
                "看够 20 秒再收回来。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "rest_body_scan", "睡前身体扫描", ActionCategory.REST, 3,
            setOf(Slot.EVENING, Slot.NIGHT),
            listOf(
                "躺下或靠好，手放在肚子上。",
                "把注意力放到脚，慢慢往上走。",
                "走到哪里松到哪里，不用刻意。",
                "走完一遍就结束，睡着了也没关系。"
            ),
            ALL_WEATHER
        ),

        // ---- 光照 ----
        MicroAction(
            "light_sun", "去有光的地方待 5 分钟", ActionCategory.LIGHT, 5,
            setOf(Slot.MORNING, Slot.NOON),
            listOf(
                "走到窗边或阳台，不用直晒。",
                "待 5 分钟，光会告诉身体现在是白天。",
                "站着或者坐着都行，不用顺便做别的事。"
            ),
            setOf(WeatherKind.RECOVERING, WeatherKind.SENSITIVE)
        ),
        MicroAction(
            "light_lamp_dim", "把灯光调暗一档", ActionCategory.LIGHT, 2,
            setOf(Slot.EVENING, Slot.NIGHT),
            listOf(
                "关掉最亮的那盏灯。",
                "只留一盏暖色的。",
                "让房间提前进入夜晚。"
            ),
            ALL_WEATHER
        ),
        MicroAction(
            "light_balcony", "阳台光合作用", ActionCategory.LIGHT, 2,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "走到阳台或窗边，不用直晒。",
                "把脸朝向亮的地方，让光落在眼睛上。",
                "站两分钟，什么都不用做。"
            ),
            ALL_WEATHER
        ),

        // ---- 连接 ----
        MicroAction(
            "connect_one_msg", "给一个人发条消息", ActionCategory.CONNECT, 3,
            setOf(Slot.MORNING, Slot.NOON, Slot.EVENING),
            listOf(
                "想一个最近没联系的人。",
                "发一句话就够，不用找话题。",
                "发完就把手机放下，不用等回复。"
            ),
            setOf(WeatherKind.PRESSURE, WeatherKind.SENSITIVE, WeatherKind.RECOVERING)
        ),
        MicroAction(
            "connect_touch_ground", "脚踩地面 2 分钟", ActionCategory.CONNECT, 2,
            ALL_WEATHER_SLOTS(),
            listOf(
                "脱掉鞋，脚掌完整地踩在地板上。",
                "感受地面的温度和硬度。",
                "2 分钟，什么都不用做。"
            ),
            setOf(WeatherKind.SENSITIVE, WeatherKind.PRESSURE)
        ),
        MicroAction(
            "connect_name_feeling", "给现在的感觉起个名字", ActionCategory.CONNECT, 2,
            ALL_WEATHER_SLOTS(),
            listOf(
                "不用写下来，在心里想一个词。",
                "想不出来也没关系，「说不清」就是一个名字。",
                "起完名字就去做手边的事，不用分析它。"
            ),
            ALL_WEATHER
        )
    )

    fun byId(id: String): MicroAction? = ALL.firstOrNull { it.id == id }

    /** 适合某个天气、且属于某个时段或全天可做的动作。 */
    fun forWeather(kind: WeatherKind, slot: Slot? = null): List<MicroAction> =
        ALL.filter { it.forKinds.contains(kind) && (slot == null || it.slots.contains(slot)) }

    private fun ALL_WEATHER_SLOTS(): Set<Slot> = Slot.entries.toSet()
}

/** 处方偏好，来自设置页。 */
data class PrescriptionPrefs(
    val maxMinutes: Int = 5,
    val preferQuiet: Boolean = false,
    /** 不想收到的分类 */
    val avoid: Set<ActionCategory> = emptySet()
)

/**
 * 一张处方。
 *
 * [ticketNo] 是「车票编号」，只为了让它像一张可以被收好的东西，
 * 不承载任何排名或达成度的含义。
 */
data class Prescription(
    val ticketNo: String,
    val issuedAt: Long,
    val slot: Slot,
    val items: List<MicroAction>,
    /** 处方笺上的一句话 */
    val note: String,
    val weatherKind: WeatherKind
) {
    val totalMinutes: Int get() = items.sumOf { it.minutes }
}
