package com.example.yikupayloadexample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

class FourInOne2AddRecordActivity: AppCompatActivity() {
    private val TAG = "FourInOne2AddRecordActivity"
    private lateinit var mSelectFile: Button
    private lateinit var mFileNameEditText: EditText
    private lateinit var uploadFile: File
    private lateinit var mUploadBtn: Button
    private lateinit var mUploadPlan: LinearLayout
    private lateinit var mProgressBarUpload: ProgressBar
    var isStartRecord: Boolean = false
    private fun httpUploadFile() {
        runOnUiThread {
            mProgressBarUpload.max = uploadFile.length().toInt()
            mUploadBtn.setText(R.string.button_uploading)
            mUploadBtn.isEnabled = false
//            mUploadPlan.visibility = View.VISIBLE
        }
        val callback =
            ProgressRequestBody.ProgressCallback { totalBytesRead ->
                runOnUiThread {
                    Log.d(TAG, "上传进度：$totalBytesRead")
                    mProgressBarUpload.progress = totalBytesRead.toInt()
                }
            }
        thread {
            try {
                Log.i(TAG, "uploadFile:${uploadFile}")
                if (fourInOne2Service.uploadFile(uploadFile, callback)) {
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
//                    mUploadPlan.visibility = View.INVISIBLE
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
                sendDataArr.add(header)
                while (bip.read(buffer) != -1) {
                    val data = headerFLAG + buffer
                    sendDataArr.add(data)
                }
                Log.i(TAG, "读取完成，开始发送")
                mUploadPlan.visibility = View.VISIBLE
                try {
                    thread {
                        var lastUpdatePackage = 0
                        fourInOne2Service.uploadFile(sendDataArr, object : UploadFileCallback {
                            override fun onUploadPackageSuccess(totalNum: Int, finishNum: Int) {
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

        mUploadPlan = findViewById(R.id.upload_plan)
        mUploadPlan.visibility = View.INVISIBLE
        mProgressBarUpload = findViewById(R.id.progress_bar_upload)

        mFileNameEditText.isFocusable = false
        mFileNameEditText.isFocusableInTouchMode = false
        mFileNameEditText.isCursorVisible = false
        mFileNameEditText.setOnClickListener {
            // 如果uploadFile未被初始化，说明没有选择文件，先跳转到选择文件页面
            if(!::uploadFile.isInitialized) {
                selectFile()
                return@setOnClickListener
            }

        }
        mUploadBtn.setOnClickListener {
            // 如果uploadFile未被初始化，说明没有选择文件，先跳转到选择文件页面
            if(!::uploadFile.isInitialized) {
                selectFile()
                return@setOnClickListener
            }
            // 针对大疆因速率限制无法使用http上传，上传会导致超时，在此限制使用socket 方式上传。
            if (fourInOne2Service.platform == VehiclePlatform.M300) {
                socketUploadFile()
            } else {
                httpUploadFile()
            }
        }
        mSelectFile.setOnClickListener {
            selectFile()
        }

    }

    // 打开文件选择窗口
    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        //任意类型文件
        intent.type = "audio/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        ActivityCompat.startActivityForResult(this, intent, 100, null)
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
        mFileNameEditText.isFocusable = true
        mFileNameEditText.isFocusableInTouchMode = true
        mFileNameEditText.isCursorVisible = true
        mFileNameEditText.setText(uploadFile.name.split(":").last())
        mFileNameEditText.setOnClickListener(null)
        Log.e(TAG, "Size: ${uploadFile.length()}")
    }

    private fun showToast(msg: String) {
        this.runOnUiThread {
            Toast.makeText(
                this,
                msg,
                Toast.LENGTH_LONG
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