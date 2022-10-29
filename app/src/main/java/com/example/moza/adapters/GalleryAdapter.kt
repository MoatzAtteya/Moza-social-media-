package com.example.moza.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.models.GalleryImages

class GalleryAdapter(
    var list : ArrayList<GalleryImages>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    lateinit var onSendImage : SendImage

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.add_post_images_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val image = list[position]

        Glide.with(holder.itemView.context.applicationContext)
            .load(image.uri)
            .centerCrop()
            .placeholder(R.drawable.login_image)
            .into(holder.itemView.findViewById(R.id.add_posts_image_iv))

        holder.itemView.setOnClickListener {
            chooseImage(image.uri)
        }
    }

    private fun chooseImage(uri: Uri?) {
        onSendImage.onSend(uri)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)


    public interface SendImage{
        fun onSend(uri: Uri?)
    }

    public fun sendImage(sendImage : SendImage){
        this.onSendImage = sendImage
    }

    fun updateAdapter(list: ArrayList<GalleryImages>) {
        this.list = list
        notifyDataSetChanged()
    }

}

