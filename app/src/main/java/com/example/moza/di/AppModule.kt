package com.example.moza.di

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.annotation.Nullable
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRepository() : FireBaseRepository {
        return FireBaseRepository()
    }



    @Provides
    @Singleton
    fun provideSharedPrefrences(@ApplicationContext context: Context) : SharedPreferences{
        return context.getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE)
    }


    @Provides
    @Singleton
    fun provideContextWrapper(@ApplicationContext context: Context) : ContextWrapper{
        return ContextWrapper(context)
    }

    @Provides
    @Singleton
    fun provideFireBaseUser(): FirebaseUser {
        return FirebaseAuth.getInstance().currentUser!!
    }



}