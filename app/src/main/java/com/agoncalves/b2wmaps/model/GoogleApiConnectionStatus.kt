package com.agoncalves.b2wmaps.model

import com.google.android.gms.common.ConnectionResult

data class GoogleApiConnectionStatus(
    val success: Boolean,
    val connectionResult: ConnectionResult? = null
)