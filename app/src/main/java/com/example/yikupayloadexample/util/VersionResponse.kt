package com.example.yikupayloadexample.util

data class VersionResponse(
    val result: String,
    val message: String,
    val data: VersionData
)

data class VersionData(
    val version: String,
    val description: String,
    val downloadUrl: String,
    val releaseDate: String,
    val updateNote: String
)
