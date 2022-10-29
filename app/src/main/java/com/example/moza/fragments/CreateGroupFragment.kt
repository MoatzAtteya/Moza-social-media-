package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.adapters.SelectMultiUsersAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentCreateGroupBinding
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import com.example.moza.viewmodels.CreateGroupViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateGroupFragment : Fragment() {

    private lateinit var viewModel: CreateGroupViewModel
    private lateinit var binding: FragmentCreateGroupBinding
    var groupUsers = mutableListOf<String>()
    private lateinit var firebaseUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(CreateGroupViewModel::class.java)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        viewModel.getUserData()
        viewModel.getUserDataResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    var followingList = response.data!!.following
                    println("following list is: $followingList")
                    viewModel.getUsers(followingList)
                }
            }
        }

        viewModel.getUsersDataResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    //binding.progressbar.visibility = View.GONE
                    Toast.makeText(requireContext(), response.message , Toast.LENGTH_SHORT).show()
                    Log.e("Getting users profiles: ", response.message!!)
                    requireActivity().onBackPressed()
                }
                is Resource.Loading -> {
                    //binding.progressbar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    //binding.progressbar.visibility = View.GONE
                    val usersList = response.data!!
                    setUpRecyclerView(usersList)
                }
            }
        }

        binding.chatBtn.setOnClickListener {
            if (binding.groupTitleEt.text.isNotEmpty()) {
                if (groupUsers.size <= 1) {
                    Toast.makeText(requireContext(), "Please select more than 1 person.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val chat = ChatUser(
                        "", groupUsers as ArrayList<String>, ArrayList(), "",
                        System.currentTimeMillis(),
                    )
                    chat.isGroupChat = true
                    chat.adminID = firebaseUser.uid
                    chat.groupTitle = binding.groupTitleEt.text.toString()
                    viewModel.safeStartChat(chat)
                    viewModel.startGroupChat.observe(viewLifecycleOwner) { response ->
                        when (response) {
                            is Resource.Error -> {
                                Log.e("Creating new group chat: ", response.message!!)
                                Toast.makeText(
                                    requireContext(),
                                    "Error while creating group",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is Resource.Loading -> {}
                            is Resource.Success -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Group chat created.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        }
                    }
                }

            } else
                Toast.makeText(requireContext(), "Group title can't be empty!", Toast.LENGTH_SHORT)
                    .show()
        }

        return binding.root
    }

    private fun setUpRecyclerView(usersList: MutableList<User>) {
        binding.selectUsersRv.layoutManager = LinearLayoutManager(requireContext())
        binding.selectUsersRv.setHasFixedSize(true)
        val adapter = SelectMultiUsersAdapter()
        adapter.updateList(usersList)
        binding.selectUsersRv.adapter = adapter

        adapter.setOnItemClickListener(object : SelectMultiUsersAdapter.OnItemClickListener {
            override fun onAddClick(position: Int, id: String) {
                groupUsers.add(id)
            }

            override fun onRemoveClick(position: Int, id: String) {
                groupUsers.remove(id)
            }

        })
    }

}