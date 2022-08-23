package com.agos.ddamise2022.service

import com.agos.ddamise2022.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ServiceHub {

    @POST("index.php")
    fun postLocation(@Body user: User): Call<Any>
}