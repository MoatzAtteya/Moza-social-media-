package com.example.moza.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moza.R
import com.example.moza.adapters.HiddenChatAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentHiddenMessagesBinding
import com.example.moza.models.ChatUser
import com.example.moza.viewmodels.HiddenMessagesViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.myshoppal.utils.SwipeToShowCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiddenMessagesFragment : Fragment() {

    private lateinit var viewModel: HiddenMessagesViewModel
    private lateinit var binding: FragmentHiddenMessagesBinding
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var hiddenChats: MutableList<ChatUser>
    private lateinit var adapter: HiddenChatAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHiddenMessagesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(HiddenMessagesViewModel::class.java)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        adapter = HiddenChatAdapter(this)
        binding.hiddenChatRv.layoutManager = LinearLayoutManager(requireContext())
        binding.hiddenChatRv.setHasFixedSize(true)

        viewModel.safeGetHiddenMessages(firebaseUser.uid)
        viewModel.getHiddenMessages.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    Log.e("getting user hidden chat: ", response.data.toString())
                }
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    hiddenChats = response.data!!
                    adapter.updateChatList(hiddenChats)
                    binding.hiddenChatRv.adapter = adapter

                }
            }
        }

        val showSwipeHelper = object : SwipeToShowCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val chat = hiddenChats[viewHolder.bindingAdapterPosition]
                var hideIDS = chat.hideIDS
                hideIDS.remove(firebaseUser.uid)
                println("user id: ${chat.id} , hide ids:  $hideIDS")
                viewModel.safeShowChat(chat.id!!, hideIDS)
                Snackbar.make(
                    binding.coordinatorLayout,
                    "Undo successfully.",
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        }
        val showItemTouchHelper = ItemTouchHelper(showSwipeHelper)
        showItemTouchHelper.attachToRecyclerView(binding.hiddenChatRv)

        adapter.setOnItemClickListener(object : HiddenChatAdapter.OnItemClickListener {
            override fun onItemClick(
                position: Int, chatID: String, idList: ArrayList<String>, unReadSenderID: String
            ) {
                var oppositeUID = ""
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (!idList[0].equals(firebaseUser!!.uid, ignoreCase = false))
                    oppositeUID = idList[0]
                else
                    oppositeUID = idList[1]
                val action =
                    HiddenMessagesFragmentDirections.actionHiddenMessagesFragmentToChatFragment(
                        oppositeUID,
                        chatID,
                        unReadSenderID
                        )
                findNavController().navigate(action)

            }
        })

        binding.allMessagesCv.setOnClickListener {
            requireActivity().onBackPressed()

        }

        return binding.root
    }

}