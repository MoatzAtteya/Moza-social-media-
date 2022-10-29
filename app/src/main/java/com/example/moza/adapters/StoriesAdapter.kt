package com.example.moza.adapters

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.activities.ViewStoryActivity
import com.example.moza.common.Constants
import com.example.moza.models.Story
import com.example.moza.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mikhaellopez.circularimageview.CircularImageView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StoriesAdapter(val fragment: Fragment) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var stories = listOf<Story>()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseUserEntryPoint {
        fun provideFireBaseUser(): FirebaseUser
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.story_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val story = stories[position]
        if (holder is MyViewHolder) {

            if (story.uid == firebaseUser!!.uid) {
                val circleImageView = holder.itemView.findViewById<CircularImageView>(R.id.story_iv)
                circleImageView.borderColor = Color.GRAY
                circleImageView.borderWidth = 1F

                holder.itemView.findViewById<ImageView>(R.id.story_add_iv).visibility =
                    View.GONE
                holder.itemView.findViewById<TextView>(R.id.story_username_tv).text =
                    "Your Story"
                Glide.with(fragment)
                    .load(story.profileIMGURL)
                    .placeholder(R.drawable.profile_pic)
                    .timeout(6500)
                    .into(holder.itemView.findViewById(R.id.story_iv))
                holder.itemView.findViewById<CircularImageView>(R.id.story_iv)
                    .setOnClickListener {
                        val intent =
                            Intent(fragment.requireActivity(), ViewStoryActivity::class.java)
                        intent.putExtra(Constants.USER_STORY_KEY, story)
                        fragment.requireActivity().startActivity(intent)
                    }
            } else {
                holder.itemView.findViewById<ImageView>(R.id.story_add_iv).visibility =
                    View.GONE
                holder.itemView.findViewById<TextView>(R.id.story_username_tv).text =
                    story.username
                Glide.with(fragment)
                    .load(story.profileIMGURL)
                    .placeholder(R.drawable.profile_pic)
                    .timeout(6500)
                    .into(holder.itemView.findViewById(R.id.story_iv))
                holder.itemView.findViewById<CircularImageView>(R.id.story_iv)
                    .setOnClickListener {
                        val intent =
                            Intent(fragment.requireActivity(), ViewStoryActivity::class.java)
                        intent.putExtra(Constants.USER_STORY_KEY, story)
                        fragment.requireActivity().startActivity(intent)
                    }
            }

        }
    }

    private fun loadUserProfileImg(holder: MyViewHolder) {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                fragment.requireContext(),
                FirebaseUserEntryPoint::class.java
            )
        var firebaseUser = hiltEntryPoint.provideFireBaseUser()
        val userDocument =
            FirebaseFirestore.getInstance().collection("Users").document(firebaseUser.uid)
        GlobalScope.launch(Dispatchers.IO) {
            val user = userDocument.get().await().toObject(User::class.java)
            withContext(Dispatchers.Main) {
                Glide.with(fragment)
                    .load(user!!.profilePicture)
                    .placeholder(R.drawable.profile_pic)
                    .into(holder.itemView.findViewById(R.id.story_iv))
            }
        }

    }

    override fun getItemCount(): Int {
        return stories.size
    }

    fun setStoryList(stories: MutableList<Story>) {
        this.stories = stories
        notifyDataSetChanged()
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

}