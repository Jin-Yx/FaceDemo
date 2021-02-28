package com.jinyx.facedemo.activity

import android.animation.ObjectAnimator
import android.os.Bundle
import com.jinyx.facedemo.R
import com.jinyx.facedemo.widgets.FaceView
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

class MainActivity : FaceActivity(), FaceView.OnFaceAreaChangeListener {

    private val screenPointX by lazy {
        mWidth * 0.5F
    }
    private val screenPointY by lazy {
        mHeight * 0.5F
    }

    private var mLastPointX: Float? = null
    private var mLastPointY: Float? = null
    private var mLastTransX = 0F
    private var mLastTransY = 0F
    private var mLastScale = 1.0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setCameraSurfaceView(surfaceView)
        setFaceView(faceView)
        faceView.addOnFaceAreaChangeListener(this)
    }

    override fun onFaceAreaChange(faceCount: Int, left: Float, right: Float, top: Float, bottom: Float) {
        if (faceCount > 0) {
            if (mLastPointX == null) {
                mLastPointX = screenPointX
            }
            if (mLastPointY == null) {
                mLastPointY = screenPointY
            }

            val scaleLeft: Float = (left + mWidth) / mWidth
            val scaleRight: Float = (mWidth - right + mWidth) / mWidth
            val scaleTop: Float = (top + mHeight) / mHeight
            val scaleBottom: Float = (mHeight - bottom + mHeight) / mHeight

            var maxScale = scaleLeft
            if (scaleRight > maxScale) {
                maxScale = scaleRight
            }
            if (scaleTop > maxScale) {
                maxScale = scaleTop
            }
            if (scaleBottom > maxScale) {
                maxScale = scaleBottom
            }

            val pointX = (left + right) * 0.5F
            val pointY = (top + bottom) * 0.5F

            // 对比上一次移动量 和 本次直接移到中心的量，算出本次相对上一次还需要移动多少像素
            val lastDetaX = (screenPointX - mLastPointX!!) * (maxScale - mLastScale)
            val lastDetaY = (screenPointY - mLastPointY!!) * (maxScale - mLastScale)
            val detaX = screenPointX - pointX
            val detaY = screenPointY - pointY
            val transX = detaX * maxScale - lastDetaX
            val transY = detaY * maxScale - lastDetaY

            // 标记按照缩放比例 [maxScale]，水平和竖直方向可以移入进来的像素点
            val leftOrRightOutSize = mWidth * (maxScale - 1) * 0.5F
            val topAndBottomOutSize = mHeight * (maxScale - 1) * 0.5F
            // 当平移到中间的需要的像素大于对应方向可移动的像素时，不做对应平移和缩放处理，避免边缘留白/黑（颜色和主题相关）
            if (abs(transX + lastDetaX) > leftOrRightOutSize || abs(transY + lastDetaY) > topAndBottomOutSize) {
                return
            }

            // 这是移动到中间，然后如果要保证上下左右边缘
            val translateAnimX = ObjectAnimator.ofFloat(frameFace, "translationX", mLastTransX, transX)
            val translateAnimY = ObjectAnimator.ofFloat(frameFace, "translationY", mLastTransY, transY)
            val scaleAnimX = ObjectAnimator.ofFloat(frameFace, "scaleX", mLastScale, maxScale)
            val scaleAnimY = ObjectAnimator.ofFloat(frameFace, "scaleY", mLastScale, maxScale)
            translateAnimX.duration = 400
            translateAnimY.duration = 400
            scaleAnimX.duration = 400
            scaleAnimY.duration = 400
            translateAnimX.start()
            translateAnimY.start()
            scaleAnimX.start()
            scaleAnimY.start()
            mLastPointX = pointX
            mLastPointY = pointY
            mLastTransX = transX
            mLastTransY = transY
            mLastScale = maxScale
        } else {
            val translateAnimX = ObjectAnimator.ofFloat(frameFace, "translationX", mLastTransX, 0F)
            val translateAnimY = ObjectAnimator.ofFloat(frameFace, "translationY", mLastTransY, 0F)
            val scaleAnimX = ObjectAnimator.ofFloat(frameFace, "scaleX", mLastScale, 1.0F)
            val scaleAnimY = ObjectAnimator.ofFloat(frameFace, "scaleY", mLastScale, 1.0F)
            translateAnimX.duration = 200
            translateAnimY.duration = 200
            scaleAnimX.duration = 200
            scaleAnimY.duration = 200
            translateAnimX.start()
            translateAnimY.start()
            scaleAnimX.start()
            scaleAnimY.start()
            mLastPointX = screenPointX
            mLastPointY = screenPointY
            mLastTransX = 0F
            mLastTransY = 0F
            mLastScale = 1.0F
        }
    }

}