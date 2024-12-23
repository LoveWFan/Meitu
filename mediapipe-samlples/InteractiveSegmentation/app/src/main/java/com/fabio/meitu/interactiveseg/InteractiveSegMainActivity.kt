/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fabio.meitu.interactiveseg.interactivesegmentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.fabio.meitu.databinding.ActivityInteractiveSegBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class InteractiveSegMainActivity : AppCompatActivity(),
    InteractiveSegmentationHelper.InteractiveSegmentationListener {

    private lateinit var activityMainBinding: ActivityInteractiveSegBinding
    private lateinit var interactiveSegmentationHelper: InteractiveSegmentationHelper
    private var isAllFabsVisible = false
    private var pictureUri: Uri? = null

    // Launch camera to receive new image for segmentation
    // Set image in View, start segmentation helper
    // Update UI
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess && pictureUri != null) {
                val bitmap = pictureUri!!.toBitmap()
                activityMainBinding.imgSegmentation.setImageBitmap(bitmap)
                interactiveSegmentationHelper.setInputImage(bitmap)
            }

            if (isAllFabsVisible) {
                fabsStateChange(false)
                isAllFabsVisible = false
            }
            activityMainBinding.tvDescription.visibility =
                if (isSuccess) View.GONE else View.VISIBLE
        }

    // Open user gallery to select a photo for segmentation
    // Set image in View, start segmentation helper
    // Update UI
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it?.toBitmap()?.let { bitmap ->
                activityMainBinding.imgSegmentation.setImageBitmap(bitmap)
                interactiveSegmentationHelper.setInputImage(
                    bitmap
                )
            }

            if (isAllFabsVisible) {
                fabsStateChange(false)
                isAllFabsVisible = false
            }
            activityMainBinding.tvDescription.visibility =
                if (it != null) View.GONE else View.VISIBLE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityInteractiveSegBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        interactiveSegmentationHelper = InteractiveSegmentationHelper(
            this,
            this
        )

        fabsStateChange(false)
        initListener()
        initTouch()
    }

    private fun clearOverlapResult() {
        activityMainBinding.overlapView.clearAll()
        activityMainBinding.imgSegmentation.setImageBitmap(null)
    }

    private fun initListener() {
        activityMainBinding.addFab.setOnClickListener {
            isAllFabsVisible = if (!isAllFabsVisible) {
                fabsStateChange(true)
                true
            } else {
                fabsStateChange(false)
                false
            }
        }

        activityMainBinding.takePicture.setOnClickListener {
            clearOverlapResult()
            pictureUri = getImageUri()
            pictureUri?.let {
                takePictureLauncher.launch(it)
            }
        }

        activityMainBinding.pickPicture.setOnClickListener {
            clearOverlapResult()
            pickImageLauncher.launch("image/*")
        }
    }

    /**
     * Takes the position where the user touches on image (x and y)
     * and draws a marker above it to highlight where item of significance is found
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initTouch() {
        val viewCoords = IntArray(2)
        activityMainBinding.imgSegmentation.getLocationOnScreen(viewCoords)
        activityMainBinding.imgSegmentation.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (interactiveSegmentationHelper.isInputImageAssigned()) {
                        val touchX = event.x.toInt()
                        val touchY = event.y.toInt()

                        val imageX =
                            touchX - viewCoords[0] // viewCoords[0] is the X coordinate
                        val imageY =
                            touchY - viewCoords[1] // viewCoords[1] is the y coordinate

                        activityMainBinding.overlapView.setSelectPosition(
                            imageX.toFloat(),
                            imageY.toFloat()
                        )

                        val normX =
                            imageX.toFloat() / activityMainBinding.imgSegmentation.width
                        val normY =
                            imageY.toFloat() / activityMainBinding.imgSegmentation.height

                        interactiveSegmentationHelper.segment(normX, normY)
                    }
                }

                else -> {
                    // no-op
                }
            }
            true
        }
    }

    /**
     * Controls the state of the FAB buttons to show or hide.
     */
    private fun fabsStateChange(isStateShow: Boolean) {
        if (isStateShow) {
            with(activityMainBinding) {
                takePicture.show()
                pickPicture.show()
                tvPickImageDescription.visibility = View.VISIBLE
                tvTakePictureDescription.visibility = View.VISIBLE
                addFab.extend()
            }
        } else {
            with(activityMainBinding) {
                takePicture.hide()
                pickPicture.hide()
                tvPickImageDescription.visibility = View.GONE
                tvTakePictureDescription.visibility = View.GONE
                addFab.shrink()
            }
        }
    }

    /**
     * Create file ready for taking picture.
     */
    private fun getImageUri(): Uri {
        val filePicture = File(
            cacheDir.path + File.separator + "JPEG_" + SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date()) + ".jpg"
        )

        return FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".fileprovider",
            filePicture
        )
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    /**
     * Converts Uri to Bitmap.
     * If a Bitmap is not of the ARGB_8888 type, it needs to be converted to
     * that type because the interactive segmentation helper requires that
     * specific Bitmap type.
     */
    private fun Uri.toBitmap(): Bitmap {
        val maxWidth = 512f
        var bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(contentResolver, this)
        } else {
            val source = ImageDecoder.createSource(contentResolver, this)
            ImageDecoder.decodeBitmap(source)
        }
        // reduce the size of image if it larger than maxWidth
        if (bitmap.width > maxWidth) {
            val scaleFactor = maxWidth / bitmap.width
            bitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scaleFactor).toInt(),
                (bitmap.height * scaleFactor).toInt(),
                false
            )
        }
        return if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    override fun onError(error: String) {
        showError(error)
    }

    override fun onResults(result: InteractiveSegmentationHelper.ResultBundle?) {
        // Inform the overlap view to draw over the area of significance returned
        // from the helper


        result?.let {
            activityMainBinding.overlapView.setMaskResult(
                it.byteBuffer,
                it.maskWidth,
                it.maskHeight
            )

            Handler(Looper.getMainLooper()).post {
                // 在UI线程中更新UI
                // 把目标图抠出来
                // 假设 image 是原始图像，categoryMask 是输入的 CATEGORY_MASK 图像（ByteBuffer）
                val extractedBitmap =
                    extractObjectFromMask(it.inputImage!!, it.byteBuffer)

                if (extractedBitmap != null) {
                    // 处理裁剪后的图像（包含目标物体）
                    // 例如，显示在 ImageView 中
                    activityMainBinding.tvSegmentationResult.visibility = View.VISIBLE
                    activityMainBinding.imgSegmentationResult.visibility = View.VISIBLE
                    activityMainBinding.imgSegmentationResult.setImageBitmap(extractedBitmap)
                } else {
                    // 处理没有检测到目标物体的情况
                    Log.e("MainActivity", "没有检测到目标物体")
                }
            }

        } ?: kotlin.run {
            activityMainBinding.overlapView.clearAll()
        }
    }

    // 1. API: 从 CATEGORY_MASK 中提取目标物体并返回裁剪图像，背景透明
    fun extractObjectFromMask(image: Bitmap, categoryMask: ByteBuffer): Bitmap? {
        val bounds = getObjectBounds(categoryMask, image.width, image.height)

        return if (bounds != null) {
            createTransparentBitmap(image, bounds, categoryMask)
        } else {
            null // 如果没有检测到目标物体，返回 null
        }
    }

    // 2. 获取目标物体的边界（最小矩形区域）
    fun getObjectBounds(categoryMask: ByteBuffer, width: Int, height: Int): Rect? {
        var left = width
        var right = 0
        var top = height
        var bottom = 0

        // 遍历 CATEGORY_MASK，找到目标物体的边界
        for (y in 0 until height) {
            for (x in 0 until width) {
                val categoryValue = categoryMask.get(y * width + x).toInt() // uint8 值是 0 或 1
                if (categoryValue == 0) { // 如果是目标物体的像素
                    // 更新目标物体的边界
                    left = Math.min(left, x)
                    right = Math.max(right, x)
                    top = Math.min(top, y)
                    bottom = Math.max(bottom, y)
                }
            }
        }

        return if (left < right && top < bottom) {
            Rect(left, top, right, bottom) // 返回目标物体的最小矩形区域
        } else {
            null // 没有目标物体，返回 null
        }
    }

    // 3. 创建透明背景的目标物体图像
    fun createTransparentBitmap(image: Bitmap, bounds: Rect, categoryMask: ByteBuffer): Bitmap {
        val width = bounds.width()
        val height = bounds.height()

        // 创建一个透明背景的 Bitmap
        val transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 遍历目标物体的区域
        for (y in 0 until height) {
            for (x in 0 until width) {
                val originalX = bounds.left + x
                val originalY = bounds.top + y
                val categoryValue = categoryMask.get(originalY * image.width + originalX).toInt()

                // 如果是目标物体的像素（categoryMask 中的值为 1），保留原始像素值
                if (categoryValue == 0) {
                    val pixelColor = image.getPixel(originalX, originalY)
                    transparentBitmap.setPixel(x, y, pixelColor)
                } else {
                    // 否则设置为透明
                    transparentBitmap.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }

        return transparentBitmap
    }

    // 4. 辅助类：表示矩形区域
    data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        fun width() = right - left
        fun height() = bottom - top
    }
}
