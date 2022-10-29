package com.example.moza.adapters

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.common.FireBaseRepository
import com.example.moza.models.Comment
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class CommentsAdapter(val fragment: Fragment) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var commentList: ArrayList<Comment> = ArrayList()

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onUsernameClick(position: Int, uid: String)
        fun onDeleteClick(position: Int, uid: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.mListener = listener
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseUserEntryPoint {
        fun provideFireBaseUser(): FirebaseUser
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.comment_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = commentList[position]
        if (holder is MyViewHolder) {
            val hiltEntryPoint =
                EntryPointAccessors.fromApplication(
                    fragment.requireContext(),
                    FirebaseUserEntryPoint::class.java
                )
            val firebaseUser = hiltEntryPoint.provideFireBaseUser()

            if (post.uid == firebaseUser.uid)
                holder.itemView.findViewById<ImageView>(R.id.delete_comment_btn).visibility =
                    View.VISIBLE
            else
                holder.itemView.findViewById<ImageView>(R.id.delete_comment_btn).visibility =
                    View.GONE

            val repo = FireBaseRepository()
            val response2 = repo.getUserDataLive(post.uid!!)
            response2.addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(
                        "Error while getting user data in CommentAdapter",
                        error.message.toString()
                    )
                    return@addSnapshotListener

                }
                if (value == null) {
                    Log.e("Error while getting user data in CommentAdapter", "User data is empty!")
                    return@addSnapshotListener
                }
                if (value.exists()) {
                    val user = value.toObject(User::class.java)
                    val ago = DateUtils.getRelativeTimeSpanString(
                        post.timeStamp!!,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    if (ago == "0 minutes ago")
                        holder.itemView.findViewById<TextView>(R.id.all_comment_item_date).text =
                            "Now"
                    else
                        holder.itemView.findViewById<TextView>(R.id.all_comment_item_date).text =
                            ago

                    Glide.with(fragment)
                        .load(user!!.profilePicture)
                        .placeholder(R.drawable.profile_pic)
                        .timeout(6500)
                        .into(holder.itemView.findViewById(R.id.all_comment_item_profilePic))
                    holder.itemView.findViewById<TextView>(R.id.all_comment_item_username).text =
                        user.fullName
                    holder.itemView.findViewById<TextView>(R.id.all_comment_item_comment).text =
                        post.comment.toString()

                    holder.itemView.findViewById<TextView>(R.id.all_comment_item_username)
                        .setOnClickListener {
                            mListener.onUsernameClick(position, post.uid!!)
                        }
                    holder.itemView.findViewById<ImageView>(R.id.delete_comment_btn).setOnClickListener{
                        mListener.onDeleteClick(position,post.commentID!!)
                    }

                }
            }

        }

    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    fun updateList(list: ArrayList<Comment>) {
        this.commentList = list
        notifyDataSetChanged()
    }


    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

}