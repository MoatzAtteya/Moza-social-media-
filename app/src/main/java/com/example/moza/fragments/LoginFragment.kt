package com.example.moza.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.moza.R
import com.example.moza.activities.HomeActivity
import com.example.moza.common.Resource
import com.example.moza.databinding.FragmentLoginBinding
import com.example.moza.models.User
import com.example.moza.viewmodels.LoginInViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginFragment : Fragment(), View.OnClickListener {

    lateinit var binding: FragmentLoginBinding
    lateinit var viewModel: LoginInViewModel
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        viewModel =
            ViewModelProvider(requireActivity()).get(LoginInViewModel::class.java)

        auth = FirebaseAuth.getInstance()

        binding.signInButton.setOnClickListener(this)
        binding.forgetPasswordTv.setOnClickListener(this)
        binding.backImg.setOnClickListener(this)
        binding.googleSignInButton.setOnClickListener(this)


        return binding.root
    }


    private fun validateUserInputs(): Boolean {
        return when {

            TextUtils.isEmpty(binding.emailEt.text.toString().trim { it <= ' ' }) -> {
                binding.emailTil.isErrorEnabled = true
                binding.emailEt.error = "You need to enter your email"
                false
            }

            TextUtils.isEmpty(binding.passwordEt.text.toString().trim { it <= ' ' }) -> {
                binding.passwordTil.isErrorEnabled = true
                binding.passwordEt.error = "You need to enter your password"
                false
            }


            else -> {
                binding.emailTil.isErrorEnabled = false
                binding.passwordTil.isErrorEnabled = false
                true
            }
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.sign_in_button -> {
                    if (validateUserInputs()) {
                        viewModel.firebaseUserLogin(
                            binding.emailEt.text.toString(),
                            binding.passwordEt.text.toString()
                        )
                        viewModel.loginFirebaseResponse.observe(
                            viewLifecycleOwner,
                            Observer { response ->
                                when (response) {
                                    is Resource.Success -> {
                                        binding.progressBar.visibility = View.GONE

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
                                            "Please enter valid inputs!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // viewModel.loginFirebaseResponse.removeObservers(viewLifecycleOwner)
                                    }
                                    is Resource.Loading -> {
                                        binding.progressBar.visibility = View.VISIBLE
                                    }
                                }

                            })
                    }
                }
                R.id.forget_password_tv -> {
                    findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
                }
                R.id.back_img -> {
                    findNavController().navigate(R.id.action_loginFragment_to_welcomeFragment)
                }
                R.id.google_sign_in_button -> {
                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.webClient_id))
                        .requestEmail()
                        .build()
                    val signInClient = GoogleSignIn.getClient(requireContext(), options)
                    googleSignIn(signInClient)
                }
            }
        }
    }

    private fun googleSignIn(signInClient: GoogleSignInClient) {
        val signInIntent = signInClient.signInIntent
        startActivityForResult(signInIntent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                println("auth with google with id: ${account.id}")
                viewModel.googleSignIn(account.idToken!!)
                viewModel.googleSignInResponse.observe(viewLifecycleOwner) { response ->
                    when (response) {
                        is Resource.Error -> {
                            Toast.makeText(
                                requireContext(),
                                "Something went wrong.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("Auth with google: ", response.message!!)
                        }
                        is Resource.Loading -> TODO()
                        is Resource.Success -> {
                            createGoogleAccount(response.data!!)
                        }
                    }
                }
            } catch (e: ApiException) {
                Log.e("Auth with google: ", e.message.toString())
            }
        }
    }

    private fun createGoogleAccount(account: FirebaseUser) {
        val user = User(
            account.displayName.toString(),
            "",
            account.email.toString(),
            account.photoUrl.toString(),
            account.uid,
            ArrayList(),
            ArrayList(),
            "",
        )
        viewModel.createAccount(user)
        viewModel.createAccountResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Something went wrong.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("Auth with google: ", response.message!!)
                }
                is Resource.Loading -> TODO()
                is Resource.Success -> {
                    startActivity(
                        Intent(
                            requireActivity().applicationContext,
                            HomeActivity::class.java
                        )
                    )
                    Toast.makeText(requireContext(), "Logged in successfully.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


}