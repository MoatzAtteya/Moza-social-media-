package com.example.moza.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.moza.R
import com.example.moza.databinding.FragmentForgotPasswordBinding
import com.example.moza.common.Resource
import com.example.moza.viewmodels.ForgotPasswordViewModel

class ForgotPasswordFragment : Fragment() {


    private lateinit var viewModel: ForgotPasswordViewModel
    private lateinit var binding: FragmentForgotPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this).get(ForgotPasswordViewModel::class.java)


        binding.forgetPassSendEmailBtn.setOnClickListener {
            if (binding.emailEt.text!!.isEmpty()) {
                binding.forgetpassEmailTil.isErrorEnabled = true
                binding.emailEt.error = "Please enter your email"
            } else {
                binding.forgetpassEmailTil.isErrorEnabled = false
                viewModel.resetPasswordViaEmail(binding.emailEt.text.toString())
                viewModel.resetPasswordResponse.observe(viewLifecycleOwner, Observer { response ->
                    when (response) {
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), response.data, Toast.LENGTH_SHORT)
                                .show()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                    }
                })
            }


        }

        binding.backImg.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordFragment_to_loginFragment)
        }

        return binding.root
    }


}