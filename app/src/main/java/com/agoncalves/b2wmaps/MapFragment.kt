package com.agoncalves.b2wmaps

import android.graphics.Color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.agoncalves.b2wmaps.model.MapLoc
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapFragment : SupportMapFragment() {

    private val viewModel: MapViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MapViewModel::class.java)
    }
    private var googleMap: GoogleMap? = null
    private lateinit var markerCurrentLocation: Marker

    override fun getMapAsync(callback: OnMapReadyCallback?) {
        super.getMapAsync {
            googleMap = it
            setupMap()
            setObservers()
            callback?.onMapReady(googleMap)
        }
    }

    private fun setupMap() {
        googleMap?.run {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isZoomControlsEnabled = true
            setOnMapLongClickListener { latLng ->
                onMapLongClick(latLng)
            }
        }
    }

    private fun setObservers() {
        viewModel.viewState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is MapsViewState.CurrentLocation -> if (this::markerCurrentLocation.isInitialized) {
                    markerCurrentLocation.position = it.latLang
                }
            }
        })

        viewModel.getMapState()
            .observe(this, Observer { mapState ->
                if (mapState != null) {
                    updateMap(mapState)
                }
            })
    }

    private fun updateMap(mapState: MapLoc) {
        googleMap?.run {
            clear()
            val area = LatLngBounds.Builder()
            mapState.origin?.let {
                addMarker(
                    MarkerOptions()
                        .position(it)
                        .title(getString(R.string.map_marker_origin))
                )
                area.include(it)
            }

            mapState.destination?.let {
                addMarker(
                    MarkerOptions()
                        .position(it)
                        .title(getString(R.string.map_marker_destination))
                )
                area.include(it)
            }


            val route = mapState.route
            if (!route.isNullOrEmpty()) {
                val polylineOptions = PolylineOptions()
                    .addAll(route)
                    .width(5f)
                    .color(Color.BLUE)
                    .visible(true)
                addPolyline(polylineOptions)
                route.forEach { area.include(it) }
            }
            if (mapState.origin != null) {
                if (mapState.destination != null) {
                    animateCamera(CameraUpdateFactory.newLatLngBounds(area.build(), 50))
                } else {
                    animateCamera(CameraUpdateFactory.newLatLngZoom(mapState.origin, 17f))
                }
            }
            mapState.geoLocResponse?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                addCircle(
                    CircleOptions()
                        .strokeWidth(2f)
                        .fillColor(0x990000FF.toInt())
                        .center(latLng)
                        .radius(it.radius.toDouble())
                )
            }
        }
    }

    private fun onMapLongClick(latLng: LatLng) {
        viewModel.setDestination(latLng)
    }
}
