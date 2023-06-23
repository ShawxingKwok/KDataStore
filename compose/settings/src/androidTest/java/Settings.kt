import pers.shawxingkwok.kdatastore.KDataStore
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

object Settings : KDataStore("settings") {
    val isVip by storeNullableBool()
    val age by storeNullableInt()
    val id by storeNullableLong()
    val latitude by storeNullableFloat()
    val rate by storeNullableDouble()
    val username by storeNullableString()
    val timeUnit by storeNullableEnum<TimeUnit>()
    val price by storeNullableJavaSerializable<BigDecimal>()
    val location by storeNullableKtSerializable<Location>()
    val user by storeNullableAny<User, Array<*>>(
        convert = { arrayOf(it.id, it.password, it.location.latitude, it.location.longitude) },
        recover = {
            val (id, password, latitude, longitude) = it
            val location = Location(latitude as Float, longitude as Float)
            User(id as Long, password as String, location)
        },
    )
}


data class Location(val latitude: Float, val longitude: Float)

data class User(val id: Long, val password: String, val location: Location)



// val isVip by storeBool(false)
// val id by storeLong(0)
//
// init {
//     val name = "preferences"
//     val sp = appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
//
//     if (sp.all.any()){
//         isVip.value = sp.getBoolean(TODO("IS_VIP_KEY"), false)
//         id.value = sp.getLong(TODO("LONG_KEY"), 0)
//
//         appContext.deleteSharedPreferences(name)
//     }
// }