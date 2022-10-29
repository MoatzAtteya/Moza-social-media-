package com.example.moza.fragments

import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.databinding.FragmentImageViewerBinding
import java.io.File
import java.io.FileOutputStream


class ImageViewerFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentImageViewerBinding
    private val args: ImageViewerFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImageViewerBinding.inflate(inflater, container, false)



        binding.apply {
            Glide.with(requireActivity())
                .load(args.message.messageUrl)
                .into(viewImageMessageIv)

            val ago = DateUtils.getRelativeTimeSpanString(
                args.message.time!!,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            messageSendDateTv.text = ago.toString()
            downloadImageIv.setOnClickListener(this@ImageViewerFragment)
            closeImageViewerIv.setOnClickListener(this@ImageViewerFragment)

        }

        return binding.root
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.download_image_iv -> {
                    binding.downloadImageIv.setImageResource(R.drawable.download_anim)
                    val avd = binding.downloadImageIv.drawable as AnimatedVectorDrawable
                    avd.start()

                    val bitmapDrawable = binding.viewImageMessageIv.drawable as BitmapDrawable
                    val bitmap = bitmapDrawable.bitmap

                    var outPutStream: FileOutputStream?
                    val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val dir = File(file.absolutePath + "/Moza")
                    dir.mkdir()

                    val fileName = String.format("%d.jpg", System.currentTimeMillis())
                    val outPut = File(dir, fileName)

                    try {
                        outPutStream = FileOutputStream(outPut)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outPutStream)
                        outPutStream.flush()
                        outPutStream.close()
                        Toast.makeText(requireContext(), "Image saved", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e("Saving picture: ", e.message.toString())
                        e.printStackTrace()
                    }
                }
                R.id.close_image_viewer_iv -> {
                    requireActivity().onBackPressed()
                }
            }
        }
    }


}