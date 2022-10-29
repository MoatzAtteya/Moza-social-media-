package com.example.moza.utils

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moza.R
import com.example.moza.adapters.StoryViewsAdapter
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import com.example.moza.databinding.StoryViewsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StoryViewBottomSheet(val storyID: String ) : BottomSheetDialogFragment() {

    lateinit var binding: StoryViewsBottomSheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = StoryViewsBottomSheetBinding.inflate(inflater, container, false)

        val repository = FireBaseRepository()
        CoroutineScope(Dispatchers.Main).launch {
            repository.getStoryViews(storyID).collect { response ->
                when (response) {
                    is Resource.Error -> Log.e("Getting story views: ", response.message!!)
                    is Resource.Loading -> TODO()
                    is Resource.Success -> {
                        val viewsList = response.data
                        binding.viewStoryRv.layoutManager = LinearLayoutManager(context)
                        binding.viewStoryRv.setHasFixedSize(true)
                        val adapter = StoryViewsAdapter(viewsList!!,context)
                        binding.viewStoryRv.adapter = adapter
                    }
                }
            }
        }

        return binding.root
    }
}