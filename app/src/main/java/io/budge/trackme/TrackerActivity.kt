package io.budge.trackme

import android.os.Bundle
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
import io.budge.trackme.utils.Constants.IP_ADDRESS
import io.budge.trackme.utils.Constants.PORT
import io.budge.trackme.utils.animateMarkerToPosition
import io.budge.trackme.utils.getAddress
import kotlinx.android.synthetic.main.activity_tracker.*


class TrackerActivity :
    AppCompatActivity(),
    GoogleMap.OnMarkerClickListener,
    OnMapReadyCallback{

    private lateinit var mMap: GoogleMap

    private val viewModel: TrackerViewModel by viewModels()

    private var markersMap = mutableMapOf<Int, Marker>()

    private lateinit var toast: Toast

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
                updateUsersLocations(it)
            }
        })

    }

    private fun updateUsersLocations(it: User.UserLocation) {
        val marker = markersMap[it.id]
        val user = marker?.tag as User
        user.address = LatLng(it.lat, it.lng).getAddress(this)
        user.location = it
        marker.tag = user
        markersMap[it.id] = marker
        if (marker.isInfoWindowShown) {
            marker.hideInfoWindow()
        }
        val finalPosition = LatLng(it.lat, it.lng)
        marker.animateMarkerToPosition(finalPosition)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(finalPosition, 15f)
        mMap.animateCamera(cameraUpdate)
    }

    private fun showToast(message: String){
        toast.setText(message)
        toast.show()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        viewModel.startConnection(
            IP_ADDRESS,
            PORT)
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
        for (marker in markersMap.values) marker.remove()
        viewModel.closeConnection()
        finish()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }
}