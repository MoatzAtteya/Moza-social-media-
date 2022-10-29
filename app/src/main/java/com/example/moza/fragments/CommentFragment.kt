package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.R
import com.example.moza.adapters.CommentsAdapter
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentCommentBinding
import com.example.moza.models.Comment
import com.example.moza.viewmodels.CommentViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommentFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentCommentBinding
    private lateinit var viewModel: CommentViewModel
    private lateinit var postID: String
    private lateinit var postUID: String
    private lateinit var postUrl: String
    private lateinit var repository: FireBaseRepository
    private lateinit var firebaseUSer: FirebaseUser
    private var adapter = CommentsAdapter(this)
    private var commentList = ArrayList<Comment>()
    val args: CommentFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCommentBinding.inflate(inflater, container, false)
        repository = FireBaseRepository()
        firebaseUSer = FirebaseAuth.getInstance().currentUser!!
        viewModel = ViewModelProvider(this).get(CommentViewModel::class.java)

        postID = args.postID!!
        postUID = args.postUID!!
        postUrl = args.postUrl!!

        binding.postsAllCommentsApplyIb.setOnClickListener(this)
        binding.postAllCommentsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.postAllCommentsRv.setHasFixedSize(true)

        viewModel.getPostComments(postUID, postID)
        viewModel.getCommentsResponse.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    commentList = response.data!!
                    adapter.updateList(commentList)
                    binding.postAllCommentsRv.adapter = adapter
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Log.e("Error while getting post comment: ", response.message!!)
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        })

        adapter.setOnItemClickListener(object : CommentsAdapter.OnItemClickListener {
            override fun onUsernameClick(position: Int, uid: String) {
                val action =
                    CommentFragmentDirections.actionCommentFragmentToNavigationMyProfile(uid)
                findNavController().navigate(action)
            }

            override fun onDeleteClick(position: Int, commentId: String) {
                viewModel.deleteComment(postID, commentId)
                viewModel.deleteCommentResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> Toast.makeText(
                            requireContext(),
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            adapter.notifyDataSetChanged()
                            Toast.makeText(
                                requireContext(),
                                "Comment deleted.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        })

        return binding.root
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.posts_all_comments_apply_ib -> {
                val comment = binding.postCommentEt.text.toString()
                if (!comment.isNullOrEmpty()) {
                    viewModel.updatePostComments(postID, comment)
                    viewModel.sendCommentResponse.observe(viewLifecycleOwner, Observer { response ->
                        if (response is Resource.Success) {
                            if (postUID != firebaseUSer.uid){
                                viewModel.safeUpdateNotifications(
                                    postUID,
                                    firebaseUSer.uid,
                                    postUrl,
                                    postID
                                )
                            }
                            binding.postCommentEt.setText("")
                        }
                    })
                } else
                    Toast.makeText(
                        requireContext(),
                        "Can't send empty comment.",
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

}