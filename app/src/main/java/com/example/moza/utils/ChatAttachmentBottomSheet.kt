package com.example.moza.utils

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.common.Resource
import com.example.moza.databinding.AttachmentSelectLayoutBinding
import com.example.moza.models.ChatMessage
import com.example.moza.models.NotificationsData
import com.example.moza.models.PushNotification
import com.example.moza.viewmodels.ChatAttachmentViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.gowtham.library.utils.CompressOption
import com.gowtham.library.utils.TrimType
import com.gowtham.library.utils.TrimVideo
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ChatAttachmentBottomSheet(val chatId: String, val bundle: Bundle) :
    BottomSheetDialogFragment(),
    View.OnClickListener {

    lateinit var binding: AttachmentSelectLayoutBinding
    lateinit var firebaseUser: FirebaseUser
    lateinit var viewModel: ChatAttachmentViewModel
    private var uri = Uri.parse("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AttachmentSelectLayoutBinding.inflate(inflater, container, false)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        viewModel = ViewModelProvider(this)[ChatAttachmentViewModel::class.java]


        full_name = bundle.getString("FULL_NAME")!!
        profile_url = bundle.getString("PROFILE_URL")!!
        profile_url = bundle.getString("TOKEN")!!
        notificationID = bundle.getInt("NOTIFICATIONS_ID")

        binding.sendPhotoCv.setOnClickListener(this)
        binding.sendDocumentCv.setOnClickListener(this)
        binding.sendVideoLayout.setOnClickListener(this)

        return binding.root
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.send_photo_cv -> {
                launchGallery()
            }
            R.id.send_document_cv -> {
                val docIntent = Intent()
                docIntent.action = Intent.ACTION_GET_CONTENT
                docIntent.type = "application/pdf"
                pickDocument.launch(docIntent)
            }
            R.id.send_video_layout -> {
                val i = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                pickVideo.launch(i)
            }
        }
    }


    private fun launchGallery() {
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setCropShape(CropImageView.CropShape.RECTANGLE)
                setOutputCompressQuality(30)
            })
    }

    private fun sendUriMessage(uriContent: Uri?) {
        val message = ChatMessage(
            "",
            firebaseUser.uid,
            "",
            System.currentTimeMillis()
        )
        viewModel.safeSendUriMessage(chatId, uriContent!!, message, firebaseUser!!.uid)
        viewModel.sendUriMessageResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    binding.progressbar.visibility = View.GONE
                    Log.e(
                        "error while sending the message: ",
                        response.message!!
                    )
                }
                is Resource.Loading -> {
                    binding.progressbar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressbar.visibility = View.GONE
                    Log.d("sending the message: ", response.data!!)
                    val notification = NotificationsData(
                        full_name,
                        profile_url,
                        "${full_name} sent photo",
                        notificationID,
                        Constants.NOTIFICATION_MESSAGE_TYPE
                    )
                    println("notification before send:$notification")
                    val pushNotification = PushNotification(notification, token)
                    //viewModel.sendNotification(pushNotification)
                    viewModel.sendNotification(pushNotification)
                }
            }
        }
    }

    private fun sendDocMessage(uri: Uri?) {
        val message = ChatMessage(
            "",
            firebaseUser.uid,
            "",
            System.currentTimeMillis()
        )
        var fileName = getFileName(uri)

        viewModel.safeSendDocument(chatId, uri!!, message, firebaseUser!!.uid, fileName)
        viewModel.sendDocMessageResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    binding.progressbar.visibility = View.GONE
                    Log.e(
                        "error while sending the message: ",
                        response.message!!
                    )
                }
                is Resource.Loading -> {
                    binding.progressbar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressbar.visibility = View.GONE
                    Log.d("sending the message: ", response.data!!)
                    val notification = NotificationsData(
                        full_name,
                        profile_url,
                        "${full_name} sent document",
                        notificationID,
                        Constants.NOTIFICATION_MESSAGE_TYPE
                    )
                    println("notification before send:$notification")
                    val pushNotification = PushNotification(notification, token)
                    //viewModel.sendNotification(pushNotification)
                    viewModel.sendNotification(pushNotification)
                }
            }
        }
    }

    private fun getFileName(uri: Uri?): String {
        val returnCursor: Cursor =
            requireActivity().contentResolver.query(uri!!, null, null, null, null)!!
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // use the returned uri
            val uriContent = result.uriContent
            sendUriMessage(uriContent)

        } else {
            // an error occurred
            val exception = result.error
            Log.e("Error while cropping image: ", exception!!.message.toString())
        }
    }

    private val pickDocument =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data!!.data
                sendDocMessage(uri)
            }
        }

    private val pickVideo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data!!.data
                this.uri = uri
                if (uri.toString().contains("video")) {
                    TrimVideo.activity(uri.toString())
                        .setCompressOption(object : CompressOption() {})
                        .setTrimType(TrimType.MIN_MAX_DURATION)
                        .setHideSeekBar(true)
                        .setMinToMax(1, 660)
                        .start(this, trimVideo)
                }
            }
        }

    private val trimVideo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == AppCompatActivity.RESULT_OK &&
                result.data != null
            ) {
                val uri = Uri.parse(TrimVideo.getTrimmedVideoPath(result.data))
                Log.d("selected video ", "Trimmed path:: $uri")
                //send video uri as a message
                val message = ChatMessage(
                    "",
                    firebaseUser.uid,
                    "",
                    System.currentTimeMillis()
                )
                var fileName = "${System.currentTimeMillis()}.mp4"
                viewModel.safeSendVideo(chatId, this.uri!!, message, firebaseUser!!.uid, fileName)
                viewModel.sendVideoMessageResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            binding.progressbar.visibility = View.GONE
                            Log.e(
                                "error while sending the message: ",
                                response.message!!
                            )
                        }
                        is Resource.Loading -> {
                            binding.progressbar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressbar.visibility = View.GONE
                            Log.d("sending the message: ", response.data!!)
                            val notification = NotificationsData(
                                full_name,
                                profile_url,
                                "${full_name} sent a video.",
                                notificationID,
                                Constants.NOTIFICATION_MESSAGE_TYPE
                            )
                            println("notification before send:$notification")
                            val pushNotification = PushNotification(notification, token)
                            viewModel.sendNotification(pushNotification)
                        }
                    }

                }

            } else {
                Log.e("selected story", "videoTrimResultLauncher data is null")
            }

        }


    companion object {
        var profile_url = ""
        var full_name = ""
        var token = ""
        var notificationID = 0
    }

}