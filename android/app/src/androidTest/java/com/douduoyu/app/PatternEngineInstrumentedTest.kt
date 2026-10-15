package com.douduoyu.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PatternEngineInstrumentedTest {
    @Test
    fun samplingBitmapDimension_capsLargeCropWithoutUpscalingSmallCrop() {
        assertEquals(768, PatternEngine.samplingBitmapDimension(2048, 256, 3))
        assertEquals(128, PatternEngine.samplingBitmapDimension(128, 256, 3))
        assertEquals(1, PatternEngine.samplingBitmapDimension(0, 0, 0))
    }

    @Test
    fun generateProject_supports256By256OutputFromLargeSource() {
        val source = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(180, 65, 80))
        try {
            val project =
                PatternEngine.generateProject(
                    source = source,
                    settings =
                        GenerationSettings(
                            title = "large",
                            width = 256,
                            height = 256,
                            maxColors = 2,
                            colorScheme = ColorSchemePreset.MardBrand24,
                            paletteMode = ColorSchemePreset.MardBrand24.paletteMode,
                            backgroundMode = BackgroundMode.Keep,
                            speckleReduction = 0,
                        ),
                )

            assertEquals(256, project.width)
            assertEquals(256, project.height)
            assertEquals(256 * 256, project.cells.size)
        } finally {
            source.recycle()
        }
    }

    @Test
    fun generateProject_supportsFull512By512OutputWithPostProcessing() {
        val source = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(source)
        val paint = Paint()
        val palette = BeadPalette.palette(ColorSchemePreset.MardBrand221)
        palette.forEachIndexed { index, color ->
            paint.color = color.argb
            val left = source.width * index / palette.size.toFloat()
            val right = source.width * (index + 1) / palette.size.toFloat()
            canvas.drawRect(left, 0f, right, source.height.toFloat(), paint)
        }
        try {
            val project =
                PatternEngine.generateProject(
                    source = source,
                    settings =
                        GenerationSettings(
                            title = "full-512",
                            width = 512,
                            height = 512,
                            maxColors = palette.size,
                            colorScheme = ColorSchemePreset.MardBrand221,
                            paletteMode = ColorSchemePreset.MardBrand221.paletteMode,
                            backgroundMode = BackgroundMode.Keep,
                            speckleReduction = 2,
                        ),
                )

            assertEquals(512, project.width)
            assertEquals(512, project.height)
            assertEquals(OutputSizePolicy.MAX_TOTAL_CELLS, project.cells.size)
        } finally {
            source.recycle()
        }
    }

    @Test
    fun generateProject_constrainsOversizedOutputBeforeAllocating() {
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLACK)
        try {
            val project =
                PatternEngine.generateProject(
                    source = source,
                    settings = GenerationSettings(width = 4096, height = 4096, maxColors = 2),
                )

            assertEquals(512, project.width)
            assertEquals(512, project.height)
            assertEquals(OutputSizePolicy.MAX_TOTAL_CELLS, project.cells.size)
        } finally {
            source.recycle()
        }
    }

    @Test
    fun generateProject_remainsCancellableDuringSampling() {
        val source = Bitmap.createBitmap(2048, 2048, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        val checks = AtomicInteger()
        try {
            assertThrows(CancellationException::class.java) {
                PatternEngine.generateProject(
                    source = source,
                    settings = GenerationSettings(width = 256, height = 256, maxColors = 2),
                    shouldCancel = { checks.incrementAndGet() >= 3 },
                )
            }
        } finally {
            source.recycle()
        }
    }
}
