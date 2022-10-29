package com.example.moza.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moza.common.FireBaseRepository
import com.example.moza.common.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditSensetiveViewModel @Inject constructor(
    private val repository: FireBaseRepository
) : ViewModel() {

    val changePassResponse: MutableLiveData<Resource<String>> = MutableLiveData()
    val sendEmailResponse: MutableLiveData<Resource<String>> = MutableLiveData()

    fun safeChangePassword(oldPassword: String, newPassword: String, email: String) =
        viewModelScope.launch {
            changePassword(oldPassword, newPassword, email)
        }

    private suspend fun changePassword(oldPassword: String, newPassword: String, email: String) {
        repository.changeUserPassword(oldPassword, newPassword, email).collect { response ->
            when (response) {
                is Resource.Error -> changePassResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> changePassResponse.postValue(Resource.Loading())
                is Resource.Success -> changePassResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }

    fun safeSendEmail(email: String) = viewModelScope.launch {
        repository.resetPasswordViaEmail(email).collect { response ->
            when (response) {
                is Resource.Error -> sendEmailResponse.postValue(Resource.Error(response.message!!))
                is Resource.Loading -> sendEmailResponse.postValue(Resource.Loading())
                is Resource.Success -> sendEmailResponse.postValue(Resource.Success(response.data!!))
            }
        }
    }


}