package com.example.moza.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.databinding.LeftMessageBottomSheetBinding
import com.example.moza.models.ChatMessage
import com.example.moza.viewmodels.MessageReactViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LeftMessageBottomSheet(val chatID: String, val message: ChatMessage) :
    BottomSheetDialogFragment() {
    lateinit var binding: LeftMessageBottomSheetBinding
    lateinit var viewModel: MessageReactViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LeftMessageBottomSheetBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(MessageReactViewModel::class.java)

        val ago = DateUtils.getRelativeTimeSpanString(
            message.time!!,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        if (ago.equals("0 minutes ago"))
            binding.messageDateTv.text = "Now"
        else
            binding.messageDateTv.text = "From: $ago"


        if (message.editedText!!.isEmpty()) {
            binding.editedMessageTv.visibility = View.GONE
        } else {
            binding.editedMessageTv.visibility = View.VISIBLE
            binding.editedMessageTv.text = "Original Message: ${message.message.toString()}"
        }


        var emojiTyp = ""
        binding.laughEmoji.setOnClickListener {
            emojiTyp = Constants.Laugh_EMOJI
            viewModel.safeSendReact(chatID, message.id!!, emojiTyp)
            this.dismiss()
        }
        binding.sadEmoji.setOnClickListener {
            emojiTyp = Constants.SAD_EMOJI
            viewModel.safeSendReact(chatID, message.id!!, emojiTyp)
            this.dismiss()
        }
        binding.wowEmoji.setOnClickListener {
            emojiTyp = Constants.WOW_EMOJI
            viewModel.safeSendReact(chatID, message.id!!, emojiTyp)
            this.dismiss()
        }
        binding.angryEmoji.setOnClickListener {
            emojiTyp = Constants.ANGRY_EMOJI
            viewModel.safeSendReact(chatID, message.id!!, emojiTyp)
            this.dismiss()
        }
        binding.likeEmoji.setOnClickListener {
            emojiTyp = Constants.LIKE_EMOJI
            viewModel.safeSendReact(chatID, message.id!!, emojiTyp)
            this.dismiss()
        }
        binding.noneEmoji.setOnClickListener {
            emojiTyp = Constants.NONE_EMOJI
            viewModel.safeSendReact(chatID, message.id!!, emojiTyp)
            this.dismiss()
        }

        binding.copyMessageLayout.setOnClickListener {
            val clipboardManager =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("copied message", message.message)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(context, "Message copied.", Toast.LENGTH_SHORT).show()
            this.dismiss()
        }

        binding.forwardMessageLayout.setOnClickListener {
            when (message.messageType) {
                Constants.TEXT_MESSAGE, Constants.REPLY_MESSAGE -> {
                    val bottomSheetDialog =
                        SendMessageToBottomSheet(message.message!!, Constants.TEXT_MESSAGE, true)
                    bottomSheetDialog.show(
                        requireActivity().supportFragmentManager,
                        "Send Message To: "
                    )
                }
                Constants.IMAGE_MESSAGE -> {
                    val bottomSheetDialog = SendMessageToBottomSheet(
                        message.messageUrl!!,
                        Constants.IMAGE_MESSAGE,
                        true
                    )
                    bottomSheetDialog.show(
                        requireActivity().supportFragmentManager,
                        "Send Message To: "
                    )
                }
                Constants.VIDEO_MESSAGE -> {
                    val bottomSheetDialog = SendMessageToBottomSheet(
                        message.messageUrl!!,
                        Constants.VIDEO_MESSAGE,
                        true
                    )
                    bottomSheetDialog.show(
                        requireActivity().supportFragmentManager,
                        "Send Message To: "
                    )
                }
                Constants.DOCUMENT_MESSAGE -> {
                    val bottomSheetDialog = SendMessageToBottomSheet(
                        message.messageUrl!!,
                        Constants.DOCUMENT_MESSAGE,
                        true
                    )
                    bottomSheetDialog.show(
                        requireActivity().supportFragmentManager,
                        "Send Message To: "
                    )
                }
                Constants.VOICE_MESSAGE -> {
                    val bottomSheetDialog = SendMessageToBottomSheet(
                        message.messageUrl!!,
                        Constants.VOICE_MESSAGE,
                        true
                    )
                    bottomSheetDialog.show(
                        requireActivity().supportFragmentManager,
                        "Send Message To: "
                    )
                }
            }

        }

        return binding.root
    }

}