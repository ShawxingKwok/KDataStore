package pers.apollokwok.preferencestore.example.compose.data.ds

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)