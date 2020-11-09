package io.budge.trackme.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.util.Log.e
import io.budge.trackme.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import java.util.*

class MessageProcessor(private val context: Context) {

    @Throws(NumberFormatException::class)
    suspend fun processUserList(message: String): MutableList<User> {
        val users = mutableListOf<User>()
        withContext(Dispatchers.IO) {
            val usersString = message.removePrefix("USERLIST").trim().removeSuffix(";")
            val usersStringList = usersString.split(";")
            for (userString in usersStringList) {
                val userObject = userString.split(",")
                val imageUrl = userObject[2].trim()

                val imageBitmap = async {getBitmap(imageUrl)}
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
                    getAddress(lat, lng, context),
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
        val updateString = message.removePrefix("UPDATE").trim()
        val updateObject = updateString.split(",")
        val id = updateObject[0].trim().toInt()
        return User.UserLocation(
            id,
            updateObject[1].trim().toDouble(),
            updateObject[2].trim().toDouble()
        )
    }

    private fun getBitmap(imageUrl: String): Bitmap? {
        val url = URL(imageUrl)
        val inputStream: InputStream = url.openStream()
        return BitmapFactory.decodeStream(inputStream)
    }


}

fun getAddress(lat: Double, lng: Double, context: Context): String {
    val addresses: List<Address>
    val geoCoder = Geocoder(context, Locale.getDefault())

    addresses = geoCoder.getFromLocation(
        lat,
        lng,
        1)

    val address = addresses[0]
    return address.getAddressLine(0)
}