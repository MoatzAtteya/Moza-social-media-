package com.example.moza.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.fragments.LikesFragment
import com.example.moza.models.Notification
import com.example.moza.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LikesAdapter(val fragment: LikesFragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var notifications: MutableList<Notification> = mutableListOf()

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int, uid: String, PostID: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.notification_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notification = notifications[position]
        if (holder is MyViewHolder) {
            fetchUserImage(notification.uid, holder)

            Glide.with(fragment.requireActivity())
                .load(notification.postUrl)
                .into(holder.itemView.findViewById(R.id.notification_post_iv))


            if (notification.type == "like") {
                holder.itemView.findViewById<TextView>(R.id.notification_username_tv).text =
                    "${notification.username} liked your photo."
            } else if (notification.type == Constants.NOTIFICATION_COMMENT_TYPE) {
                holder.itemView.findViewById<TextView>(R.id.notification_username_tv).text =
                    "${notification.username} commented on your photo."
            }


            val ago = DateUtils.getRelativeTimeSpanString(
                notification.time!!,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            if (ago == "0 minutes ago")
                holder.itemView.findViewById<TextView>(R.id.notification_time_tv).text = "Now"
            else
                holder.itemView.findViewById<TextView>(R.id.notification_time_tv).text =
                    ago.toString()

            holder.itemView.setOnClickListener {
                mListener.onItemClick(position, notification.uid, notification.postID)
            }

        }
    }

    private fun fetchUserImage(uid: String, holder: MyViewHolder) {
        val userDocument = FirebaseFirestore.getInstance().collection("Users").document(uid)
        GlobalScope.launch(Dispatchers.IO) {
            val user = userDocument.get().await().toObject(User::class.java)
            withContext(Dispatchers.Main) {
                Glide.with(fragment.requireActivity())
                    .load(user!!.profilePicture)
                    .placeholder(R.drawable.profile_pic)
                    .into(holder.itemView.findViewById(R.id.notification_user_iv))
            }
        }
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun updateList(list: MutableList<Notification>) {
        this.notifications = list
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

}