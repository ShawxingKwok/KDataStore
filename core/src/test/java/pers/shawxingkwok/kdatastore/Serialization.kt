package pers.shawxingkwok.kdatastore

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class Serialization{
    @OptIn(ExperimentalTime::class)
    @Test
    fun ktSerializable(){
        val b = B("S", Language.ENGLISH)
        val a = A(1, 1.2, b)

        repeat(5){
            measureTime {
                val converted = Json.encodeToString(a)
                Json.decodeFromString<A>(converted).let(::println)
            }
            .let(::println)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun javaSerializable(){
        val jb = JB("S", Language.ENGLISH)
        val ja = JA(1, 1.2, jb)

        repeat(5){
            measureTime {
                val bos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(bos)
                oos.writeObject(ja)
                oos.close()
                bos.close()

                val bytes = bos.toByteArray()
                val bis = ByteArrayInputStream(bytes)
                val ois = ObjectInputStream(bis)
                ois.close()
                bis.close()
                (ois.readObject() as JA).let(::println)
            }
            .let(::println)
        }
    }
}
@Serializable
data class A(val i: Int, val d: Double, val b: B)

@Serializable
data class B(val s: String, val language: Language)

enum class Language{
    ENGLISH
}

data class JA(val i: Int, val d: Double, val b: JB) : java.io.Serializable

data class JB(val s: String, val language: Language) : java.io.Serializable