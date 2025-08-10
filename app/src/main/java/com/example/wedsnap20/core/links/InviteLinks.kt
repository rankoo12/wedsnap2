package com.example.wedsnap20.core.links

object InviteLinks {
    private const val BASE = "https://wedsnapv2.web.app/join"
    fun forEvent(eventId: String) = "$BASE/$eventId"
}
