package com.example.moza.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.R
import com.example.moza.adapters.ThemesAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentChatSettingBinding
import com.example.moza.models.Theme
import com.example.moza.viewmodels.ChatSettingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.marsad.stylishdialogs.StylishAlertDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatSettingFragment : Fragment(), View.OnClickListener {

    private lateinit var viewModel: ChatSettingViewModel
    private lateinit var binding: FragmentChatSettingBinding
    private lateinit var firebaseUser: FirebaseUser

    val args: ChatSettingFragmentArgs by navArgs()
    private var chatID = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatSettingBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ChatSettingViewModel::class.java)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        args.let {
            chatID = it.chatID!!
        }
        updateUI()

        val strangeBG = BitmapFactory.decodeResource(
            requireContext().resources,
            R.drawable.doctor_strange_chat_bg
        )
        val batmanBG =
            BitmapFactory.decodeResource(requireContext().resources, R.drawable.batman_chat_bg)
        val spiderManBg =
            BitmapFactory.decodeResource(
                requireContext().resources,
                R.drawable.spiderman_chat_background
            )
        val jotaro =
            BitmapFactory.decodeResource(
                requireContext().resources,
                R.drawable.jotaroo_kujo_chat_bg
            )
        val strangerThings =
            BitmapFactory.decodeResource(
                requireContext().resources,
                R.drawable.stranger_things_bg
            )
        val defaultTheme =
            BitmapFactory.decodeResource(
                requireContext().resources,
                R.drawable.main_theme_bg
            )

        val themes = mutableListOf<Theme>(
            Theme(strangeBG, "DOCTOR_STRANGE"),
            Theme(batmanBG, "BATMAN"),
            Theme(spiderManBg, "SPIDER_MAN"),
            Theme(jotaro, "JOTARO"),
            Theme(strangerThings, "STRANGER_THINGS"),
            Theme(defaultTheme, "DEFAULT_THEME")
        )

        val adapter = ThemesAdapter(themes)
        binding.themesRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.themesRv.setHasFixedSize(true)
        binding.themesRv.adapter = adapter

        adapter.setOnItemClickListener(object : ThemesAdapter.OnItemClickListener {
            override fun onItemClick(position: Int, id: String, theme: Bitmap) {
                val sharedPref = requireActivity().getSharedPreferences(
                    "THEME_SHAREDPREF:$chatID",
                    Context.MODE_PRIVATE
                )
                var editor = sharedPref.edit()
                Palette.from(theme).generate {
                    val intColor = it?.vibrantSwatch?.rgb ?: 0
                    editor.putInt("VIEWS_BACKGROUND", intColor)
                }
                // var imageStr = BitMapToString(theme)
                editor.putString("THEME_NAME", id)
                editor.putString("THEME_CHAT_ID", chatID)
                editor.commit()
                Toast.makeText(requireContext(), "Theme: $id applied.", Toast.LENGTH_SHORT).show()
            }
        })

        binding.deleteChatTv.setOnClickListener(this)
        binding.chatSettingBackIv.setOnClickListener(this)

        viewModel.deleteChatResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error ->{
                    Toast.makeText(
                        requireContext(),
                        "Error while requesting to delete the chat!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("Requesting chat delete: " , response.message!!)
                }
                is Resource.Loading -> {}
                is Resource.Success -> Toast.makeText(
                    requireContext(),
                    "Chat delete request is sent.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return binding.root
    }



    private fun updateUI() {
        viewModel.safeGetChat(chatID)
        viewModel.getChatResponse.observe(viewLifecycleOwner){response->
            when(response){
                is Resource.Error ->  Log.e("Requesting chat delete: " , response.message!!)

                is Resource.Loading -> {}
                is Resource.Success -> {
                    if (response.data!!.deleteRequest.isNullOrEmpty()){
                        binding.deleteChatTv.visibility = View.VISIBLE
                        binding.cancelDeleteChatTv.visibility = View.GONE
                    }else{
                        binding.deleteChatTv.visibility = View.GONE
                        binding.cancelDeleteChatTv.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.delete_chat_tv -> {
                buildAlertDialog(true)
            }
            R.id.cancel_delete_chat_tv -> {
                buildAlertDialog(false)
            }
            R.id.chat_setting_back_iv-> {
                requireActivity().onBackPressed()
            }

        }
    }

    private fun buildAlertDialog(delete: Boolean) {
        val pDialog = StylishAlertDialog(requireContext(), StylishAlertDialog.NORMAL)

        if (delete){
            pDialog.setTitleText("You will wait for the second user to accept this request.")
                .setCancellable(true)
                .setConfirmButton("okay") {
                    it.dismissWithAnimation()
                    viewModel.safeRequestChatDelete(chatID, firebaseUser!!.uid)
                }

                .setCancelButton("Cancel") {
                    it.dismissWithAnimation()
                }
                //.setCancelledOnTouchOutside(false)
                .show()
        }else{
            pDialog.setTitleText("Are you sure?")
                .setCancellable(true)
                .setConfirmButton("okay") {
                    it.dismissWithAnimation()
                    viewModel.safeRequestChatDelete(chatID, "")
                }

                .setCancelButton("Cancel") {
                    it.dismissWithAnimation()
                }
                //.setCancelledOnTouchOutside(false)
                .show()
        }


    }


}