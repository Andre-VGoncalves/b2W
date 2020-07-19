package com.agoncalves.b2wmaps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.agoncalves.b2wmaps.model.GoogleApiConnectionStatus
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_ERROR_PLAY_SERVICES = 1
        private const val REQUEST_PERMISSIONS = 2
        private const val REQUEST_CHECK_GPS = 3
        private const val EXTRA_GPS_DIALOG = "gpsDialogIsOpen"
    }

    private lateinit var viewModel: MapViewModel
    private val fragment: MapFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fragmentMap) as MapFragment
    }

    private var isGpsDialogOpened: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        viewModel = ViewModelProvider(this).get(MapViewModel::class.java)

        isGpsDialogOpened = savedInstanceState?.getBoolean(EXTRA_GPS_DIALOG) ?: false
    }

    override fun onStart() {
        super.onStart()
        fragment.getMapAsync {
            initUi()
            viewModel.connectGoogleApiClient()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.disconnectGoogleApiClient()
        viewModel.stopLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_GPS_DIALOG, isGpsDialogOpened)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ERROR_PLAY_SERVICES && resultCode == Activity.RESULT_OK) {
            viewModel.connectGoogleApiClient()
        } else if (requestCode == REQUEST_CHECK_GPS) {
            isGpsDialogOpened = false
            if (resultCode == RESULT_OK) {
                loadLastLocation()
            } else {
                finish()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && permissions.isNotEmpty()) {
            if (permissions.firstOrNull() == Manifest.permission.ACCESS_FINE_LOCATION &&
                grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            ) {
                loadLastLocation()
            } else {
                showError("é necessaria a permissão")
                finish()
            }
        }
    }

    private fun initUi() {
        viewModel.viewState.observe(this, Observer {
            when (it) {
                is MapsViewState.ConectionStatus -> showLocation(it.googleConection)
                is MapsViewState.ShowError -> handleLocationError()
                is MapsViewState.ShowAddres -> showAddressListDialog(it.addres)
            }
        })

        btnSearch.setOnClickListener {
            searchAddress()
        }
    }

    private fun showLocation(status: GoogleApiConnectionStatus) {
        if (status.success) {
            loadLastLocation()
        } else {
            status.connectionResult?.let {
                handleConnectionError(it)
            }
        }
    }


    private fun handleLocationError() {
        if (!isGpsDialogOpened) {
            isGpsDialogOpened = true
            showError("GPS Desativado")
        } else {
            showError("Localização Indisponivel")
        }


    }

    private fun loadLastLocation() {
        if (!hasPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS
            )
            return
        }
        viewModel.requestLocation()
    }

    private fun handleConnectionError(result: ConnectionResult) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(
                    this, REQUEST_ERROR_PLAY_SERVICES
                )
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
            }
        } else {
            showPlayServicesErrorMessage(result.errorCode)
        }
    }

    private fun hasPermission(): Boolean {
        val granted = PackageManager.PERMISSION_GRANTED
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == granted
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showPlayServicesErrorMessage(errorCode: Int) {
        GoogleApiAvailability.getInstance()
            .getErrorDialog(this, errorCode, REQUEST_ERROR_PLAY_SERVICES)
            .show()
    }

    private fun searchAddress() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
             imm.hideSoftInputFromWindow(edtSearch.windowToken, 0)
               viewModel.searchAddress(edtSearch.text.toString())
    }



    private fun showAddressListDialog(addresses: List<Address>) {
           AddressListFragment.newInstance(addresses).show(supportFragmentManager, null)
    }
}
