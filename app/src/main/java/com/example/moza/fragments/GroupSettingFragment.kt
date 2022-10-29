package com.example.moza.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.moza.R
import com.example.moza.adapters.GroupMembersAdapter
import com.example.moza.adapters.ThemesAdapter
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentGroupSettingBinding
import com.example.moza.models.ChatUser
import com.example.moza.models.Theme
import com.example.moza.models.User
import com.example.moza.utils.AddMemberBottomSheet
import com.example.moza.viewmodels.GroupSettingViewModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupSettingFragment : Fragment(), View.OnClickListener {

    private lateinit var viewModel: GroupSettingViewModel
    private lateinit var binding: FragmentGroupSettingBinding
    val args: ChatSettingFragmentArgs by navArgs()
    private var chatID = ""
    private var isMembersListOpen = false
    private var isThemeListOpen = false
    private var idList = arrayListOf<String>()
    private var chat = ChatUser()


    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseUserEntryPoint {
        fun provideFireBaseUser(): FirebaseUser
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupSettingBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[GroupSettingViewModel::class.java]


        args.let {
            chatID = it.chatID!!
        }

        viewModel.safeGetChat(chatID)
        viewModel.getChatResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Getting user data: ", response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
                    chat = response.data!!
                    binding.groupTitleTv.text = chat.groupTitle
                    Glide.with(requireContext())
                        .load(chat.groupProfileImg)
                        .placeholder(R.drawable.profile_pic)
                        .into(binding.groupImageIv)
                    idList = chat.uid
                    updateUI()
                }
            }
        }

        viewModel.updateGroupResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> Log.e("Updating group data: ", response.message!!)
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Group data updated.", Toast.LENGTH_SHORT)
                        .show()
                    viewModel.getUsers(chat.uid)
                }
            }
        }

        binding.viewGroupTheme.setOnClickListener(this)
        binding.viewGroupMembers.setOnClickListener(this)
        binding.groupTitleEditTv.setOnClickListener(this)
        binding.editGroupImageIb.setOnClickListener(this)
        binding.chatSettingBackIv.setOnClickListener(this)
        binding.addGroupMemberIv.setOnClickListener(this)

        return binding.root
    }

    private fun updateUI() {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(requireContext(), FirebaseUserEntryPoint::class.java)
        val firebaseUser = hiltEntryPoint.provideFireBaseUser()
        if (chat.adminID != firebaseUser.uid){
            binding.groupTitleEditTv.visibility = View.GONE
            binding.editGroupImageIb.visibility = View.GONE
        }
    }

    private fun setUpMembersRV(usersList: MutableList<User>) {
        binding.groupMembersRv.layoutManager = LinearLayoutManager(requireContext())
        binding.groupMembersRv.setHasFixedSize(true)
        val adapter = GroupMembersAdapter(requireContext())
        adapter.updateList(usersList)
        adapter.setAdminID(chat.adminID)
        binding.groupMembersRv.adapter = adapter

        adapter.setOnItemClickListener(object : GroupMembersAdapter.OnItemClickListener {
            override fun onRemoveCLick(position: Int, userId: String) {
                chat.uid.remove(userId)
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Remove member")
                builder.setMessage("Are you sure?")
                builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                    viewModel.updateGroupData(chatID, chat)

                }
                builder.setNegativeButton(android.R.string.no) { dialog, which ->

                }
                builder.show()
            }
        })
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // use the returned uri
            val uriContent = result.uriContent
            viewModel.updateGroupImg(chatID , uriContent!!)
            viewModel.updateGroupImgResponse.observe(viewLifecycleOwner){response->
                when (response) {
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Log.e("Updating group data: ", response.message!!)
                    }
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Group data updated.", Toast.LENGTH_SHORT)
                            .show()
                        viewModel.getUsers(chat.uid)
                    }
                }
            }

        } else {
            // an error occurred
            val exception = result.error
        }
    }


    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.view_group_members -> {
                if (!isMembersListOpen) {
                    binding.groupMembersRv.visibility = View.VISIBLE
                    isMembersListOpen = true
                    binding.viewGroupMembers.text = "Hide"
                    binding.viewGroupMembers.setTextColor(resources.getColor(R.color.red))
                    viewModel.getUsers(idList)
                    viewModel.getUsersDataResponse.observe(viewLifecycleOwner) { response ->
                        when (response) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Log.e("Getting users profiles: ", response.message!!)
                            }
                            is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                var usersList = response.data!!
                                setUpMembersRV(usersList)
                            }
                        }
                    }
                } else {
                    isMembersListOpen = false
                    binding.viewGroupMembers.text = "View"
                    binding.viewGroupMembers.setTextColor(resources.getColor(R.color.light_blue))
                    binding.groupMembersRv.visibility = View.GONE
                }
            }

            R.id.view_group_theme -> {
                if (!isThemeListOpen) {
                    binding.groupThemeRv.visibility = View.VISIBLE
                    setUpThemeRV()
                    isThemeListOpen = true
                    binding.viewGroupTheme.text = "Hide"
                    binding.viewGroupTheme.setTextColor(resources.getColor(R.color.red))
                } else {
                    isThemeListOpen = false
                    binding.viewGroupTheme.text = "View"
                    binding.viewGroupTheme.setTextColor(resources.getColor(R.color.light_blue))
                    binding.groupThemeRv.visibility = View.GONE
                }
            }

            R.id.group_title_edit_tv -> {
                CoroutineScope(Dispatchers.Main).launch {
                    val builder = AlertDialog.Builder(context)
                    val inflater = layoutInflater
                    val dialogLayout = inflater.inflate(R.layout.message_edit_dialog, null)
                    val editText = dialogLayout.findViewById<EditText>(R.id.message_edit_et)
                    editText.hint = "New title..."
                    editText.setText(chat.groupTitle)
                    dialogLayout.findViewById<TextView>(R.id.edit_message_tv).setText("Group Title")
                    with(builder) {
                        setPositiveButton("Change") { dialog, which ->
                            if (editText.text.toString().isNotEmpty()) {
                                chat.groupTitle = editText.text.toString()
                                viewModel.updateGroupData(chatID, chat)
                            } else
                                Toast.makeText(
                                    requireContext(),
                                    "Group title can't be empty!",
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                        setNegativeButton("Cancel") { dialog, which ->

                        }
                        setView(dialogLayout)
                        show()
                    }
                }
            }

            R.id.edit_group_image_ib -> {
                cropImage.launch(
                    options {
                        setGuidelines(CropImageView.Guidelines.ON)
                        setCropShape(CropImageView.CropShape.OVAL)
                        setAspectRatio(1, 1)
                        setOutputCompressFormat(Bitmap.CompressFormat.PNG)

                    }
                )
            }
            R.id.chat_setting_back_iv -> requireActivity().onBackPressed()

            R.id.add_group_member_iv -> {
                val bottomSheetDialog = AddMemberBottomSheet(chat.uid , chatID)
                bottomSheetDialog.show(
                    requireActivity().supportFragmentManager,
                    "Send Message To: "
                )
            }
        }
    }

    private fun setUpThemeRV() {
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

        var themes = mutableListOf<Theme>(
            Theme(strangeBG, "DOCTOR_STRANGE"),
            Theme(batmanBG, "BATMAN"),
            Theme(spiderManBg, "SPIDER_MAN"),
            Theme(jotaro, "JOTARO")
        )

        val adapter = ThemesAdapter(themes)
        binding.groupThemeRv.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.groupThemeRv.setHasFixedSize(true)
        binding.groupThemeRv.adapter = adapter

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
    }
}