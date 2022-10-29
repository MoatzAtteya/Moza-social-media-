package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.adapters.UsersAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentSearchBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: SearchViewModel
    lateinit var adapter: UsersAdapter
    lateinit var users: MutableList<User>
    lateinit var firebaseUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        users = mutableListOf()
        adapter = UsersAdapter()

        binding.searchProfilesRv.layoutManager = LinearLayoutManager(requireContext())
        binding.searchProfilesRv.setHasFixedSize(true)

        viewModel.getSearchHistory(firebaseUser.uid)
        viewModel.getSearchHistoryResponse.observe(viewLifecycleOwner, Observer { response->
            when(response){
                is Resource.Error -> Log.e("Getting search history: ", response.message!!)
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    users = response.data!!
                    setupRecyclerView(users, true)
                }
            }

        })
        binding.profileSearchSv.setOnQueryTextListener(this)

        return binding.root
    }

    private fun setupRecyclerView(users: MutableList<User>, isSearchProfile: Boolean) {
        println("users are: $users")
        users.reverse()
        adapter.updateList(users)
        adapter.updateSearchHistoryBool(isSearchProfile)
        binding.searchProfilesRv.adapter = adapter
        adapter.notifyDataSetChanged()
        adapter.setOnItemClickListener(object : UsersAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, id: String) {
                val action =
                    SearchFragmentDirections.actionNavigationSearchToNavigationMyProfile(id)
                findNavController().navigate(action)
            }

            override fun onRemoveClick(position: Int, id: String) {
                viewModel.removeSearchItem(firebaseUser.uid , id)
                adapter.list.removeAt(position)
                adapter.notifyItemChanged(position)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(),"Search Removed", Toast.LENGTH_SHORT).show()

            }

            override fun onSendClick(position: Int, id: String) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (!newText.isNullOrEmpty()) {
            viewModel.getUserData(newText!!)
            viewModel.getUsersResponse.observe(viewLifecycleOwner, Observer {
                setupRecyclerView(it, false)
            })
        } else {
            setupRecyclerView(users, false)
        }
        return false
    }

}