package pers.shawxingkwok.kdatastore.demo.settings

import kotlinx.serialization.Serializable

@Serializable
data class Info(val firstName: String, val lastName: String, val isMale: Boolean)