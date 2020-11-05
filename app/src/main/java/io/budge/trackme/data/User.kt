package io.budge.trackme.data

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class User(
    val id: Int,
    val fullName: String,
    val imageUrl: String,
    var address: String,
    var image: Bitmap?,
    var location: UserLocation
) {

    data class UserLocation(
        val id: Int,
        val lat: Double,
        val lng: Double
    )
}
