package com.yiku.yikupayload.util

interface MsgCallback {
    //    fun setId(id: String)
    fun getId(): String
    fun onMsg(msg: ByteArray)
}