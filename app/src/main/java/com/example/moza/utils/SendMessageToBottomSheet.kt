package com.example.moza.utils

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.R
import com.example.moza.adapters.UsersAdapter
import com.example.moza.common.Constants
import com.example.moza.common.Resource
import com.example.moza.databinding.SendMessageToBottomSheetBinding
import com.example.moza.models.ChatMessage
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import com.example.moza.viewmodels.SendMessageToViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendMessageToBottomSheet(val postUrl: String, val messageType: Int , var isForwarded : Boolean) :
    BottomSheetDialogFragment(),
    SearchView.OnQueryTextListener {

    lateinit var binding: SendMessageToBottomSheetBinding
    lateinit var viewModel: SendMessageToViewModel
    val adapter = UsersAdapter()
    var usersList: MutableList<User> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SendMessageToBottomSheetBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SendMessageToViewModel::class.java]
        binding.usersProfilesRv.layoutManager = LinearLayoutManager(requireContext())
        binding.usersProfilesRv.setHasFixedSize(true)

        viewModel.getUserData()
        viewModel.getUserDataResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    var followingList = response.data!!.following
                    viewModel.getUsers(followingList)
                }
            }
        }

        viewModel.getUsersDataResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    binding.progressbar.visibility = View.GONE
                    Log.e("Getting users profiles: ", response.message!!)
                }
                is Resource.Loading -> binding.progressbar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressbar.visibility = View.GONE
                    usersList = response.data!!
                    setupRecyclerView(response.data!!, false)
                }
            }
        }

        binding.chatSearchSv.setOnQueryTextListener(this)



        return binding.root
    }


    private fun setupRecyclerView(users: MutableList<User>, isSearchProfile: Boolean) {
        adapter.updateList(users)
        adapter.updateSearchHistoryBool(isSearchProfile)
        adapter.enableSendMessageBtn(!isSearchProfile)
        binding.usersProfilesRv.adapter = adapter
        adapter.notifyDataSetChanged()
        adapter.setOnItemClickListener(object : UsersAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, id: String) {
                TODO("Not yet implemented")
            }

            override fun onRemoveClick(position: Int, id: String) {
                TODO("Not yet implemented")
            }

            override fun onSendClick(position: Int, id: String) {
                val chatUIDS = ArrayList<String>()
                chatUIDS.add(id)
                var chatUser = ChatUser(
                    "",
                    chatUIDS,
                    arrayListOf(),
                    "",
                    System.currentTimeMillis()
                )
                var message = ChatMessage(
                    "",
                    "",
                    "",
                    System.currentTimeMillis(),
                    false,
                    "",
                    messageType,
                    isForwarded
                )
                when (messageType) {
                    Constants.TEXT_MESSAGE -> {
                        chatUser.lastMessage = postUrl
                        message.message = postUrl
                    }
                    Constants.IMAGE_MESSAGE -> {
                        chatUser.lastMessage = "Image"
                        message.message = "Image"
                        message.messageUrl = postUrl
                    }
                    Constants.VOICE_MESSAGE -> {
                        chatUser.lastMessage = "Voice"
                        message.message = "Voice"
                        message.messageUrl = postUrl
                    }
                    Constants.DOCUMENT_MESSAGE -> {
                        chatUser.lastMessage = "Document"
                        message.message = "Document"
                        message.messageUrl = postUrl
                    }
                    Constants.VIDEO_MESSAGE -> {
                        chatUser.lastMessage = "Video"
                        message.message = "Video"
                        message.messageUrl = postUrl
                    }
                }

                viewModel.safeStartChat(chatUser, message)
                viewModel.sendMessageResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            binding.progressbar.visibility = View.GONE
                            Log.e("Sending url message: ", response.message!!)
                        }
                        is Resource.Loading -> binding.progressbar.visibility = View.VISIBLE

                        is Resource.Success -> {
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Message sent.",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }
                }
            }
        })
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (!newText.isNullOrEmpty()) {
            var searchedUsers = mutableListOf<User>()
            viewModel.safeGetUserByName(newText)
            viewModel.getUserByNameResponse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Error -> Log.e("getting user data by name: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> {
                        searchedUsers = response.data!!
                        adapter.updateList(searchedUsers)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        } else {
            adapter.updateList(usersList)
            adapter.notifyDataSetChanged()
        }
        return false
    }
}