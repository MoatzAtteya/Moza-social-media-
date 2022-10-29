package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.adapters.UsersAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentFollowersBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.FollowersViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowersFragment : Fragment() {

    lateinit var binding: FragmentFollowersBinding
    lateinit var viewModel : FollowersViewModel
    val args : FollowersFragmentArgs by navArgs()
    val adapter = UsersAdapter()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFollowersBinding.inflate(layoutInflater, container, false)
        viewModel = ViewModelProvider(this).get(FollowersViewModel::class.java)
        binding.followersRv.layoutManager = LinearLayoutManager(requireContext())
        binding.followersRv.setHasFixedSize(true)

        args.let {
            if (it.type == "following"){
                binding.followersTitleTv.text = it.type
                viewModel.getUsers(it.user.following)
                viewModel.getUsersResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> Log.e("Getting search history: ", response.message!!)
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            setupRecyclerView(response.data!!, false)
                        }
                    }
                }
            }else{
                binding.followersTitleTv.text = it.type
                viewModel.getUsers(it.user.followers)
                viewModel.getUsersResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> Log.e("Getting search history: ", response.message!!)
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            setupRecyclerView(response.data!!, false)
                        }
                    }
                }
            }
        }

        binding.followersBackBtn.setOnClickListener{
            requireActivity().onBackPressed()
        }

        return binding.root
    }

    private fun setupRecyclerView(users: MutableList<User>, isSearchProfile: Boolean) {
        adapter.updateList(users)
        adapter.updateSearchHistoryBool(isSearchProfile)
        binding.followersRv.adapter = adapter
        adapter.notifyDataSetChanged()
        adapter.setOnItemClickListener(object : UsersAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, id: String) {
                val action =
                    FollowersFragmentDirections.actionFollowersFragmentToNavigationMyProfile(id)
                println("clicked user id is: $id")
                findNavController().navigate(action)
            }

            override fun onRemoveClick(position: Int, id: String) {
            }

            override fun onSendClick(position: Int, id: String) {
                TODO("Not yet implemented")
            }

        })
    }

}