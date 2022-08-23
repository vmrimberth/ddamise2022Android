package com.agos.ddamise2022.model

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class User(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Double? = null,
    val username: String? = null
) : Serializable {

}