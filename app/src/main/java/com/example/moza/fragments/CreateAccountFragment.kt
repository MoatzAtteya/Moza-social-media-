package com.example.moza.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.moza.R
import com.example.moza.activities.HomeActivity
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentCreateAccountBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.CreateAccountFragmentVM
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateAccountFragment : Fragment() {

    lateinit var binding: FragmentCreateAccountBinding
    lateinit var viewModel: CreateAccountFragmentVM

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCreateAccountBinding.inflate(inflater, container, false)

        viewModel =
            ViewModelProvider(this).get(CreateAccountFragmentVM::class.java)

        var followers = ArrayList<String>()
        var following = ArrayList<String>()

        binding.createAccountBtn.setOnClickListener {
            if (validateUserInputs()) {
                val user = User(
                    binding.fullNameEt.text.toString(),
                    "",
                    binding.emailAddressEt.text.toString(),
                    "",
                    "",
                    followers,
                    following,
                    "",
                )
                viewModel.createAccount(user, binding.passwordEt.text.toString())
                viewModel.createAccountResponse.observe(viewLifecycleOwner, Observer { response ->
                    when (response) {
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Your account has been created.",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(
                                Intent(
                                    requireActivity().applicationContext,
                                    HomeActivity::class.java
                                )
                            )
                            requireActivity().finish()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                response.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                    }


                })
            }
        }

        binding.backImg.setOnClickListener {
            findNavController().navigate(R.id.action_createAccountFragment_to_welcomeFragment)
        }
        return (binding.root)
    }


    private fun validateUserInputs(): Boolean {
        return when {
            TextUtils.isEmpty(binding.fullNameEt.text.toString().trim { it <= ' ' }) -> {
                binding.fullNameTil.isErrorEnabled = true
                binding.fullNameEt.error = "You need to enter your Full name"
                false
            }

            TextUtils.isEmpty(binding.emailAddressEt.text.toString().trim { it <= ' ' }) -> {
                binding.emailAddressTil.isErrorEnabled = true
                binding.emailAddressEt.error = "You need to enter your email"
                false
            }

            TextUtils.isEmpty(binding.passwordEt.text.toString().trim { it <= ' ' }) -> {
                binding.passwordTil.isErrorEnabled = true
                binding.passwordEt.error = "You need to enter your password"
                false
            }

            TextUtils.isEmpty(binding.conPasswordEt.text.toString().trim { it <= ' ' }) -> {
                binding.conPasswordTil.isErrorEnabled = true
                binding.conPasswordEt.error = "You need to enter your password"
                false
            }

            !TextUtils.equals(binding.passwordEt.text, binding.conPasswordEt.text) -> {
                binding.conPasswordTil.isErrorEnabled = true
                binding.passwordTil.isErrorEnabled = true
                showSnackbar("Password doesn't match!")
                false
            }

            else -> {
                binding.fullNameTil.isErrorEnabled = false
                binding.emailAddressTil.isErrorEnabled = false
                binding.passwordTil.isErrorEnabled = false
                binding.conPasswordTil.isErrorEnabled = false
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

    companion object {
        val EMAIL_REGEX = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$"
    }
}