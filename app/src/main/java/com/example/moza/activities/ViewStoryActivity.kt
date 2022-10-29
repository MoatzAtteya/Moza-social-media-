package com.example.moza.activities

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.moza.common.Constants
import com.example.moza.common.FireBaseRepository
import com.example.moza.databinding.ActivityViewStoryBinding
import com.example.moza.models.Story
import com.example.moza.models.StoryView
import com.example.moza.utils.StoryViewBottomSheet
import com.example.moza.viewmodels.ViewStoryViewModel
import com.genius.multiprogressbar.MultiProgressBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ViewStoryActivity : AppCompatActivity(), MultiProgressBar.ProgressFinishListener {
    lateinit var binding: ActivityViewStoryBinding
    lateinit var viewModel: ViewStoryViewModel
    lateinit var repository: FireBaseRepository
    lateinit var firebaseUser: FirebaseUser
    private lateinit var detector: GestureDetectorCompat
    lateinit var story: Story
    private var isMyProfile = true


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = FireBaseRepository()
        viewModel = ViewModelProvider(this)[ViewStoryViewModel::class.java]
        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        detector = GestureDetectorCompat(this, DiaryGestureListener())

        story = intent.getSerializableExtra(Constants.USER_STORY_KEY) as Story

        if (story.uid != firebaseUser.uid) {
            val storyView = StoryView(
                "",
                firebaseUser.uid,
                "",
                System.currentTimeMillis()
            )
            viewModel.safeUploadView(story.id!!, storyView)
            binding.viewStoryTv.visibility = View.GONE
            binding.storyDeleteBtn.visibility = View.GONE
            isMyProfile = false
        }

        binding.progressbar.visibility = View.VISIBLE

        Glide.with(this)
            .load(story.profileIMGURL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(
                        this@ViewStoryActivity,
                        "Something went wrong!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.progressbar.visibility = View.GONE
                    if (story.uid != firebaseUser.uid) {
                        binding.storyProgressBar.visibility = View.VISIBLE
                        binding.storyProgressBar.start(0)
                    }
                    return false
                }

            })
            .into(binding.viewStoryProfileImgIv)
        binding.storyViewUsernameTv.text = story.username.toString()
        binding.storyProgressBar.setFinishListener(this)

        val ago = DateUtils.getRelativeTimeSpanString(
            story.timeStamp!!.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        if (ago == "0 minutes ago")
            binding.viewStoryDateAgo.text = ago
        else
            binding.viewStoryDateAgo.text = ago


        if (story.videoUrl.toString().contains(".png")) {
            binding.storyImageViewIv.visibility = View.VISIBLE
            binding.storyVideoView.visibility = View.GONE
            Glide.with(this)
                .load(story.videoUrl)
                .into(binding.storyImageViewIv)
            if (story.uid!=firebaseUser.uid){
                binding.storyImageViewIv.setOnTouchListener(OnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        binding.storyProgressBar.pause()
                        return@OnTouchListener true
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        binding.storyProgressBar.start()
                        return@OnTouchListener true
                    }
                    false
                })
            }

        }

        else if (story.videoUrl.toString().contains(".video")) {
            binding.storyVideoView.setVideoURI(Uri.parse(story.videoUrl))
            binding.storyVideoView.start()

            binding.storyVideoView.setOnPreparedListener {
                val duration = (it.duration / 1000).toFloat()
                if (story.uid != firebaseUser.uid) {
                    binding.storyProgressBar.visibility = View.VISIBLE
                    binding.storyProgressBar.start(0)
                    binding.storyProgressBar.setSingleDisplayTime(duration)

                    binding.storyVideoView.setOnTouchListener(OnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            binding.storyProgressBar.pause()
                            it.pause()
                            return@OnTouchListener true
                        } else if (event.action == MotionEvent.ACTION_UP) {
                            binding.storyProgressBar.start()
                            it.start()
                            return@OnTouchListener true
                        }
                        false
                    })
                }
            }
        }

        binding.viewStoryTv.setOnClickListener {
            val bottomSheetDialog = StoryViewBottomSheet(story.id!!)
            bottomSheetDialog.show(supportFragmentManager, "story views bottom sheet")
        }

        binding.storyDeleteBtn.setOnClickListener {
            try {
                viewModel.deleteStory(story.id!!)
                Toast.makeText(this, "Your story has been deleted.", Toast.LENGTH_SHORT).show()
                finish()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }


    }


    override fun finish() {
        super.finish()
        overridePendingTransition(
            com.karumi.dexter.R.anim.abc_slide_in_bottom,
            com.karumi.dexter.R.anim.abc_slide_out_bottom
        )
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (detector.onTouchEvent(event!!)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    inner class DiaryGestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            downEvent: MotionEvent?,
            moveEvent: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var diffX = moveEvent?.x?.minus(downEvent!!.x) ?: 0.0F
            var diffY = moveEvent?.y?.minus(downEvent!!.y) ?: 0.0F

            return if (Math.abs(diffX) > Math.abs(diffY)) {
                // this is a left or right swipe
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // right swipe
                        this@ViewStoryActivity.onSwipeRight()
                    } else {
                        // left swipe.
                        this@ViewStoryActivity.onLeftSwipe()
                    }
                    true
                } else {
                    super.onFling(downEvent, moveEvent, velocityX, velocityY)
                }
            } else {
                // this is either a bottom or top swipe.
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        this@ViewStoryActivity.onSwipeTop()
                    } else {
                        this@ViewStoryActivity.onSwipeBottom()
                    }
                    true
                } else {
                    super.onFling(downEvent, moveEvent, velocityX, velocityY)
                }
            }


        }
    }

    private fun onSwipeBottom() {
        if (isMyProfile) {
            val bottomSheetDialog = StoryViewBottomSheet(story.id!!)
            bottomSheetDialog.show(supportFragmentManager, "story views bottom sheet")
        }
    }

    private fun onSwipeTop() {
        finish()
    }

    internal fun onLeftSwipe() {
        // Toast.makeText(this, "left Swipe", Toast.LENGTH_LONG).show()

    }

    internal fun onSwipeRight() {
        //Toast.makeText(this, "right Swipe", Toast.LENGTH_LONG).show()

    }

    override fun onProgressFinished() {
        finish()
    }

}

