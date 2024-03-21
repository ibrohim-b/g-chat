package tj.ibrohim.gchat

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val TAG = "MainFragment"

class MainFragment : Fragment() {

    private lateinit var startChatMaterialButton: MaterialButton

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_main, container, false)
        startChatMaterialButton = view.findViewById(R.id.startChatMaterialButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startChatMaterialButton.setOnClickListener {
            val geminiModels = resources.getStringArray(R.array.gemini_models)
            var modelSelected: GeminiModels = GeminiModels.GEMINI_PRO

            MaterialAlertDialogBuilder(requireContext()).setTitle("Start chat")
                .setSingleChoiceItems(geminiModels, 0) { _, which ->
                    Log.d(TAG, "onClick: item selected: ${geminiModels[which]}")
                    when (geminiModels[which]) {
                        GeminiModels.GEMINI_PRO.tag -> {
                            modelSelected = GeminiModels.GEMINI_PRO
                        }

                        GeminiModels.GEMINI_VISION.tag -> {
                            modelSelected = GeminiModels.GEMINI_VISION
                        }
                    }
                }
                .setPositiveButton("Start") { _, _ ->
                    Log.d(TAG, "onViewCreated: chat with $modelSelected started")
                    if (savedInstanceState == null) {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.container, ChatFragment.newInstance(modelSelected))
                            .addToBackStack(null)
                            .commit()
                    }
                }
                .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                }
                .show()

        }
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}

enum class GeminiModels(val tag: String, val id: String) {
    GEMINI_PRO("Gemini Pro", "gemini-pro"),
    GEMINI_VISION("Gemini Vision Pro", "gemini-pro-vision")
}