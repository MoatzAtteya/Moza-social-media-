package com.example.moza.utils

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.databinding.RightMessageBottomSheetBinding
import com.example.moza.models.ChatMessage
import com.example.moza.models.MessageOptionViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RightMessageBottomSheet(
    val messageID: String,
    val chatID: String,
    val message: ChatMessage
) :
    BottomSheetDialogFragment(),
    View.OnClickListener {
    lateinit var binding: RightMessageBottomSheetBinding
    lateinit var viewModel: MessageOptionViewModel
    val repository = FireBaseRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RightMessageBottomSheetBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[MessageOptionViewModel::class.java]

        binding.deleteMessageLayout.setOnClickListener(this)
        binding.editMessageLayout.setOnClickListener(this)
        binding.copyMessageLayout.setOnClickListener(this)
        binding.forwardMessageLayout.setOnClickListener(this)

        updateUI()

        return binding.root
    }

    private fun updateUI() {
        val ago = DateUtils.getRelativeTimeSpanString(
            message.time!!,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        if (ago.equals("0 minutes ago"))
            binding.messageDateTv.text = "Now"
        else
            binding.messageDateTv.text = "From: $ago"


        if (message.messageType == 0 || message.messageType == 2) {
            binding.editMessageLayout.visibility = View.VISIBLE
            binding.copyMessageLayout.visibility = View.VISIBLE
            if (message.editedText!!.isEmpty()) {
                binding.editedMessageTv.visibility = View.GONE
            } else {
                binding.editedMessageTv.visibility = View.VISIBLE
                binding.editedMessageTv.text = "Original Message: ${message.message.toString()}"
            }
        } else {
            binding.editMessageLayout.visibility = View.GONE
            binding.copyMessageLayout.visibility = View.GONE
        }

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.delete_message_layout -> {
                if (message.deleted) {
                    Toast.makeText(context, "The message is already deleted.", Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                val expireDate = message.time!! + (24 * 60 * 60 * 1000)
                if (expireDate <= System.currentTimeMillis()){
                    Toast.makeText(
                        context,
                        "Can't delete message after 24 hours.",
                        Toast.LENGTH_SHORT
                    ).show()
                    this.dismiss()
                }

                else {
                    viewModel.safeDeleteMessage(messageID,chatID)
                    viewModel.deleteMessageResponse.observe(viewLifecycleOwner){ response->
                        when(response){
                            is Resource.Error -> Log.e("Deleting message: " , response.message!!)
                            is Resource.Loading -> TODO()
                            is Resource.Success ->{
                                Toast.makeText(
                                    requireContext(),
                                    "Message deleted.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                this.dismiss()
                            }
                        }
                    }
                }
            }
            R.id.edit_message_layout -> {
                // allow only the (textMessage - reply Message) to be edited.
                if (message.messageType == 0 || message.messageType == 2) {
                    if (message.editedText!!.isEmpty()) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val builder = AlertDialog.Builder(context)
                            val inflater = layoutInflater
                            val dialogLayout = inflater.inflate(R.layout.message_edit_dialog, null)
                            val editText = dialogLayout.findViewById<EditText>(R.id.message_edit_et)

                            with(builder) {
                                setPositiveButton("Change") { dialog, which ->
                                    val editedText = editText.text.toString()
                                    if (editText.text.toString().isNotEmpty()){
                                        viewModel.safeEditMessage(messageID, chatID, editedText)
                                        viewModel.editMessageResponse.observe(viewLifecycleOwner){ response->
                                            when(response){
                                                is Resource.Error -> Log.e("Editing message: " , response.message!!)
                                                is Resource.Loading -> TODO()
                                                is Resource.Success ->{
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Message edited.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    this@RightMessageBottomSheet.dismiss()
                                                }
                                            }
                                        }
                                    }
                                }
                                setNegativeButton("Cancel") { dialog, which ->

                                }
                                setView(dialogLayout)
                                show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "You can't edit an edited message.",
                            Toast.LENGTH_SHORT
                        ).show()
                        this.dismiss()
                    }
                }
            }

            R.id.copy_message_layout -> {
                val clipboardManager =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("copied message", message.message)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(context, "Message copied.", Toast.LENGTH_SHORT).show()
                this.dismiss()
            }

            R.id.forward_message_layout -> {
                when (message.messageType) {
                    Constants.TEXT_MESSAGE, Constants.REPLY_MESSAGE -> {
                        val bottomSheetDialog = SendMessageToBottomSheet(message.message!!,Constants.TEXT_MESSAGE,false)
                        bottomSheetDialog.show(
                            requireActivity().supportFragmentManager,
                            "Send Message To: "
                        )
                    }
                    Constants.IMAGE_MESSAGE -> {
                        val bottomSheetDialog = SendMessageToBottomSheet(message.messageUrl!!,Constants.IMAGE_MESSAGE,false)
                        bottomSheetDialog.show(
                            requireActivity().supportFragmentManager,
                            "Send Message To: "
                        )
                    }
                    Constants.VIDEO_MESSAGE -> {
                        val bottomSheetDialog = SendMessageToBottomSheet(message.messageUrl!!,Constants.VIDEO_MESSAGE,false)
                        bottomSheetDialog.show(
                            requireActivity().supportFragmentManager,
                            "Send Message To: "
                        )
                    }
                    Constants.DOCUMENT_MESSAGE -> {
                        val bottomSheetDialog = SendMessageToBottomSheet(message.messageUrl!!,Constants.DOCUMENT_MESSAGE,false)
                        bottomSheetDialog.show(
                            requireActivity().supportFragmentManager,
                            "Send Message To: "
                        )
                    }
                    Constants.VOICE_MESSAGE -> {
                        val bottomSheetDialog = SendMessageToBottomSheet(message.messageUrl!!,Constants.VOICE_MESSAGE,false)
                        bottomSheetDialog.show(
                            requireActivity().supportFragmentManager,
                            "Send Message To: "
                        )
                    }
                }
            }
        }
    }


}