package com.example.googlemap

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.databinding.ActivityStartEndLocationBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class StartEndLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartEndLocationBinding

    private val LOCATION_PERMISSION_REQUEST_CODE = 123
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var pickupLatitude: String = ""
    private var pickupLongitude: String = ""

    private var dropOffLatitude: String = ""
    private var dropOffLongitude: String = ""

    private var pickupAddress: String = ""
    private var dropOffAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartEndLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()

    }

    private fun initView(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationPermissionGranted()){
            requestLocationPermission()  // Location permission is not granted
        } else {
            getCurrentLocation()
        }

        binding.tvPickUpLocation.setOnClickListener {
            val destIntent = Intent(this, SearchLocationActivity::class.java)
            destIntent.putExtra("address", pickupAddress)
            pickupLocationActivityResultLauncher.launch(destIntent)
        }

        binding.tvDropOffLocation.setOnClickListener {
            val destIntent = Intent(this, SearchLocationActivity::class.java)
            dropOffLocationActivityResultLauncher.launch(destIntent)
        }

    }


    private val pickupLocationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result data here (result.data)
            pickupAddress = result.data!!.getStringExtra("address")!!
            pickupLatitude = result.data!!.getStringExtra("latitude")!!
            pickupLongitude = result.data!!.getStringExtra("longitude")!!

            Log.e("TAG", "pickupLatitude: ${pickupLatitude} - pickupLongitude: ${pickupLongitude} - pickupAddress: ${pickupAddress}")
            binding.tvPickUpLocation.text = pickupAddress
        }
    }

    private val dropOffLocationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            dropOffAddress = result.data!!.getStringExtra("address")!!
            dropOffLatitude = result.data!!.getStringExtra("latitude")!!
            dropOffLongitude = result.data!!.getStringExtra("longitude")!!

            Log.e("TAG", "dropOffLatitude: $dropOffLatitude - dropOffLongitude: $dropOffLongitude - dropOffAddress: $dropOffAddress")
            binding.tvDropOffLocation.text = dropOffAddress
        }
    }


    private  fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val pickupLatLng = LatLng(location.latitude, location.longitude)

                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses: List<Address> = geocoder.getFromLocation(pickupLatLng.latitude, pickupLatLng.longitude, 1)!!

                // Fetch and set current location
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    binding.tvPickUpLocation.text = address.getAddressLine(0)
                    pickupLatitude = pickupLatLng.latitude.toString()
                    pickupLongitude = pickupLatLng.longitude.toString()
                    pickupAddress = address.getAddressLine(0)
                }
            } else {
                isLocationEnabled()
            }
        }
    }


    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Snackbar.make(binding.main, "Location permission is required for better user experience", Snackbar.LENGTH_INDEFINITE)
                .setAction("Try Again") {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }.show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun isLocationEnabled() {

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { getCurrentLocation() }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, 101)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

    }

}