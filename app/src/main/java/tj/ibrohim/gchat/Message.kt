package tj.ibrohim.gchat

import android.graphics.Bitmap
import java.time.LocalDateTime

data class Message(
    var text: String,
    val type: MessageTypes,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val imagesBitmaps : MutableList<Bitmap>? = null,
)

