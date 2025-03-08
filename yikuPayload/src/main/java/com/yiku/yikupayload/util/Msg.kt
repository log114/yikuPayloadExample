package com.yiku.yikupayload.util

data class Msg(
    private val header: Byte = 0x8D.toByte(),

    private var len: Byte = 0.toByte(),

    var msgId: Byte = 0x00.toByte(),

    var payload: ByteArray = ByteArray(0),

    private var checksum: Byte = 0x00.toByte()
) {
    fun getMsg(): ByteArray {
        val crcUtil = CrcUtil()
        len = (payload.size).toByte()
        var checksumData = ByteArray(0)
        checksumData += len
        checksumData += msgId
        if (payload.isNotEmpty()) {
            checksumData += payload
        }
//        checksumData += checksum
        checksum = crcUtil.crc8_cal(checksumData, checksumData.size)
        var data = ByteArray(0)
        data += header
        data += len
        data += msgId
        data += payload
        data += checksum
        return data
    }
}


class MsgRecv {

}