package com.agos.ddamise2022.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.agos.ddamise2022.Configuration
import com.agos.ddamise2022.R
import com.agos.ddamise2022.model.Location
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.*


class ASearch : AppCompatActivity() {

    private lateinit var myLocation: Location
    lateinit var myLatLong: LatLng
    lateinit var configuration: Configuration
    lateinit var mapFragment: SupportMapFragment
    lateinit var autocompleteFragment: AutocompleteSupportFragment

    lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.search)


        myLocation = intent?.extras?.getSerializable("myLocation") as Location
        myLatLong = LatLng(myLocation.latitude, myLocation.longitude)

        configuration = Configuration.create(this@ASearch)

        autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment

        mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync {
            it.addMarker(
                MarkerOptions()
                    .position(myLatLong)
                    .title(getString(R.string.my_location))
            )
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLong, configuration.defaultZoom))
        }

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.d(Configuration.tag, "Place:  ${place.id}/${place.name}")
                val placeFields: List<Place.Field> = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                val request = FetchPlaceRequest.newInstance(place.id!!, placeFields)
                placesClient.fetchPlace(request).addOnSuccessListener { response: FetchPlaceResponse ->
                    val placeResponse = response.place
                    Log.d(Configuration.tag, "Place found: ${placeResponse.name}")

                    mapFragment.getMapAsync {
                        it.clear()
                        it.addMarker(
                            MarkerOptions()
                                .position(placeResponse.latLng!!)
                                .title(placeResponse.name)
                        )
                        it.addCircle(
                            CircleOptions()
                                .center(placeResponse.latLng!!)
                                .radius(10.0)
                                .strokeColor(Color.RED)
                                .fillColor(0x22FF0000)
                                .strokeWidth(3f)
                        )

                        it.moveCamera(CameraUpdateFactory.newLatLngZoom(placeResponse.latLng!!, 20f))
                    }
                }.addOnFailureListener { exception: Exception ->
                    if (exception is ApiException) {
                        Log.e(Configuration.tag, "Place not found: ${exception.message}", exception)
                    }
                }
            }

            override fun onError(status: Status) {
                Log.w(Configuration.tag, "An error occurred: $status")
            }
        })

        Places.initialize(applicationContext, getString(R.string.google_map_key))
        placesClient = Places.createClient(this)

    }
}