package com.example.moza.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentAddStoryBinding
import com.example.moza.models.Story
import com.example.moza.models.User
import com.example.moza.viewmodels.AddStoryViewModel
import com.gowtham.library.utils.CompressOption
import com.gowtham.library.utils.TrimType
import com.gowtham.library.utils.TrimVideo
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddStoryFragment : Fragment(), View.OnClickListener {

    lateinit var binding: FragmentAddStoryBinding
    lateinit var viewModel: AddStoryViewModel
    private var user = User()
    private var uri = Uri.parse("")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddStoryBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[AddStoryViewModel::class.java]

        viewModel.safeGetUserData()
        viewModel.getUserDataResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting user data: ", response.message.toString())
                is Resource.Loading -> {
                }
                is Resource.Success -> user = response.data!!
            }
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/* video/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, "image/* video/*")
        resultLauncher.launch(intent)

        binding.confirmStoryNextBtn.setOnClickListener(this)


        return binding.root
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {

                uri = result.data!!.data

                if (uri.toString().contains("image")) {
                    binding.storyVideoView.visibility = View.GONE
                    binding.storyImageViewIv.visibility = View.VISIBLE

                    Glide.with(this)
                        .load(uri)
                        .into(binding.storyImageViewIv)


                } else if (uri.toString().contains("video")) {
                    TrimVideo.activity(uri.toString())
                        .setCompressOption(object : CompressOption() {})
                        .setTrimType(TrimType.MIN_MAX_DURATION)
                        .setMinToMax(5, 20)
                        .setHideSeekBar(true)
                        .start(this, startForResult)
                }
            }
        }


    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == AppCompatActivity.RESULT_OK &&
                result.data != null
            ) {
                val uri = Uri.parse(TrimVideo.getTrimmedVideoPath(result.data))
                Log.d("selected story ", "Trimmed path:: $uri")
                binding.storyVideoView.setVideoURI(uri)
                binding.storyVideoView.start()
            } else {
                Log.e("selected story", "videoTrimResultLauncher data is null")
                Toast.makeText(requireContext(), "Data is null", Toast.LENGTH_SHORT).show()
            }

        }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.confirm_story_next_btn -> {
                val story = Story(
                    "",
                    user.fullName,
                    "",
                    user.id,
                    user.profilePicture,
                    null
                )
                viewModel.safeUploadUserStory(uri, story)
                viewModel.uploadUserStory.observe(this) { response ->
                    when (response) {
                        is Resource.Error -> {
                            binding.storyProgressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Error while uploading story",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            Log.e("uploading user story: ", response.message.toString())
                        }
                        is Resource.Loading -> {
                            binding.storyProgressBar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.storyProgressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), response.data, Toast.LENGTH_SHORT)
                                .show()
                            requireActivity().onBackPressed()
                        }
                    }
                }
            }
        }
    }

}