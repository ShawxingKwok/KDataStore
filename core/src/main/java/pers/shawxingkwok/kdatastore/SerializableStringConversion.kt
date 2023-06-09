package pers.shawxingkwok.kdatastore

import android.util.Base64
import pers.shawxingkwok.ktutil.updateIf
import java.io.*

@PublishedApi
internal fun Serializable.convertToString(cypher: Cypher?): String {
    val bos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(bos)
    oos.writeObject(this)
    oos.close()
    bos.close()
    val bytes = bos.toByteArray().updateIf({ cypher != null }){ cypher!!.encrypt(it) }
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

@PublishedApi
internal inline fun <reified S: Serializable> String.recoverToSerializable(cypher: Cypher?): S {
    val bytes = Base64.decode(this, Base64.DEFAULT)
        .updateIf({ cypher != null }){ cypher!!.decrypt(it) }

    val bis = ByteArrayInputStream(bytes)
    val ois = ObjectInputStream(bis)
    ois.close()
    bis.close()

    return ois.readObject() as S
}