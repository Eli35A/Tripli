package com.example.tripli.features.profile

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class LeafAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Leaf(
        val baseX: Float,
        val baseY: Float,
        val size: Float,
        val baseRotation: Float,
        val swayAmplitude: Float,
        val swaySpeed: Float,
        val phase: Float,
        val colorIndex: Int
    )

    private val leafColors = intArrayOf(
        Color.parseColor("#9DC49A"),  // sage green
        Color.parseColor("#C4956A"),  // warm brown
        Color.parseColor("#E0CB82"),  // soft yellow
        Color.parseColor("#E0A878"),  // pastel orange
        Color.parseColor("#B8D4A0"),  // light green
        Color.parseColor("#CBA882"),  // tan
    )

    private var leaves = emptyList<Leaf>()
    private var animAngle = 0f

    private val animator = ValueAnimator.ofFloat(0f, (2.0 * PI).toFloat()).apply {
        duration = 7000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener {
            animAngle = it.animatedValue as Float
            invalidate()
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafPath = Path()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            leaves = buildLeaves(w.toFloat(), h.toFloat())
            if (!animator.isRunning) animator.start()
        }
    }

    private fun buildLeaves(w: Float, h: Float): List<Leaf> {
        val rng = Random(1337)
        return List(22) { i ->
            Leaf(
                baseX = rng.nextFloat() * w,
                baseY = rng.nextFloat() * h,
                size = 8f + rng.nextFloat() * 12f,
                baseRotation = rng.nextFloat() * 360f,
                swayAmplitude = 10f + rng.nextFloat() * 18f,
                swaySpeed = 0.4f + rng.nextFloat() * 0.8f,
                phase = (rng.nextFloat() * 2.0 * PI).toFloat(),
                colorIndex = i % leafColors.size
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (leaf in leaves) {
            val angle = (animAngle * leaf.swaySpeed + leaf.phase).toDouble()
            val x = leaf.baseX + sin(angle).toFloat() * leaf.swayAmplitude
            val y = leaf.baseY + cos(angle * 0.6).toFloat() * leaf.swayAmplitude * 0.35f
            val rot = leaf.baseRotation + sin(angle).toFloat() * 22f

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rot)

            paint.color = leafColors[leaf.colorIndex]
            paint.alpha = 200

            leafPath.reset()
            val s = leaf.size
            leafPath.moveTo(0f, -s)
            leafPath.cubicTo(s * 0.65f, -s * 0.4f, s * 0.65f, s * 0.4f, 0f, s)
            leafPath.cubicTo(-s * 0.65f, s * 0.4f, -s * 0.65f, -s * 0.4f, 0f, -s)

            canvas.drawPath(leafPath, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
