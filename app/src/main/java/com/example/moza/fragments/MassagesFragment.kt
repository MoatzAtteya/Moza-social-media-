package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moza.R
import com.example.moza.adapters.ChatUserAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentMassagesBinding
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import com.example.moza.viewmodels.MassagesViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.myshoppal.utils.SwipeToDeleteCallback
import com.myshoppal.utils.SwipeToHideCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MassagesFragment : Fragment(), SearchView.OnQueryTextListener, View.OnClickListener {
    private lateinit var binding: FragmentMassagesBinding
    private lateinit var viewModel: MassagesViewModel
    private lateinit var chats: MutableList<ChatUser>
    private lateinit var adapter: ChatUserAdapter
    private lateinit var firebaseUser: FirebaseUser


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMassagesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(MassagesViewModel::class.java)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        chats = arrayListOf()
        adapter = ChatUserAdapter(this)

        binding.chatRv.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRv.setHasFixedSize(true)
        binding.hiddenMessagesCv.setOnClickListener(this)
        binding.allMessagesCv.setOnClickListener(this)
        binding.createGroupBtn.setOnClickListener(this)



        viewModel.safeGetUserChats(firebaseUser.uid)
        viewModel.gettingMessagesResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    Log.e("getting user chat: ", response.data.toString())
                }
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    chats = response.data!!
                    if (chats.size == 0) {
                        Toast.makeText(
                            requireContext(),
                            "No conversations found.",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.chatRv.visibility = View.GONE
                        binding.noMessagesIv.visibility = View.VISIBLE
                    } else {
                        binding.chatRv.visibility = View.VISIBLE
                        binding.noMessagesIv.visibility = View.GONE
                        adapter.updateChatList(chats)
                        binding.chatRv.adapter = adapter
                    }
                }
            }
        }

        adapter.setOnItemClickListener(object : ChatUserAdapter.OnItemClickListener {
            override fun onItemClick(
                position: Int,
                chatID: String,
                idList: ArrayList<String>,
                unreadSenderID: String,
                isGroup: Boolean
            ) {
                if (!isGroup) {
                    var oppositeUID = ""
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    oppositeUID = if (!idList[0].equals(firebaseUser!!.uid, ignoreCase = false))
                        idList[0]
                    else
                        idList[1]

                    val action = MassagesFragmentDirections.actionMassagesFragmentToChatFragment(
                        oppositeUID,
                        chatID,
                        unreadSenderID
                    )
                    findNavController().navigate(action)
                } else {
                    val action = MassagesFragmentDirections.actionMassagesFragmentToChatFragment(
                        "",
                        chatID,
                        unreadSenderID
                    )
                    findNavController().navigate(action)
                }
            }
        })


        val hideSwipeHelper = object : SwipeToHideCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val chat = chats[viewHolder.bindingAdapterPosition]
                viewModel.safeHideChat(chat.id!!, firebaseUser.uid)
                viewModel.hideChatResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            Log.e("Hiding user chat: ", response.message!!)
                            Toast.makeText(
                                requireContext(),
                                "Error while hiding user chat.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            Snackbar.make(
                                binding.coordinatorLayout,
                                "Chat is hidden.",
                                Snackbar.LENGTH_LONG
                            )
                                .setAction("Undo") { _ ->
                                    var hideIDS = chat.hideIDS
                                    hideIDS.remove(firebaseUser.uid)
                                    viewModel.safeShowChat(chat.id!!, hideIDS)
                                    Snackbar.make(
                                        binding.coordinatorLayout,
                                        "Undo successfully.",
                                        Snackbar.LENGTH_SHORT
                                    )
                                        .show()
                                }
                                .show()
                        }
                    }
                }
            }
        }
        val hideItemTouchHelper = ItemTouchHelper(hideSwipeHelper)
        hideItemTouchHelper.attachToRecyclerView(binding.chatRv)

        val archiveSwipeHelper = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val chat = chats[viewHolder.bindingAdapterPosition]
                viewModel.safeDeleteChat(chat.id!!, firebaseUser.uid)
                viewModel.deleteChatResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            Log.e("Deleting user chat: ", response.message!!)
                            Toast.makeText(
                                requireContext(),
                                "Error while hiding user chat.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            Snackbar.make(
                                binding.coordinatorLayout,
                                "Chat is Deleted.",
                                Snackbar.LENGTH_LONG
                            ).setAction("Undo") { _ ->
                                val deleteIDS = chat.deleteIDS
                                deleteIDS.remove(firebaseUser.uid)
                                viewModel.safeUnDeleteChat(chat.id!!, deleteIDS)
                                Snackbar.make(
                                    binding.coordinatorLayout,
                                    "Undo successfully.",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .show()
                            }
                                .show()
                        }
                    }
                }
            }
        }
        val archiveItemTouchHelper = ItemTouchHelper(archiveSwipeHelper)
        archiveItemTouchHelper.attachToRecyclerView(binding.chatRv)

        binding.chatSearchSv.setOnQueryTextListener(this)

        return binding.root
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        var searchedChat = mutableListOf<ChatUser>()
        if (!newText.isNullOrEmpty()) {
            var searchedUsers: MutableList<User>
            viewModel.safeGetUserByName(newText)
            viewModel.getUserByNameResponse.observe(viewLifecycleOwner){ response ->
                when (response) {
                    is Resource.Error -> Log.e("getting user data by name: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> {
                        searchedUsers = response.data!!
                        for (chat in chats) {
                            for (user in searchedUsers) {
                                var uidList = listOf(user.id, firebaseUser.uid)
                                if (chat.uid.containsAll(uidList))
                                    searchedChat.add(chat)
                            }
                        }
                        searchedChat = searchedChat.distinct() as MutableList<ChatUser>
                        adapter.updateChatList(searchedChat)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

        } else {
            adapter.updateChatList(chats)
            adapter.notifyDataSetChanged()
        }
        return false
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.hidden_messages_cv -> {
                findNavController().navigate(R.id.action_massagesFragment_to_hiddenMessagesFragment)
            }
            R.id.create_group_btn -> {
                findNavController().navigate(R.id.action_massagesFragment_to_createGroupFragment)
            }

        }
    }

}

