package com.example.moza.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.moza.R
import com.example.moza.databinding.FragmentConfirmPostBinding
import com.example.moza.models.PostImage
import com.example.moza.common.Resource
import com.example.moza.viewmodels.ConfirmPostViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmPostFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentConfirmPostBinding
    private lateinit var viewModel: ConfirmPostViewModel
    private lateinit var imageUri: Uri
    private lateinit var description: String
    private lateinit var firebaseUser: FirebaseUser
    val args: ConfirmPostFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConfirmPostBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ConfirmPostViewModel::class.java)

        description = args.description
        imageUri = Uri.parse(args.imageUri)
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        binding.confirmPostDescriptionTv.text = description
        binding.confirmPostImageIv.setImageURI(imageUri)
        binding.confirmPostNextBtn.setOnClickListener(this)



        return binding.root
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.confirm_post_next_btn -> {

                val postImage = PostImage(
                    null,
                    null,
                    null,
                    description,
                    firebaseUser.photoUrl.toString(),
                    firebaseUser.displayName,
                    ArrayList(),
                    firebaseUser.uid
                )



                viewModel.storeImageToStorage( description, imageUri, postImage)

                viewModel.storeImageToStorageResponse.observe(
                    viewLifecycleOwner,
                    Observer { response ->
                        when (response) {
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(
                                    requireContext(),
                                    response.data.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                findNavController().navigate(R.id.action_confirmPostFragment_to_navigation_my_profile)
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
    }



}