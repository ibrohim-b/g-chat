package tj.ibrohim.gchat

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.type.InvalidStateException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.UnknownException
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


// Constant for the Gemini model tag.
const val GEMINI_MODEL = "GEMINI_MODEL"
private const val TAG = "ChatFragment"

class ChatFragment : Fragment(), OnMessageAction, OnResponseProcessingCompletion {

    // Declare the views used in the fragment.
    private lateinit var sendMessageTextField: TextInputLayout
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var chatMaterialToolbar: MaterialToolbar
    private lateinit var messageGeneratingLinearProgressIndicator: LinearProgressIndicator

    // Initialize the Markwon library for rendering Markdown.
    private lateinit var markwon: Markwon

    // Get the Gemini model tag from the arguments.
    private val geminiModelTag: String? by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getString(GEMINI_MODEL)
    }


    // Initialize the view model.
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: geminiModelTag = $geminiModelTag")
        // Get the Gemini model from the tag.
        val geminiModel: GeminiModels = GeminiModels.entries.find { it.tag == geminiModelTag }!!

        // Initialize the view model with the Gemini model.
        viewModel.init(geminiModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for the fragment.
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Find the views by their IDs.
        sendMessageTextField = view.findViewById(R.id.send_message_text_field)
        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view)
        chatMaterialToolbar = view.findViewById(R.id.chat_material_toolbar)
        messageGeneratingLinearProgressIndicator =
            view.findViewById(R.id.message_generating_linear_progress_indicator)

        // Return the view.
        return view
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Build the Markwon instance.
        markwon = Markwon.builder(requireContext()).build()

        // Collect the messages from the view model and set the adapter for the recycler view.
        lifecycleScope.launch {
            viewModel.messagesMutableStateFlow.collect {
                messagesRecyclerView.adapter = MessagesAdapter(
                    it,
                    this@ChatFragment,
                    markwon,
                    modelName = "Gemini Model"
                )
                messagesRecyclerView.adapter?.notifyDataSetChanged()
            }
        }

        // Set the end icon click listener for the send message text field.
        sendMessageTextField.setEndIconOnClickListener {
            // Get the text from the send message text field.
            val prompt = sendMessageTextField.editText?.text.toString()

            // If the prompt is not empty, send the message.
            if (prompt.isNotEmpty()) {
                // Create a user message.
                val userMessage = Message(
                    text = prompt,
                    type = MessageTypes.USER,
                )

                // Add the user message to the messages mutable state flow.
                viewModel.messagesMutableStateFlow.value.add(userMessage)

                // Create a response message.
                val responseMessage = Message(
                    text = "",
                    type = MessageTypes.MODEL,
                )

                // Add the response message to the messages mutable state flow.
                viewModel.messagesMutableStateFlow.value.add(responseMessage)

                // Send the message to the view model and collect the response.
                val response = viewModel.sendMessage(userMessage.text, this)
                lifecycleScope.launch {
                    response.collect {
                        // Update the response message with the response text.
                        viewModel.messagesMutableStateFlow.value.last().text += it
                        viewModel.messagesMutableStateFlow.value = viewModel.messagesMutableStateFlow.value
                        messagesRecyclerView.adapter?.notifyDataSetChanged()
                        messagesRecyclerView.scrollToPosition(viewModel.messagesMutableStateFlow.value.size - 1)
                        Log.d(TAG, "Response chunk: $it ")
                    }
                }

                // Clear the send message text field.
                sendMessageTextField.editText?.text?.clear()
            }
        }
    }


    /**
     * Handles the click event on the "Copy" button.
     *
     * @param text The text to be copied.
     */
    override fun onContentCopyClick(text: String) {
        // Get the clipboard manager from the activity context.
        val clipboardManager =
            requireActivity().applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a clip data object with the text and label.
        val clipData = ClipData.newPlainText("label", text)

        // Set the primary clip to the clipboard manager.
        clipboardManager.setPrimaryClip(clipData)

        // Show a toast message to indicate that the text has been copied.
        Toast.makeText(this.context, "Text copied", Toast.LENGTH_SHORT).show()
    }

    /**
     * Blocks user input by disabling the send message text field and showing the message generating linear progress indicator.
     */
    override fun blockInput() {
        // Disable the send message text field.
        sendMessageTextField.isEnabled = false

        // Show the message generating linear progress indicator.
        messageGeneratingLinearProgressIndicator.visibility = View.VISIBLE
    }

    /**
     * Unblocks the user input.
     *
     * - Enables the send message text field.
     * - Hides the message generating linear progress indicator.
     */
    override fun unblockInput() {
        // Enable the send message text field.
        sendMessageTextField.isEnabled = true

        // Hide the message generating linear progress indicator.
        messageGeneratingLinearProgressIndicator.visibility = View.GONE
    }

    /**
     * Shows an error message to the user.
     *
     * @param error The error went during generating response
     */
    override fun showError(error: Exception) {
        // Hide the message generating linear progress indicator.
        messageGeneratingLinearProgressIndicator.visibility = View.GONE

        // Enable the send message text field.
        sendMessageTextField.isEnabled = true

        Log.d(TAG, "showError: $error")
        when (error) {
            is ServerException -> {
                Snackbar.make(
                    this.requireView(),
                    "Something went wrong on the server side",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is SerializationException -> {
                Snackbar.make(
                    this.requireView(),
                    "Something went wrong while trying to deserialize a response from the server.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is PromptBlockedException -> {
                Snackbar.make(
                    this.requireView(),
                    "A request was blocked for some reason.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is InvalidStateException -> {
                Snackbar.make(
                    this.requireView(),
                    "Some form of state occurred that shouldn't have.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is ResponseStoppedException -> {
                Snackbar.make(
                    this.requireView(),
                    "A request was stopped during generation for some reason.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            is UnknownException -> {
                Snackbar.make(
                    this.requireView(),
                    "Exception not explicitly expected.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

        }
    }

    companion object {
        // Create a new instance of the fragment with the specified Gemini model.
        fun newInstance(geminiModel: GeminiModels) = ChatFragment().apply {
            arguments = bundleOf(GEMINI_MODEL to geminiModel.tag)
        }
    }

}

interface OnMessageAction {
    fun onContentCopyClick(text: String)
}