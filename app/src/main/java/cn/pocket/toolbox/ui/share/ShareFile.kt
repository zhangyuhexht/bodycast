package cn.pocket.toolbox.ui.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import cn.pocket.toolbox.i18n.BodyCastText
import java.io.File

/**
 * 通用「文件分享」（22.41.0）：把一段文本落到缓存目录，走系统分享面板发出去。
 *
 * 与天气明信片共用同一个 FileProvider（缓存目录下的 exports/）；
 * 导出数据的两条出路——「另存为」走系统文档创建器、「分享」走这里——
 * 同一份内容、两种去向，用户自己选。
 */
object ShareFile {

    fun share(context: Context, text: String, fileName: String, mime: String = "application/octet-stream") {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(text)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, BodyCastText.text("把文件发出去")))
    }
}
