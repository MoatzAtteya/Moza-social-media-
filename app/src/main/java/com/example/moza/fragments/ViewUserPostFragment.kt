package com.example.moza.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentViewUserPostBinding
import com.example.moza.models.Comment
import com.example.moza.models.Notification
import com.example.moza.models.PostImage
import com.example.moza.utils.BubbleInterpolate
import com.example.moza.viewmodels.ViewUserPostViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class ViewUserPostFragment : Fragment() {

    private lateinit var viewModel: ViewUserPostViewModel
    private lateinit var binding: FragmentViewUserPostBinding
    private val args: ViewUserPostFragmentArgs by navArgs()
    private var userID = ""
    private var postID = ""
    private var sameUser = false
    lateinit var firebaseUser: FirebaseUser
    private lateinit var profileImg: Bitmap
    var commentList = mutableListOf<Comment>()
    var postImage = PostImage()
    var isPostLiked = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewUserPostBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ViewUserPostViewModel::class.java)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        args.apply {
            this@ViewUserPostFragment.userID = this.userID
            this@ViewUserPostFragment.postID = this.postID
            viewModel.safeGetPost(postID)
            sameUser = userID == firebaseUser.uid
        }

        val pref = requireActivity().getSharedPreferences(
            Constants.PREF_NAME,
            AppCompatActivity.MODE_PRIVATE
        )
        val directory = pref.getString(Constants.PREF_DIRECTORY, "")
        profileImg = loadProfileImage(directory!!)!!


        viewModel.userPostResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    postImage = response.data!!
                    updateUI(postImage)
                    viewModel.safeGetComments(userID , postID)
                }
            }
        }
        viewModel.getPostComments.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {}
                is Resource.Success -> {
                    commentList = response.data!!
                    if (!commentList.isNullOrEmpty()){
                        binding.postViewAllComment.apply {
                            visibility = View.VISIBLE
                            setOnClickListener {
                                val action = ViewUserPostFragmentDirections.actionViewUserPostFragmentToCommentFragment(
                                    postID,
                                    userID,
                                    postImage.imageUrl
                                )
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
            }
        }

        if (userID != firebaseUser.uid){
            binding.viewPostOption.visibility = View.GONE
        }

        binding.viewPostOption.setOnClickListener {
            val popup = PopupMenu(
                requireContext(),
                binding.viewPostOption
            )
            popup.inflate(R.menu.view_post_option)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete_post_option -> {
                        viewModel.deletePost(postID)
                        viewModel.deleteResponse.observe(viewLifecycleOwner){response->
                            when(response){
                                is Resource.Error -> Toast.makeText(requireContext(),response.message,Toast.LENGTH_SHORT).show()
                                is Resource.Loading -> TODO()
                                is Resource.Success -> {
                                    Toast.makeText(requireContext(),"Post deleted.",Toast.LENGTH_SHORT).show()
                                    requireActivity().onBackPressed()
                                }
                            }
                        }
                    }
                }
                false
            }
            popup.show()
        }

        var doubleClickLastTime = 0L
        binding.postsPostImageIv.setOnClickListener {
            if (System.currentTimeMillis() - doubleClickLastTime < 300) {
                if (!isPostLiked){
                    doubleClickLastTime = 0
                    val bubbleAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.bubble_anim)
                    val bubbleInterpolator = BubbleInterpolate(0.2 , 20.0)
                    bubbleAnim.interpolator = bubbleInterpolator

                    binding.postImageLikeIv.visibility = View.VISIBLE
                    binding.postImageLikeIv.startAnimation(bubbleAnim)

                    GlobalScope.launch(Dispatchers.Main){
                        delay(1500)
                        binding.postImageLikeIv.visibility = View.GONE
                    }
                    likeBtnClickListener(postImage)
                    isPostLiked = true
                }else{
                    clickedLickBtnClickListener(postImage)
                    isPostLiked = false
                }

            } else {
                doubleClickLastTime = System.currentTimeMillis()
            }
        }

        binding.chatSettingBackIv.setOnClickListener {
            requireActivity().onBackPressed()
        }


        return binding.root
    }

    private fun updateUI(post: PostImage?) {
        binding.apply {
            postsUsernameTv.text = post!!.userName
            val ago = DateUtils.getRelativeTimeSpanString(
                post.timeStamp!!.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            postsTimeTv.text = ago.toString()
            when (val likesCount = post.likes.size) {
                0 -> {
                    postsLikesNumberTv.visibility = View.GONE
                }
                1 -> {
                    postsLikesNumberTv.visibility = View.VISIBLE
                    postsLikesNumberTv.text = "$likesCount like"
                }
                else -> {
                    postsLikesNumberTv.visibility = View.VISIBLE
                    postsLikesNumberTv.text = "$likesCount likes"
                }
            }
            val description = post.description
            if (description.isNullOrEmpty()) {
                postsUsernameCommentTv.visibility = View.GONE
                postsDescriptionTv.visibility = View.GONE
            } else {
                postsUsernameCommentTv.text = post.userName
                postsDescriptionTv.text = post.description
            }
            if (post.likes.contains(firebaseUser.uid)) {
                postsLikeIb.visibility = View.GONE
                postsLikeIbClicked.visibility = View.VISIBLE
            } else {
                postsLikeIb.visibility = View.VISIBLE
                postsLikeIbClicked.visibility = View.GONE
            }



            Glide.with(requireContext())
                .load(profileImg)
                .placeholder(R.drawable.profile_pic)
                .timeout(6500)
                .into(postsProfileImage)

            Glide.with(requireContext())
                .load(post.imageUrl)
                .timeout(6500)
                .fitCenter()
                .into(postsPostImageIv)

            postsLikeIb.setOnClickListener {
                likeBtnClickListener(post)
            }
            postsLikeIbClicked.setOnClickListener {
                clickedLickBtnClickListener(post)
            }
            postsCommentIb.setOnClickListener {
                if (postCommentLayout.visibility == View.GONE)
                    slideDown(postCommentLayout)
                else
                    slideUp(postCommentLayout)
            }
            postsCommentApplyIb.setOnClickListener {
                sendCommentClickListener(post)
            }
        }
    }

    private fun sendCommentClickListener(post: PostImage) {
        if (!binding.postCommentEt.text.isNullOrEmpty()) {
            val comment = binding.postCommentEt.text.toString()
            viewModel.sendComment(post, comment)
            if (!sameUser) {
                viewModel.sendCommentResponse.observe(viewLifecycleOwner) { response ->
                    if (response is Resource.Success) {
                        viewModel.safeUpdateNotifications(post.uid, post.imageUrl!!)
                    }
                }
            }
            binding.postCommentEt.setText("")
        }
    }

    private fun clickedLickBtnClickListener(post: PostImage) {
        post.likes.remove(firebaseUser.uid)
        viewModel.updatePostLike(post)
        if (!sameUser) {
            viewModel.safeDeleteNotification(post.uid, firebaseUser.uid, post.imageUrl!!)
        }
    }

    private fun likeBtnClickListener(post: PostImage) {
        post.likes.add(firebaseUser.uid)
        viewModel.updatePostLike(post)

        if (!sameUser) {
            var notification = Notification(
                "",
                "",
                post.imageUrl!!,
                firebaseUser.uid,
                "like",
                System.currentTimeMillis()
            )
            viewModel.safeUpdateNotifications(post.uid, notification)
        }
    }


    private fun loadProfileImage(directory: String): Bitmap? {
        return try {
            val file = File(directory, "profile.png")
            BitmapFactory.decodeStream(FileInputStream(file))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun slideUp(view: View) {
        view.visibility = View.GONE
        val animate = TranslateAnimation(
            0F,  // fromXDelta
            0F,  // toXDelta
            0F,  // fromYDelta
            view.height.toFloat()
        ) // toYDelta
        animate.duration = 300
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    private fun slideDown(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0F,  // fromXDelta
            0F,  // toXDelta
            view.height.toFloat(),  // fromYDelta
            0F
        ) // toYDelta
        animate.duration = 400
        animate.fillAfter = true
        view.startAnimation(animate)
    }
}