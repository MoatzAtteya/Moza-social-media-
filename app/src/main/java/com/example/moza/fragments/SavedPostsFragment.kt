package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.adapters.HomePostsAdapter
import com.example.moza.common.Constants
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentSavedPostsBinding
import com.example.moza.models.Notification
import com.example.moza.models.PostImage
import com.example.moza.models.User
import com.example.moza.utils.SendMessageToBottomSheet
import com.example.moza.viewmodels.SavedPostsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SavedPostsFragment : Fragment() {

    private lateinit var viewModel: SavedPostsViewModel
    private lateinit var binding: FragmentSavedPostsBinding
    private val args: SavedPostsFragmentArgs by navArgs()
    private lateinit var adapter: HomePostsAdapter
    private var user = User()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSavedPostsBinding.inflate(layoutInflater, container, false)
        viewModel = ViewModelProvider(this)[SavedPostsViewModel::class.java]

        args.apply {
            this@SavedPostsFragment.user = args.user
        }

        viewModel.getUsersPosts(args.user.savedPosts)
        viewModel.getPostsResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    Log.e("Getting user posts: ", response.message!!)
                    Toast.makeText(
                        requireContext(),
                        "something wrong happened.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    if (response.data!!.size > 0)
                        setUpRecyclerView(response.data)
                    else{
                        binding.savedPostsRv.visibility = View.GONE
                        binding.noPostsIv.visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.messageBackIv.setOnClickListener{
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    private fun setUpRecyclerView(data: MutableList<PostImage>?) {
        binding.savedPostsRv.visibility = View.VISIBLE
        binding.noPostsIv.visibility = View.GONE
        binding.savedPostsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.savedPostsRv.setHasFixedSize(true)
        adapter = HomePostsAdapter(this)
        adapter.updateList(data!!)
        adapter.setSavedPost(true)
        binding.savedPostsRv.adapter = adapter

        adapter.setOnItemClickListener(object :
            HomePostsAdapter.OnItemClickListener {
            override fun onLikeClick(
                position: Int,
                post: PostImage
            ) {
                if (post.likes.contains(user.id)) {
                    post.likes.remove(user.id)
                    viewModel.safeDeleteNotification(
                        post.uid,
                        user.id,
                        post.imageUrl!!
                    )
                } else {
                    post.likes.add(user.id)
                    val notification = Notification(
                        "",
                        "",
                        post.imageUrl!!,
                        user.id,
                        "like",
                        System.currentTimeMillis(),
                        post.id!!

                    )
                    if (post.uid != user.id)
                        viewModel.safeUpdateNotifications2(post.uid, notification)
                }

                viewModel.updatePostLikesList(post.uid, post.id!!, post.likes)
            }

            override fun onShareClick(position: Int, postUrl: String) {
                val bottomSheetDialog =
                    SendMessageToBottomSheet(postUrl, Constants.TEXT_MESSAGE, false)
                bottomSheetDialog.show(
                    requireActivity().supportFragmentManager,
                    "Send Message To: "
                )
            }

            override fun onViewLikesClick(position: Int, post: PostImage) {
                TODO("Not yet implemented")
            }

            override fun onCommentClick(
                position: Int,
                id: String,
                uId: String,
                comment: String,
                commentEt: EditText,
                imageUrl: String?,
                postID: String
            ) {
                if (comment.isEmpty() || comment == "") {
                    Toast.makeText(
                        requireContext(),
                        "Can not send an empty comment.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                viewModel.updatePostComments(uId, comment)
                viewModel.sendCommentResponse.observe(
                    viewLifecycleOwner
                ) { response ->
                    if (response is Resource.Success) {
                        if (uId != user.id)
                            viewModel.safeUpdateNotifications(
                                uId,
                                imageUrl!!,
                                postID
                            )
                        commentEt.setText("")
                    }
                }
            }

            override fun onViewAllCommentsClick(
                position: Int,
                postID: String,
                postUID: String,
                imageUrl: String?
            ) {
                val action =
                    SavedPostsFragmentDirections.actionSavedPostsFragmentToCommentFragment(
                        postID,
                        postUID,
                        imageUrl
                    )
                findNavController().navigate(action)
            }

            override fun onUsernameClick(position: Int, uid: String) {
                val action =
                    SavedPostsFragmentDirections.actionSavedPostsFragmentToNavigationMyProfile(
                        uid
                    )
                findNavController().navigate(action)
            }

            override fun onSaveClick(position: Int, postId: String) {}

            override fun onHideClick(position: Int, post: PostImage) {}

            override fun onUnSaveClick(position: Int, postID: String) {
                viewModel.updateSavedPostsList(postID)
                viewModel.updateSavedPostsResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> Toast.makeText(
                            requireContext(),
                            "Something went wrong!",
                            Toast.LENGTH_SHORT
                        ).show()
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT)
                                .show()
                            adapter.notifyItemChanged(position)
                        }
                    }
                }
            }
        })

    }

}