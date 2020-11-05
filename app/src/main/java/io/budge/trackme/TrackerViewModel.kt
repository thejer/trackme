package io.budge.trackme

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.budge.trackme.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket
import java.net.URL
import java.util.*


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

    private var isConnectionOpen = false
    private var outWriter: PrintWriter? = null
    private var inReader: BufferedReader? = null

    fun openConnection(ipAddress: String, port: Int, emailAddress: String) {
        isConnectionOpen = true
        _isLoading.postValue(true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while (isConnectionOpen) {
                    try {
                        val socket = Socket(ipAddress, port)
                        try {
                            val outputStreamWriter =
                                OutputStreamWriter(socket.getOutputStream())
                            val inputStreamReader = InputStreamReader(socket.getInputStream())
                            outWriter = PrintWriter(BufferedWriter(outputStreamWriter), true)
                            inReader = BufferedReader(inputStreamReader)
                            authenticate(emailAddress)
                            while (isConnectionOpen) {
                                inReader?.let {
                                    val response = it.readLine()
                                    processResponse(response.trim())
                                }
                            }
                        } catch (e: Exception) {
                            _isLoading.postValue(false)
                                _errorMessage.postValue(e.message)
                            socket.close()
                            continue
                        }
                        socket.close()
                        break
                    } catch (e: Exception) {
                        _errorMessage.postValue(e.message)
                        _isLoading.postValue(false)
                    }
                }
            }
        }
    }

    fun getAddress(lat: Double, lng: Double): String {
        val addresses: List<Address>
        val geoCoder = Geocoder(context, Locale.getDefault())

        addresses = geoCoder.getFromLocation(
            lat,
            lng,
            1)

        val address = addresses[0]
        val fullAddress = address.getAddressLine(0)
        return fullAddress
    }

    private suspend fun processResponse(response: String) {
        withContext(Dispatchers.IO) {
            val isUserList = response.startsWith("USERLIST")
            val isUpdate = response.startsWith("UPDATE")
            if (isUserList) {
                val usersString = response.removePrefix("USERLIST").trim().removeSuffix(";")
                val usersStringList = usersString.split(";")
                val users = mutableListOf<User>()
                for ((index, userString) in usersStringList.withIndex()) {
                    val userObject = userString.split(",")
                    val imageUrl = userObject[2].trim()

                    val imageBitmap = async { getBitmap(imageUrl) }
                    val lat = userObject[3].trim().toDouble()
                    val lng = userObject[4].trim().toDouble()
                    val location = User.UserLocation(
                        userObject[0].trim().toInt(),
                        lat,
                        lng
                    )
                    val newUser = User(
                        userObject[0].trim().toInt(),
                        userObject[1].trim(),
                        imageUrl,
                        getAddress(lat, lng),
                        imageBitmap.await(),
                        location
                    )

                    users.add(newUser)
                }
                _isLoading.postValue(false)
                _userList.postValue(users)
            } else if (isUpdate) {
                val updateString = response.removePrefix("UPDATE").trim()
                val updateObject = updateString.split(",")
                val id = updateObject[0].trim().toInt()
                val location = User.UserLocation(
                    id,
                    updateObject[1].trim().toDouble(),
                    updateObject[2].trim().toDouble()
                )
                _locationUpdate.postValue(location)
            }
        }
    }

    private fun getBitmap(imageUrl: String): Bitmap? {
        val url = URL(imageUrl)
        val inputStream: InputStream = url.openStream()
        return BitmapFactory.decodeStream(inputStream)
    }

    fun closeConnection() {
        isConnectionOpen = false
        outWriter?.apply {
            flush()
            close()
        }
        outWriter = null
        inReader = null
    }

    private fun authenticate(emailAddress: String) {
        if (outWriter?.checkError() != false) return
        outWriter?.apply {
            println("AUTHORIZE $emailAddress")
            flush()
        }
    }
}