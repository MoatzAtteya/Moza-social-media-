package com.example.moza.fragments

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.activities.HomeActivity
import com.example.moza.adapters.HomePostsAdapter
import com.example.moza.adapters.StoriesAdapter
import com.example.moza.common.Constants
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentHomeBinding
import com.example.moza.models.Notification
import com.example.moza.models.PostImage
import com.example.moza.models.User
import com.example.moza.utils.SendMessageToBottomSheet
import com.example.moza.viewmodels.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HomePostsAdapter
    lateinit var binding: FragmentHomeBinding
    lateinit var user: FirebaseUser
    lateinit var oppositeUser : User
    var followingList: ArrayList<String> = ArrayList()
    var postsList = mutableListOf<PostImage>()
    val storyListIDS = arrayListOf<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val auth = FirebaseAuth.getInstance()
        user = auth.currentUser!!
        FirebaseFirestore.getInstance().collection("Users").document(user.uid)
            .update("id", user.uid)

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        viewModel.safeGetAndStoreUserToken(user.uid)

        binding.storiesRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.storiesRv.setHasFixedSize(true)
        adapter = HomePostsAdapter(this)


        binding.storyUsernameTv.text = "Add story"
        viewModel.getUserData()
        viewModel.getUserDataResponse.observe(viewLifecycleOwner, Observer {response->
            when(response){
                is Resource.Error -> TODO()
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    oppositeUser = response.data!!
                    Glide.with(requireContext())
                        .load(oppositeUser.profilePicture)
                        .placeholder(R.drawable.profile_pic)
                        .into(binding.storyIv)
                }
            }
        })

        binding.storyIv.setOnClickListener {
            checkStoragePermissions()
        }

        //make user status -> online
        viewModel.safeUpdateUserStatus(user.uid)

        //get the following list first then we get posts and stories for those users.
        viewModel.getUserFollowList(user.uid)

        viewModel.getUserFollowListResponse.observe(viewLifecycleOwner, Observer {
           // followingList.clear()
           // storyListIDS.clear()
            followingList = it
            followingList.add("")
            storyListIDS.addAll(it)
            storyListIDS.add(user.uid)
            println("story list ids: $storyListIDS")
            viewModel.safeGetUsersStories(storyListIDS)
            //stories.add(Story())


            viewModel.safeGetUsersPosts(followingList)
        })


        viewModel.getUsersStoriesResponse.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Error -> {
                    Log.e("Getting users stories: ", response.message.toString())
                }
                is Resource.Loading -> {

                }
                is Resource.Success -> {

                    //response.data!!.add(Story())
                    val storyAdapter = StoriesAdapter(this)
                    storyAdapter.setStoryList(response.data!!)
                    binding.storiesRv.visibility = View.VISIBLE
                    binding.storiesRv.adapter = storyAdapter
                }
            }
        })

        viewModel.getPostsResponse2.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Error -> TODO()
                is Resource.Loading -> binding.progressbar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressbar.visibility = View.GONE
                    postsList = response.data!!
                    adapter.updateList(postsList!!)
                    setRecyclerView(adapter)
                    adapter.notifyDataSetChanged()
                    adapter.setOnItemClickListener(object :
                        HomePostsAdapter.OnItemClickListener {
                        override fun onLikeClick(
                            position: Int,
                            post: PostImage
                        ) {
                            if (post.likes.contains(user.uid)) {
                                post.likes.remove(user.uid)
                                viewModel.safeDeleteNotification(
                                    post.uid,
                                    user.uid,
                                    post.imageUrl!!
                                )
                            } else {
                                post.likes.add(user.uid)
                                val notification = Notification(
                                    "",
                                    "",
                                    post.imageUrl!!,
                                    user.uid,
                                    "like",
                                    System.currentTimeMillis(),
                                    post.id!!

                                )
                                if (post.uid != user.uid)
                                    viewModel.safeUpdateNotifications(post.uid, notification)
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
                            val action = HomeFragmentDirections.actionNavigationHomeToPostLikesFragment(post)
                            findNavController().navigate(action)
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
                            viewModel.sendComment(postID, comment)
                            viewModel.sendCommentResponse.observe(
                                viewLifecycleOwner
                            ) { response ->
                                if (response is Resource.Success) {
                                    if (uId != user.uid)
                                        viewModel.safeUpdateNotifications(
                                            uId,
                                            imageUrl!!,
                                            postID
                                        )
                                    commentEt.setText("")
                                }
                               /* val notify = NotificationsData(
                                    oppositeUser!!.fullName,
                                    imageUrl!!,
                                    "${oppositeUser.fullName} commented on your photo",
                                    Random().nextInt(),
                                    Constants.NOTIFICATION_COMMENT_TYPE
                                )
                                val pushNotification = PushNotification(notify, oppositeUser.token!!)*/

                            }
                        }

                        override fun onViewAllCommentsClick(
                            position: Int,
                            postID: String,
                            postUID: String,
                            imageUrl: String?
                        ) {
                            val action =
                                HomeFragmentDirections.actionNavigationHomeToCommentFragment(
                                    postID,
                                    postUID,
                                    imageUrl
                                )
                            findNavController().navigate(action)
                        }

                        override fun onUsernameClick(position: Int, uid: String) {
                            val action =
                                HomeFragmentDirections.actionNavigationHomeToNavigationMyProfile(
                                    uid
                                )
                            findNavController().navigate(action)
                        }

                        override fun onSaveClick(position: Int, postId : String) {
                            viewModel.updateUserSavedPosts(postId)
                            viewModel.savePostResponse.observe(viewLifecycleOwner){response->
                                when(response){
                                    is Resource.Error -> Log.e("Saving post: " , response.message!!)
                                    is Resource.Loading -> {}
                                    is Resource.Success -> Toast.makeText(requireContext(), "Post Saved.", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }

                        override fun onHideClick(position: Int, post: PostImage) {
                            post.hideIds.add(user.uid)
                            viewModel.updatePostHideIds(post.id!!, post.hideIds.distinct())
                            viewModel.hidePostResponse.observe(viewLifecycleOwner){response->
                                when(response){
                                    is Resource.Error -> Log.e("Updating post ids: " , response.message!!)
                                    is Resource.Loading -> {}
                                    is Resource.Success -> {
                                        Toast.makeText(requireContext(), "Post Hided.", Toast.LENGTH_SHORT)
                                            .show()
                                        adapter.removeItem(position)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }

                        override fun onUnSaveClick(position: Int, postID: String) {
                            TODO("Not yet implemented")
                        }
                    })
                }
            }

        })


        binding.directMessagesIv.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_massagesFragment)
        }

        binding.nestedSv.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->

            if (scrollY > oldScrollY) {
                if (activity is HomeActivity) {
                    val activity = activity as HomeActivity
                    activity.hideBottomNavigationBar()
                }
            }
            if (scrollY < oldScrollY) {
                if (activity is HomeActivity) {
                    val activity = activity as HomeActivity
                    activity.showBottomNavigationBar()
                }
            }
        }




        return binding.root
    }

    private fun checkStoragePermissions() {
        Dexter.withContext(requireActivity())
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()) {
                        findNavController().navigate(R.id.action_navigation_home_to_addStoryFragment)

                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Please allow the permissions.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }

            }).check()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setActionBar(((binding.homePageActionbar)))
    }

    private fun setRecyclerView(adapter: HomePostsAdapter) {
        binding.allPostsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.allPostsRv.setHasFixedSize(true)
        binding.allPostsRv.adapter = adapter

    }
}
