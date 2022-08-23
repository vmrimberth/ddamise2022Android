package com.agos.ddamise2022

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.agos.ddamise2022.extensions.toIntOrDefault


class Configuration {


    var environment: String = "debug"
    var urlBase: String = ""
    var defaultZoom: Float = 0F
    var locationUpdateTime: Int = 0
    var locationUpdateDistance: Int = 0

    companion object {

        @JvmStatic
        val tag: String = "DDAMISE2022"

        @JvmStatic
        fun create(context: Context): Configuration {
            val env = Configuration()
            try {
                val metaData: Bundle = context.getPackageManager()
                    .getApplicationInfo(
                        context.getPackageName(),
                        PackageManager.GET_META_DATA
                    ).metaData
                env.environment = metaData["com.agos.ddamise2022.ENVIRONMENT"].toString()
                env.urlBase = metaData["com.agos.ddamise2022.URLBASE"].toString()
                env.defaultZoom = metaData["com.agos.ddamise2022.DEFAULTZOOM"].toString().toIntOrDefault().toFloat()
                env.locationUpdateTime = metaData["com.agos.ddamise2022.LOCATIONUPDATETIME"].toString().toIntOrDefault()
                env.locationUpdateDistance = metaData["com.agos.ddamise2022.LOCATIONUPDATEDISTANCE"].toString().toIntOrDefault()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return env
        }
    }

}