package com.example.moza.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseAuth

class UsersAdapter(
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

     var list: MutableList<User> = mutableListOf()
    private val firebaseUser = FirebaseAuth.getInstance().currentUser
    private var isSearchHistory = false
    private var isSendMessageEnabled = false

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int, id: String)
        fun onRemoveClick(position: Int, id: String)
        fun onSendClick(position: Int, id: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.profile_search_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val user = list[position]
        if (holder is MyViewHolder) {

            if (user.id == firebaseUser!!.uid) {
                holder.itemView.visibility =
                    View.GONE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)

            } else {
                holder.itemView.findViewById<AppCompatButton>(R.id.send_message_btn).visibility =
                    View.GONE
                holder.itemView.findViewById<TextView>(R.id.search_username_tv).text = user.fullName
                holder.itemView.findViewById<TextView>(R.id.search_status_tv).text = user.status
                Glide.with(holder.itemView.context.applicationContext)
                    .load(user.profilePicture)
                    .placeholder(R.drawable.profile_pic)
                    .timeout(6500)
                    .into(holder.itemView.findViewById(R.id.search_profileImg_iv))

                holder.itemView.setOnClickListener {
                    mListener.onItemClick(position, user.id)
                }
                if (isSearchHistory) {
                    holder.itemView.findViewById<ImageView>(R.id.search_remove_profile_history).visibility =
                        View.VISIBLE
                    holder.itemView.findViewById<ImageView>(R.id.search_remove_profile_history)
                        .setOnClickListener {
                            mListener.onRemoveClick(position,user.id)
                        }

                }
                if (isSendMessageEnabled){
                    holder.itemView.findViewById<AppCompatButton>(R.id.send_message_btn).visibility =
                        View.VISIBLE
                    holder.itemView.findViewById<AppCompatButton>(R.id.send_message_btn).setOnClickListener {
                        mListener.onSendClick(position , user.id)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateList(list: MutableList<User>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun updateSearchHistoryBool(boolean: Boolean) {
        this.isSearchHistory = boolean
        notifyDataSetChanged()
    }

    fun enableSendMessageBtn(boolean: Boolean){
        this.isSendMessageEnabled = boolean
        notifyDataSetChanged()
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)


}