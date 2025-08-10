package com.example.wedsnap20.model

data class Album(
    val eventId: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val hostUid: String = ""
)
