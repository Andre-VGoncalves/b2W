package com.agoncalves.b2wmaps.model

import com.google.android.gms.maps.model.LatLng

data class MapLoc(
    val origin: LatLng? = null,
    val destination: LatLng? = null,
    val route: List<LatLng>? = null,
    val geoLocResponse: GeoLocResponse? = null
)