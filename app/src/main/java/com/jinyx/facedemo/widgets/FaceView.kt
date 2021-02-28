package com.jinyx.facedemo.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.media.FaceDetector
import android.util.AttributeSet
import android.view.View
import com.jinyx.facedemo.utils.DensityUtil
import com.jinyx.facedemo.utils.ScreenUtil
import kotlin.math.max
import kotlin.math.min

class FaceView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    constructor(context: Context) : this(context, null)

    private companion object {
        private const val EYES_DISTANCE = 6
    }

    private val mFacePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val mScreenWidth by lazy {
        ScreenUtil.getScreenWidth(context)
    }

    private val mScreenHeight by lazy {
        ScreenUtil.getScreenHeight(context)
    }

    private var mCameraWidth: Int = 0
    private var mCameraHeight: Int = 0

    private var mOnFaceAreaChangeListener: OnFaceAreaChangeListener? = null

    private val dp4 by lazy {
        DensityUtil.dip2px(context, 4F).toFloat()
    }

    private val textSize by lazy {
        DensityUtil.sp2px(context, 24F).toFloat()
    }

    private val mFaceList: ArrayList<FaceDetector.Face> by lazy {
        ArrayList<FaceDetector.Face>()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        // 记录多个人脸时，最小的全包区域（上下左右）坐标值
        var leftValue = mScreenWidth.toFloat()
        var rightValue = 0F
        var topValue = mScreenHeight.toFloat()
        var bottomValue = 0F
        if (!mFaceList.isNullOrEmpty()) {
            val angleList = ArrayList<Int>()
            for (index in mFaceList.indices) {
                val face = mFaceList[index]
                val centerPoint = PointF()
                // 获取人脸中心点坐标
                face.getMidPoint(centerPoint)
                val centerPointX = if (mCameraWidth > 0) {
                    centerPoint.x * mScreenWidth / mCameraWidth
                } else {
                    centerPoint.x
                }
                val centerPointY = if (mCameraHeight > 0) {
                    centerPoint.y * mScreenHeight / mCameraHeight
                } else {
                    centerPoint.y
                }
                // 获取人脸两眼之间的距离
                val eyesDistance = if (mCameraWidth > 0) {
                    face.eyesDistance() * mScreenWidth / mCameraWidth
                } else {
                    face.eyesDistance()
                }
                val left = max(centerPointX - eyesDistance * 1.3F, 0F)
                val top = max(centerPointY - eyesDistance * 1.6F, 0F)
                val right = min(centerPointX + eyesDistance * 1.3F, mScreenWidth * 1.0F)
                val bottom = min(centerPointY + eyesDistance * 1.8F, mScreenHeight * 1.0F)
                if (left < leftValue) {
                    leftValue = left
                }
                if (top < topValue) {
                    topValue = top
                }
                if (right > rightValue) {
                    rightValue = right
                }
                if (bottom > bottomValue) {
                    bottomValue = bottom
                }

                mFacePaint.strokeWidth = dp4
                mFacePaint.style = Paint.Style.STROKE
                mFacePaint.color = Color.YELLOW
                canvas?.drawRect(RectF(left, top, right, bottom), mFacePaint)
            }
        }
        mOnFaceAreaChangeListener?.onFaceAreaChange(mFaceList.size, leftValue, rightValue, topValue, bottomValue)
    }

    /**
     * 设置摄像头 previewSize 的宽和高，因为其和屏幕宽高不一定一致，所以要计算比例
     */
    fun setCameraSize(width: Int, height: Int) {
        mCameraWidth = width
        mCameraHeight = height
    }

    fun setFaceList(faces: List<FaceDetector.Face>?) {
        mFaceList.clear()
        if (!faces.isNullOrEmpty()) {
            mFaceList.addAll(faces)
        }
        postInvalidate()
    }

    fun addOnFaceAreaChangeListener(listener: OnFaceAreaChangeListener) {
        mOnFaceAreaChangeListener = listener
    }

    interface OnFaceAreaChangeListener {
        fun onFaceAreaChange(faceCount: Int, left: Float, right: Float, top: Float, bottom: Float)
    }

}