package tj.ibrohim.gchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import io.noties.markwon.Markwon

private const val MESSAGE_FROM_USER = 0
private const val MESSAGE_FROM_MODEL = 1

class MessagesAdapter(
    private val items: MutableList<Message>,
    private val onMessageAction: OnMessageAction,
    private val markwon: Markwon,
    private val modelName: String,
) : RecyclerView.Adapter<ViewHolder>() {


    inner class ModelMessageViewHolder(itemView: View) : ViewHolder(itemView) {
        private val messageContentTextView: TextView =
            itemView.findViewById(R.id.message_content_text_view)
        private val modelTextView: TextView = itemView.findViewById(R.id.model_text_view)
        private val copyContentMaterialButton: MaterialButton =
            itemView.findViewById(R.id.copy_content_material_button)

        fun bind(message: Message) {
            markwon.setMarkdown(messageContentTextView, message.text)
            copyContentMaterialButton.isEnabled = message.text.isNotEmpty()
            modelTextView.text =
                if (message.type.name == MessageTypes.MODEL.name) modelName else "You"
            copyContentMaterialButton.setOnClickListener {
                onMessageAction.onContentCopyClick(message.text)
            }

        }
    }

    inner class UserMessageViewHolder(itemView: View) : ViewHolder(itemView) {
        private val messageContentTextView: TextView =
            itemView.findViewById(R.id.message_content_text_view)

        fun bind(message: Message) {
            messageContentTextView.text = message.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == MESSAGE_FROM_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_from_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.message_from_model, parent, false)
            ModelMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when (holder.itemViewType) {
            MESSAGE_FROM_USER -> (holder as UserMessageViewHolder).bind(item)
            MESSAGE_FROM_MODEL -> (holder as ModelMessageViewHolder).bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type.name) {
            MessageTypes.USER.name -> MESSAGE_FROM_USER
            MessageTypes.MODEL.name -> MESSAGE_FROM_MODEL
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}
