package io.budge.trackme

import android.app.Application
import android.util.Log.e
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.budge.trackme.data.Result
import io.budge.trackme.data.User
import io.budge.trackme.utils.MessageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext

    private val _userList = MutableLiveData<MutableList<User>>()
    val userList: LiveData<MutableList<User>>
        get() = _userList

    private val _locationUpdate = MutableLiveData<User.UserLocation>()
    val locationUpdate: LiveData<User.UserLocation>
        get() = _locationUpdate

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val messageProcessor = MessageProcessor(context)

    private var socketManager = SocketManager(object : SocketManager.ResponseCallbacks {
        override fun onResponse(result: Result<Any>) {
            when (result) {
                is Result.Success -> {
                    e("Success", result.data)
                    process(result)
                }
                is Result.Error -> {
                    result.exception.message?.let { e("Error", it) }
                    _errorMessage.postValue(result.exception.message)
                    _isLoading.postValue(false)
                }
            }
        }

    })

    private fun process(result: Result.Success<Any>) {
        e("process", result.data)
        viewModelScope.launch {
            val isUserList = result.data.startsWith("USERLIST")
            val isUpdate = result.data.startsWith("UPDATE")
            try {
                if (isUserList) {
                    val users = messageProcessor.processUserList(result.data)
                    _userList.value = users
                } else if (isUpdate) {
                    val locationUpdate = messageProcessor.processLocationUpdate(result.data)
                    _locationUpdate.value = locationUpdate
                }
            } catch (e: NumberFormatException) {
                _errorMessage.postValue("Invalid response format")
                _isLoading.postValue(false)
            }
        }
    }

    fun startConnection(ipAddress: String, port: Int, emailAddress: String) {
        _isLoading.postValue(true)
        e("startConnection", "start")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                socketManager.openConnection(ipAddress, port, emailAddress)
            }
        }
    }

    fun closeConnection() {
        socketManager.closeConnection()
    }
}