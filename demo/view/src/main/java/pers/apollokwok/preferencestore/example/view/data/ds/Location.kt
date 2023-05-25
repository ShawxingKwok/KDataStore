package pers.apollokwok.preferencestore.example.view.data.ds

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)