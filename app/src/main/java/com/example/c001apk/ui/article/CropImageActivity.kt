package com.example.c001apk.ui.article

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.c001apk.databinding.ActivityCropImageBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.makeToast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 封面裁剪页：固定比例 1600:719，输出精确 1600x719 的 JPEG。
 * 结果通过 RESULT_URI 返回（file:// Uri 字符串）。
 */
class CropImageActivity : BaseActivity<ActivityCropImageBinding>() {

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val RESULT_URI = "result_uri"
        const val COVER_W = 1600
        const val COVER_H = 719
        private const val MAX_SCALE = 4f
    }

    private var srcBitmap: Bitmap? = null
    private var scale = 1f
    private var transX = 0f
    private var transY = 0f
    private var overlayLeft = 0f
    private var overlayTop = 0f
    private var minScale = 1f

    private val matrix = Matrix()
    private lateinit var scaleDetector: ScaleGestureDetector
    private var lastX = 0f
    private var lastY = 0f

    private val srcUri by lazy { intent.getStringExtra(EXTRA_URI) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, v.paddingTop + top, v.paddingRight, v.paddingBottom)
            insets
        }

        binding.cancel.setOnClickListener { finish() }
        binding.confirm.setOnClickListener { confirmCrop() }
        binding.cropImage.post { loadAndInit() }
    }

    private fun loadAndInit() {
        val uri = try {
            Uri.parse(srcUri)
        } catch (e: Exception) {
            null
        }
        val bmp = try {
            uri?.let { contentResolver.openInputStream(it)?.use { s -> BitmapFactory.decodeStream(s) } }
        } catch (e: Exception) {
            null
        }
        if (bmp == null) {
            makeToast("无法加载图片")
            finish()
            return
        }
        srcBitmap = bmp
        binding.cropImage.setImageBitmap(bmp)

        val overlayLoc = IntArray(2)
        val imgLoc = IntArray(2)
        binding.cropOverlay.getLocationOnScreen(overlayLoc)
        binding.cropImage.getLocationOnScreen(imgLoc)
        overlayLeft = (overlayLoc[0] - imgLoc[0]).toFloat()
        overlayTop = (overlayLoc[1] - imgLoc[1]).toFloat()

        val cw = binding.cropOverlay.width.toFloat()
        val ch = binding.cropOverlay.height.toFloat()
        minScale = max(cw / bmp.width, ch / bmp.height)
        scale = minScale
        val sw = bmp.width * scale
        val sh = bmp.height * scale
        transX = overlayLeft + (cw - sw) / 2
        transY = overlayTop + (ch - sh) / 2
        applyTransform()
        initGesture()
    }

    private fun applyTransform() {
        matrix.reset()
        matrix.setScale(scale, scale)
        matrix.postTranslate(transX, transY)
        binding.cropImage.imageMatrix = matrix
    }

    private fun clamp() {
        val bmp = srcBitmap ?: return
        val cw = binding.cropOverlay.width.toFloat()
        val ch = binding.cropOverlay.height.toFloat()
        scale = scale.coerceIn(minScale, minScale * MAX_SCALE)
        val sw = bmp.width * scale
        val sh = bmp.height * scale
        transX = transX.coerceIn(overlayLeft + cw - sw, overlayLeft)
        transY = transY.coerceIn(overlayTop + ch - sh, overlayTop)
    }

    private fun initGesture() {
        scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cw = binding.cropOverlay.width.toFloat()
                    val ch = binding.cropOverlay.height.toFloat()
                    val cx = overlayLeft + cw / 2
                    val cy = overlayTop + ch / 2
                    val factor = detector.scaleFactor
                    scale *= factor
                    transX = cx - (cx - transX) * factor
                    transY = cy - (cy - transY) * factor
                    clamp()
                    applyTransform()
                    return true
                }
            }
        )

        binding.cropImage.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        transX += event.x - lastX
                        transY += event.y - lastY
                        lastX = event.x
                        lastY = event.y
                        clamp()
                        applyTransform()
                    }
                }
            }
            true
        }
    }

    private fun confirmCrop() {
        val bmp = srcBitmap ?: run {
            makeToast("裁剪失败")
            return
        }
        val inv = Matrix()
        if (!matrix.invert(inv)) {
            makeToast("裁剪失败")
            return
        }
        val cw = binding.cropOverlay.width.toFloat()
        val ch = binding.cropOverlay.height.toFloat()
        val pts = floatArrayOf(
            overlayLeft, overlayTop,
            overlayLeft + cw, overlayTop + ch
        )
        inv.mapPoints(pts)
        val left = pts[0].coerceIn(0f, bmp.width.toFloat())
        val top = pts[1].coerceIn(0f, bmp.height.toFloat())
        val right = pts[2].coerceIn(0f, bmp.width.toFloat())
        val bottom = pts[3].coerceIn(0f, bmp.height.toFloat())
        val cropW = (right - left).roundToInt()
        val cropH = (bottom - top).roundToInt()
        if (cropW <= 0 || cropH <= 0) {
            makeToast("裁剪失败")
            return
        }
        val cropped = Bitmap.createBitmap(bmp, left.toInt(), top.toInt(), cropW, cropH)
        val scaled = Bitmap.createScaledBitmap(cropped, COVER_W, COVER_H, true)
        if (cropped !== scaled) cropped.recycle()

        val file = File(cacheDir, "article_cover_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
        } catch (e: Exception) {
            makeToast("保存图片失败")
            return
        }
        val data = Intent().putExtra(RESULT_URI, Uri.fromFile(file).toString())
        setResult(RESULT_OK, data)
        finish()
    }
}
