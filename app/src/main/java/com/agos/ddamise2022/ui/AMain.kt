package com.agos.ddamise2022.ui

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.agos.ddamise2022.Configuration
import com.agos.ddamise2022.R
import com.agos.ddamise2022.model.Location
import com.agos.ddamise2022.model.User
import com.agos.ddamise2022.service.Foreground
import com.agos.ddamise2022.service.ServiceHub
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.textfield.TextInputEditText
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException


class AMain : AppCompatActivity() {

    private lateinit var configuration: Configuration

    private var serviceIntent: Intent? = null
    private var myLocation: Location? = null

    lateinit var mapFragment: SupportMapFragment

    private var retrofit: Retrofit? = null

    lateinit var userName: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        configuration = Configuration.create(this@AMain)

        mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync {
            it.addMarker(
                MarkerOptions()
                    .position(LatLng(0.0, 0.0))
            )
        }

        userName = findViewById(R.id.username)

        /**
         * Retrofit
         */
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = unsafeOkHttpClient()?.addInterceptor(loggingInterceptor)?.build()

        retrofit = Retrofit.Builder()
            .baseUrl(configuration.urlBase)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun unsafeOkHttpClient(): OkHttpClient.Builder? {
        return try {
            val trustAllCerts: Array<TrustManager> = arrayOf(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }
            )

            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            val sslSocketFactory: SSLSocketFactory = sslContext.getSocketFactory()
            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        var intent: Intent? = null

        when (item.itemId) {
            R.id.action_search -> {
                Log.d(Configuration.tag, "Search")
                intent = Intent(this@AMain, ASearch::class.java)

            }
            R.id.action_firebase -> {
                Log.d(Configuration.tag, "Firebase")
                intent = Intent(this@AMain, AFirebase::class.java)
            }
            R.id.action_route -> {
                Log.d(Configuration.tag, "Route")
                intent = Intent(this@AMain, ARoutes::class.java)
            }
        }

        intent?.putExtra("myLocation", myLocation)
        startActivity(intent)

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (isAppOnForeground(applicationContext)) {
            LocalBroadcastManager.getInstance(applicationContext)
                .registerReceiver(messageReceiver, IntentFilter(Configuration.tag))
            if (serviceIntent == null) {
                serviceIntent = Intent(applicationContext, Foreground::class.java)
            }
            ContextCompat.startForegroundService(applicationContext, serviceIntent!!)
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(messageReceiver)
        if (serviceIntent != null) {
            stopService(serviceIntent)
        }
    }

    private fun isAppOnForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val extras = intent.extras
            val location = Location(
                latitude = extras?.get("latitude").toString().toDouble(),
                longitude = extras?.get("longitude").toString().toDouble(),
                accuracy = extras?.get("accuracy").toString().toDouble()
            )

            Log.d(Configuration.tag, "Location [${location.latitude};${location.longitude} / ${location.accuracy}]}")

            if (myLocation == null) {
                var myLatLng = LatLng(location.latitude, location.longitude)
                //center map
                mapFragment.getMapAsync {
                    it.clear()
                    it.addMarker(
                        MarkerOptions()
                            .position(myLatLng)
                            .title(getString(R.string.my_location))
                    )
                    it.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, configuration.defaultZoom))
                }
            }

            myLocation = location

            /**
             * Retrofit
             */
            retrofit?.create(ServiceHub::class.java)?.postLocation(
                User(
                    username = userName.text.toString(),
                    latitude = extras?.get("latitude").toString().toDouble(),
                    longitude = extras?.get("longitude").toString().toDouble(),
                    accuracy = extras?.get("accuracy").toString().toDouble()
                )
            )?.enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    Log.d(Configuration.tag, "Response ${response.message()}")
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.e(Configuration.tag, t.message, t)
                }
            })
        }
    }

}