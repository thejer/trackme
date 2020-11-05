package io.budge.trackme

import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.os.Bundle
import android.util.Property
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.budge.trackme.data.User
import kotlinx.android.synthetic.main.activity_tracker.*
import kotlin.math.abs
import kotlin.math.sign


class TrackerActivity :
    AppCompatActivity(),
    GoogleMap.OnMarkerClickListener,
    OnMapReadyCallback{

    private lateinit var mMap: GoogleMap

    private val viewModel: TrackerViewModel by viewModels()

    private var markersMap = mutableMapOf<Int, Marker>()

    private lateinit var toast: Toast

    private var openOnResume = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)

        viewModel.isLoading.observe(this, {
            if (it) loading_indicator.show()
            else loading_indicator.hide()
        })

        viewModel.errorMessage.observe(this, {
            it?.let {
                showToast(it)
            }
        })

        viewModel.userList.observe(this, { users: MutableList<User>? ->
            users?.let {
                addMarkers(it)
            }
        })

        viewModel.locationUpdate.observe(this, {
            it?.let {
                val marker = markersMap[it.id]
                val user = marker?.tag as User
                user.location = it
                user.address = viewModel.getAddress(it.lat, it.lng)
                marker.tag = user
                markersMap[it.id] = marker
                if (marker.isInfoWindowShown) {
                    marker.hideInfoWindow()
                }
                val finalPosition = LatLng(it.lat, it.lng)
                animateMarker(marker, finalPosition)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(finalPosition, 15f)
                mMap.animateCamera(cameraUpdate)
            }
        })

    }

    private fun showToast(message: String){
        toast.setText(message)
        toast.show()
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


    private fun animateMarker(
        marker: Marker?,
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
        val animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition)
        animator.duration = 500
        animator.start()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        viewModel.openConnection(
            "ios-test.printful.lv",
            6111,
            "email@address.com"
        )
    }

    private fun addMarkers(users: MutableList<User>) {
        for (user in users) {
            val userLocation = user.location
            val location = LatLng(userLocation.lat, userLocation.lng)
            val addMarker = mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(user.fullName)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pegman))
            )
            addMarker.tag = user
            markersMap[user.id] = addMarker
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 15f)
            mMap.animateCamera(cameraUpdate)
        }
    }

    override fun onPause() {
        super.onPause()
        openOnResume = true
        for (marker in markersMap.values) marker.remove()
        viewModel.closeConnection()
    }

    override fun onResume() {
        super.onResume()
        if (openOnResume) {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(0.0,0.0), 1f)
            mMap.animateCamera(cameraUpdate)
            viewModel.openConnection(
                "ios-test.printful.lv",
                6111,
                "jerryb.adeleye@gmail.com"
            )
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }
}