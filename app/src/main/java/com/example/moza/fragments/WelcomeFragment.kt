package com.example.moza.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.moza.R
import com.example.moza.databinding.FragmentWelcomeBinding


class WelcomeFragment : Fragment() {
    lateinit var binding : FragmentWelcomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWelcomeBinding.inflate(inflater , container , false)
        // Inflate the layout for this fragment

        binding.logInBtn.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }

        binding.createAccountBtn.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_createAccountFragment)
        }



        return binding.root


    }



}