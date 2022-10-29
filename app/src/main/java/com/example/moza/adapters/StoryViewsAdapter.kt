package com.example.moza.adapters

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.models.StoryView
import com.example.moza.models.User
import com.google.firebase.firestore.FirebaseFirestore

class StoryViewsAdapter(val views: MutableList<StoryView>, val context: Context?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.story_view_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = views[position]
        println("views number is: ${views.size}")
        var user = User()

        FirebaseFirestore.getInstance().collection("Users").document(view.uid!!)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user = task.result.toObject(User::class.java)!!
                    Glide.with(context!!)
                        .load(user.profilePicture)
                        .placeholder(R.drawable.profile_pic)
                        .into(holder.itemView.findViewById(R.id.storyview_profileImg_iv))

                    holder.itemView.findViewById<TextView>(R.id.view_story_username_tv).text =
                        user.fullName

                    val ago = DateUtils.getRelativeTimeSpanString(
                        view.time!!,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    if (ago == "0 minutes ago")
                        holder.itemView.findViewById<TextView>(R.id.story_view_time_tv).text = "Now"
                    else
                        holder.itemView.findViewById<TextView>(R.id.story_view_time_tv).text = ago


                } else {
                    Log.e(
                        "Getting user data in chat adapter: ",
                        task.exception!!.message.toString()
                    )
                }
            }


    }

    override fun getItemCount(): Int {
        return views.size
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)


}