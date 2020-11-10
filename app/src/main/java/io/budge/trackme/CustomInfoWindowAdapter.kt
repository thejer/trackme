package io.budge.trackme

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import io.budge.trackme.data.User

class CustomInfoWindowAdapter(context: Context) : GoogleMap.InfoWindowAdapter {

    private val layoutInflater = (context as Activity).layoutInflater
    private val window: View = layoutInflater.inflate(R.layout.info_bubble_window, null)
    private val content: View = layoutInflater.inflate(R.layout.info_bubble_content, null)

    override fun getInfoWindow(marker: Marker): View? {
        render(marker, window)
        return window
    }

    override fun getInfoContents(marker: Marker): View? {
        render(marker, content)
        return content
    }

    private fun render(marker: Marker, view: View) {
        val userObject = marker.tag as User

        val userName = view.findViewById<TextView>(R.id.user_name)
        val userAddress = view.findViewById<TextView>(R.id.user_address)
        val userImage = view.findViewById<ImageView>(R.id.user_image)

        userName.text = userObject.fullName
        userAddress.text = userObject.address
        userImage.setImageBitmap(userObject.image)
    }
}