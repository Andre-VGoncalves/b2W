package com.agoncalves.b2wmaps

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.agoncalves.b2wmaps.model.GeoLocResponse
import com.agoncalves.b2wmaps.model.GoogleApiConnectionStatus
import com.agoncalves.b2wmaps.model.MapLoc
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MapViewModel(app: Application) : BaseViewModel(app) {

    val viewState = MutableLiveData<MapsViewState>()

    private var googleApiClient: GoogleApiClient? = null
    private val locationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(getContext())
    }

    private val mapState = MutableLiveData<MapLoc>().apply {
        value = MapLoc()
    }

    fun getMapState(): LiveData<MapLoc> {
        return mapState
    }

    fun connectGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(getContext())
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(args: Bundle?) {
                    viewState.value = MapsViewState.ConectionStatus(GoogleApiConnectionStatus(true))
                }

                override fun onConnectionSuspended(i: Int) {
                    viewState.value =
                        MapsViewState.ConectionStatus(GoogleApiConnectionStatus(false))
                    googleApiClient?.connect()
                }
            })
            .addOnConnectionFailedListener { _ ->
                viewState.value = MapsViewState.ConectionStatus(GoogleApiConnectionStatus(false))

            }
            .build()
        googleApiClient?.connect()
    }

    fun disconnectGoogleApiClient() {
        viewState.value = MapsViewState.ConectionStatus(GoogleApiConnectionStatus(false))
        if (googleApiClient?.isConnected == true) {
            googleApiClient?.disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun loadLastLocation(): Boolean = suspendCoroutine { continuation ->
        fun updateOriginByLocation(location: Location) {
            val latLng = LatLng(location.latitude, location.longitude)
            mapState.value = mapState.value?.copy(origin = latLng)
            continuation.resume(true)
        }

        fun waitForLocation() {
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 1000)
                .setFastestInterval(1 * 1000)
            locationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult?) {
                        super.onLocationResult(result)
                        locationClient.removeLocationUpdates(this)
                        val location = result?.lastLocation
                        if (location != null) {
                            updateOriginByLocation(location)
                        } else {
                            continuation.resume(false)
                        }
                    }
                }, null
            )
        }
        locationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    updateOriginByLocation(location)

                } ?: waitForLocation()

            }
            .addOnFailureListener {
                waitForLocation()
            }
            .addOnCanceledListener {
                continuation.resume(false)
            }
    }

    private suspend fun checkGpsStatus(): Boolean = suspendCoroutine { continuation ->
        val request = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .setAlwaysShow(true)
            .addLocationRequest(request)
        SettingsClient(getContext()).checkLocationSettings(locationSettingsRequest.build())
            .addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                    continuation.resume(true)
                } catch (exception: ApiException) {
                    continuation.resumeWithException(exception)
                }
            }
            .addOnCanceledListener {
                continuation.resume(false)
            }
    }

    fun requestLocation() {
        launch {
            try {
                checkGpsStatus()
                val success = withTimeout(2000) { loadLastLocation() }
                if (success) {
                    startLocationUpdates()
                } else {
                    viewState.value = MapsViewState.ShowError(true)
                }
            } catch (timeout: TimeoutCancellationException) {
                viewState.value = MapsViewState.ShowError(true)
            } catch (exception: ApiException) {
                viewState.value = MapsViewState.ShowError(true)

            }
        }
    }

    fun searchAddress(s: String) {
        launch {
            val geoCoder = Geocoder(getContext(), Locale.getDefault())
            viewState.value = MapsViewState.ShowAddres(withContext(Dispatchers.IO) {
                geoCoder.getFromLocationName(s, 10)
            } as ArrayList<Address>)
        }
    }

    fun setDestination(latLng: LatLng) {
        mapState.value = mapState.value?.copy(destination = latLng)
        loadRoute()
    }

    private fun loadRoute() {
        if (mapState.value != null) {
            val orig = mapState.value?.origin
            val dest = mapState.value?.destination
            if (orig != null && dest != null) {
                launch {
                    val route = withContext(Dispatchers.IO) {
                        RouteHttp.searchRoute(orig, dest)
                    }
                    mapState.value = mapState.value?.copy(route = route)
                }
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            val location = locationResult?.lastLocation
            viewState.value = location?.latitude?.let { LatLng(it, location.longitude) }?.let {
                MapsViewState.CurrentLocation(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5 * 1000)
            .setFastestInterval(1 * 1000)
        locationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    fun stopLocationUpdates() {
        LocationServices.getFusedLocationProviderClient(getContext())
            .removeLocationUpdates(locationCallback)
    }

    private fun getContext() = getApplication<Application>()

}