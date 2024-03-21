package tj.ibrohim.gchat

import java.time.LocalDateTime

data class Message(
    var text: String,
    val type: MessageTypes,
    val dateTime: LocalDateTime = LocalDateTime.now()
)

