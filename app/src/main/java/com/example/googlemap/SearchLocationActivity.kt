package com.example.googlemap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.databinding.ActivitySearchLocationBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place

class SearchLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchLocationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySearchLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initView(){
        Places.initialize(this, resources.getString(R.string.google_maps_key))

        if (intent.hasExtra("address")) {
           binding.edtPickUpLocation.setText(intent.getStringExtra("address"))
        }

        isLocationEnabled()

        binding.apply {
            imgClear.setOnClickListener {
                edtPickUpLocation.setText("")
            }

                val placeAutocompleteAdapter = PlacesAutoCompleteAdapter(this@SearchLocationActivity)
                edtPickUpLocation.threshold = 3
                edtPickUpLocation.setAdapter(placeAutocompleteAdapter)

            placeAutocompleteAdapter.setListener { place ->
                hideSoftKeyboard()

                val resultIntent = Intent()
                // Set the data when BackPress from SearchActivity
                resultIntent.putExtra("address", place!!.address?.trim())
                resultIntent.putExtra("latitude", place.latLng?.latitude.toString())
                resultIntent.putExtra("longitude", place.latLng?.longitude.toString())

                Log.e(
                    "initView",
                    " initView Selected Address: ${place.address}, Latitude: ${place.latLng?.latitude}, Longitude: ${place.latLng?.longitude}"
                )

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
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
          //  getCurrentLocation()
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
}



// Hide Keyboard
fun Activity.hideSoftKeyboard() {
    val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = this.currentFocus
    if (view == null)
        view = View(this)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}