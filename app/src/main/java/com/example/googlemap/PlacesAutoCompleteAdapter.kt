package com.example.googlemap

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import com.example.googlemap.databinding.RawPlacesBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import java.util.concurrent.TimeUnit


class PlacesAutoCompleteAdapter(context: Context?) : BaseAdapter(), Filterable {

    private val inflater = LayoutInflater.from(context)
    private var places = ArrayList<PlaceAutocomplete>()
    private val placesClient = Places.createClient(context!!)
    private var listener: ((Place?) -> Unit)? = null

    override fun getCount() = places.size

    override fun getItem(position: Int) = places[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: RawPlacesBinding = if (convertView == null) {
            RawPlacesBinding.inflate(inflater, parent, false)
        } else {
            RawPlacesBinding.bind(convertView)
        }

        val result = places[position]

        binding.tvAddress.text = result.area
        binding.tvAddressDesc.text = result.address
        binding.tvAddressDesc.visibility = View.VISIBLE
        binding.ViewDivider.visibility = if (position == places.size - 1) View.GONE else View.VISIBLE

        binding.llMain.setOnClickListener {
            getPlaceDetail(result.placeId.toString())
        }

        return binding.root
    }

    fun setListener(listener: (Place?) -> Unit) {
        this.listener = listener
    }

    override fun getFilter(): Filter = PlaceFilter()

    private inner class PlaceFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            if (!constraint.isNullOrEmpty()) {
                places = getPlacePredictions(constraint.toString())
                results.values = places
                results.count = places.size
            }
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }

    private fun getPlacePredictions(query: String): ArrayList<PlaceAutocomplete> {
        val resultList = ArrayList<PlaceAutocomplete>()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        val predictions = placesClient.findAutocompletePredictions(request)
        try {
            Tasks.await(predictions, 60, TimeUnit.SECONDS)
            predictions.result?.autocompletePredictions?.forEach {
                resultList.add(
                    PlaceAutocomplete(
                        it.placeId,
                        it.getPrimaryText(null).toString(),
                        it.getFullText(null).toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching predictions: ${e.localizedMessage}")
        }
        return resultList
    }

    private fun getPlaceDetail(placeId: String) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            listener?.invoke(response.place)
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e(TAG, "Place fetch error: ${exception.message}")
            }
        }
    }

    data class PlaceAutocomplete(val placeId: CharSequence, val area: CharSequence, val address: CharSequence)

    companion object {
        private const val TAG = "PlaceAutoCompleteAdap"
    }
}

