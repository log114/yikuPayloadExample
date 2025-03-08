package com.yiku.yikupayload.service

import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.yiku.yikupayload.protocol.ALARM_PLAY
import com.yiku.yikupayload.protocol.AUDIO_DEL
import com.yiku.yikupayload.protocol.AUDIO_PLAY
import com.yiku.yikupayload.protocol.DISABLE_RADIO
import com.yiku.yikupayload.protocol.GET_AUDIO_FILES
import com.yiku.yikupayload.protocol.REAL_TIME_SHOUT
import com.yiku.yikupayload.protocol.REBOOT
import com.yiku.yikupayload.protocol.RESTART_RADIO
import com.yiku.yikupayload.protocol.SET_VOLUME
import com.yiku.yikupayload.protocol.START_RADIO
import com.yiku.yikupayload.protocol.STOP_ALARM_PLAY
import com.yiku.yikupayload.protocol.STOP_AUDIO_PLAY
import com.yiku.yikupayload.protocol.STOP_RADIO
import com.yiku.yikupayload.protocol.STOP_REAL_TIME_SHOUT
import com.yiku.yikupayload.protocol.STOP_TTS_LOOP_PLAY
import com.yiku.yikupayload.protocol.TTS_LOOP_PLAY
import com.yiku.yikupayload.protocol.TTS_LOOP_PLAY_V2
import com.yiku.yikupayload.protocol.TTS_PLAY
import com.yiku.yikupayload.protocol.TTS_PLAY_V2
import com.yiku.yikupayload.util.MsgCallback
import com.yiku.yikupayload.util.MsgRecv
import com.yiku.yikupayload.util.OpusUtils
import com.yiku.yikupayload.util.Uilts
import com.yiku.yikupayload.util.VehiclePlatform
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread
import okhttp3.OkHttpClient
import okhttp3.Request
import com.alibaba.fastjson.JSONObject
import com.yiku.yikupayload.util.ProgressRequestBody
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody


interface UploadFileCallback {
    fun onUploadPackageSuccess(totalNum: Int, finishNum: Int);
}


interface GetAudioFilesCallback {
    fun onResult(files: String);
}


open class BaseMegaphoneService {
    private var loopTTSPlaying: Boolean = false
    private val TAG = "BaseMegaphoneService"
    private var sharedPreferences: SharedPreferences? = null
    private var recordFile = File.createTempFile("temp", ".pcm") // 最后需要转wav
    var msgCallbacks: List<MsgCallback> = ArrayList()

    lateinit var platform: VehiclePlatform
    var isRecording: Boolean = false
    var isStartRecord: Boolean = false
    var mAudioRecord: AudioRecord? = null
    var isPlayAlarm = false
    var getAudioFilesCallback: GetAudioFilesCallback? = null
    private val client = OkHttpClient()
    private var host = ""


    private lateinit var servoControlOut: OutputStream
    private var servoControlClient: Socket? = null
    open fun registMsgCallback(msgCallback: MsgCallback) {
        this.msgCallbacks += msgCallback
    }

    open fun unRegistMsgCallback(id: String) {
//        this.msgCallbacks += msgCallback
        val callbacks = ArrayList<MsgCallback>()
        for (callback in this.msgCallbacks) {
            if (id == callback.getId()) {
                continue
            }
            callbacks += callback
        }
        this.msgCallbacks = callbacks
    }

    open fun connect(): Boolean {
        return false
    }

    open fun connect(callback: MsgRecv): Boolean {
        return false
    }

    open fun getIsConnected(): Boolean {
        return false
    }

    open fun getIsConnectedYA3(): Boolean {
        return false
    }

    open fun setIp(ip: String) {

    }
    open fun getIp(): String {
        return host
    }

    open fun setHost(host: String) {
        this.host = host
    }

    open fun getHost(): String {
        return host
    }

    open fun sendData2Payload(data: ByteArray): Int {
        return 0
    }


