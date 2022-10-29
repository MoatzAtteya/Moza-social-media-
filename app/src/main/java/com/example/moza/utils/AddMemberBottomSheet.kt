package com.example.moza.utils

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.R
import com.example.moza.adapters.SelectMultiUsersAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.AddMemberBottomSheetBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.AddMemberViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddMemberBottomSheet(val membersList: ArrayList<String>, val chatID: String) : BottomSheetDialogFragment() {

    lateinit var binding: AddMemberBottomSheetBinding
    lateinit var viewModel: AddMemberViewModel
    var newMembers = arrayListOf<String>()
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
        binding = AddMemberBottomSheetBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(AddMemberViewModel::class.java)

        viewModel.getUserData()
        viewModel.getUserDataResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val followingList = response.data!!.following
                    followingList.removeAll(membersList.toSet())
                    if (followingList.size > 0)
                        viewModel.getUsers(followingList)
                    else {
                        Toast.makeText(
                            requireContext(),
                            "All your friends are in this group.",
                            Toast.LENGTH_SHORT
                        ).show()
                        this.dismiss()
                    }
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
                    setUpRecyclerView(usersList)
                }
            }
        }
        binding.confirmAddingMembersBtn.setOnClickListener {
            if (newMembers.size >= 1){
                viewModel.getAndUpdateGroup(chatID,newMembers)
                viewModel.addMemberResponse.observe(viewLifecycleOwner){response->
                    when(response){
                        is Resource.Error -> {
                            Log.e("Adding group members: " , response.message!!)
                            Toast.makeText(requireContext(), "Error happened" , Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Loading -> {}
                        is Resource.Success -> {
                            Toast.makeText(requireContext(), "Users added." , Toast.LENGTH_SHORT).show()
                            this.dismiss()
                        }
                    }
                }
            }
        }


        return binding.root
    }

    private fun setUpRecyclerView(usersList: MutableList<User>) {
        binding.usersProfilesRv.layoutManager = LinearLayoutManager(requireContext())
        binding.usersProfilesRv.setHasFixedSize(true)
        val adapter = SelectMultiUsersAdapter()
        adapter.updateList(usersList)
        binding.usersProfilesRv.adapter = adapter

        adapter.setOnItemClickListener(object : SelectMultiUsersAdapter.OnItemClickListener {
            override fun onAddClick(position: Int, id: String) {
                newMembers.add(id)
                showHideConfirmBtn()
            }

            override fun onRemoveClick(position: Int, id: String) {
                newMembers.remove(id)
                showHideConfirmBtn()
            }

        })
    }

    private fun showHideConfirmBtn() {
        if (newMembers.size > 0)
            binding.confirmAddingMembersBtn.visibility = View.VISIBLE
        else
            binding.confirmAddingMembersBtn.visibility = View.GONE

    }
}