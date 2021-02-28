package com.jinyx.facedemo.activity

import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Build
import android.os.Environment
import com.jinyx.facedemo.face.FaceHelper
import com.jinyx.facedemo.utils.ToastUtil
import com.jinyx.facedemo.widgets.FaceView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream

abstract class FaceActivity : CameraActivity() {

    private var faceView: FaceView? = null

    private val mFaceDir: String by lazy {
        val path = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Environment.getExternalStorageDirectory().absolutePath + File.separator + packageName
        } else {   // Android 大于等于 Android 10，存储路径为 /sdcard/Android/data/packageName/files/
            getExternalFilesDir("")!!.absolutePath
        }
        "$path${File.separator}images${File.separator}"
    }
    private var mFaceFile: File? = null

    private var mFaceDisposable: Disposable? = null

    private var mFaceCheckOver = true

    fun setFaceView(faceView: FaceView?) {
        this.faceView = faceView
    }

    override fun onCameraPreviewData(data: ByteArray, camera: Camera?) {
        synchronized(mFaceCheckOver) {
            if (!mFaceCheckOver) return
            mFaceCheckOver = false
            mFaceDisposable?.dispose()
            mFaceDisposable = Flowable.create<Boolean>({ emitter ->
                try {
                    val size = mCamera?.parameters?.previewSize
                    val picWidth = size?.width ?: mWidth
                    val picHeight = size?.height ?: (mScale * mWidth).toInt()
                    // 将预览的视频帧数据转换成对应格式 RGB_565 的 Bitmap 再进行人脸识别
                    val bmp = FaceHelper.preCameraData(DIRECTION_VERTICAL, CAMERA_FRONT, picWidth, picHeight, data)
                    val imgWidth = bmp?.width ?: 0
                    val imgHeight = bmp?.height ?: 0
                    if (bmp != null && imgWidth > 0 && imgHeight > 0) {
                        val faces = FaceHelper.findFaceInPic(bmp)
                        if (!isDestroyed) {
                            if (faces.isNullOrEmpty()) {
                                emitter.onError(Throwable(""))
                            } else {
                                faceView?.setCameraSize(imgWidth, imgHeight)
                                faceView?.setFaceList(faces)
//                                saveImage(bmp)
                                emitter.onNext(true)
                            }
                        }
                    } else {
                        emitter.onError(Throwable("人脸识别失败，参数异常"))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mFaceCheckOver = true
                }, {
                    faceView?.setFaceList(null)
                    mFaceCheckOver = true
                    ToastUtil.showShortToast(this, it.message)
                })
        }
    }

    private fun saveImage(bmp: Bitmap?) {
        if (bmp == null) return
        mFaceFile = File(mFaceDir, "${System.currentTimeMillis()}.jpg")
        if (mFaceFile!!.parentFile == null) return
        try {
            if (!mFaceFile!!.parentFile!!.exists() || !mFaceFile!!.parentFile!!.isDirectory) {
                mFaceFile!!.parentFile!!.mkdirs()
            }
            val fos = FileOutputStream(mFaceFile!!)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            mFaceFile = null
        }
    }

    private fun delFaceImg() {
        val file = File(mFaceDir)
        if (file.exists() && file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.exists() && it.isFile) {
                    it.delete()
                }
            }
        }
    }

    override fun onDestroy() {
        // delFaceImg()
        mFaceDisposable?.dispose()
        super.onDestroy()
    }

}