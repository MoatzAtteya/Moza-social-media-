package com.example.moza.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.moza.databinding.FragmentVideoCallBinding

class VideoCallFragment : Fragment() {


    private lateinit var binding : FragmentVideoCallBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentVideoCallBinding.inflate(layoutInflater , container, false)
        return binding.root
    }


}