package com.example.moza.adapters

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.databinding.ChatItemBinding
import com.example.moza.fragments.HiddenMessagesFragment
import com.example.moza.models.ChatUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HiddenChatAdapter(val fragment: HiddenMessagesFragment) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var chats: MutableList<ChatUser> = mutableListOf()
    private var firebaseUser = FirebaseAuth.getInstance().currentUser
    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(position: Int, chatID: String, id: ArrayList<String>, unReadSenderID: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.chat_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chats[position]
        if (holder is MyViewHolder) {
            holder.binding.chatItemLayout.visibility = View.VISIBLE
            fetchImageUrl(chat.uid, holder)
            val ago = DateUtils.getRelativeTimeSpanString(
                chat.timeStamp!!,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            if (ago == "0 minutes ago")
                holder.itemView.findViewById<TextView>(R.id.chat_time_tv).text = "Now"
            else
                holder.itemView.findViewById<TextView>(R.id.chat_time_tv).text =
                    ago.toString()

            holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv).text =
                chat.lastMessage
            if (chat.unReadCounter != 0) {
                if (chat.unReadSender != firebaseUser!!.uid) {

                    holder.itemView.findViewById<CardView>(R.id.messages_counter_cv).visibility =
                        View.VISIBLE
                    holder.itemView.findViewById<TextView>(R.id.messages_counter_tv).text =
                        chat.unReadCounter.toString()
                }
            } else
                holder.itemView.findViewById<CardView>(R.id.messages_counter_cv).visibility =
                    View.GONE


            if (chat.deleteRequest.isNullOrEmpty()) {
                holder.itemView.findViewById<TextView>(R.id.chat_delete_request_tv).visibility =
                    View.GONE

            } else {
                if (chat.deleteRequest != firebaseUser!!.uid) {
                    holder.itemView.findViewById<TextView>(R.id.chat_delete_request_tv).visibility =
                        View.VISIBLE

                }
            }

            holder.itemView.setOnClickListener {
                mListener.onItemClick(position, chat.id!!, chat.uid ,chat.unReadSender)
            }
        }
    }

    override fun getItemCount(): Int {
        return chats.size
    }


    fun updateChatList(list: MutableList<ChatUser>) {
        this.chats = list
    }

    private fun fetchImageUrl(idList: ArrayList<String>, holder: RecyclerView.ViewHolder) {
        var oppositeUID = ""
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        oppositeUID = if (!idList[0].equals(firebaseUser!!.uid, ignoreCase = true))
            idList[0]
        else
            idList[1]

        FirebaseFirestore.getInstance().collection("Users").document(oppositeUID)
            .get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    Glide.with(fragment.requireContext())
                        .load(snapshot.getString("profilePicture"))
                        .into(holder.itemView.findViewById(R.id.chat_profile_iv))

                    holder.itemView.findViewById<TextView>(R.id.chat_username_tv).text =
                        snapshot.getString("fullName")

                } else {
                    Log.e(
                        "Getting user data in chat adapter: ",
                        task.exception!!.message.toString()
                    )
                }
            }

    }


    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ChatItemBinding.bind(view)

    }
}