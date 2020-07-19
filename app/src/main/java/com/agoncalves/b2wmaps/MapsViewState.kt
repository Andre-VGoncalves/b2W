package com.agoncalves.b2wmaps

import android.location.Address
import com.agoncalves.b2wmaps.model.GoogleApiConnectionStatus
import com.google.android.gms.maps.model.LatLng

sealed class MapsViewState {
    data class ConectionStatus(val googleConection: GoogleApiConnectionStatus) : MapsViewState()
    data class ShowError(val isError: Boolean) : MapsViewState()
    data class ShowAddres(val addres: ArrayList<Address>) : MapsViewState()
    data class CurrentLocation(val latLang: LatLng) : MapsViewState()

}