package com.jinyx.facedemo.activity

import android.Manifest
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.jinyx.facedemo.expand.hideNavigationBar
import com.jinyx.facedemo.expand.initStatusBar
import com.jinyx.facedemo.utils.ScreenUtil
import com.jinyx.facedemo.utils.ToastUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import kotlin.math.abs

/**
 * 摄像头采集 + 预览的功能提到此处，预览数据给子类进行处理和实现
 */
@RuntimePermissions
abstract class CameraActivity : AppCompatActivity(), SurfaceHolder.Callback, Camera.PreviewCallback {

    companion object {
        // 摄像头自动聚焦时间间隔（s）
        private const val AUTO_FOCUS_CAMERA_INTERVAL = 3
        // 摄像头类型， true 表示前置摄像头， false 表示后置摄像头
        const val CAMERA_FRONT = true
        // 表示当前页面的显示方向，应该通过代码去获取的！ true 表示竖屏，false 表示横屏
        const val DIRECTION_VERTICAL = false
    }

    protected var mScale: Float = 0F
    protected var mWidth: Int = 0
    protected var mHeight: Int = 0

    protected var mCamera: Camera? = null
    private var mSurfaceHolder: SurfaceHolder? = null

    private var mAutoFocusLoopJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initStatusBar()
        mWidth = ScreenUtil.getScreenWidth(this)
        mHeight = ScreenUtil.getScreenHeight(this)
        mScale = if (DIRECTION_VERTICAL) {
            mHeight.toFloat() / mWidth
        } else {
            mWidth.toFloat() / mHeight
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch(Dispatchers.Main) {
            getCameraWithPermissionCheck()
        }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    override fun onDestroy() {
        releaseCamera()
        mAutoFocusLoopJob?.cancel()
        super.onDestroy()
    }

    protected fun setCameraSurfaceView(surfaceView: SurfaceView?) {
        mSurfaceHolder = surfaceView?.holder
        mSurfaceHolder?.addCallback(this)
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun getCamera() {
        releaseCamera()
        switchCamera(CAMERA_FRONT)
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun focusCamera() {
        try {
            mCamera?.autoFocus { _, _ -> }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onDeniedCamera() {
        ToastUtil.showLongToast(this, "没有权限")
    }

    private fun startPreview(holder: SurfaceHolder?) {
        try {
            mCamera?.setPreviewDisplay(holder)
            if (DIRECTION_VERTICAL) {
                mCamera?.setDisplayOrientation(90)
            } else {
                mCamera?.setDisplayOrientation(0)
            }
            mCamera?.startPreview()
            mCamera?.setPreviewCallback(this)
        } catch (e: Exception) {
            ToastUtil.showLongToast(this, "预览相机失败")
            getCameraWithPermissionCheck()
        }
    }

    private fun releaseCamera() {
        mCamera?.setPreviewCallback(null)
        mCamera?.stopPreview()
        mCamera?.release()
        mCamera = null
        mScale = if (DIRECTION_VERTICAL) {
            mHeight.toFloat() / mWidth
        } else {
            mWidth.toFloat() / mHeight
        }
        mAutoFocusLoopJob?.cancel()
    }

    private fun switchCamera(isFront: Boolean) {
        val cameraCount = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        val facing = if (isFront) {
            Camera.CameraInfo.CAMERA_FACING_FRONT
        } else {
            Camera.CameraInfo.CAMERA_FACING_BACK
        }
        for (i in 0 until cameraCount) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == facing) { // 前置
                releaseCamera()
                mCamera = Camera.open(i)    // 打开当前选中的摄像头
                setCameraParametersWithPermissionCheck()
                try {
                    mCamera?.setPreviewDisplay(mSurfaceHolder)  // 通过surfaceView显示取景画面
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                startPreview(mSurfaceHolder)
                loopAutoFocusCamera()
                return
            }
        }
        if (mCamera == null) {
            ToastUtil.showLongToast(this, "无法连接到相机")
        }
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun setCameraParameters() {
        val parameters = mCamera?.parameters
        val previewList = parameters?.supportedPreviewSizes
        // 注意，如果使用 takePicture 拍照，还需要设置支持的拍照尺寸 supportedPictureSizes
        // previewSize 和 全屏显示的宽高并不一定相同，多以计算最接近的比例，设置对应的 previewSize
        if (previewList != null) {
            var minPreScale = mScale
            var preViewWidth = mWidth
            var preViewHeight = mHeight
            for (item in previewList) {
                val scale: Float = item.width.toFloat() / item.height
                val remainScale = abs(scale - mScale)
                if (remainScale < minPreScale && item.width >= 0.5 * mWidth && item.height >= 0.5 * mWidth) {
                    if (DIRECTION_VERTICAL) {
                        preViewWidth = item.height
                        preViewHeight = item.width
                    } else {
                        preViewWidth = item.width
                        preViewHeight = item.height
                    }
                    parameters.setPreviewSize(item.width, item.height)
                    minPreScale = remainScale
                }
            }
            mScale = preViewHeight.toFloat() / preViewWidth
        }
        parameters?.pictureFormat = ImageFormat.JPEG
        parameters?.jpegQuality = 100
        parameters?.sceneMode = Camera.Parameters.SCENE_MODE_AUTO
        mCamera?.parameters = parameters
    }

    private fun loopAutoFocusCamera() {
        mAutoFocusLoopJob?.cancel()
        val tickerChannel = ticker(AUTO_FOCUS_CAMERA_INTERVAL * 1_000L, 0)
        mAutoFocusLoopJob = GlobalScope.launch {
            repeat(Int.MAX_VALUE) {
                withContext(Dispatchers.Main) {
                    hideNavigationBar()
                    focusCameraWithPermissionCheck()
                    if (it > 2000000000) {
                        loopAutoFocusCamera()
                    }
                }
                tickerChannel.receive()
            }
        }
    }

    abstract fun onCameraPreviewData(data: ByteArray, camera: Camera?)

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (data == null || data.isEmpty()) return
        onCameraPreviewData(data, camera)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        releaseCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        startPreview(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mCamera?.stopPreview()
        startPreview(holder)
    }

}