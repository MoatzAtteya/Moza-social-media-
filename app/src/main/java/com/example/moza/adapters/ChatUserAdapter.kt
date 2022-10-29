package com.example.moza.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.databinding.ChatItemBinding
import com.example.moza.databinding.GroupChatItemBinding
import com.example.moza.fragments.MassagesFragment
import com.example.moza.models.ChatUser
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatUserAdapter(val massagesFragment: MassagesFragment) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var chats: MutableList<ChatUser> = mutableListOf()

    private lateinit var mListener: OnItemClickListener
    private var firebaseUser = FirebaseAuth.getInstance().currentUser

    interface OnItemClickListener {
        fun onItemClick(
            position: Int,
            chatID: String,
            id: ArrayList<String>,
            unReadSenderID: String,
            isGroup: Boolean
        )
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            return MyViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.chat_item,
                        parent,
                        false
                    )
            )
        } else {
            return GroupChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.group_chat_item,
                        parent,
                        false
                    )
            )
        }

    }

    override fun getItemViewType(position: Int): Int {
        return when (chats[position].isGroupChat) {
            false -> 0
            true -> 1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chats[position]
        if (holder is MyViewHolder) {
            if (!chat.hideIDS.contains(firebaseUser!!.uid) && !chat.deleteIDS.contains(firebaseUser!!.uid)) {

                getOppositeUserData(chat.uid, holder)
                holder.binding.chatItemLayout.visibility = View.VISIBLE
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


                if (chat.lastMessage == Constants.DELETED_MESSAGE_CODE) {
                    holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv).text =
                        "Deleted message"
                    holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv)
                        .setTypeface(null, Typeface.BOLD_ITALIC)
                    holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv)
                        .setTextColor(Color.parseColor("#BFBFBF"))
                    holder.itemView.findViewById<ImageView>(R.id.deleted_message_iv).visibility =
                        View.VISIBLE

                } else {
                    holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv).text =
                        chat.lastMessage
                    holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv)
                        .setTypeface(null, Typeface.NORMAL)
                    holder.itemView.findViewById<ImageView>(R.id.deleted_message_iv).visibility =
                        View.GONE
                    holder.itemView.findViewById<TextView>(R.id.chat_last_message_tv)
                        .setTextColor(Color.parseColor("#989898"))

                }

                if (chat.deleteRequest.isEmpty()) {
                    holder.itemView.findViewById<TextView>(R.id.chat_delete_request_tv).visibility =
                        View.GONE
                } else {
                    if (chat.deleteRequest != firebaseUser!!.uid) {
                        holder.itemView.findViewById<TextView>(R.id.chat_delete_request_tv).visibility =
                            View.VISIBLE
                    }
                }

                holder.itemView.setOnClickListener {
                    mListener.onItemClick(position, chat.id!!, chat.uid, chat.unReadSender, false)
                }
            } else {
                holder.itemView.visibility = View.GONE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            }


        } else if (holder is GroupChatViewHolder) {
            if (!chat.hideIDS.contains(firebaseUser!!.uid) && !chat.deleteIDS.contains(firebaseUser!!.uid)) {
                holder.binding.apply {
                    chatItemLayout.visibility = View.VISIBLE
                    fetchGroupUI(chat, holder)
                    val ago = DateUtils.getRelativeTimeSpanString(
                        chat.timeStamp!!,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                    if (ago == "0 minutes ago")
                        chatTimeTv.text = "Now"
                    else
                        chatTimeTv.text = ago.toString()

                    if (chat.unReadCounter != 0) {
                        if (chat.unReadSender != firebaseUser!!.uid) {
                            messagesCounterCv.visibility =
                                View.VISIBLE
                            messagesCounterTv.text =
                                chat.unReadCounter.toString()
                        }
                    } else
                        messagesCounterCv.visibility = View.GONE

                    chatLastMessageTv.text = chat.lastMessage

                    if (chat.lastMessage == Constants.DELETED_MESSAGE_CODE) {
                        chatLastMessageTv.text = "Deleted message"
                        chatLastMessageTv.setTypeface(null, Typeface.BOLD_ITALIC)
                        chatLastMessageTv.setTextColor(Color.parseColor("#BFBFBF"))
                        groupDeletedMessageIv.visibility = View.VISIBLE

                    } else {
                        chatLastMessageTv.text = chat.lastMessage
                        chatLastMessageTv.setTypeface(null, Typeface.NORMAL)
                        groupDeletedMessageIv.visibility = View.GONE
                        chatLastMessageTv.setTextColor(Color.parseColor("#989898"))

                    }
                }

                holder.itemView.setOnClickListener {
                    mListener.onItemClick(position, chat.id!!, chat.uid, chat.unReadSender, true)
                }

            } else {
                holder.itemView.visibility = View.GONE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            }
        }
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    private fun getOppositeUserData(idList: ArrayList<String>, holder: RecyclerView.ViewHolder) {
        var oppositeUID = ""
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        oppositeUID = if (!idList[0].equals(firebaseUser!!.uid, ignoreCase = true))
            idList[0]
        else
            idList[1]

        val userDocument = FirebaseFirestore.getInstance().collection("Users").document(oppositeUID)
        GlobalScope.launch(Dispatchers.IO) {
            userDocument.addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value == null) {
                    return@addSnapshotListener
                }
                val user = value.toObject(User::class.java)
                fetchChatUi(user, holder)
            }
        }
    }

    private fun fetchChatUi(user: User?, holder: RecyclerView.ViewHolder) {
        GlobalScope.launch(Dispatchers.Main) {
            Glide.with(massagesFragment.requireContext())
                .load(user!!.profilePicture)
                .placeholder(R.drawable.profile_pic)
                .into(holder.itemView.findViewById(R.id.chat_profile_iv))
            holder.itemView.findViewById<TextView>(R.id.chat_username_tv).text =
                user.fullName
            if (user.online!!)
                holder.itemView.findViewById<CardView>(R.id.online_green_dot).visibility =
                    View.VISIBLE
            else
                holder.itemView.findViewById<CardView>(R.id.online_green_dot).visibility = View.GONE
        }
    }

    private fun fetchGroupUI(chat: ChatUser, holder: GroupChatViewHolder) {
        val chatDocument =
            FirebaseFirestore.getInstance().collection("Messages").document(chat.id!!)
        GlobalScope.launch(Dispatchers.IO) {
            val chat = chatDocument.get().await().toObject(ChatUser::class.java)
            withContext(Dispatchers.Main) {
                holder.binding.chatUsernameTv.text = chat!!.groupTitle
                Glide.with(massagesFragment.requireContext())
                    .load(chat.groupProfileImg)
                    .placeholder(R.drawable.profile_pic)
                    .into(holder.binding.chatProfileIv)
            }
        }
    }

    fun updateChatList(list: MutableList<ChatUser>) {
        this.chats = list
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ChatItemBinding.bind(view)

    }

    private class GroupChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = GroupChatItemBinding.bind(view)

    }
}