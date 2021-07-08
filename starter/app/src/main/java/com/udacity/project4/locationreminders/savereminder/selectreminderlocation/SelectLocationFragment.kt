package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment() {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var googleMap: GoogleMap
    private var selectedPoi: PointOfInterest? = null
    private lateinit var locationManager: FusedLocationProviderClient

    private var snackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        locationManager = LocationServices.getFusedLocationProviderClient(requireContext())

        _viewModel.showSnackBar.observe(viewLifecycleOwner, Observer {

        })

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            googleMap = map
            setPoiMapClick(googleMap)
            setMapStyle(googleMap)
            setMapClick(googleMap)
            enableUserLocation()
        }

        binding.savePoiButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun clearMapOnClick(map: GoogleMap) {
        googleMap.clear()
        selectedPoi = null
    }

    private fun setMapClick(map: GoogleMap) {
        googleMap.setOnMapLongClickListener {
            clearMapOnClick(map)
            selectedPoi = PointOfInterest(it, "Custom Location", "Custom Location")
            val snippet = String.format(
                Locale.getDefault(),
                resources.getString(R.string.lat_long_snippet),
                it.latitude,
                it.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Custom Location")
                    .snippet(snippet)
            )
        }
    }

    private fun setPoiMapClick(map: GoogleMap) {
        googleMap.setOnPoiClickListener { poi ->
            clearMapOnClick(map)
            selectedPoi = poi
            val snippet = String.format(
                Locale.getDefault(),
                resources.getString(R.string.lat_long_snippet),
                poi.latLng.latitude,
                poi.latLng.longitude
            )

            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .snippet(snippet)
            )
            poiMarker.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireActivity(),
                R.raw.map_style)
            )

            if (!success) { // Failed to properly load map resource
                Log.e("SelectLocationFragment", "Failed to load Map style resouce")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("SelectLocationFragment", "Style does not exist")
        }
    }

    private fun onLocationSelected() {
        _viewModel.validatePoiSelected(selectedPoi)

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationManager.lastLocation.addOnSuccessListener {
                zoomToCurrentLocation(it)
            }
            binding.savePoiButton.visibility = View.VISIBLE
            googleMap.isMyLocationEnabled = true
        } else {
            // If the permission hasn't been enabled, send a request to the user to grant it
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            binding.savePoiButton.visibility = View.GONE
        }
    }

    private fun zoomToCurrentLocation(location: Location) {
        val currentLocationLatLng = LatLng(location.latitude, location.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocationLatLng, 15f))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableUserLocation()
            } else {
                snackbar = Snackbar.make(
                    requireContext(),
                    binding.savePoiButton,
                    getString(R.string.permission_denied_explanation),
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        startActivityForResult(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        }, LOCATION_PERMISSION_REQUEST_CODE)
                    }
                snackbar?.show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE ) {
            enableUserLocation()
        }
    }

    override fun onDestroy() {
        snackbar?.dismiss()
        super.onDestroy()
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}
