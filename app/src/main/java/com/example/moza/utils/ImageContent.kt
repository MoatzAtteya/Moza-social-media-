package com.example.moza.utils

import android.net.Uri
import com.example.moza.models.GalleryImages
import java.io.File

 class ImageContent {

    var list: ArrayList<GalleryImages> = ArrayList()

    public fun loadImages(file: File) {
        var galleryImages = GalleryImages()
        galleryImages.uri = Uri.fromFile(file)
        addImages(galleryImages)
    }

    private fun addImages(galleryImages: GalleryImages) {
        list.add(0, galleryImages)
    }

    public fun loadSavedImages(directory: File) {
        list.clear()

        if (directory.exists()) {
            var files = directory.listFiles()

            for (file in files) {
                var absolutePath = file.absolutePath
                var extension = absolutePath.substring(absolutePath.indexOf("."))

                if (extension.equals(".jph") || extension.equals(".png")) {
                    loadImages(file)
                }

            }

        }

    }

    companion object {

    }

}