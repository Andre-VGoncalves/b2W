package com.agoncalves.b2wmaps

import android.location.Address
import com.agoncalves.b2wmaps.model.GoogleApiConnectionStatus

sealed class MapsViewState {
    data class ConectionStatus(val googleConection: GoogleApiConnectionStatus): MapsViewState()
    data class ShowError(val isError: Boolean): MapsViewState()
    data class ShowAddres(val addres : ArrayList<Address>): MapsViewState()

}