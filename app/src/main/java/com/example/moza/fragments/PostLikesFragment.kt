package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.moza.R
import com.example.moza.adapters.PostLikesAdapter
import com.example.moza.adapters.UsersAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentFollowersBinding
import com.example.moza.databinding.FragmentPostLikesBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.FollowersViewModel


class PostLikesFragment : Fragment() {

    lateinit var binding: FragmentPostLikesBinding
    lateinit var viewModel: FollowersViewModel
    val args: PostLikesFragmentArgs by navArgs()
    val adapter = PostLikesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPostLikesBinding.inflate(inflater, container, false)
        args.let {
            viewModel.getUsers(it.post.likes)
            viewModel.getUsersResponse.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Resource.Error -> Log.e("Getting search history: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> {
                        setupRecyclerView(response.data!!)
                    }
                }
            }
        }
        return binding.root
    }

    private fun setupRecyclerView(users: MutableList<User>) {
        adapter.updateList(users)
        binding.likesRv.adapter = adapter
        adapter.notifyDataSetChanged()
        adapter.setOnItemClickListener(object : PostLikesAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, id: String) {
                val action =
                    PostLikesFragmentDirections.actionPostLikesFragmentToNavigationMyProfile(id)
                findNavController().navigate(action)
            }
        })
    }

}