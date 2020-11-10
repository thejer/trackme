package io.budge.trackme.utils

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.util.Property
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.io.InputStream
import java.net.URL
import java.util.*
import kotlin.math.abs
import kotlin.math.sign

fun LatLng.getAddress(context: Context): String {
    val addresses: List<Address>
    val geoCoder = Geocoder(context, Locale.getDefault())

    addresses = geoCoder.getFromLocation(
        latitude,
        longitude,
        1)

    val address = addresses[0]
    return address.getAddressLine(0)
}

fun String.getBitmapFromStringUrl(): Bitmap? {
    val url = URL(this)
    val inputStream: InputStream = url.openStream()
    return BitmapFactory.decodeStream(inputStream)
}


private fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
    val lat = (b.latitude - a.latitude) * fraction + a.latitude
    var lngDelta = b.longitude - a.longitude

    if (abs(lngDelta) > 180) {
        lngDelta -= sign(lngDelta) * 360
    }
    val lng = lngDelta * fraction + a.longitude
    return LatLng(lat, lng)
}


fun Marker?.animateMarkerToPosition(
    finalPosition: LatLng?,
) {
    val typeEvaluator: TypeEvaluator<LatLng> =
        TypeEvaluator { fraction, startValue, endValue ->
            interpolate(
                fraction,
                startValue,
                endValue
            )
        }
    val property: Property<Marker, LatLng> =
        Property.of(Marker::class.java, LatLng::class.java, "position")
    val animator = ObjectAnimator.ofObject(this, property, typeEvaluator, finalPosition)
    animator.duration = 500
    animator.start()
}
