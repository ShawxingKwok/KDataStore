package pers.apollokwok.prefstore.example.compose.data.ds

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)