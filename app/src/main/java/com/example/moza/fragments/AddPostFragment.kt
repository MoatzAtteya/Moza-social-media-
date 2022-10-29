package com.example.moza.fragments

import android.Manifest
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.moza.R
import com.example.moza.adapters.GalleryAdapter
import com.example.moza.databinding.FragmentAddPostBinding
import com.example.moza.models.GalleryImages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import id.zelory.compressor.Compressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class AddPostFragment : Fragment() {


    private lateinit var binding: FragmentAddPostBinding
    private lateinit var adapter: GalleryAdapter
    private lateinit var imagesList: ArrayList<GalleryImages>
    private lateinit var imageUri: Uri
    private lateinit var cropImage: ActivityResultLauncher<CropImageContractOptions>
    private lateinit var firebaseUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddPostBinding.inflate(layoutInflater, container, false)
        requireActivity().setActionBar(((binding.addPostCustomToolbar)))

        imagesList = ArrayList()
        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        cropImage = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                val uriContent = result.uriContent
                val description = binding.addPostAboutEt.text.toString()

                val action = AddPostFragmentDirections.actionNavigationAddPostToConfirmPostFragment(
                    uriContent.toString(),
                    description
                )
                 findNavController().navigate(action)

            } else {
                Toast.makeText(requireContext(), "error while cropping", Toast.LENGTH_SHORT).show()
            }
        }

        adapter = GalleryAdapter(imagesList)
        binding.addPostAllImagesRv.setHasFixedSize(true)
        binding.addPostAllImagesRv.layoutManager = GridLayoutManager(requireContext(), 3)

        clickListener()

        binding.openGalleryBtn.setOnClickListener {
            pickImgFromGallery.launch(
                options {
                    setGuidelines(CropImageView.Guidelines.ON)
                    setCropShape(CropImageView.CropShape.RECTANGLE)
                    setOutputCompressQuality(30)
                })
        }

        return binding.root
    }

    private val pickImgFromGallery = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // use the returned uri
            val uriContent = result.uriContent
            imageUri = uriContent!!
            Glide.with(requireContext())
                .load(uriContent)
                .placeholder(R.drawable.login_image)
                .into(binding.addPostImageIv)

            binding.addPostImageIv.visibility = View.VISIBLE
            binding.addPostNextBtn.visibility = View.VISIBLE

        } else {
            // an error occurred
            val exception = result.error
            Log.e("Error while cropping image: ", exception!!.message.toString())
        }
    }


    private fun clickListener() {
        adapter.sendImage(object : GalleryAdapter.SendImage {
            override fun onSend(uri: Uri?) {

                CoroutineScope(Dispatchers.Main).launch {
                    val compressedImageFile = Compressor.compress(requireContext(),File(uri!!.path!!))
                    imageUri = Uri.fromFile(compressedImageFile)
                    Glide.with(requireContext())
                        .load(imageUri)
                        .placeholder(R.drawable.login_image)
                        .into(binding.addPostImageIv)

                    binding.addPostImageIv.visibility = View.VISIBLE
                    binding.addPostNextBtn.visibility = View.VISIBLE
                }



            }
        })

        binding.addPostNextBtn.setOnClickListener {
            cropImage.launch(
                options(uri = imageUri) {
                    setGuidelines(CropImageView.Guidelines.ON)
                    setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        requireActivity().runOnUiThread {
            Dexter.withContext(requireContext())
                .withPermissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        adapter.updateAdapter(fetchImages())
                        binding.addPostAllImagesRv.adapter = adapter
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        p1!!.continuePermissionRequest()
                    }

                }).check()
        }

    }

    fun fetchImages(): ArrayList<GalleryImages> {
        val imageList: ArrayList<String> = ArrayList()
        val imageListUri : ArrayList<GalleryImages> = ArrayList()
        val columns = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID)
        val imagecursor: Cursor = requireActivity().managedQuery(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
            null, ""
        )
        for (i in 0 until imagecursor.count) {
            imagecursor.moveToPosition(i)
            val dataColumnIndex =
                imagecursor.getColumnIndex(MediaStore.Images.Media.DATA)
            imageList.add(imagecursor.getString(dataColumnIndex))
            println("image $i is: ${imagecursor.getString(dataColumnIndex)}")
            //imageListUri.add(GalleryImages(Uri.parse(imagecursor.getString(dataColumnIndex))))
            imageListUri.add(GalleryImages(Uri.fromFile(File(imagecursor.getString(dataColumnIndex)))))
        }
        return imageListUri
    }

}