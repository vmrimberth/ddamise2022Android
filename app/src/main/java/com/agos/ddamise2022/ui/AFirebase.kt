package com.agos.ddamise2022.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.agos.ddamise2022.Configuration
import com.agos.ddamise2022.R
import com.agos.ddamise2022.model.Location
import com.agos.ddamise2022.model.User
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class AFirebase : AppCompatActivity() {

    lateinit var myLocation: Location
    lateinit var myLatLong: LatLng
    private lateinit var configuration: Configuration
    lateinit var mapFragment: SupportMapFragment
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var usersReference: DatabaseReference

    lateinit var username: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.firebase)


        username = findViewById(R.id.username)

        myLocation = intent?.extras?.getSerializable("myLocation") as Location
        myLatLong = LatLng(myLocation.latitude, myLocation.longitude)

        configuration = Configuration.create(this@AFirebase)

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

        /**
         * Firebase
         */
        firebaseDatabase = Firebase.database
        usersReference = firebaseDatabase.getReference("users")

        /**
         * Getting Markers
         */
        usersReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<User>()
                Log.d(Configuration.tag, "User : $value")
                mapFragment.getMapAsync {
                    it.addMarker(
                        MarkerOptions()
                            .position(LatLng(value?.latitude!!, value?.longitude!!))
                            .title(value.username)
                    )
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_firebase, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val key = usersReference.push().key
        usersReference.child(key.toString())
            .setValue(
                User(
                    latitude = myLocation.latitude,
                    longitude = myLocation.longitude,
                    accuracy = myLocation.accuracy,
                    username = username.text.toString()
                )
            )

        mapFragment.getMapAsync {
            it.addCircle(
                CircleOptions()
                    .center(myLatLong)
                    .radius(10.0)
                    .strokeColor(Color.RED)
                    .fillColor(0x22FF0000)
                    .strokeWidth(3f)
            )

            it.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLong, 20f))
        }

        return super.onOptionsItemSelected(item)
    }
}