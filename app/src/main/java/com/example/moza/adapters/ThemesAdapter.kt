package com.example.moza.adapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moza.R
import com.example.moza.models.Theme

class ThemesAdapter(private val themes : MutableList<Theme>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(){


    private lateinit var mListener: OnItemClickListener

    interface OnItemClickListener {
        fun onItemClick(
            position: Int,
            id: String,
            theme : Bitmap
        )
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.theme_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val theme = themes[position]
        Glide.with(holder.itemView.context)
            .load(theme.drawable)
            .into(holder.itemView.findViewById(R.id.theme_iv))

        holder.itemView.setOnClickListener {
            mListener.onItemClick(position , theme.themeName , theme.drawable)
        }

    }

    override fun getItemCount(): Int {
        return themes.size
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

}