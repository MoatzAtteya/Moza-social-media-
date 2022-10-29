package com.example.moza.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.moza.R
import com.example.moza.databinding.FragmentEditSensetiveBinding
import com.example.moza.models.User
import com.example.moza.common.Resource
import com.example.moza.viewmodels.EditSensetiveViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditSensetiveFragment : Fragment(), View.OnClickListener {

    private lateinit var viewModel: EditSensetiveViewModel
    private lateinit var binding: FragmentEditSensetiveBinding
    private var user = User()
    private val args: EditSensetiveFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditSensetiveBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(EditSensetiveViewModel::class.java)
        args.apply {
            this@EditSensetiveFragment.user = this.user
        }

        binding.profileSensitiveSettingApply.setOnClickListener(this)
        binding.forgetPasswordBtn.setOnClickListener(this)
        binding.profileSettingBackBtn.setOnClickListener(this)
        return binding.root
    }

    private fun validateUserInputs(): Boolean {
        return when {
            TextUtils.isEmpty(binding.currentPasswordEt.text.toString().trim { it <= ' ' }) -> {
                binding.currentPasswordTil.isErrorEnabled = true
                binding.currentPasswordEt.error = "You need to enter your Full name"
                false
            }

            TextUtils.isEmpty(binding.newPasswordEt.text.toString().trim { it <= ' ' }) -> {
                binding.newPasswordTil.isErrorEnabled = true
                binding.newPasswordEt.error = "You need to enter your password"
                false
            }

            TextUtils.isEmpty(binding.confirmPasswordEt.text.toString().trim { it <= ' ' }) -> {
                binding.confirmPasswordTil.isErrorEnabled = true
                binding.confirmPasswordEt.error = "You need to enter your password"
                false
            }

            !TextUtils.equals(binding.newPasswordEt.text, binding.confirmPasswordEt.text) -> {
                binding.confirmPasswordTil.isErrorEnabled = true
                binding.newPasswordTil.isErrorEnabled = true
                showSnackbar("Password doesn't match!")
                false
            }

            else -> {
                binding.currentPasswordTil.isErrorEnabled = false
                binding.newPasswordTil.isErrorEnabled = false
                binding.confirmPasswordTil.isErrorEnabled = false
                true
            }
        }
    }

    private fun showSnackbar(msg: String) {
        val snackbar = Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            msg, Snackbar.LENGTH_SHORT
        )
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(
            ContextCompat.getColor(
                requireActivity(),
                R.color.main_btns_color
            )
        )
        snackbar.show()
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.profile_sensitive_setting_apply -> {
                if (validateUserInputs()) {
                    val oldPass = binding.currentPasswordEt.text.toString()
                    val newPass = binding.newPasswordEt.text.toString()
                    viewModel.safeChangePassword(oldPass, newPass, user.email)
                    viewModel.changePassResponse.observe(viewLifecycleOwner) { response ->
                        when (response) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(
                                    requireContext(),
                                    response.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), response.data, Toast.LENGTH_SHORT)
                                    .show()
                                val action =
                                    EditSensetiveFragmentDirections.actionEditSensetiveFragmentToNavigationMyProfile(
                                        user.id
                                    )
                                findNavController().navigate(action)
                            }
                        }
                    }
                }
            }

            R.id.profile_Setting_back_Btn -> {
                requireActivity().onBackPressed()
            }

            R.id.forget_password_btn -> {
                viewModel.safeSendEmail(user.email)
                viewModel.sendEmailResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                        is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), response.data, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

            }

        }
    }


}