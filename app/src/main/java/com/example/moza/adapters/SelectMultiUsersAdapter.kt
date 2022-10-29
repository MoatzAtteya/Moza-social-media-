package com.example.moza.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.databinding.ProfileUserSelectItemBinding
import com.example.moza.models.User

class SelectMultiUsersAdapter : RecyclerView.Adapter<ViewHolder>() {

    private var list: MutableList<User> = mutableListOf()


    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onAddClick(position: Int, id: String)
        fun onRemoveClick(position: Int, id: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.profile_user_select_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var user = list[position]
        if (holder is MyViewHolder) {

            holder.binding.apply {
                removeUserBtn.visibility = View.GONE

                selectUserUsernameTv.text = user.fullName
                Glide.with(holder.itemView.context.applicationContext)
                    .load(user.profilePicture)
                    .placeholder(R.drawable.profile_pic)
                    .timeout(6500)
                    .into(selectUserProfileImgIv)

                addUserBtn.setOnClickListener {
                    mListener.onAddClick(position, user.id)
                    addUserBtn.visibility = View.GONE
                    removeUserBtn.visibility = View.VISIBLE
                }

                removeUserBtn.setOnClickListener {
                    mListener.onRemoveClick(position , user.id)
                    addUserBtn.visibility = View.VISIBLE
                    removeUserBtn.visibility = View.GONE
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


    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = ProfileUserSelectItemBinding.bind(view)
    }

}