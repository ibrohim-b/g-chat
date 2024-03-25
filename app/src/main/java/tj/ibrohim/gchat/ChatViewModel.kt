package tj.ibrohim.gchat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


private const val TAG = "ChatViewModel"

class ChatViewModel : ViewModel() {
    private val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE)

    private val hateSpeechSafety = SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE)

    private val sexuallyExplicitSafety =
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE)

    private val dangerousContentSafety =
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)

    private lateinit var generativeModel: GenerativeModel

    private lateinit var chat: Chat

    val messagesMutableStateFlow: MutableStateFlow<MutableList<Message>> = MutableStateFlow(
        emptyList<Message>().toMutableList()
    )

    /**
     * Initializes the generative model and chat.
     *
     * @param geminiModel The Gemini model to use.
     */
    fun init() {
        generativeModel = GenerativeModel(
            modelName = "gemini-pro", apiKey = BuildConfig.apiKey, safetySettings = listOf(
                harassmentSafety, hateSpeechSafety, sexuallyExplicitSafety, dangerousContentSafety
            )
        )
        chat = generativeModel.startChat()
    }

    /**
     * Sends a message to the chat and returns a MutableStateFlow with the response.
     *
     * @param userMessage The message to send.
     * @param onResponseProcessingCompletion A callback to handle response processing events.
     * @return A MutableStateFlow containing the response.
     */
    fun sendMessage(
        userMessage: String, onResponseProcessingCompletion: OnResponseProcessingCompletion,
    ): MutableStateFlow<String> {
        val response = MutableStateFlow("")
        onResponseProcessingCompletion.blockInput()
        viewModelScope.launch {
            try {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    response.value = chunk.text.toString()
                }
            } catch (e: Exception) {
                Log.d(TAG, "sendMessage: e = $e")
                onResponseProcessingCompletion.showError(e)
                response.value = e.localizedMessage?.toString() ?: "Error occurred"
                onResponseProcessingCompletion.unblockInput()
            }
        }
        onResponseProcessingCompletion.unblockInput()
        return response

    }

}