    private fun connectServoControlServer(): Boolean {
        if (servoControlClient != null && servoControlClient!!.isConnected) {
            return true
        }
        if (getHost().isEmpty()) {
            Log.e(TAG,"host未初始化")
        }
        //开启一个链接，需要指定地址和端口
        return try {
            servoControlClient = Socket(getHost(), 12345)
            servoControlOut = servoControlClient!!.getOutputStream()
            true
        } catch (e: Exception) {
//            Log.e(TAG, "connect error:${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 喊话器舵机控制，占空比范围80-220
     */
    open fun servoControl(dutyCycle: UInt) {
        Log.i(TAG, "servoControl....")
        val dc = dutyCycle + 80u
        if (!connectServoControlServer()) {
            Log.i(TAG, "not connect....")
            Log.i(TAG,"该版本设备不支持APP控制俯仰")
            return
        }
        val data: UInt = if (dc < 80u) {
            80u
        } else (if (dc > 220u) {
            220u
        } else {
            dc
        })

        val sendData = ByteArray(2)
        sendData[0] = 0x8d.toByte()
        sendData[1] = data.toByte()
        Log.i(TAG, "sendData:${sendData.asList()}")
        servoControlOut.write(sendData)
    }

    fun playAlarm() {
        val sendData = ALARM_PLAY.toByteArray()
        // 播放警报
        isPlayAlarm = true
        sendData2Payload(sendData)
    }

    fun reboot(){
        val sendData = REBOOT.toByteArray()
        sendData2Payload(sendData)
    }

    fun stopPlayAlarm() {
        val sendData = STOP_ALARM_PLAY.toByteArray()
        // 播放警报
        isPlayAlarm = false
        sendData2Payload(sendData)
    }

    fun setVolume(volume: Int) {
        var sendData = SET_VOLUME.toByteArray()
        val arr = Integer.toHexString(volume).toByteArray()
        if (arr.size == 1) {
            // 个位数特殊处理
            sendData += '0'.code.toByte();
            sendData += arr[0]
        } else {
            sendData += arr[0]
            sendData += arr[1]
        }
        sendData2Payload(sendData)
    }

    fun disableRadio() {
        val sendData = DISABLE_RADIO.toByteArray()
        sendData2Payload(sendData)
    }
    fun restartRadio() {
        val sendData = RESTART_RADIO.toByteArray()
        sendData2Payload(sendData)
    }

    open fun startRealTimeShout(isDisableRadio: Boolean) {
        var audioSource = MediaRecorder.AudioSource.MIC //来源
        if (platform == VehiclePlatform.H30) {
            audioSource = MediaRecorder.AudioSource.MIC //来源
        }
        val rate = 8000 //采样频率
        val track = AudioFormat.CHANNEL_IN_MONO //声道
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT //格式
        var bufferSize = 960
        if (platform == VehiclePlatform.H30) {
            bufferSize = 640
        }
        Log.i(TAG, "startRecord...")
        if (mAudioRecord == null) {
            mAudioRecord = AudioRecord(
                audioSource, rate,
                track, audioFormat, bufferSize
            )
        }
        val data = ByteArray(bufferSize)
        mAudioRecord!!.startRecording()
        isRecording = true

        val opusUtils = OpusUtils.getInstant()
        thread {
            val createEncoder = opusUtils.createEncoder(rate, 1, 1)
            while (isRecording) {
                val read = mAudioRecord!!.read(data, 0, bufferSize)
                val ret = ByteArray(bufferSize / 8)
                val rc = opusUtils.encode(
                    createEncoder, Uilts.byteArrayToShortArray(data), 0, ret
                )
                var sendData = REAL_TIME_SHOUT.toByteArray()
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        sendData += ret
                        sendData2Payload(sendData)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                Thread.sleep(10)
            }
        }
    }

    fun stopRealTimeShout() {
        isRecording = false
        if (mAudioRecord != null) {
            mAudioRecord!!.stop()
        }
//        mAudioRecord?.stop()
//        mAudioRecord?.release()
//        mAudioRecord = null
        // 发送停止标识，关闭喊话器的功放
        val sendData = STOP_REAL_TIME_SHOUT.toByteArray()
        sendData2Payload(sendData)
    }

    open fun startRecord() {
        var audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION //来源
        if (platform == VehiclePlatform.H30) {
            audioSource = MediaRecorder.AudioSource.MIC //来源
        }
        val rate = 8000 //采样频率
        val track = AudioFormat.CHANNEL_IN_MONO //声道
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT //格式
        var bufferSize = 960
        if (platform == VehiclePlatform.H30) {
            bufferSize = 640
        }
        Log.i(TAG, "startRecord...")
        mAudioRecord = AudioRecord(
            audioSource, rate,
            track, audioFormat, bufferSize
        )
        val data = ByteArray(bufferSize)
        mAudioRecord!!.startRecording()
        isStartRecord = true

        val writer = FileWriter(recordFile)
        writer.write("")// 清空文件内容

        thread {
            while (isStartRecord) {
                val read = mAudioRecord!!.read(data, 0, bufferSize)
                writer.write(data.toString())
                Thread.sleep(10)
            }
            writer.close()
            println("录音文件大小: " + recordFile.length())
        }
    }

    open fun stopRecord() {
        isStartRecord = false
        mAudioRecord?.release()
        mAudioRecord = null
    }

    open fun getRecordFile(): File? {
        return recordFile;
    }

    fun tts(ttsText: String) {
        var sendData = TTS_PLAY.toByteArray()
        sendData += ttsText.encodeToByteArray();
        sendData2Payload(sendData)
    }


    fun startLoopTts(ttsText: String) {
        var sendData = TTS_LOOP_PLAY.toByteArray()
        sendData += ttsText.encodeToByteArray()
        sendData2Payload(sendData)
        loopTTSPlaying = true
    }

    fun ttsV2(ttsText: String, voice: Int) {
        var sendData = TTS_PLAY_V2.toByteArray()
        sendData += voice.toString().toByteArray() + ttsText.encodeToByteArray();
        sendData2Payload(sendData)
    }


    fun startLoopTtsV2(ttsText: String, voice: Int) {
        var sendData = TTS_LOOP_PLAY_V2.toByteArray()
        sendData += voice.toString().toByteArray() + ttsText.encodeToByteArray()
        sendData2Payload(sendData)
        loopTTSPlaying = true
    }

    fun stopLoopTts() {
        val sendData = STOP_TTS_LOOP_PLAY.toByteArray()
        sendData2Payload(sendData)
        loopTTSPlaying = false
    }

    @Deprecated(message = "该方法已弃用，请使用uploadFileForHttp")
    open fun uploadFile(bArrs: List<ByteArray>, callback: UploadFileCallback?) {
        uploadFile(bArrs, 0, callback)
    }

    @Deprecated(message = "该方法已弃用，请使用uploadFileForHttp")
    private fun uploadFile(bArrs: List<ByteArray>, packageNum: Int, callback: UploadFileCallback?) {
//        var sendNum = 0;
//        Log.i(
//            TAG,
//            "uploadFile packageNum:${packageNum}, data:${bArrs[packageNum].contentToString()}"
//        )
//        if (packageNum % 1000 == 0) {
//            System.gc()
//        }
        sendData2Payload(bArrs[packageNum])
        if (packageNum + 1 == bArrs.size) {
            Log.i(TAG, "uploadFile完成...")
            return
        }
        callback?.onUploadPackageSuccess(bArrs.size, packageNum + 1)
        Thread.sleep(100)
        uploadFile(bArrs, packageNum + 1, callback)
    }


    @Deprecated(message = "该方法已弃用，请使用delFile")
    fun delAudio(fileName: String) {
        var sendData = AUDIO_DEL.toByteArray()
        sendData += fileName.encodeToByteArray()
        sendData2Payload(sendData)
    }

    @Deprecated(message = "该方法已弃用，请使用fetchFiles")
    fun getAudioList(callback: GetAudioFilesCallback) {
        getAudioFilesCallback = callback
        sendData2Payload(GET_AUDIO_FILES.toByteArray())

    }

    fun playAudio(audioName: String) {
        stopPlayAudio()// 先关闭之前播放的内容
        Thread.sleep(200)
        val sendData = "${AUDIO_PLAY}0".toByteArray() + audioName.toByteArray()
        sendData2Payload(sendData)
    }

    fun stopPlayAudio() {
        sendData2Payload(STOP_AUDIO_PLAY.toByteArray())
    }

    fun startLoopPlayAudio(audioName: String) {
        val sendData = "${AUDIO_PLAY}1".toByteArray() + audioName.toByteArray()
        sendData2Payload(sendData)
    }

    fun stopLoopPlayAudio() {
        stopPlayAudio()
    }

    fun startRadio() {
        val sendData = START_RADIO.toByteArray()
        sendData2Payload(sendData)
    }

    fun stopRadio() {
        val sendData = STOP_RADIO.toByteArray()
        sendData2Payload(sendData)
    }

    fun fetchFiles(): ArrayList<String>? {
        val request =
            Request.Builder().url("http://" + getHost() + ":8222/fetch-files").get().build()

        val response = client.newCall(request).execute()
        val respStr = response.body?.string()
        Log.i(TAG, "获取数据：${respStr}")
        val jsonResp = JSONObject.parseObject(respStr)
        if (jsonResp.getIntValue("code") == 0) {
            return jsonResp.getObject("data", ArrayList<String>().javaClass)
        }
        return null
    }

    fun delFile(fileName: String): Boolean {
        val formBody = FormBody.Builder().add("filename", fileName).build()
        val request =
            Request.Builder().url("http://" + getHost() + ":8222/del-file").post(formBody).build()

        val response = client.newCall(request).execute()
        val jsonResp = JSONObject.parseObject(response.body?.string())
        return jsonResp.getIntValue("code") == 0
    }

    fun uploadFile(
        file: File,
        callback: ProgressRequestBody.ProgressCallback
    ): Boolean {
        val requestBody: MultipartBody.Builder =
            MultipartBody.Builder().setType(MultipartBody.FORM) //文件和json参数共同上传
        val MEDIA_TYPE_MARKDOWN: MediaType? = "text/x-markdown; charset=utf-8".toMediaTypeOrNull()
        if (file != null) { //添加文件到form-data
            val body = RequestBody.create(MEDIA_TYPE_MARKDOWN, file)
            // 参数分别为， 请求key ，文件名称 ， RequestBody
            requestBody.addFormDataPart("file", file.name, body)
        }
        val request =
            Request.Builder()
                .url("http://" + getHost() + ":8222/upload-file")
                .post(requestBody.build())
                .build()
        val response = client.newCall(request).execute()
        val jsonResp = JSONObject.parseObject(response.body?.string())
        if (jsonResp.getIntValue("code") == 0) {
            return true
        }
        Log.i(TAG, "错误代号:${jsonResp.getIntValue("code")}")
        return false
    }

    // 四合一的灯光控制
    open fun openLight(open: Int, query: Boolean) {}
    open fun luminanceChange(lum: Int, query: Boolean) {}
    open fun sharpFlash(open: Int, query: Boolean) {}
    open fun fetchTemperature() {}
    open fun redBlueLedControl(model: Byte) {}
    open fun controlServo(cval: Int) {}
}