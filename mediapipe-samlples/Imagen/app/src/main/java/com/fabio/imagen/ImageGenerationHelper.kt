package com.fabio.imagen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator.ConditionOptions.ConditionType
import com.google.mediapipe.tasks.vision.imagegenerator.ImageGenerator.ImageGeneratorOptions


class ImageGenerationHelper(
    val context: Context
) {

    lateinit var imageGenerator: ImageGenerator

    // Setup image generation model with output size, iteration
    fun initializeImageGenerator(modelPath: String) {
        val options = ImageGeneratorOptions.builder()
            .setImageGeneratorModelDirectory(modelPath)
            .build()

        imageGenerator = ImageGenerator.createFromOptions(context, options)
    }



    fun initializeLoRAWeightGenerator(modelPath: String, weightsPath: String) {
        val options = ImageGeneratorOptions.builder()
            .setLoraWeightsFilePath(weightsPath)
            .setImageGeneratorModelDirectory(modelPath)
            .build()

        imageGenerator = ImageGenerator.createFromOptions(context, options)
    }

    fun setInput(
        prompt: String,
        conditionalImage: MPImage,
        conditionType: ConditionType,
        iteration: Int,
        seed: Int
    ) {
        imageGenerator.setInputs(
            prompt,
            conditionalImage,
            conditionType,
            iteration,
            seed
        )
    }

    // Set input prompt, iteration, seed
    fun setInput(prompt: String, iteration: Int, seed: Int) {
        imageGenerator.setInputs(prompt, iteration, seed)
    }


    fun generate(prompt: String, iteration: Int, seed: Int): Bitmap {
        val result = imageGenerator.generate(prompt, iteration, seed)
        val bitmap = BitmapExtractor.extract(result?.generatedImage())
        return bitmap
    }

    fun generate(
        prompt: String,
        inputImage: MPImage,
        conditionType: ConditionType,
        iteration: Int,
        seed: Int
    ): Bitmap {
        val result = imageGenerator.generate(
            prompt,
            inputImage,
            conditionType,
            iteration,
            seed
        )
        val bitmap = BitmapExtractor.extract(result?.generatedImage())
        return bitmap
    }

    fun execute(showResult: Boolean): Bitmap {
        // execute image generation model
        val result = imageGenerator.execute(showResult)

        if (result == null || result.generatedImage() == null) {
            return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                .apply {
                    val canvas = Canvas(this)
                    val paint = Paint()
                    paint.color = Color.WHITE
                    canvas.drawPaint(paint)
                }
        }

        val bitmap =
            BitmapExtractor.extract(result.generatedImage())

        return bitmap
    }

    fun createConditionImage(
        inputImage: MPImage,
        conditionType: ConditionType
    ): Bitmap {
        return BitmapExtractor.extract(imageGenerator.createConditionImage(inputImage, conditionType))
    }

    fun close() {
        try {
            imageGenerator.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
