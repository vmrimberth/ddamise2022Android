package com.agos.ddamise2022.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.agos.ddamise2022.Configuration
import com.agos.ddamise2022.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest


class Foreground : Service() {

    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.default_notification_foreground_channel_id)
            val description = getString(R.string.default_notification_channel_description)
            val notificationChannel = NotificationChannel(name, name, NotificationManager.IMPORTANCE_LOW)
            notificationChannel.description = description
            notificationChannel.enableVibration(true)
            notificationManager.deleteNotificationChannel(notificationChannel.id)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationIntent = Intent(this@Foreground, Foreground::class.java)
        val pendingIntent = PendingIntent.getActivity(this@Foreground, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this@Foreground, getString(R.string.default_notification_foreground_channel_id))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.message_location))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.i(Configuration.tag, locationResult.lastLocation.toString())

                val intent = Intent(Configuration.tag)
                intent.putExtra("latitude", locationResult.lastLocation?.latitude)
                intent.putExtra("longitude", locationResult.lastLocation?.longitude)
                intent.putExtra("accuracy", locationResult.lastLocation?.accuracy)
                LocalBroadcastManager.getInstance(this@Foreground).sendBroadcast(intent)
            }
        }

        val configuration = Configuration.create(this@Foreground)

        locationRequest = LocationRequest.create()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest!!.interval = (configuration.locationUpdateTime * 1000).toLong()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@Foreground)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback as LocationCallback, Looper.getMainLooper())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (locationCallback != null) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
        }
    }
}