package com.example.moza.fragments

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.moza.R
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentProfileBinding
import com.example.moza.models.ChatMessage
import com.example.moza.models.ChatUser
import com.example.moza.models.PostImage
import com.example.moza.models.User
import com.example.moza.viewmodels.ProfileViewModel
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.marsad.stylishdialogs.StylishAlertDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment(), View.OnClickListener {
    private lateinit var viewModel: ProfileViewModel
    private lateinit var binding: FragmentProfileBinding
    private var isMyProfile: Boolean = true
    private lateinit var adapter: FirestoreRecyclerAdapter<PostImage, PostImageViewHolder>
    private lateinit var firebaseUser: FirebaseUser
    lateinit var userID: String
    private var isFollowed: Boolean = true
    private var adapterListen: Boolean = true
    val args: ProfileFragmentArgs by navArgs()
    lateinit var user: User
    lateinit var ownerUser : User

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        requireActivity().setActionBar(((binding.profileToolbar)))

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        binding.profilePostsRv.setHasFixedSize(true)
        binding.profilePostsRv.layoutManager = GridLayoutManager(requireContext(), 3)

        if (!args.uid.isNullOrEmpty()) {
            if (args.uid == firebaseUser.uid) {
                isMyProfile = true
                userID = firebaseUser.uid
                setProfileUi()
            } else {
                isMyProfile = false
                userID = args.uid!!
                setProfileUi()
            }
            viewModel.getUserData(userID)
            viewModel.getUserDataResponse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> {
                        user = response.data!!
                        updateUI(user)
                    }
                }

            }
        } else {
            userID = firebaseUser.uid
            setProfileUi()
            viewModel.getUserData(userID)
            viewModel.getUserDataResponse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> {
                        user = response.data!!
                        updateUI(user)
                    }
                }
            }
        }



        viewModel.getPostsSize(userID)
        viewModel.getPostsSize.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting user posts: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    val size = response.data
                    binding.profilePostsCountsTv.text = size.toString()
                    if (size == 0) {
                        adapterListen = false
                        binding.noPostsIv.visibility = View.VISIBLE
                    } else {
                        adapterListen = true
                        binding.noPostsIv.visibility = View.GONE
                    }
                }
            }
        }

        val options = viewModel.getUserPostsImages(userID)
        createPostsImagesAdapter(options)




        binding.editProfileImageIb.setOnClickListener(this)
        binding.profileFollowBtn.setOnClickListener(this)
        binding.profileChatBtn.setOnClickListener(this)
        binding.profileEditBtn.setOnClickListener(this)
        binding.profileFollowersCounterTv.setOnClickListener(this)
        binding.profileFollowingCounterTv.setOnClickListener(this)
        binding.savedPostsIv.setOnClickListener(this)
        return binding.root
    }


    private fun createPostsImagesAdapter(options: FirestoreRecyclerOptions<PostImage>) {
        this.adapter = object : FirestoreRecyclerAdapter<PostImage, PostImageViewHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostImageViewHolder {
                return PostImageViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(
                            R.layout.profile_images_item,
                            parent,
                            false
                        )
                )
            }

            override fun onBindViewHolder(
                holder: PostImageViewHolder,
                position: Int,
                model: PostImage
            ) {
                Glide.with(holder.itemView.context.applicationContext)
                    .load(model.imageUrl)
                    .timeout(6500)
                    .centerCrop()
                    .into(holder.itemView.findViewById(R.id.profile_posts_image_iv))

                holder.itemView.setOnClickListener {
                    val action =
                        ProfileFragmentDirections.actionNavigationMyProfileToViewUserPostFragment(
                            model.uid,
                            model.id!!
                        )
                    findNavController().navigate(action)
                }

                binding.profilePostsCountsTv.text = itemCount.toString()
            }

            override fun getItemCount(): Int {
                return super.getItemCount()
            }
        }
        binding.profilePostsRv.adapter = adapter

    }


    private fun setProfileUi() {
        if (isMyProfile) {
            binding.profileFollowingCounterCl.visibility = View.VISIBLE
            binding.profileFollowBtn.visibility = View.GONE
            binding.profileEditBtn.visibility = View.VISIBLE
            binding.editProfileImageIb.visibility = View.VISIBLE
            binding.profileChatBtn.visibility = View.GONE
            binding.savedPostsIv.visibility = View.VISIBLE
            binding.profilePostsRv.visibility = View.VISIBLE

        } else {
            binding.profileFollowBtn.visibility = View.VISIBLE
            binding.editProfileImageIb.visibility = View.GONE
            binding.profileEditBtn.visibility = View.GONE
            binding.profileChatBtn.visibility = View.VISIBLE
            binding.savedPostsIv.visibility = View.GONE


        }
    }


    private fun updateUI(user: User) {
        binding.profileToolbarUsernameTv.text = user.fullName
        binding.profileStatusTv.text = user.status
        binding.profileFollowersCountsTv.text = user.followers.size.toString()
        binding.profileFollowingCountsTv.text = user.following.size.toString()

        Glide.with(requireContext().applicationContext)
            .load(user.profilePicture)
            .circleCrop()
            .placeholder(R.drawable.profile_pic)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    if (isMyProfile) {
                        val bitmap = ((resource) as BitmapDrawable).bitmap
                        viewModel.storeImageToDevice(bitmap, user.profilePicture, user.fullName)
                    }
                    return false
                }

            })
            .timeout(6500)
            .into(binding.profileProfileImageIv)

        if (user.followers.contains(firebaseUser.uid)) {
            binding.profileFollowBtn.text = "UnFollow"
            binding.profileFollowBtn.setTextColor(resources.getColor(R.color.red))
            isFollowed = true
            binding.profileChatBtn.visibility = View.VISIBLE
            binding.profilePostsRv.visibility = View.VISIBLE
            binding.privateProfileLayout.visibility = View.GONE
        } else {
            binding.profileFollowBtn.text = "Follow"
            isFollowed = false
            binding.profileFollowBtn.setTextColor(resources.getColor(R.color.primary_cyan))
            binding.profileChatBtn.visibility = View.GONE
            println("user is: $user")
            if (!isMyProfile) {
                viewModel.updateSearchHistory(firebaseUser.uid, user)
                if (user.isprivate) {
                    binding.profilePostsRv.visibility = View.GONE
                    binding.noPostsIv.visibility = View.GONE
                    binding.privateProfileLayout.visibility = View.VISIBLE
                } else {
                    binding.profilePostsRv.visibility = View.VISIBLE
                    binding.privateProfileLayout.visibility = View.GONE
                }
            }


        }

        if (!isMyProfile) {
            viewModel.updateSearchHistory(firebaseUser.uid, user)
        }

    }

    override fun onStart() {
        super.onStart()
        binding.profilePostsRv.recycledViewPool.clear()
        adapter.notifyDataSetChanged()
        adapter.startListening()


    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // use the returned uri
            val uriContent = result.uriContent

            viewModel.storeImageToStorage(firebaseUser.uid, uriContent!!)
            viewModel.storeImageToStorageResponse.observe(viewLifecycleOwner, Observer { response ->

                when (response) {
                    is Resource.Success -> {
                        binding.progressbar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            response.data.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.profileProfileImageIv.setImageURI(uriContent)
                    }
                    is Resource.Error -> {
                        binding.progressbar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            response.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is Resource.Loading -> {
                        binding.progressbar.visibility = View.VISIBLE
                    }
                }

            })

        } else {
            // an error occurred
            val exception = result.error
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.edit_profile_image_ib -> {
                cropImage.launch(
                    options {
                        setGuidelines(CropImageView.Guidelines.ON)
                        setCropShape(CropImageView.CropShape.OVAL)
                        setAspectRatio(1, 1)
                    }
                )
            }
            R.id.profile_follow_btn -> {
                if (isFollowed) {
                    isFollowed = false
                    user.followers.remove(firebaseUser.uid)
                    viewModel.updateUserFollowList(user.id, user.followers, "followers")

                    binding.profileFollowBtn.setText("Follow")
                    binding.profileFollowersCountsTv.text = user.followers.size.toString()
                    viewModel.getUserFollowList(firebaseUser.uid)
                    viewModel.getUserFollowListResponse.observe(viewLifecycleOwner, Observer {
                        val following = it
                        following.remove(userID)
                        viewModel.updateUserFollowList(firebaseUser.uid, following, "following")

                    })


                } else {
                    isFollowed = true
                    user.followers.add(firebaseUser.uid)
                    viewModel.updateUserFollowList(user.id, user.followers, "followers")
                    binding.profileFollowBtn.setText("UnFollow")
                    binding.profileFollowersCountsTv.text = user.followers.size.toString()
                    viewModel.getUserFollowList(firebaseUser.uid)
                    viewModel.getUserFollowListResponse.observe(viewLifecycleOwner, Observer {
                        val following = it
                        following.add(userID)
                        Log.d("follow list observer2: ", "list is $following")
                        viewModel.updateUserFollowList(firebaseUser.uid, following, "following")
                    })
                    viewModel.sendFollowNotification(firebaseUser.uid , user.fullName , user.token!!)


                }
            }

            R.id.profile_chat_btn -> {
                var alertDialog = StylishAlertDialog(requireContext(), StylishAlertDialog.PROGRESS)
                alertDialog.setTitleText("Starting Chat...")
                alertDialog.cancellable = false
                alertDialog.show()
                var idList = ArrayList<String>()
                idList.add(userID)
                idList.add(firebaseUser.uid)
                var chatUser = ChatUser(
                    "",
                    idList,
                    arrayListOf(),
                    "Hi..",
                    System.currentTimeMillis()
                )
                val message = ChatMessage(
                    "",
                    firebaseUser.uid,
                    "Hi..",
                    System.currentTimeMillis()
                )

                viewModel.safeStartChat(chatUser, message)
                viewModel.createChatResponse.observe(viewLifecycleOwner, Observer { response ->
                    when (response) {
                        is Resource.Error -> {
                            Handler().postDelayed({
                                alertDialog.dismissWithAnimation()
                                Log.e("Error while creating chat", response.message!!)
                            }, 2000)
                        }
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            Handler().postDelayed({
                                alertDialog.dismissWithAnimation()
                                chatUser.id = response.data!!
                                val action =
                                    ProfileFragmentDirections.actionNavigationMyProfileToChatFragment(
                                        userID,
                                        chatUser.id,
                                        chatUser.unReadSender
                                    )
                                println("chat id from profile frag: ${chatUser.id}")
                                findNavController().navigate(action)
                            }, 2000)
                        }
                    }
                })
            }

            R.id.profile_edit_btn -> {
                val action =
                    ProfileFragmentDirections.actionNavigationMyProfileToEditProfileFragment(
                        user
                    )
                findNavController().navigate(action)
            }

            R.id.profile_following_counter_tv -> {
                val action = ProfileFragmentDirections.actionNavigationMyProfileToFollowersFragment(
                    user,
                    "following"
                )
                findNavController().navigate(action)
            }

            R.id.profile_followers_counter_tv -> {
                val action = ProfileFragmentDirections.actionNavigationMyProfileToFollowersFragment(
                    user,
                    "followers"
                )
                findNavController().navigate(action)
            }
            R.id.saved_posts_iv -> {
                val action =
                    ProfileFragmentDirections.actionNavigationMyProfileToSavedPostsFragment(
                        user
                    )
                findNavController().navigate(action)
            }
        }
    }

}

private class PostImageViewHolder(view: View) : RecyclerView.ViewHolder(view)
