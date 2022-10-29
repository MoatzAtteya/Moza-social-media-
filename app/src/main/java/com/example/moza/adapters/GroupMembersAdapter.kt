package com.example.moza.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.databinding.GroupMemberItemBinding
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class GroupMembersAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list: MutableList<User> = mutableListOf()
    private var adminID = ""

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseUserEntryPoint {
        fun provideFireBaseUser(): FirebaseUser
    }

    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onRemoveCLick(
            position: Int,
            userId: String
        )
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.group_member_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var user = list[position]
        if (holder is MyViewHolder) {

            val hiltEntryPoint =
                EntryPointAccessors.fromApplication(context, FirebaseUserEntryPoint::class.java)
            var firebaseUser = hiltEntryPoint.provideFireBaseUser()


            holder.binding.apply {
                memberUsernameTv.text = user.fullName
                Glide.with(holder.itemView.context.applicationContext)
                    .load(user.profilePicture)
                    .placeholder(R.drawable.profile_pic)
                    .timeout(6500)
                    .into(memberProfileIv)

                //make the remove_member button only available to the admin.
                if (adminID == firebaseUser.uid) {
                    if (user.id == firebaseUser.uid)
                        removeMemberBtn.visibility = View.GONE
                    else
                        removeMemberBtn.visibility = View.VISIBLE
                } else
                    removeMemberBtn.visibility = View.VISIBLE

                removeMemberBtn.setOnClickListener {
                    mListener.onRemoveCLick(position, user.id)
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

    fun setAdminID(id: String) {
        this.adminID = id
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var binding = GroupMemberItemBinding.bind(view)
    }

}