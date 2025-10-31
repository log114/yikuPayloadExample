package com.example.yikupayloadexample.util

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("your-version-api-endpoint") // 替换为您的实际API地址
    suspend fun checkVersion(): Response<VersionResponse>
}