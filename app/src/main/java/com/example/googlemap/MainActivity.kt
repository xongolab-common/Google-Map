package com.example.googlemap

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.googlemap.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager

import java.util.Locale


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding

    private lateinit var mMap: GoogleMap
    private var currentLocationMarker: Marker? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 123

    private var pickupLatitude: String = ""
    private var pickupLongitude: String = ""

    private var dropOffLatitude: String = ""
    private var dropOffLongitude: String = ""

    private var pickupAddress: String = ""
    private var dropOffAddress: String = ""

    private lateinit var clusterManager: ClusterManager<MyClusterItem>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()

    }

    private fun initView(){
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation.let { location ->
                    updateLocation(location!!.latitude, location.longitude)
                }
            }
        }

        binding.icCurrentLocation.setOnClickListener {
            if (isLocationPermissionGranted()) {
                fetchCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }

        binding.icDirection.setOnClickListener {
            startActivity(Intent(this, StartEndLocationActivity::class.java))
        }

        // StartEndLocationActivity

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

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateLocation(location.latitude, location.longitude)
                Log.e("Current Location", "Lat: ${location.latitude}, Lng: ${location.longitude}")
            } else {
                isLocationEnabled() // Request location updates if null
            }
        }.addOnFailureListener {
            Log.e("Error", "Failed to fetch location: ${it.message}")
        }
    }


    override fun onResume() {
        super.onResume()
        if (!isLocationPermissionGranted()) {
            requestLocationPermission()
        }
    }


    fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //enableMyLocation()
        setupClusterManager()
        startLocationUpdates()
    }

    private fun setupClusterManager() {
        clusterManager = ClusterManager(this, mMap)
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)

        // Add demo cluster items
        addClusterItems()
    }

    private fun addClusterItems() {
        // Example data — you can use your own pickup/drop data or anything else
        val locations = listOf(
            LatLng(28.7041, 77.1025), // Delhi
            LatLng(19.0760, 72.8777), // Mumbai
            LatLng(13.0827, 80.2707), // Chennai
            LatLng(12.9716, 77.5946), // Bangalore
            LatLng(22.5726, 88.3639)  // Kolkata
        )

        for ((index, latLng) in locations.withIndex()) {
            val offsetItem = MyClusterItem(
                latLng.latitude,
                latLng.longitude,
                "Location $index",
                "This is location $index"
            )
            clusterManager.addItem(offsetItem)
        }

        clusterManager.cluster()
    }


    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Snackbar.make(binding.main, "Location permission is required for batter user experience", Snackbar.LENGTH_INDEFINITE)
                .setAction("Try Again") {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                updateLocation(location.latitude, location.longitude)
                Log.e("TAG", "startLocationUpdates: $location")
            } else {
                isLocationEnabled()
            }
        }
    }

    private fun isLocationEnabled() {

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

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

    // Resize Marker
    private fun resizeBitmap(icon: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(icon, width, height, false)
    }

    private fun updateLocation(latitude: Double, longitude: Double) {
        val currentLatLng = LatLng(latitude, longitude)

        val originalMarkerIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_location_pin)
        val scaledMarkerIcon = resizeBitmap(originalMarkerIcon, 100, 100)
        val markerIcon = BitmapDescriptorFactory.fromBitmap(scaledMarkerIcon)

        if (currentLocationMarker == null) {
            currentLocationMarker = mMap.addMarker(
                MarkerOptions().position(currentLatLng).title("Your location").icon(markerIcon))
        } else {
            currentLocationMarker?.position = currentLatLng
            currentLocationMarker?.setIcon(markerIcon)
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                startLocationUpdates()
            } else {
                Snackbar.make(binding.main, "Location permission required", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Settings") {
                        openAppSettings()
                    }
                    .show()
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }

    // StartEndLocationActivity

    private val pickupLocationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result data here (result.data)
            pickupAddress = result.data!!.getStringExtra("address")!!
            pickupLatitude = result.data!!.getStringExtra("latitude")!!
            pickupLongitude = result.data!!.getStringExtra("longitude")!!

            Log.e("TAG", "pickupLatitude: ${pickupLatitude} - pickupLongitude: ${pickupLongitude} - pickupAddress: ${pickupAddress}")
            binding.tvPickUpLocation.text = pickupAddress
            drawPolyline()
        }
    }

    private val dropOffLocationActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            dropOffAddress = result.data!!.getStringExtra("address")!!
            dropOffLatitude = result.data!!.getStringExtra("latitude")!!
            dropOffLongitude = result.data!!.getStringExtra("longitude")!!

            Log.e("TAG", "dropOffLatitude: $dropOffLatitude - dropOffLongitude: $dropOffLongitude - dropOffAddress: $dropOffAddress")
            binding.tvDropOffLocation.text = dropOffAddress
            drawPolyline()
        }
    }

    private fun drawPolyline() {
        if (pickupLatitude.isNotEmpty() && pickupLongitude.isNotEmpty() &&
            dropOffLatitude.isNotEmpty() && dropOffLongitude.isNotEmpty()
        ) {
            val pickupLatLng = LatLng(pickupLatitude.toDouble(), pickupLongitude.toDouble())
            val dropOffLatLng = LatLng(dropOffLatitude.toDouble(), dropOffLongitude.toDouble())

            // Clear previous markers and polylines
            mMap.clear()

            // Add markers for pickup and drop-off locations
            mMap.addMarker(MarkerOptions().position(pickupLatLng).title("Pickup Location"))
            mMap.addMarker(MarkerOptions().position(dropOffLatLng).title("Drop-Off Location"))

            // Draw polyline between pickup and drop-off locations
            mMap.addPolyline(
                PolylineOptions()
                    .add(pickupLatLng, dropOffLatLng)
                    .width(8f)
                    .color(Color.BLUE)
            )

            // Adjust the camera view to include both locations
            val bounds = LatLngBounds.builder()
                .include(pickupLatLng)
                .include(dropOffLatLng)
                .build()

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } else {
            Log.e("Polyline", "Pickup or Drop-Off location is empty!")
        }
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
}


class MyClusterItem(
    private val lat: Double,
    private val lng: Double,
    private val title: String,
    private val snippet: String
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(lat, lng)
    override fun getTitle(): String = title
    override fun getSnippet(): String = snippet
}
