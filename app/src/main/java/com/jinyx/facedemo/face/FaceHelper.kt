package com.jinyx.facedemo.face

import android.graphics.*
import android.media.FaceDetector
import java.io.ByteArrayOutputStream

object FaceHelper {

    private val MAX_FACE_NUM = 30

    fun findFaceInPic(bitmap: Bitmap?): List<FaceDetector.Face>? {
        if (bitmap == null || bitmap.isRecycled) return null
        val bmp = bitmap.copy(Bitmap.Config.RGB_565, true)
        val width = bmp.width
        val height = bmp.height
        val faceDetector = FaceDetector(width, height, MAX_FACE_NUM)
        val faces = arrayOfNulls<FaceDetector.Face>(MAX_FACE_NUM)
        faceDetector.findFaces(bmp, faces)
        bmp.recycle()
        return faces.filterNotNull().filter {
            it.confidence() > 0.3F  // 检测到人脸的置信度需要大于 0.3
        }
    }

    /**
     * 预处理照相机的预览数据，将其转换成对应可以进行人脸识别的 Bitmap 格式
     */
    fun preCameraData(directionVertical: Boolean, isFrontCamera: Boolean, picWidth: Int, picHeight: Int, data: ByteArray?): Bitmap? {
        if (data == null || data.isEmpty()) return null
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        val yuvImage = YuvImage(data, ImageFormat.NV21, picWidth, picHeight, null)
        val bos = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, picWidth, picHeight), 50, bos)

        val rawImage = bos.toByteArray()

        //将rawImage转换成bitmap
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        options.inSampleSize = 2
        var bmp = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.size, options)

        val matrix = Matrix()
        if (directionVertical) {
            if (isFrontCamera) {
                matrix.postRotate(270F)
            } else {
                matrix.postRotate(90F)
            }
        } else {
            if (isFrontCamera) {
                matrix.postRotate(0F)
            } else {
                matrix.postRotate(0F)
            }
        }
        if (bmp != null) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            if (/*isFront && */bmp != null) {
                matrix.setScale(-1F, 1F)
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            }
        }
        return bmp
    }

}