package com.example.yikupayloadexample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import com.lzf.easyfloat.EasyFloat
import com.yiku.yikupayloadSDK.protocol.BEGIN_UPLOAD_AUDIO
import com.yiku.yikupayloadSDK.protocol.UPLOAD_AUDIO
import com.yiku.yikupayloadSDK.service.UploadFileCallback
import com.yiku.yikupayloadSDK.util.GetFilePathFromUri
import com.yiku.yikupayloadSDK.util.ProgressRequestBody
import com.yiku.yikupayloadSDK.util.VehiclePlatform
import com.yiku.yikupayloadSDK.util.int16ToByteArray
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

class AddRecordActivity : AppCompatActivity() {
    private val TAG = "AddRecordActivity"
    private lateinit var mSelectFile: Button
    private lateinit var mFileNameEditText: EditText
    private lateinit var uploadFile: File
    private lateinit var mUploadBtn: Button
//    private lateinit var mRecordButton: Button //录音上传
    private lateinit var mUploadPlan: LinearLayout
    private lateinit var mProgressBarUpload: ProgressBar
    var isStartRecord: Boolean = false
    private fun httpUploadFile() {
        runOnUiThread {
            mProgressBarUpload.max = uploadFile.length().toInt()
            mUploadBtn.setText(R.string.button_uploading)
            mUploadBtn.isEnabled = false
        }
        val callback =
            ProgressRequestBody.ProgressCallback { totalBytesRead ->
                runOnUiThread {
                    mProgressBarUpload.progress = totalBytesRead.toInt()
                }
            }
        thread {
            try {
                Log.i(TAG, "uploadFile:${uploadFile}")
                if (megaphoneService?.uploadFile(uploadFile, callback) == true) {
                    showToast(R.string.upload_successful)
                } else {
                    showToast(R.string.upload_failed)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val message = getString(R.string.upload_failed_msg, e.message)
                showToast(message)
            } finally {
                runOnUiThread {
                    mUploadBtn.setText(R.string.upload)
                    mUploadBtn.isEnabled = true
                }
            }
        }
    }

    private fun socketUploadFile() {
        mUploadBtn.isEnabled = false
        val bufferSize = 1024
        run {
            try {
//                    uploadFile()
                val bip = BufferedInputStream(FileInputStream(uploadFile))
                val headerFLAG = UPLOAD_AUDIO.toByteArray()

                val buffer = ByteArray(bufferSize - headerFLAG.size)
                var header = BEGIN_UPLOAD_AUDIO.toByteArray()
                val fileName = Uri.encode(mFileNameEditText.text.toString())
                Log.i(TAG, "上传文件名称:${fileName}")
                header = header.plus(int16ToByteArray(fileName.length))
                Log.i(
                    TAG,
                    "上传文件名称长度:${int16ToByteArray(fileName.length).contentToString()}"
                )
                header = header.plus(fileName.toByteArray())
                Log.i(TAG, "header:${header}")
                val sendDataArr = ArrayList<ByteArray>()
//                    MApplication.payloadSDK.send(header)
                sendDataArr.add(header)
                while (bip.read(buffer) != -1) {
                    val data = headerFLAG + buffer
                    sendDataArr.add(data)
//                        Thread.sleep(1000)
                }
//                    sendDataArr.add("AUPA".toByteArray())
//                    val data = headerFLAG + MApplication.payloadSDK.EOF
//                    sendDataArr.add(data)

//                    MApplication.payloadSDK.send(headerFLAG.plus(MApplication.payloadSDK.EOF))
                Log.i(TAG, "读取完成，开始发送")
                mUploadPlan.visibility = View.VISIBLE
                try {
                    thread {
                        var lastUpdatePackage = 0
                        megaphoneService?.uploadFile(sendDataArr, object : UploadFileCallback {
                            override fun onUploadPackageSuccess(totalNum: Int, finishNum: Int) {
//                                    Log.i(TAG, "t:${totalNum} f:${finishNum}")
//                                    if (totalNum - 1 == finishNum) {
//                                        showToast("上传成功!")
//                                        runOnUiThread {
//                                            mUploadPlan.visibility = View.INVISIBLE
//                                        }
//                                    }
                                if (finishNum > lastUpdatePackage + 100) {
                                    runOnUiThread {
                                        mProgressBarUpload.max = totalNum
                                        mProgressBarUpload.progress = finishNum
                                        lastUpdatePackage = finishNum
                                    }
                                }
                                Log.i(
                                    TAG,
                                    "上传的进度: totalNum:${totalNum}, finishNum:${finishNum}"
                                )
                                if (totalNum - 1 == finishNum) {
                                    showToast(R.string.upload_successful)
                                    runOnUiThread {
                                        mUploadPlan.visibility = View.INVISIBLE
                                    }
                                }

                            }
                        })
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    showToast(R.string.upload_failed_please_try_again)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, e.message.toString())
            } finally {
                mUploadBtn.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_record)

        mSelectFile = findViewById(R.id.selectFile)
        mFileNameEditText = findViewById(R.id.fileNameEditText)
        mUploadBtn = findViewById(R.id.uploadBtn)
//        mRecordButton = findViewById(R.id.recordButton)

        mUploadPlan = findViewById(R.id.upload_plan)
        mUploadPlan.visibility = View.INVISIBLE
        mProgressBarUpload = findViewById(R.id.progress_bar_upload)
//        runOnUiThread { MsgRecv("MsgRecv").start() }
//        runOnUiThread { getBoardEMMCStorageSpace() }
        mFileNameEditText.setOnClickListener {
            // 如果uploadFile未被初始化，说明没有选择文件，先跳转到选择文件页面
            if(!::uploadFile.isInitialized) {
                EasyFloat.hide("yk_shout_weight_op")
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                //任意类型文件
                intent.type = "audio/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(this, intent, 100, null)
                return@setOnClickListener
            }

        }
        mUploadBtn.setOnClickListener {
            // 如果uploadFile未被初始化，说明没有选择文件，先跳转到选择文件页面
            if(!::uploadFile.isInitialized) {
                EasyFloat.hide("yk_shout_weight_op")
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                //任意类型文件
                intent.type = "audio/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(this, intent, 100, null)
                return@setOnClickListener
            }
            // 针对大疆因速率限制无法使用http上传，上传会导致超时，在此限制使用socket 方式上传。
            if (megaphoneService?.platform == VehiclePlatform.M300) {
                socketUploadFile()
            } else {
                httpUploadFile()
            }
        }
        mSelectFile.setOnClickListener {
            EasyFloat.hide("yk_shout_weight_op")
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            //任意类型文件
            intent.type = "audio/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(this, intent, 100, null)
//            EasyFloat.show("yk_shout_weight_op")
        }

    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(
            TAG,
            "onActivityResult... requestCode:${requestCode}, resultCode:${resultCode}, data:${data} "
        )
        super.onActivityResult(requestCode, resultCode, data)

//
        if (data == null || requestCode != 100) {
            // 用户未选择任何文件，直接返回
            Log.i(TAG, "data:${data}, req:${requestCode}")
            return
        }
        val path = GetFilePathFromUri.getFileAbsolutePath(this, data.data)
//        path.split("/").last()
        Log.i(TAG, "name: ${path.split("/").last()}")
        Log.i(TAG, "path:$path")
        uploadFile = File(path)
        if (!uploadFile.exists()) {
            val _path = path.replace("emulated/0", "external_sd")
            Log.i(TAG, "path:${_path}")
            uploadFile = File(_path)
        }
        if (!uploadFile.exists()) {
            showToast(R.string.failed_to_get_file)
            return
        }
        Log.e(TAG, "Name: ${uploadFile.name}")
        mFileNameEditText.setText(uploadFile.name.split(":").last())
        Log.e(TAG, "Size: ${uploadFile.length()}")
    }

    private fun showToast(msg: String) {
        this.runOnUiThread {
            Toast.makeText(
                this,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showToast(msg: Int) {
        this.runOnUiThread {
            Toast.makeText(
                this,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}