package io.budge.trackme.utils

import android.content.Context
import android.util.Log.e
import com.google.android.gms.maps.model.LatLng
import io.budge.trackme.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class MessageProcessor(private val context: Context) {

    @Throws(NumberFormatException::class)
    suspend fun processUserList(message: String): MutableList<User> {
        val users = mutableListOf<User>()
        withContext(Dispatchers.IO) {
            val usersStringList = message.split(";")
            for (userString in usersStringList) {
                val userObject = userString.split(",")
                val imageUrl = userObject[2].trim()

                val imageBitmap = async {imageUrl.getBitmapFromStringUrl()}
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
                    LatLng(lat, lng).getAddress(context),
                    imageBitmap.await(),
                    location
                )

                users.add(newUser)
                e("processUserList", users.toString())
            }
        }
        return users
    }

    @Throws(NumberFormatException::class)
    fun processLocationUpdate(message: String): User.UserLocation {
        val updateObject = message.split(",")
        val id = updateObject[0].trim().toInt()
        return User.UserLocation(
            id,
            updateObject[1].trim().toDouble(),
            updateObject[2].trim().toDouble()
        )
    }
}