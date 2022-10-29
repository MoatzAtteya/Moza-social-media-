package com.example.moza.activities

import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.moza.R
import com.example.moza.common.Constants
import com.example.moza.databinding.ActivityHome2Binding
import com.example.moza.utils.ConnectivityObserver
import com.example.moza.utils.ConnectivityObserver.Status.*
import com.example.moza.utils.NetworkConnectivityObserver
import com.example.moza.viewmodels.HomeActivityViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException


@AndroidEntryPoint
class HomeActivity() : AppCompatActivity() {

    private lateinit var binding: ActivityHome2Binding
    private lateinit var connectivityObserver: ConnectivityObserver
    private var openedTime = 0
    lateinit var navView: BottomNavigationView
    lateinit var viewModel: HomeActivityViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHome2Binding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[HomeActivityViewModel::class.java]
        setContentView(binding.root)

        connectivityObserver = NetworkConnectivityObserver(applicationContext)

        observeConnectionState()
        cancelAllNotifications()

        //setSupportActionBar(binding.homePageActionbar)
        val pref = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
        val directory = pref.getString(Constants.PREF_DIRECTORY, "")
        val bitmap = loadProfileImage(directory!!)
        val drawable = BitmapDrawable(resources, bitmap)


        navView = binding.navView
        val menuItem = navView.menu.findItem(R.id.navigation_my_profile)

        if (directory == "")
            menuItem.setIcon(R.drawable.profile_pic)
        else
            menuItem.setIcon(drawable)

        navView.itemIconTintList = null


        val navController = findNavController(R.id.nav_host_fragment_activity_home2)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_search,
                R.id.navigation_add_post,
                R.id.navigation_likes,
                R.id.navigation_my_profile
            )
        )
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {

                R.id.navigation_home -> {
                    navView.visibility = View.VISIBLE
                }
                R.id.navigation_search -> {
                    navView.visibility = View.VISIBLE
                }
                R.id.navigation_likes -> {
                    navView.visibility = View.VISIBLE
                }
                R.id.navigation_add_post -> {
                    navView.visibility = View.VISIBLE
                }
                R.id.navigation_my_profile -> {
                    navView.visibility = View.VISIBLE
                }
                else -> {
                    navView.visibility = View.GONE
                }
            }
        }
    }

    fun hideBottomNavigationBar() {
        navView.visibility = View.GONE
    }

    fun showBottomNavigationBar() {
        navView.visibility = View.VISIBLE
    }

    private fun observeConnectionState() = CoroutineScope(Dispatchers.Main).launch {
        connectivityObserver.observe().collect { status ->

            when (status) {
                Available -> {
                    if (openedTime != 0) {
                        showSnackBar("Internet connection restored successfully.", false)
                    }
                    openedTime++
                }
                Unavailable -> {
                    showSnackBar("It seems that there is no internet connection.", true)
                }
                Losing -> {}
                Lost -> {
                    showSnackBar("It seems that you have lost your connection.", true)

                }
            }

        }
    }

    private fun loadProfileImage(directory: String): Bitmap? {
        return try {
            val file = File(directory, "profile.png")
            BitmapFactory.decodeStream(FileInputStream(file))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.safeUpdateUserStatus(true)

    }

    override fun onStop() {
        super.onStop()
        viewModel.safeUpdateUserStatus(false)

    }

    fun cancelAllNotifications(){
        val ns: String = Context.NOTIFICATION_SERVICE
        val nMgr = this.getSystemService(ns) as NotificationManager
        nMgr.cancelAll()
    }

    private fun showSnackBar(msg: String, error: Boolean) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            msg, Snackbar.LENGTH_SHORT
        )
        val snackbarView = snackbar.view
        if (error) {
            snackbarView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.red
                )
            )
        } else {
            snackbarView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.green
                )
            )
        }
        snackbar.show()
    }


}