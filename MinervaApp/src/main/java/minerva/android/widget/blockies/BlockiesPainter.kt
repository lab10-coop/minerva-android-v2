package minerva.android.widget.blockies

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

class BlockiesPainter {

    private val canvasPaint = Paint().apply { style = Paint.Style.FILL }
    private var dimen = 0.0f
    private var offsetX = 0.0f
    private var offsetY = 0.0f
    private val path = Path()

    fun setDimensions(width: Float, height: Float) {
        dimen = width.coerceAtMost(height)
        offsetX = width - dimen
        offsetY = height - dimen
        path.reset()
        path.addCircle(offsetX + (dimen / 2), offsetY + (dimen / 2), dimen / 2, Path.Direction.CCW)
        path.close()
    }

    fun draw(canvas: Canvas, blockies: Blockies) {
        canvas.save()
        canvas.clipPath(path)
        canvasPaint.color = blockies.backgroundColor
        canvas.drawRect(
            offsetX, offsetY, offsetX + dimen, offsetY + dimen,
            canvasPaint
        )

        val scale = dimen / Blockies.SIZE
        val main = blockies.primaryColor
        val sColor = blockies.spotColor

        for (i in blockies.data.indices) {
            val col = i % Blockies.SIZE
            val row = i / Blockies.SIZE

            canvasPaint.color = if (blockies.data[i] == 1.0) main else sColor

            if (blockies.data[i] > 0.0) {
                canvas.drawRect(
                    offsetX + (col * scale),
                    offsetY + (row * scale),
                    offsetX + (col * scale + scale),
                    offsetY + (row * scale + scale),
                    canvasPaint
                )
            }
        }
        canvas.restore()
    }
}