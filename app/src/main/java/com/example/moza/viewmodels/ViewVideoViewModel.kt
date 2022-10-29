package com.example.moza.viewmodels

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ViewVideoViewModel @Inject constructor(@ApplicationContext val context: Context)  : ViewModel() {


    fun downloadVideo(url : String) {
        val fileName = "${System.currentTimeMillis()}.mp4"
        val request = DownloadManager.Request(Uri.parse(url))
        request.apply {
            setTitle(fileName)
            setDescription("$fileName is Downloading...")
            allowScanningByMediaScanner()
            setAllowedOverMetered(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context , "$fileName is Downloading..." , Toast.LENGTH_SHORT).show()
    }

}