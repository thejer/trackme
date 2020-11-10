package io.budge.trackme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.budge.trackme.data.Result
import io.budge.trackme.data.User
import io.budge.trackme.utils.Constants.INVALID_MESSAGE_FORMAT
import io.budge.trackme.utils.Constants.UPDATE_PREFIX
import io.budge.trackme.utils.Constants.USER_LIST_PREFIX
import io.budge.trackme.utils.MessageProcessor
import io.budge.trackme.utils.SocketManager
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
                    process(result)
                }
                is Result.Error -> {
                    _errorMessage.postValue(result.exception.message)
                    _isLoading.postValue(false)
                }
            }
        }

    })

    private fun process(result: Result.Success<Any>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val isUserList = result.data.startsWith(USER_LIST_PREFIX)
                val isUpdate = result.data.startsWith(UPDATE_PREFIX)
                try {
                    if (isUserList) {
                        val usersString =
                            result.data.removePrefix(USER_LIST_PREFIX).trim().removeSuffix(";")
                        val users = messageProcessor.processUserList(usersString)
                        _isLoading.postValue(false)
                        _userList.postValue(users)
                    } else if (isUpdate) {
                        val updateString = result.data.removePrefix(UPDATE_PREFIX).trim()
                        val locationUpdate = messageProcessor.processLocationUpdate(updateString)
                        _isLoading.postValue(false)
                        _locationUpdate.postValue(locationUpdate)
                    }
                    socketManager.readInputStream()
                } catch (e: Exception) {
                    if (e is NumberFormatException) {
                        _errorMessage.postValue(INVALID_MESSAGE_FORMAT)
                    }
                    _isLoading.postValue(false)
                }
            }
        }
    }

    fun startConnection(ipAddress: String, port: Int) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            socketManager.openConnection(ipAddress, port)
        }
    }

    fun closeConnection() {
        socketManager.closeConnection()
    }
}