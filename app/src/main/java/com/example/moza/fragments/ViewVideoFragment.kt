package com.example.moza.fragments

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.moza.R
import com.example.moza.databinding.FragmentViewVideoBinding
import com.example.moza.models.ChatMessage
import com.example.moza.viewmodels.ViewVideoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewVideoFragment : Fragment(), View.OnClickListener {
    lateinit var binding : FragmentViewVideoBinding
    val args: ViewVideoFragmentArgs by navArgs()
    var message = ChatMessage()
    lateinit var viewModel : ViewVideoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewVideoBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ViewVideoViewModel::class.java)

        args.let {
            message = args.message
            binding.storyVideoView.setVideoURI(Uri.parse(message.messageUrl))
            val mediaController = MediaController(requireContext())
            binding.rightProgressBar.visibility = View.VISIBLE
            mediaController.setAnchorView(binding.storyVideoView)
            mediaController.setMediaPlayer(binding.storyVideoView)
            binding.storyVideoView.setMediaController(mediaController)
            binding.storyVideoView.setOnPreparedListener {
                binding.rightProgressBar.visibility = View.GONE
                binding.storyVideoView.start()
            }
        }

        binding.downloadVideoIv.setOnClickListener(this)


        return binding.root
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.download_video_iv -> {
                viewModel.downloadVideo(message.messageUrl!!)
            }
        }
    }


}