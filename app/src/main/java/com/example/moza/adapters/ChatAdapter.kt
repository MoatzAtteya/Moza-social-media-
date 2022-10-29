package com.example.moza.adapters

import android.app.DownloadManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.databinding.*
import com.example.moza.fragments.ChatFragment
import com.example.moza.models.ChatMessage
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


class ChatAdapter(val massagesFragment: ChatFragment, val themeName: String) :
    RecyclerView.Adapter<ViewHolder>() {

    var messages: MutableList<ChatMessage> = mutableListOf()
    private lateinit var mListener: OnItemClickListener
    private var isGroupChat = false

    interface OnItemClickListener {
        fun onItemClick(position: Int, id: String, message: ChatMessage)
        fun onLeftItemLongClick(position: Int, message: ChatMessage)
        fun onImageClick(position: Int, message: ChatMessage)
        fun onVideoClick(position: Int, message: ChatMessage)

    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //we have different views so first we need to check the message type
        //then inflate it's viewHolder.
        when (viewType) {
            Constants.TEXT_MESSAGE -> {
                return TextMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.text_message_layout, parent, false)
                )
            }
            Constants.IMAGE_MESSAGE -> {
                return ImageMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.image_message_layout, parent, false)
                )
            }
            Constants.REPLY_MESSAGE -> {
                return ReplyMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.reply_message_layout, parent, false)
                )
            }
            Constants.VOICE_MESSAGE -> {
                return VoiceMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.voice_message_layout, parent, false)
                )
            }
            Constants.DOCUMENT_MESSAGE -> {
                return DocumentMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.document_message_layout, parent, false)
                )
            }
            Constants.VIDEO_MESSAGE -> {
                return VideoMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.video_message_layout, parent, false)
                )
            }
            else -> {
                return DeletedMessageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.deleted_message_layout, parent, false)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var message = messages[position]
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (holder is TextMessageViewHolder) {
            applyTextMessageTheme(holder)
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {
                holder.binding.leftMessageItem.visibility = View.GONE
                holder.binding.leftMessageEdited.visibility = View.GONE
                holder.binding.leftReactCv.visibility = View.GONE
                holder.binding.rightReactCv.visibility = View.GONE
                holder.binding.rightMessageForwardIv.visibility = View.GONE
                holder.binding.leftMessageForwardIv.visibility = View.GONE
                holder.binding.rightMessageItemLl.visibility = View.VISIBLE
                holder.binding.rightMessageItem.visibility = View.VISIBLE

                setRightReact(holder, message.react)
                setRightMessageDate(holder, message.time)

                if (position != 0) {
                    checkMessageData(messages[position - 1], message, holder)
                }

                if (message.editedText.isNullOrEmpty()) {
                    holder.binding.rightMessageItem.text = message.message
                    holder.binding.rightMessageEdited.visibility = View.GONE
                } else {
                    holder.binding.leftMessageEdited.visibility = View.GONE
                    holder.binding.rightMessageEdited.visibility = View.VISIBLE
                    holder.binding.rightMessageItem.text = message.editedText
                }

                if (message.isForwarded)
                    holder.binding.rightMessageForwardIv.visibility = View.VISIBLE
                else
                    holder.binding.rightMessageForwardIv.visibility = View.GONE

                if (message.senderID == firebaseUser.uid) {
                    holder.itemView.setOnLongClickListener {
                        mListener.onItemClick(position, message.id!!, message)
                        return@setOnLongClickListener false
                    }
                }

            } else {
                holder.binding.rightMessageItem.visibility = View.GONE
                holder.binding.rightMessageEdited.visibility = View.GONE
                holder.binding.leftReactCv.visibility = View.GONE
                holder.binding.rightReactCv.visibility = View.GONE
                holder.binding.rightMessageForwardIv.visibility = View.GONE
                holder.binding.leftMessageForwardIv.visibility = View.GONE
                holder.binding.leftMessageItemLl.visibility = View.VISIBLE
                holder.binding.leftMessageItem.visibility = View.VISIBLE
                setLeftReact(holder, message.react)
                setLeftMessageDate(holder, message.time)
                if (message.editedText.isNullOrEmpty()) {
                    holder.binding.leftMessageItem.text = message.message
                    holder.binding.leftMessageEdited.visibility = View.GONE

                } else {
                    holder.binding.rightMessageEdited.visibility = View.GONE
                    holder.binding.leftMessageEdited.visibility = View.VISIBLE
                    holder.binding.leftMessageItem.text = message.editedText
                }

                if (message.isForwarded)
                    holder.binding.leftMessageForwardIv.visibility = View.VISIBLE
                else
                    holder.binding.leftMessageForwardIv.visibility = View.GONE


                holder.itemView.setOnLongClickListener {
                    mListener.onLeftItemLongClick(position, message)
                    return@setOnLongClickListener false
                }

                if (position != 0) {
                    checkRepeatedSender(messages[position - 1], message, holder)
                    checkMessageData(messages[position - 1], message, holder)
                }


            }
        } else if (holder is ImageMessageViewHolder) {
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {
                holder.binding.apply {
                    leftMessageItemImageCv.visibility = View.GONE
                    leftReactCv.visibility = View.GONE
                    rightReactCv.visibility = View.GONE
                    rightProgressBar.visibility = View.VISIBLE
                    rightMessageItemImageCv.visibility = View.VISIBLE

                    setRightReact(holder, message.react)
                    Glide.with(massagesFragment.requireContext())
                        .load(message.messageUrl)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                rightProgressBar.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                rightProgressBar.visibility = View.GONE
                                return false
                            }

                        })
                        .into(rightMessageItemImageIv)


                    if (message.senderID == firebaseUser.uid) {
                        holder.binding.rightMessageItemImageIv.setOnLongClickListener {
                            mListener.onItemClick(position, message.id!!, message)
                            return@setOnLongClickListener true
                        }
                        holder.binding.rightMessageItemImageIv.setOnClickListener {
                            mListener.onImageClick(position, message)
                        }
                    }


                }

            } else {
                holder.binding.apply {
                    rightMessageItemImageCv.visibility = View.GONE
                    leftReactCv.visibility = View.GONE
                    rightReactCv.visibility = View.GONE
                    leftProgressBar.visibility = View.VISIBLE
                    leftMessageItemImageCv.visibility = View.VISIBLE

                    setLeftReact(holder, message.react)

                    if (position != 0) {
                        checkRepeatedSender(messages[position - 1], message, holder)
                        checkMessageData(messages[position - 1], message, holder)
                    }

                    Glide.with(massagesFragment.requireContext())
                        .load(message.messageUrl)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                leftProgressBar.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                leftProgressBar.visibility = View.GONE
                                return false
                            }

                        })
                        .into(leftMessageItemImageIv)

                    holder.itemView.setOnLongClickListener {
                        mListener.onLeftItemLongClick(position, message)
                        return@setOnLongClickListener true
                    }
                    holder.binding.leftMessageItemImageIv.setOnClickListener {
                        mListener.onImageClick(position, message)
                    }

                }
            }

        } else if (holder is ReplyMessageViewHolder) {
            applyReplyTextMessageTheme(holder)
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {
                holder.binding.apply {
                    leftReplyingMessageItemLl.visibility = View.GONE
                    leftReplyingUryIv.visibility = View.GONE
                    replyingUryIv.visibility = View.GONE
                    rightReactCv.visibility = View.GONE
                    leftReactCv.visibility = View.GONE
                    rightReplyingMessageItemLl.visibility = View.VISIBLE
                    rightReplyingUsernameTv.text = message.replyTo

                    setRightMessageDate(holder, message.time)
                    setRightReact(holder, message.react)
                    if (message.replyType == 0) {
                        rightReplyingMessageTv.text = message.replyingText
                    } else if (message.replyType == 1) {
                        replyingUryIv.visibility = View.VISIBLE
                        rightReplyingMessageTv.text = "Photo"
                        Glide.with(massagesFragment.requireContext())
                            .load(message.replyingText)
                            .into(replyingUryIv)

                    }
                    rightReplyingMessageItem.text = message.message

                }
                if (message.senderID == firebaseUser.uid) {
                    holder.itemView.setOnLongClickListener {
                        mListener.onItemClick(position, message.id!!, message)
                        return@setOnLongClickListener true
                    }
                }
            } else {
                holder.binding.apply {
                    rightReplyingMessageItemLl.visibility = View.GONE
                    replyingUryIv.visibility = View.GONE
                    rightReactCv.visibility = View.GONE
                    leftReactCv.visibility = View.GONE
                    leftReplyingUryIv.visibility = View.GONE
                    leftReplyingMessageItemLl.visibility = View.VISIBLE
                    leftReplyingUsernameTv.text = message.replyTo
                    if (position != 0) {
                        checkRepeatedSender(messages[position - 1], message, holder)
                        checkMessageData(messages[position - 1], message, holder)
                    }

                    setLeftMessageDate(holder, message.time)
                    setLeftReact(holder, message.react)
                    if (message.replyType == 0) {
                        leftReplyingMessageTv.text = message.replyingText
                    } else if (message.replyType == 1) {
                        leftReplyingUryIv.visibility = View.VISIBLE
                        leftReplyingMessageTv.text = "Photo"
                        Glide.with(massagesFragment.requireContext())
                            .load(message.replyingText)
                            .into(leftReplyingUryIv)

                    }
                    leftReplyingMessageItem.text = message.message
                }
                holder.itemView.setOnLongClickListener {
                    mListener.onLeftItemLongClick(position, message)
                    return@setOnLongClickListener true
                }
            }
        } else if (holder is VoiceMessageViewHolder) {
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {
                holder.binding.apply {
                    leftVoicePlayerView.visibility = View.GONE
                    rightVoicePlayerView.visibility = View.VISIBLE
                    rightReactIv.visibility = View.GONE
                    leftReactIv.visibility = View.GONE

                    setRightReact(holder, message.react)


                    CoroutineScope(Dispatchers.IO).launch {
                        rightVoicePlayerView.setAudio(message.messageUrl)
                    }

                    if (message.senderID == firebaseUser.uid) {
                        holder.itemView.setOnLongClickListener {
                            mListener.onItemClick(position, message.id!!, message)
                            return@setOnLongClickListener false
                        }
                    }
                }
            } else {
                holder.binding.apply {
                    rightVoicePlayerView.visibility = View.GONE
                    leftVoicePlayerView.visibility = View.VISIBLE
                    rightReactIv.visibility = View.GONE
                    leftReactIv.visibility = View.GONE
                    if (position != 0) {
                        checkRepeatedSender(messages[position - 1], message, holder)
                        checkMessageData(messages[position - 1], message, holder)
                    }
                    setLeftReact(holder, message.react)

                    CoroutineScope(Dispatchers.IO).launch {
                        leftVoicePlayerView.setAudio(message.messageUrl)
                    }
                    holder.itemView.setOnLongClickListener {
                        mListener.onLeftItemLongClick(position, message)
                        return@setOnLongClickListener false
                    }
                }
            }
        } else if (holder is DocumentMessageViewHolder) {
            applyDocMessageTheme(holder)
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {
                holder.binding.apply {
                    leftDocMessageItemLl.visibility = View.GONE
                    rightDocMessageItemLl.visibility = View.VISIBLE

                    setRightReact(holder, message.react)
                    val fileName = getFileName(message.messageUrl)
                    rightDownloadDocTv.text = fileName

                    val storageReference =
                        FirebaseStorage.getInstance().getReferenceFromUrl(message.messageUrl!!)

                    storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener {
                        var fileSize = it.size.toLong()
                        fileSize /= 1024
                        rightDocSizeTv.text = "${fileSize}.KB"
                    }
                    rightDownloadDocumentIv.setOnClickListener {
                        downloadDoc(message.messageUrl, fileName)
                        Toast.makeText(
                            massagesFragment.requireContext(),
                            "Downloading...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (message.senderID == firebaseUser.uid) {
                        holder.itemView.setOnLongClickListener {
                            mListener.onItemClick(position, message.id!!, message)
                            return@setOnLongClickListener false
                        }
                    }

                }
            } else {
                holder.binding.apply {
                    leftDocMessageItemLl.visibility = View.VISIBLE
                    rightDocMessageItemLl.visibility = View.GONE
                    if (position != 0) {
                        checkRepeatedSender(messages[position - 1], message, holder)
                        checkMessageData(messages[position - 1], message, holder)
                    }

                    setLeftReact(holder, message.react)
                    val fileName = getFileName(message.messageUrl)
                    val storageReference =
                        FirebaseStorage.getInstance().getReferenceFromUrl(message.messageUrl!!)
                    storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener { it ->
                        var fileSize = it.size.toLong()
                        fileSize /= 1024
                        leftDocSizeTv.text = "${fileSize}.KB"

                    }
                    leftDownloadDocumentIv.setOnClickListener {
                        downloadDoc(message.messageUrl, fileName)
                        Toast.makeText(
                            massagesFragment.requireContext(),
                            "Downloading...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    holder.itemView.setOnLongClickListener {
                        mListener.onLeftItemLongClick(position, message)
                        return@setOnLongClickListener false
                    }
                }
            }
        } else if (holder is VideoMessageViewHolder) {
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {

                holder.binding.apply {
                    rightMessageItemVideoCv.visibility = View.VISIBLE
                    leftMessageItemVideoCv.visibility = View.GONE
                    rightProgressBar.visibility = View.GONE

                    setRightReact(holder, message.react)
                    Glide.with(massagesFragment.requireContext())
                        .load(message.messageUrl)
                        .thumbnail(0.1f)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                rightProgressBar.visibility = View.GONE
                                rightMessagePlayItemImageIv.visibility = View.VISIBLE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                rightProgressBar.visibility = View.GONE
                                rightMessagePlayItemImageIv.visibility = View.VISIBLE
                                return false
                            }

                        })
                        .into(rightMessageItemImageIv)
                    rightMessagePlayItemImageIv.setOnClickListener {
                        mListener.onVideoClick(position, message)
                    }
                    if (message.senderID == firebaseUser.uid) {
                        holder.itemView.setOnLongClickListener {
                            mListener.onItemClick(position, message.id!!, message)
                            return@setOnLongClickListener false
                        }
                    }

                }
            } else {
                holder.binding.apply {

                    rightMessageItemVideoCv.visibility = View.GONE
                    leftMessageItemVideoCv.visibility = View.VISIBLE
                    leftProgressBar.visibility = View.GONE
                    if (position != 0) {
                        checkRepeatedSender(messages[position - 1], message, holder)
                        checkMessageData(messages[position - 1], message, holder)
                    }

                    setLeftReact(holder, message.react)
                    Glide.with(massagesFragment.requireContext())
                        .load(message.messageUrl)
                        .thumbnail(0.1f)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                leftProgressBar.visibility = View.GONE
                                leftMessagePlayItemImageIv.visibility = View.VISIBLE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                leftProgressBar.visibility = View.GONE
                                leftMessagePlayItemImageIv.visibility = View.VISIBLE
                                return false
                            }

                        })
                        .into(leftMessageItemImageIv)
                    leftMessagePlayItemImageIv.setOnClickListener {
                        mListener.onVideoClick(position, message)
                    }
                    holder.itemView.setOnLongClickListener {
                        mListener.onLeftItemLongClick(position, message)
                        return@setOnLongClickListener true
                    }
                }
            }
        } else if (holder is DeletedMessageViewHolder) {
            if (message.senderID!!.equals(firebaseUser!!.uid, ignoreCase = true)) {
                holder.binding.apply {
                    rightDeletedMessageItemLl.visibility = View.VISIBLE
                    leftDeletedMessageItemLl.visibility = View.GONE
                    applyDeletedMessageTheme(holder)
                }
            } else {
                holder.binding.apply {
                    leftDeletedMessageItemLl.visibility = View.VISIBLE
                    rightDeletedMessageItemLl.visibility = View.GONE
                    applyDeletedMessageTheme(holder)
                    if (position != 0) {
                        checkRepeatedSender(messages[position - 1], message, holder)
                        checkMessageData(messages[position - 1], message, holder)
                    }
                }
            }
        }
    }

    private fun checkRepeatedSender(
        preMessage: ChatMessage,
        currMessage: ChatMessage,
        holder: ViewHolder
    ) {
        if (isGroupChat) {
            if (preMessage.senderID == currMessage.senderID) {
                holder.itemView.findViewById<CircleImageView>(R.id.sender_image_iv).visibility =
                    View.INVISIBLE
            } else {
                holder.itemView.findViewById<CircleImageView>(R.id.sender_image_iv).visibility =
                    View.VISIBLE
                fetchSenderImg(currMessage.senderID!!, holder)
            }
        } else
            holder.itemView.findViewById<CircleImageView>(R.id.sender_image_iv).visibility =
                View.GONE
    }

    private fun checkMessageData(
        preMessage: ChatMessage,
        currMessage: ChatMessage,
        holder: ViewHolder
    ) {
        val preMsgDays = TimeUnit.MILLISECONDS.toDays(preMessage.time!!)
        val currMsgDays = TimeUnit.MILLISECONDS.toDays(currMessage.time!!)
        val timeDifference = currMsgDays - preMsgDays
        if (timeDifference >= 2) {
            val format = SimpleDateFormat("dd/MM/yyyy")
            val ago = format.format(currMessage.time)
            holder.itemView.findViewById<TextView>(R.id.new_message_date_tv).visibility =
                View.VISIBLE
            holder.itemView.findViewById<TextView>(R.id.new_message_date_tv).text = ago
        } else
            holder.itemView.findViewById<TextView>(R.id.new_message_date_tv).visibility =
                View.GONE
    }

    private fun fetchSenderImg(senderID: String, holder: ViewHolder) {
        val userDocument = FirebaseFirestore.getInstance().collection("Users").document(senderID)
        GlobalScope.launch(Dispatchers.IO){
            val user = userDocument.get().await().toObject(User::class.java)
            withContext(Dispatchers.Main){
                Glide.with(massagesFragment.requireContext())
                    .load(user!!.profilePicture)
                    .placeholder(R.drawable.profile_pic)
                    .into(holder.itemView.findViewById<CircleImageView>(R.id.sender_image_iv))
            }
        }
    }

    private fun setRightMessageDate(holder: ViewHolder, time: Long?) {
        holder.itemView.findViewById<TextView>(R.id.left_message_date).visibility = View.GONE
        holder.itemView.findViewById<TextView>(R.id.right_message_date).visibility = View.VISIBLE
        val format = SimpleDateFormat("h:mm aa")
        val ago = format.format(time)
        if (ago.equals("0 minutes ago"))
            holder.itemView.findViewById<TextView>(R.id.right_message_date).text = "Now"
        else
            holder.itemView.findViewById<TextView>(R.id.right_message_date).text = "$ago"
    }

    private fun setLeftMessageDate(holder: ViewHolder, time: Long?) {
        holder.itemView.findViewById<TextView>(R.id.left_message_date).visibility = View.VISIBLE
        holder.itemView.findViewById<TextView>(R.id.right_message_date).visibility = View.GONE
        val format = SimpleDateFormat("h:mm aa")
        val ago = format.format(time)
        if (ago.equals("0 minutes ago"))
            holder.itemView.findViewById<TextView>(R.id.left_message_date).text = "Now"
        else
            holder.itemView.findViewById<TextView>(R.id.left_message_date).text = "$ago"
    }


    private fun downloadDoc(messageUrl: String?, fileName: String?) {
        val request = DownloadManager.Request(Uri.parse(messageUrl))
        request.apply {
            setTitle(fileName)
            setMimeType("application/pdf")
            allowScanningByMediaScanner()
            setAllowedOverMetered(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
        }
        val dm = massagesFragment.requireActivity()
            .getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }

    private fun getFileName(messageUrl: String?): String {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(messageUrl!!)

        return storageRef.name

    }

    override fun getItemViewType(position: Int): Int {
        return messages[position].messageType
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    fun updateList(messages: MutableList<ChatMessage>) {
        this.messages = messages
    }

    fun getItem(position: Int): ChatMessage {
        return messages[position]
    }

    private fun setRightReact(holder: ViewHolder, react: String) {
        when (react) {
            Constants.LIKE_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.right_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.right_react_iv)
                    .setBackgroundResource(R.drawable.like_emoji)
            }
            Constants.Laugh_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.right_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.right_react_iv)
                    .setBackgroundResource(R.drawable.laugh_emoji)
            }
            Constants.SAD_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.right_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.right_react_iv)
                    .setBackgroundResource(R.drawable.sad_emoji)
            }
            Constants.ANGRY_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.right_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.right_react_iv)
                    .setBackgroundResource(R.drawable.angry_emoji)
            }
            Constants.WOW_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.right_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.right_react_iv)
                    .setBackgroundResource(R.drawable.wow_emoji)
            }
            else -> holder.itemView.findViewById<CardView>(R.id.right_react_cv).visibility =
                View.GONE

        }
    }

    private fun setLeftReact(holder: ViewHolder, react: String) {
        when (react) {
            Constants.LIKE_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.left_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.left_react_iv)
                    .setBackgroundResource(R.drawable.like_emoji)
            }
            Constants.Laugh_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.left_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.left_react_iv)
                    .setBackgroundResource(R.drawable.laugh_emoji)
            }
            Constants.SAD_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.left_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.left_react_iv)
                    .setBackgroundResource(R.drawable.sad_emoji)
            }
            Constants.ANGRY_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.left_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.left_react_iv)
                    .setBackgroundResource(R.drawable.angry_emoji)
            }
            Constants.WOW_EMOJI -> {
                holder.itemView.findViewById<CardView>(R.id.left_react_cv).visibility =
                    View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.left_react_iv)
                    .setBackgroundResource(R.drawable.wow_emoji)
            }
            else -> holder.itemView.findViewById<CardView>(R.id.left_react_cv).visibility =
                View.GONE

        }
    }

    private fun applyTextMessageTheme(holder: TextMessageViewHolder) {
        when (themeName) {
            "DOCTOR_STRANGE" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_right_msg_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_left_msg_bg
                    )
            }
            "BATMAN" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_right_message_bg
                    )


                holder.itemView.findViewById<ConstraintLayout>(R.id.left_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_left_message_bg
                    )

            }
            "SPIDER_MAN" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_right_message_bg
                    )


                holder.itemView.findViewById<ConstraintLayout>(R.id.left_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_left_message_bg
                    )

            }
            "JOTARO" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_left_message_bg
                    )
            }
            "STRANGER_THINGS" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_left_message_bg
                    )
            }
        }

    }

    private fun applyDocMessageTheme(holder: DocumentMessageViewHolder) {
        when (themeName) {
            "DOCTOR_STRANGE" -> {

                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_right_msg_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_right_reply
                    )


                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_left_msg_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_left_reply
                    )
            }
            "BATMAN" -> {

                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_right_reply
                    )


                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_left_reply
                    )
            }
            "SPIDER_MAN" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_right_reply
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_left_reply
                    )
            }
            "JOTARO" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_right_reply
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_left_reply
                    )
            }
            "STRANGER_THINGS" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_right_reply_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_doc_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_left_reply_bg
                    )
            }
        }

    }

    private fun applyDeletedMessageTheme(holder: ViewHolder) {
        when (themeName) {
            "DOCTOR_STRANGE" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_right_msg_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_left_msg_bg
                    )
            }
            "BATMAN" -> {

                holder.itemView.findViewById<ConstraintLayout>(R.id.right_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_left_message_bg
                    )
            }
            "SPIDER_MAN" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_left_message_bg
                    )
            }
            "JOTARO" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_left_message_bg
                    )
            }
            "STRANGER_THINGS" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_deleted_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_left_message_bg
                    )
            }
        }

    }

    private fun applyReplyTextMessageTheme(holder: ReplyMessageViewHolder) {
        when (themeName) {
            "DOCTOR_STRANGE" -> {

                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_right_msg_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_right_reply
                    )


                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_left_msg_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.doctor_strange_left_reply
                    )
            }
            "BATMAN" -> {

                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_right_reply
                    )


                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.batman_left_reply
                    )
            }
            "SPIDER_MAN" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_right_reply
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.spiderman_left_reply
                    )
            }
            "JOTARO" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_right_reply
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.jotaro_left_reply
                    )
            }
            "STRANGER_THINGS" -> {
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_right_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.right_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_right_reply_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_item_ll).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_left_message_bg
                    )
                holder.itemView.findViewById<ConstraintLayout>(R.id.left_replying_message_layout).background =
                    ContextCompat.getDrawable(
                        massagesFragment.requireContext(),
                        R.drawable.stragner_thing_left_reply_bg
                    )
            }
        }
    }

    fun setGroupChat(boolean: Boolean) {
        this.isGroupChat = boolean
    }

    private class TextMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = TextMessageLayoutBinding.bind(view)
    }

    private class ImageMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = ImageMessageLayoutBinding.bind(view)
    }

    private class ReplyMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = ReplyMessageLayoutBinding.bind(view)
    }

    private class VoiceMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = VoiceMessageLayoutBinding.bind(view)
    }

    private class DocumentMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = DocumentMessageLayoutBinding.bind(view)
    }

    private class VideoMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = VideoMessageLayoutBinding.bind(view)
    }

    private class DeletedMessageViewHolder(view: View) : ViewHolder(view) {
        val binding = DeletedMessageLayoutBinding.bind(view)
    }


}