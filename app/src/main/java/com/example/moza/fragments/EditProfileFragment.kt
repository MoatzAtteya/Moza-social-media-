package com.example.moza.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.activities.MainActivity
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentEditProfileBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.EditProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.marsad.stylishdialogs.StylishAlertDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileFragment : Fragment(), View.OnClickListener {

    private lateinit var binding : FragmentEditProfileBinding
    private lateinit var viewModel: EditProfileViewModel
    private lateinit var firebaseUser : FirebaseUser
    val args : EditProfileFragmentArgs by navArgs()
    private var user =  User()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditProfileBinding.inflate(layoutInflater , container , false)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        viewModel = ViewModelProvider(this).get(EditProfileViewModel::class.java)

        args.apply {
            this@EditProfileFragment.user = this.user
            updateUI(user)
        }


        binding.logOutBtn.setOnClickListener(this)
        binding.editSensitiveInformation.setOnClickListener(this)
        binding.profileSettingApply.setOnClickListener(this)
        binding.profileSettingBackBtn.setOnClickListener(this)
        binding.privateAccountSwitcher.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked){
                viewModel.updateProfilePrivacy(true)
            }else{
                viewModel.updateProfilePrivacy(false)

            }
        }
        return binding.root
    }

    private fun updateUI(user: User) {

        Glide.with(requireContext())
            .load(user.profilePicture)
            .placeholder(R.drawable.profile_pic)
            .into(binding.profileSettingImageIv)
        binding.editFullNameEt.setText(user.fullName)
        binding.editUsernameEt.setText(user.username)
        binding.editBioEt.setText(user.status)
        binding.privateAccountSwitcher.isChecked = user.isprivate


    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.profile_setting_apply-> {
                if (validateUserInputs()) {
                    user.fullName = binding.editFullNameEt.text.toString()
                    user.username = binding.editUsernameEt.text.toString()
                    user.status = binding.editBioEt.text.toString()
                    viewModel.safeUpdateUser(user)
                    viewModel.updateUserResponse.observe(viewLifecycleOwner){ response->
                        when(response){
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Log.e("Updating user data: ", response.message!!)
                                Toast.makeText(requireContext(), "Something went wrong!" , Toast.LENGTH_SHORT).show()
                            }
                            is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Log.d("Updating user data: ", response.data!!)
                                val pDialog = StylishAlertDialog(requireContext(), StylishAlertDialog.SUCCESS)
                                pDialog.progressHelper.barColor = Color.parseColor("#A5DC86")
                                pDialog.setTitleText("Your data updated successfully.")
                                    .setCancellable(true)
                                    //.setCancelledOnTouchOutside(false)
                                    .show()
                            }
                        }
                    }
                }
            }
            R.id.profile_Setting_back_Btn -> {
                requireActivity().onBackPressed()
            }

            R.id.log_out_btn -> {
                viewModel.safeUpdateUserStatus()
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                Toast.makeText(requireContext() , "Logged out." , Toast.LENGTH_SHORT).show()
                startActivity(intent)
            }

            R.id.edit_sensitive_information -> {
                val action = EditProfileFragmentDirections.actionEditProfileFragmentToEditSensetiveFragment(
                    user
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun validateUserInputs(): Boolean {
        return when {
            TextUtils.isEmpty(binding.editFullNameEt.text.toString().trim { it <= ' ' }) -> {
                binding.editFullNameTil.isErrorEnabled = true
                binding.editFullNameEt.error = "You need to enter your Full name"
                false
            }

            TextUtils.isEmpty(binding.editUsernameEt.text.toString().trim { it <= ' ' }) -> {
                binding.editUsernameTil.isErrorEnabled = true
                binding.editUsernameEt.error = "You need to enter your email"
                false
            }

            TextUtils.isEmpty(binding.editBioEt.text.toString().trim { it <= ' ' }) -> {
                binding.editBioTil.isErrorEnabled = true
                binding.editBioEt.error = "You need to enter your password"
                false
            }

            else -> {
                binding.editFullNameTil.isErrorEnabled = false
                binding.editUsernameTil.isErrorEnabled = false
                binding.editBioTil.isErrorEnabled = false
                true
            }
        }
    }


}