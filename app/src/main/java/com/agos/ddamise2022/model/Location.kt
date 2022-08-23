package com.agos.ddamise2022.model

import java.io.Serializable

data class Location(
    var latitude: Double,
    var longitude: Double,
    var accuracy: Double
) : Serializable