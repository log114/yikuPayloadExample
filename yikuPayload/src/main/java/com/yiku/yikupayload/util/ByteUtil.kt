package com.yiku.yikupayload.util

import kotlin.experimental.and

public fun int16ToByteArray(i: Int): ByteArray {
    val arr = ByteArray(2)
    arr[0] = (i shl 8).toByte()
    arr[1] = (i and 0xff).toByte()
    return arr
}



fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    var hex = ""
    for (b in bytes) {
        val st = String.format("%02X", b)
        hex += "$st "
    }
    return hex
}