package com.example.moza.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.moza.R
import com.example.moza.common.FireBaseRepository
import com.example.moza.models.Comment
import com.example.moza.models.PostImage
import com.example.moza.models.User
import com.example.moza.utils.BubbleInterpolate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HomePostsAdapter(
    private val fragment: Fragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: OnItemClickListener
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var isLiked: Boolean = false
    private val repo = FireBaseRepository()
    var list: MutableList<PostImage> = mutableListOf()
    var isSavedPostList: Boolean = false


    interface OnItemClickListener {
        fun onLikeClick(
            position: Int,
            post: PostImage
        )

        fun onShareClick(
            position: Int,
            postUrl: String
        )

        fun onViewLikesClick(
            position: Int,
            post: PostImage
        )

        fun onCommentClick(
            position: Int,
            id: String,
            uId: String,
            comment: String,
            commentEt: EditText,
            imageUrl: String?,
            postID: String
        )

        fun onViewAllCommentsClick(
            position: Int,
            postID: String,
            postUID: String,
            imageUrl: String?
        )

        fun onUsernameClick(
            position: Int,
            uid: String
        )

        fun onSaveClick(
            position: Int,
            postID: String
        )

        fun onHideClick(
            position: Int,
            post: PostImage
        )

        fun onUnSaveClick(
            position: Int,
            postID: String
        )

    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FirebaseUserEntryPoint {
        fun provideFireBaseUser(): FirebaseUser
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(fragment.context).inflate(
                R.layout.home_posts_item,
                parent,
                false
            )
        )
    }

    @SuppressLint("CutPasteId")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = list[position]
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                fragment.requireContext(),
                FirebaseUserEntryPoint::class.java
            )
        val firebaseUser = hiltEntryPoint.provideFireBaseUser()

        if (!post.hideIds.contains(firebaseUser.uid)) {
            holder.itemView.findViewById<TextView>(R.id.posts_username_tv).text = post.userName

            val ago = DateUtils.getRelativeTimeSpanString(
                post.timeStamp!!.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            if (ago == "0 minutes ago")
                holder.itemView.findViewById<TextView>(R.id.posts_time_tv).text = "Now"
            else
                holder.itemView.findViewById<TextView>(R.id.posts_time_tv).text =
                    ago.toString()

            when (val likesCount = post.likes.size) {
                0 -> {
                    holder.itemView.findViewById<TextView>(R.id.posts_likes_number_tv).visibility =
                        View.GONE
                }
                1 -> {
                    holder.itemView.findViewById<TextView>(R.id.posts_likes_number_tv).visibility =
                        View.VISIBLE
                    holder.itemView.findViewById<TextView>(R.id.posts_likes_number_tv).text =
                        "$likesCount like"
                }
                else -> {
                    holder.itemView.findViewById<TextView>(R.id.posts_likes_number_tv).visibility =
                        View.VISIBLE
                    holder.itemView.findViewById<TextView>(R.id.posts_likes_number_tv).text =
                        "$likesCount likes"
                }
            }

            holder.itemView.findViewById<TextView>(R.id.posts_likes_number_tv).setOnClickListener {
                mListener.onViewLikesClick(position, post)
            }

            val description = post.description
            if (description.isNullOrEmpty()) {
                holder.itemView.findViewById<TextView>(R.id.posts_username_comment_tv).visibility =
                    View.GONE
                holder.itemView.findViewById<TextView>(R.id.posts_description_tv).visibility =
                    View.GONE
            } else {
                holder.itemView.findViewById<TextView>(R.id.posts_username_comment_tv).text =
                    post.userName
                holder.itemView.findViewById<TextView>(R.id.posts_description_tv).text =
                    post.description
            }

            if (post.likes.contains(firebaseUser.uid)) {
                holder.itemView.findViewById<ImageButton>(R.id.posts_like_ib)
                    .setBackgroundResource(R.drawable.favorite_48px)
            } else {
                holder.itemView.findViewById<ImageButton>(R.id.posts_like_ib)
                    .setBackgroundResource(R.drawable.like_icon)
            }

            FirebaseFirestore.getInstance().collection("Users").document(post.uid)
                .get().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val user = it.result.toObject(User::class.java)
                        Glide.with(fragment)
                            .load(user!!.profilePicture)
                            .placeholder(R.drawable.profile_pic)
                            .timeout(6500)
                            .into(holder.itemView.findViewById(R.id.posts_profile_image))
                    }
                }


            Glide.with(fragment)
                .load(post.imageUrl)
                .placeholder(R.drawable.login_image)
                .timeout(6500)
                .fitCenter()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.itemView.findViewById<ProgressBar>(R.id.progressBar)
                            .visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.itemView.findViewById<ProgressBar>(R.id.progressBar)
                            .visibility = View.GONE
                        return false
                    }

                })
                .into(holder.itemView.findViewById(R.id.posts_post_image_iv))

            var doubleClickLastTime = 0L
            holder.itemView.findViewById<ImageView>(R.id.posts_post_image_iv).setOnClickListener {
                if (System.currentTimeMillis() - doubleClickLastTime < 300) {
                    doubleClickLastTime = 0
                    val bubbleAnim =
                        AnimationUtils.loadAnimation(fragment.requireContext(), R.anim.bubble_anim)
                    val bubbleInterpolator = BubbleInterpolate(0.2, 20.0)
                    bubbleAnim.interpolator = bubbleInterpolator

                    holder.itemView.findViewById<ImageView>(R.id.post_image_like_iv).visibility =
                        View.VISIBLE
                    holder.itemView.findViewById<ImageView>(R.id.post_image_like_iv)
                        .startAnimation(bubbleAnim)

                    GlobalScope.launch(Dispatchers.Main) {
                        delay(1500)
                        holder.itemView.findViewById<ImageView>(R.id.post_image_like_iv).visibility =
                            View.GONE
                    }
                    mListener.onLikeClick(
                        position,
                        post
                    )
                    isLiked = true

                } else {
                    doubleClickLastTime = System.currentTimeMillis()
                }
            }

            holder.itemView.findViewById<ImageButton>(R.id.posts_like_ib).setOnClickListener {
                mListener.onLikeClick(
                    position,
                    post
                )
                isLiked = if (!isLiked) {

                    holder.itemView.findViewById<ImageButton>(R.id.posts_like_ib)
                        .setBackgroundResource(R.drawable.favorite_48px)
                    true
                } else {


                    holder.itemView.findViewById<ImageButton>(R.id.posts_like_ib)
                        .setBackgroundResource(R.drawable.like_icon)
                    false
                }
            }

            holder.itemView.findViewById<ImageButton>(R.id.posts_comment_apply_ib)
                .setOnClickListener {
                    val comment =
                        holder.itemView.findViewById<EditText>(R.id.post_comment_et).text.toString()
                    val commentEt = holder.itemView.findViewById<EditText>(R.id.post_comment_et)
                    mListener.onCommentClick(
                        position,
                        post.id!!,
                        post.uid,
                        comment,
                        commentEt,
                        post.imageUrl,
                        post.id!!
                    )
                }

            holder.itemView.findViewById<ImageButton>(R.id.posts_share_ib)
                .setOnClickListener {
                    mListener.onShareClick(position, post.imageUrl!!)
                }

            holder.itemView.findViewById<TextView>(R.id.post_view_all_comment).setOnClickListener {
                mListener.onViewAllCommentsClick(
                    position,
                    post.id!!,
                    post.uid,
                    post.imageUrl
                )
            }

            holder.itemView.findViewById<TextView>(R.id.posts_username_tv).setOnClickListener {
                mListener.onUsernameClick(position, post.uid)
            }
            holder.itemView.findViewById<CircleImageView>(R.id.posts_profile_image)
                .setOnClickListener {
                    mListener.onUsernameClick(position, post.uid)
                }


            holder.itemView.findViewById<ImageButton>(R.id.posts_comment_ib).setOnClickListener {
                val commentLayout =
                    holder.itemView.findViewById<LinearLayout>(R.id.post_comment_layout)
                if (commentLayout.visibility == View.GONE) {
                    slideDown(commentLayout)
                } else if (commentLayout.visibility == View.VISIBLE)
                    slideUp(commentLayout)


            }

            var commentList: ArrayList<Comment>
            val commentResponse = repo.getPostComments(post.uid, post.id!!)
            commentResponse.addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("getting post comments: ", error.message.toString())
                    return@addSnapshotListener
                }
                if (value == null) {
                    Log.e("getting post comments: ", "post comment list is empty")
                    return@addSnapshotListener
                }
                commentList = value.toObjects(Comment::class.java) as ArrayList<Comment>
                if (commentList.isNotEmpty() && commentList.size != 0) {
                    holder.itemView.findViewById<TextView>(R.id.post_view_all_comment).text =
                        "View all ${commentList.size} comment"
                } else {
                    holder.itemView.findViewById<TextView>(R.id.post_view_all_comment).visibility =
                        View.GONE
                }

            }
            if (!isSavedPostList) {
                holder.itemView.findViewById<ImageView>(R.id.post_options_iv).setOnClickListener {
                    val popup = PopupMenu(
                        fragment.requireContext(),
                        holder.itemView.findViewById<ImageView>(R.id.post_options_iv)
                    )
                    popup.inflate(R.menu.post_options_menu)
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.save_post_option -> mListener.onSaveClick(position, post.id!!)
                            R.id.hide_post_option -> mListener.onHideClick(position, post)
                        }
                        false
                    }
                    popup.show()
                }
            } else {
                holder.itemView.findViewById<ImageView>(R.id.post_options_iv).setOnClickListener {
                    val popup = PopupMenu(
                        fragment.requireContext(),
                        holder.itemView.findViewById<ImageView>(R.id.post_options_iv)
                    )
                    popup.inflate(R.menu.saved_posts_menu)
                    popup.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.unSave_post_menu -> mListener.onUnSaveClick(position, post.id!!)
                        }
                        false
                    }
                    popup.show()
                }
            }
        } else {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
    }


    override fun getItemCount(): Int {
        return list.size
    }

    fun removeItem(position: Int) {
        this.list.removeAt(position)
    }

    private fun slideUp(view: View) {
        view.visibility = View.GONE
        val animate = TranslateAnimation(
            0F,  // fromXDelta
            0F,  // toXDelta
            0F,  // fromYDelta
            view.height.toFloat()
        ) // toYDelta
        animate.duration = 300
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    private fun slideDown(view: View) {
        view.visibility = View.VISIBLE
        val animate = TranslateAnimation(
            0F,  // fromXDelta
            0F,  // toXDelta
            view.height.toFloat(),  // fromYDelta
            0F
        ) // toYDelta
        animate.duration = 400
        animate.fillAfter = true
        view.startAnimation(animate)
    }

    fun setSavedPost(check: Boolean) {
        isSavedPostList = check
        notifyDataSetChanged()
    }

    fun updateList(list: MutableList<PostImage>) {
        this.list = list
    }


    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

}