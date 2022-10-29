package com.example.moza.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.adapters.LikesAdapter
import com.example.moza.databinding.FragmentLikesBinding
import com.example.moza.common.Resource
import com.example.moza.viewmodels.LikesViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LikesFragment : Fragment() {


    private lateinit var viewModel: LikesViewModel
    private lateinit var binding: FragmentLikesBinding
    private lateinit var firebaseUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLikesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(LikesViewModel::class.java)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        binding.notificationsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRv.setHasFixedSize(true)
        val adapter = LikesAdapter(this)

        viewModel.safeGetUserNotifications(firebaseUser.uid)
        viewModel.userNotificationResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    binding.progressbar.visibility = View.GONE
                    Log.e("getting user notifications: ", response.message.toString())
                }
                is Resource.Loading -> {
                    binding.progressbar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressbar.visibility = View.GONE
                    adapter.updateList(response.data!!)
                    binding.notificationsRv.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }

        }

        adapter.setOnItemClickListener(object  : LikesAdapter.OnItemClickListener{
            override fun onItemClick(position: Int, uid: String, postId: String) {
                val action = LikesFragmentDirections.actionNavigationLikesToViewUserPostFragment(uid , postId)
                findNavController().navigate(action)
            }

        })

        return binding.root
    }

}