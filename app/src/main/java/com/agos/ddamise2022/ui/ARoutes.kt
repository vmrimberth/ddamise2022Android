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
import com.agos.ddamise2022.service.ServiceHub
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
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


class ARoutes : AppCompatActivity() {

    private lateinit var myLocation: Location
    lateinit var myLatLong: LatLng
    private lateinit var configuration: Configuration
    private lateinit var mapFragment: SupportMapFragment
    var lastLatLong: LatLng? = null
    private var retrofit: Retrofit? = null
    lateinit var userName: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routes)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.routes)

        myLocation = intent?.extras?.getSerializable("myLocation") as Location
        myLatLong = LatLng(myLocation.latitude, myLocation.longitude)

        configuration = Configuration.create(this@ARoutes)

        userName = findViewById(R.id.username)

        mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            map.addCircle(
                CircleOptions()
                    .center(myLatLong)
                    .radius(20.0)
                    .strokeColor(Color.RED)
                    .fillColor(0x22FF0000)
                    .strokeWidth(3f)
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLong, configuration.defaultZoom))

            map.setOnMapClickListener {
                map.addMarker(
                    MarkerOptions()
                        .position(it)
                )

                if (lastLatLong != null) {
                    map.addPolyline(
                        PolylineOptions()
                            .add(lastLatLong, it)
                            .color(Color.RED)
                    )
                } else {
                    map.addPolyline(
                        PolylineOptions()
                            .add(myLatLong, it)
                            .color(Color.RED)
                    )
                }

                lastLatLong = it

                /**
                 * Retrofit
                 */
                retrofit?.create(ServiceHub::class.java)?.postLocation(
                    User(
                        username = userName.text.toString(),
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracy = 0.0
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
        menuInflater.inflate(R.menu.menu_routes, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        mapFragment.getMapAsync {
            it.clear()
            it.addCircle(
                CircleOptions()
                    .center(myLatLong)
                    .radius(20.0)
                    .strokeColor(Color.RED)
                    .fillColor(0x22FF0000)
                    .strokeWidth(3f)
            )
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLong, configuration.defaultZoom))

            lastLatLong = null
        }

        return super.onOptionsItemSelected(item)
    }
}