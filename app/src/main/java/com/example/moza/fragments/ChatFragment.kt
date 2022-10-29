package com.example.moza.fragments

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capybaralabs.swipetoreply.SwipeController
import com.devlomi.record_view.OnRecordListener
import com.example.moza.R
import com.example.moza.adapters.ChatAdapter
import com.example.moza.common.Constants
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentChatBinding
import com.example.moza.models.ChatMessage
import com.example.moza.models.NotificationRetrofitObj
import com.example.moza.models.NotificationsData
import com.example.moza.models.PushNotification
import com.example.moza.utils.ChatAttachmentBottomSheet
import com.example.moza.utils.LeftMessageBottomSheet
import com.example.moza.utils.RightMessageBottomSheet
import com.example.moza.utils.VoiceAudio
import com.example.moza.viewmodels.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class ChatFragment : Fragment(), View.OnClickListener, ChatAdapter.OnItemClickListener {

    private lateinit var binding: FragmentChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var repository: FireBaseRepository
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var adapter: ChatAdapter
    private lateinit var messages: MutableList<ChatMessage>


    val args: ChatFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        repository = FireBaseRepository()
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        args.let {
            oppositeID = it.oppositeID!!
            chatID = it.chatID!!
            unReadSender = it.unReadSenderID!!

        }
        sharedPref =
            requireActivity().getSharedPreferences("THEME_SHAREDPREF:$chatID", Context.MODE_PRIVATE)
        themeName = sharedPref.getString("THEME_NAME", "")!!
        applyTheme()




        adapter = ChatAdapter(this, themeName)
        messages = mutableListOf()
        val llm = LinearLayoutManager(requireContext())
        llm.stackFromEnd = true
        binding.chatsRv.layoutManager = llm
        binding.chatsRv.setHasFixedSize(true)

        val pref = requireActivity().getSharedPreferences(
            Constants.PREF_NAME,
            AppCompatActivity.MODE_PRIVATE
        )
        profileURl = pref.getString(Constants.PREF_URL, "").toString()
        fullname = pref.getString("USER_NAME", "").toString()
        notificationsID = pref.getInt("NOTIFICATION_ID", 10)
        binding.messagesScrollDownIv.visibility = View.GONE

        updateUI()
        checkDeleteRequest()
        markChatAsRead()
        updateMessagesUI()

        binding.messageEt.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                binding.recordBtn2.visibility = View.VISIBLE
                binding.messagesSendTv.visibility = View.GONE
            } else {
                binding.recordBtn2.visibility = View.GONE
                binding.messagesSendTv.visibility = View.VISIBLE
            }

        }

        binding.selectMediaIv.setOnClickListener(this)
        binding.messagesSendTv.setOnClickListener(this)
        adapter.setOnItemClickListener(this)
        binding.messagesUsernameTv.setOnClickListener(this)
        binding.chatsRv.setOnClickListener(this)
        binding.messagesProfileImgIv.setOnClickListener(this)
        binding.messagesVideoCallIv.setOnClickListener(this)
        binding.messageBackIv.setOnClickListener(this)
        binding.closeReplyingLayoutIv.setOnClickListener(this)
        binding.chatOption.setOnClickListener(this)

        recordFile = File(requireActivity().getExternalFilesDir(null), "records")
        recordFile.mkdir()


        var firstTimeOpened = true
        binding.chatsRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    binding.messagesScrollDownIv.visibility = View.GONE
                } else if (dy < -30) {
                    if (!firstTimeOpened) {
                        binding.messagesScrollDownIv.visibility = View.VISIBLE
                        binding.messagesScrollDownIv.setOnClickListener {
                            val position = (binding.chatsRv.adapter as ChatAdapter).itemCount - 1
                            binding.chatsRv.smoothScrollToPosition(position)
                            binding.messagesScrollDownIv.visibility = View.GONE

                        }
                    }
                    firstTimeOpened = false
                }
            }
        })

        //Attaching swipe for reply feature to the recyclerview
        val controller = SwipeController(context) {
            //show device keyboard after swipe
            val inputMethodManager: InputMethodManager? =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            inputMethodManager!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

            //set vibrator when swiping
            val vibration = activity?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibration.vibrate(200)

            //enable animation for replying layout content.
            binding.replyingLayout.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.scale_up_layout)

            isReplying = true
            val message = adapter.getItem(it)
            replyingText = message.message!!
            binding.replyingLayout.visibility = View.VISIBLE

            //if the user is replying to himself.
            if (message.senderID == firebaseUser.uid) {
                replayTo = "You"
                binding.replyingToTv.text = "Replying to yourself"
            } else {
                replayTo = oppositeName
                binding.replyingToTv.text = "Replying to $oppositeName"
            }

            when (message.messageType) {
                0 -> {
                    binding.replyingMessageTv.text = message.message
                    binding.replyingUryIv.visibility = View.GONE
                    replyType = 0
                }
                1 -> {
                    replyType = 1
                    binding.replyingUryIv.visibility = View.VISIBLE
                    binding.replyingMessageTv.text = "Photo"
                    replyingText = message.messageUrl!!
                    Glide.with(requireContext())
                        .load(message.messageUrl)
                        .into(binding.replyingUryIv)
                }
                2 -> {
                    replyType = 0
                    binding.replyingMessageTv.text = message.message
                    binding.replyingUryIv.visibility = View.GONE
                }
                3 -> {
                    binding.replyingMessageTv.text = "Voice Message"
                    replyingText = "Voice Message"
                    binding.replyingUryIv.visibility = View.GONE
                    replyType = 0
                }
                4 -> {
                    binding.replyingMessageTv.text = "Document"
                    replyingText = "Document"
                    binding.replyingUryIv.visibility = View.GONE
                    replyType = 0
                }
                5 -> {
                    replyType = 1
                    binding.replyingUryIv.visibility = View.VISIBLE
                    replyingText = "Video"
                    binding.replyingMessageTv.text = "Video"
                    replyingText = message.messageUrl!!
                    Glide.with(requireContext())
                        .load(message.messageUrl)
                        .into(binding.replyingUryIv)
                }
                6 -> {
                    binding.replyingMessageTv.text = "Deleted Message"
                    replyingText = "Deleted Message"
                    replyType = 0
                    binding.replyingUryIv.visibility = View.GONE

                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(controller)
        itemTouchHelper.attachToRecyclerView(binding.chatsRv)


        binding.recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                //Start Recording..
                checkRecordingPermission()
                binding.messageEt.visibility = View.GONE
                binding.selectMediaIv.visibility = View.GONE
                binding.recordView.visibility = View.VISIBLE
            }

            override fun onCancel() {
                //On Swipe To Cancel
                stopRecording()
                binding.messageEt.visibility = View.VISIBLE
                binding.selectMediaIv.visibility = View.VISIBLE
                binding.recordView.visibility = View.GONE

            }

            override fun onFinish(recordTime: Long, limitReached: Boolean) {
                binding.messageEt.visibility = View.VISIBLE
                binding.selectMediaIv.visibility = View.VISIBLE
                binding.recordView.visibility = View.GONE
                Log.d("RecordView", "onFinish")
                stopRecording()
                sendVoiceMessage()
            }

            override fun onLessThanSecond() {
                binding.messageEt.visibility = View.VISIBLE
                binding.selectMediaIv.visibility = View.VISIBLE
                binding.recordView.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Voice message can't be less than 1 second.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("RecordView", "onLessThanSecond")
                stopRecording()
            }
        })

        binding.recordBtn2.isListenForRecord = false
        binding.recordBtn2.setRecordView(binding.recordView)
        binding.recordBtn2.setOnRecordClickListener {
            binding.recordBtn2.isListenForRecord = true
        }

        return binding.root
    }

    private fun markChatAsRead() {
        if (unReadSender != firebaseUser.uid)
            if (isChatOpen)
                viewModel.safeMarkChatASRead(chatID)
    }


    private fun checkDeleteRequest() {
        if (oppositeID.isNotEmpty()) {
            viewModel.safeGetChat(chatID)
            viewModel.getChatResponse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Error -> Log.e("Requesting chat delete: ", response.message!!)
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        if (!response.data!!.deleteRequest.isNullOrEmpty()) {
                            if (response.data.deleteRequest != firebaseUser.uid) {
                                showDeleteDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.apply {
            setTitle("Delete chat request")
            setMessage("$username is asking you to delete this chat, will you?")
            setPositiveButton("okay") { _, _ ->
                findNavController().navigate(R.id.action_chatFragment_to_massagesFragment)
                viewModel.safeDeleteChat(chatID)
                viewModel.deleteChatResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                "Error while deleting your chat",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("Deleting chat request: ", response.data!!)
                        }
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            Toast.makeText(
                                requireContext(),
                                "Chat deleted successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            setNegativeButton("cancel") { _, _ ->

            }
            show()
        }


    }

    private fun updateMessagesUI() {
        viewModel.safeGetChatMessages(chatID)
        viewModel.getChatMessagesResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load your chat.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("Getting user chat messages: ", response.message.toString())
                }
                is Resource.Loading -> {

                }
                is Resource.Success -> {
                    messages = response.data!!
                    adapter.updateList(messages)
                    if (oppositeID.isEmpty())
                        adapter.setGroupChat(true)
                    else
                        adapter.setGroupChat(false)
                    binding.chatsRv.adapter = adapter
                    adapter.notifyDataSetChanged()
                    markChatAsRead()


                }
            }
        }
    }


    private fun updateUI() {
        if (oppositeID.isNotEmpty()) {
            viewModel.safeGetUserData(oppositeID)
            viewModel.getUserDataResponse.observe(viewLifecycleOwner, Observer { response ->
                when (response) {
                    is Resource.Error -> {
                        Log.e("getting user data: ", response.message.toString())
                    }
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        val status = response.data!!.online
                        token = response.data.token!!
                        username = response.data.fullName
                        //profileURl = response.data.profilePicture
                        if (status!!)
                            binding.messagesUserStatusTv.text = "Online"
                        else
                            binding.messagesUserStatusTv.text = "Offline"

                        oppositeName = response.data.fullName
                        binding.messagesUsernameTv.text = oppositeName

                        Glide.with(this)
                            .load(response.data.profilePicture)
                            .into(binding.messagesProfileImgIv)
                        Glide.with(this)
                            .load(response.data.profilePicture)
                            .into(binding.messagesScrollDownIv)
                    }
                }
            })
        } else {
            viewModel.safeGetChat(chatID)
            viewModel.getChatResponse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Error -> Log.e("Getting chat data", response.message!!)
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val groupChat = response.data
                        binding.messagesUsernameTv.text = groupChat!!.groupTitle
                        Glide.with(requireContext())
                            .load(groupChat.groupProfileImg)
                            .placeholder(R.drawable.profile_pic)
                            .into(binding.messagesProfileImgIv)
                    }
                }
            }
        }

    }


    private fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("notification before send: $notification")
                val response = NotificationRetrofitObj.api.postNotification(notification)
                if (response.isSuccessful) {
                    Log.d("notification", "Notification sent")
                } else {
                    Log.e("error in sending notification", response.errorBody().toString())
                }
            } catch (e: Exception) {
                Log.e("error in sending notification", e.toString())
            }
        }

    private fun applyTheme() = CoroutineScope(Dispatchers.Main).launch {
        if (sharedPref.contains("THEME_NAME")) {
            println("shared pref exists")
            when (themeName) {
                "DOCTOR_STRANGE" -> {
                    binding.messageEt.setTextColor(Color.WHITE)
                    binding.chatLayout.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.doctor_strange_chat_bg
                    )
                    binding.messageEt.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.doctor_strange_message_bg
                    )

                    binding.replyingLayout.background = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.doctor_strange_message_bg
                    )


                    binding.messageEt.setTextColor(Color.BLACK)

                }
                "BATMAN" -> {
                    val bitmap = BitmapFactory.decodeResource(
                        requireContext().resources,
                        R.drawable.batman_chat_bg
                    )
                    Palette.from(bitmap).generate {
                        val intColor = it?.vibrantSwatch?.rgb ?: 0
                        // binding.messagesSendTv.setTextColor(intColor)
                        binding.replyingSeparatorView.setBackgroundColor(intColor)

                        binding.chatLayout.background =
                            ContextCompat.getDrawable(requireContext(), R.drawable.batman_chat_bg)
                        binding.messageEt.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.batman_message_bg
                        )
                        binding.replyingLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.batman_message_bg
                        )
                        binding.messageEt.setTextColor(Color.WHITE)

                    }
                }
                "SPIDER_MAN" -> {
                    val bitmap = BitmapFactory.decodeResource(
                        requireContext().resources,
                        R.drawable.spiderman_chat_background
                    )
                    Palette.from(bitmap).generate {
                        val intColor = it?.vibrantSwatch?.rgb ?: 0
                        // binding.messagesSendTv.setTextColor(intColor)
                        binding.replyingSeparatorView.setBackgroundColor(intColor)

                        binding.chatLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.spiderman_chat_background
                        )
                        binding.messageEt.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.spiderman_message_bg
                        )
                        binding.replyingLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.spiderman_message_bg
                        )
                        binding.messageEt.setTextColor(Color.WHITE)

                    }
                }
                "JOTARO" -> {
                    val bitmap = BitmapFactory.decodeResource(
                        requireContext().resources,
                        R.drawable.jotaroo_kujo_chat_bg
                    )
                    Palette.from(bitmap).generate {
                        val intColor = it?.vibrantSwatch?.rgb ?: 0
                        binding.replyingSeparatorView.setBackgroundColor(intColor)

                        //binding.messagesSendTv.setTextColor(intColor)
                        binding.chatLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.jotaroo_kujo_chat_bg
                        )
                        binding.messageEt.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.jotaro_message_bg
                        )
                        binding.replyingLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.jotaro_message_bg
                        )
                        binding.messageEt.setTextColor(Color.WHITE)

                    }
                }
                "STRANGER_THINGS" -> {
                    val bitmap = BitmapFactory.decodeResource(
                        requireContext().resources,
                        R.drawable.stranger_things_bg
                    )
                    Palette.from(bitmap).generate {
                        val intColor = it?.vibrantSwatch?.rgb ?: 0
                        binding.replyingSeparatorView.setBackgroundColor(intColor)

                        //binding.messagesSendTv.setTextColor(intColor)
                        binding.chatLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.stranger_things_bg
                        )
                        /*binding.messageEt.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.jotaro_message_bg
                        )
                        binding.replyingLayout.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.jotaro_message_bg
                        )*/
                        binding.messageEt.setTextColor(Color.WHITE)

                    }
                }
            }

        }
    }

    private fun checkReadStoragePermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    Log.d("Asking for permissions: ", "Permissions granted.")
                    val bundle = Bundle()
                    bundle.putString("FULL_NAME", fullname)
                    bundle.putString("PROFILE_URL", profileURl)
                    bundle.putString("TOKEN", token)
                    bundle.putInt("NOTIFICATIONS_ID", notificationsID)
                    val bottomSheetDialog = ChatAttachmentBottomSheet(chatID, bundle)
                    bottomSheetDialog.show(
                        requireActivity().supportFragmentManager,
                        "select attachment bottom sheet"
                    )
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()

                }
            }).check()
    }

    private fun checkRecordingPermission() {
        Dexter.withContext(requireContext())
            .withPermission(
                Manifest.permission.RECORD_AUDIO
            )
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    Log.d("Asking for permissions: ", "Permissions granted.")
                    startRecording()
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        requireContext(),
                        "Recording voice messages is not allowed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }
            }).check()
    }


    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.select_media_iv -> {
                checkReadStoragePermission()
            }

            R.id.messages_send_tv -> {
                if (binding.messageEt.text.isNullOrEmpty()) {
                    return
                }
                MediaPlayer.create(
                    requireContext(),
                    R.raw.send_message_sound
                ).start()

                if (!isReplying) {
                    val message = ChatMessage(
                        "",
                        firebaseUser.uid,
                        binding.messageEt.text.toString(),
                        System.currentTimeMillis(),
                    )
                    viewModel.safeSendMessage(chatID, message, firebaseUser.uid)
                    val msg = binding.messageEt.text.toString()
                    binding.messageEt.setText("")
                    viewModel.sendMessageResponse.observe(viewLifecycleOwner) { response ->
                        when (response) {
                            is Resource.Error -> {
                                Toast.makeText(
                                    requireContext(),
                                    "error while sending the message: ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is Resource.Loading -> TODO()
                            is Resource.Success -> {
                                Log.d("sending the message: ", response.data!!)
                            }
                        }
                    }
                    val notification = NotificationsData(
                        "New Message",
                        profileURl,
                        msg,
                        notificationsID,
                        Constants.NOTIFICATION_MESSAGE_TYPE
                    )
                    val pushNotification = PushNotification(notification, token)
                    sendNotification(pushNotification)

                } else {
                    val message = ChatMessage(
                        "",
                        firebaseUser.uid,
                        binding.messageEt.text.toString(),
                        System.currentTimeMillis(),
                        false,
                        "",
                        2,
                        false,
                        "",
                        "",
                        replyingText,
                        replayTo,
                        replyType
                    )
                    viewModel.safeSendMessage(chatID, message, firebaseUser.uid)
                    val msg = binding.messageEt.text.toString()
                    binding.messageEt.setText("")

                    viewModel.sendMessageResponse.observe(viewLifecycleOwner) { response ->
                        when (response) {
                            is Resource.Error -> Toast.makeText(
                                requireContext(),
                                "error while sending the message: ",
                                Toast.LENGTH_SHORT
                            ).show()

                            is Resource.Loading -> TODO()
                            is Resource.Success -> {
                                binding.replyingLayout.visibility = View.GONE
                                Log.d("sending the message: ", response.data!!)
                            }
                        }
                    }
                    val notification = NotificationsData(
                        "New Message",
                        profileURl,
                        msg,
                        notificationsID,
                        Constants.NOTIFICATION_MESSAGE_TYPE
                    )
                    val pushNotification = PushNotification(notification, token)
                    sendNotification(pushNotification)
                    isReplying = false
                }

            }
            R.id.messages_username_tv -> {
                val action =
                    ChatFragmentDirections.actionChatFragmentToNavigationMyProfile(oppositeID)
                findNavController().navigate(action)
            }
            R.id.messages_profileImg_iv -> {
                val action =
                    ChatFragmentDirections.actionChatFragmentToNavigationMyProfile(oppositeID)
                findNavController().navigate(action)
            }
            R.id.chat_option -> {
                if (oppositeID.isNotEmpty()) {
                    val action =
                        ChatFragmentDirections.actionChatFragmentToChatSettingFragment(chatID)
                    findNavController().navigate(action)
                } else {
                    val action =
                        ChatFragmentDirections.actionChatFragmentToGroupSettingFragment(chatID)
                    findNavController().navigate(action)
                }

            }
            R.id.message_back_iv -> {
                requireActivity().onBackPressed()
            }
            R.id.close_replying_layout_iv -> {
                isReplying = false
                binding.replyingLayout.visibility = View.GONE
            }

        }
    }

    private fun startRecording() {
        recordFilePath = createRecordFile()
        voiceAudio.voiceRecord(recordFilePath)
    }

    private fun stopRecording() {
        voiceAudio.stopRecordPlayer()
    }


    private fun sendVoiceMessage() {
        val fileName = "${UUID.randomUUID()}"
        val uriVoice = Uri.fromFile(File(recordFilePath))
        val message = ChatMessage(
            "",
            firebaseUser.uid,
            "",
            System.currentTimeMillis(),
            false,
            "",
            3,
        )
        println("record path is: $recordFilePath")
        viewModel.safeSendVoiceMessage(chatID, message, uriVoice, fileName)
        viewModel.sendVoiceMessageResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    binding.progressbar.visibility = View.GONE
                    Log.e("Sending voice message: ", response.message!!)
                }
                is Resource.Loading -> binding.progressbar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Voice message sent.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createRecordFile(): String {

        val file = File.createTempFile("file", ".mp3", recordFile)
        return file.absolutePath
    }

    override fun onItemClick(position: Int, id: String, message: ChatMessage) {
        val bottomSheetDialog = RightMessageBottomSheet(id, chatID, message)
        bottomSheetDialog.show(
            requireActivity().supportFragmentManager,
            "story views bottom sheet"
        )
        adapter.notifyItemChanged(position)

    }

    override fun onLeftItemLongClick(position: Int, message: ChatMessage) {
        val bottomSheetDialog = LeftMessageBottomSheet(chatID, message)
        bottomSheetDialog.show(
            requireActivity().supportFragmentManager,
            "Reacts views bottom sheet"
        )
        adapter.notifyItemChanged(position)

    }

    override fun onImageClick(position: Int, message: ChatMessage) {
        val action = ChatFragmentDirections.actionChatFragmentToImageViewerFragment(message)
        findNavController().navigate(action)
    }

    override fun onVideoClick(position: Int, message: ChatMessage) {
        val action = ChatFragmentDirections.actionChatFragmentToViewVideoFragment(message)
        findNavController().navigate(action)
    }

    override fun onStop() {
        super.onStop()
        isChatOpen = false
    }

    override fun onPause() {
        super.onPause()
        isChatOpen = false

    }

    override fun onResume() {
        super.onResume()
        isChatOpen = true
    }

    companion object {
        private var oppositeID = ""
        private var oppositeName = ""
        private var chatID = ""
        private var token = ""
        private var username = ""
        private var profileURl = ""
        private var fullname = ""
        private lateinit var sharedPref: SharedPreferences
        private lateinit var themeName: String
        private var notificationsID: Int = 0
        private var isChatOpen = true
        private var isReplying = false
        private var replyingText = ""
        private var replayTo = ""
        private var replyType = 0
        private lateinit var unReadSender: String
        private val voiceAudio = VoiceAudio()
        private var isRecording = false
        private lateinit var recordFile: File
        private var recordFilePath = ""

    }


}