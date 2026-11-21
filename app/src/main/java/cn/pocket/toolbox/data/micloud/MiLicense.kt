package cn.pocket.toolbox.data.micloud

/**
 * 移植来源的署名与许可原文。
 *
 * 「小米云」这一路（`data/micloud/` 下的文件与 `data/MiCloudSource.kt`）**移植/改编自**
 * [ilyasaftr/better-mi-fitness-sync](https://github.com/ilyasaftr/better-mi-fitness-sync)
 * 的 `miclient` 模块与数据层：MIT License，Copyright (c) 2026 Ilyasa Fathur Rahman。
 *
 * MIT 要求在任何副本或实质性部分里保留版权声明与许可原文，所以这里把原文全文放在
 * 应用里（设置 → 关于 → 开源许可 → 查看 MIT 原文），而不只是在仓库里留一句致谢。
 * 许可原文是法律文本，**不做翻译**，也不进翻译表（[cn.pocket.toolbox.i18n.TranslationAudit]
 * 只统计用户可见的界面文案）。
 */
object MiLicense {

    const val AUTHOR = "Ilyasa Fathur Rahman"

    const val SOURCE_URL = "https://github.com/ilyasaftr/better-mi-fitness-sync"

    /** 关于页那一条的说明。 */
    const val SUMMARY = "better-mi-fitness-sync（小米云 API 客户端与数据解析）—— MIT License, " +
        "Copyright (c) 2026 Ilyasa Fathur Rahman"

    /** MIT 许可证原文（逐行，原样，不翻译）。 */
    val TEXT_LINES: List<String> = listOf(
        "MIT License",
        "",
        "Copyright (c) 2026 Ilyasa Fathur Rahman",
        "",
        "Permission is hereby granted, free of charge, to any person obtaining a copy",
        "of this software and associated documentation files (the \"Software\"), to deal",
        "in the Software without restriction, including without limitation the rights",
        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell",
        "copies of the Software, and to permit persons to whom the Software is",
        "furnished to do so, subject to the following conditions:",
        "",
        "The above copyright notice and this permission notice shall be included in all",
        "copies or substantial portions of the Software.",
        "",
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR",
        "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,",
        "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE",
        "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER",
        "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,",
        "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE",
        "SOFTWARE."
    )
}
