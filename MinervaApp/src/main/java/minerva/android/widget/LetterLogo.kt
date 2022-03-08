package minerva.android.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import minerva.android.R
import minerva.android.kotlinUtils.Space
import minerva.android.widget.repository.generateColor

object LetterLogo {

    fun createLogo(context: Context, value: String): Drawable {
        Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).let { bitmap ->
            Canvas(bitmap).apply {
                drawCircle(HALF_SIZE, HALF_SIZE, HALF_SIZE, prepareBackgroundPaint(context, value))
                drawCenteredText(this, prepareMinervaFontPaint(context, value), getLetter(value))
            }
            return BitmapDrawable(context.resources, bitmap)
        }
    }

    private fun drawCenteredText(canvas: Canvas, paint: Paint, text: String) {
        Rect().let { rect ->
            canvas.getClipBounds(rect)
            rect.height().let { height ->
                rect.width().let { width ->
                    paint.textAlign = Paint.Align.LEFT
                    paint.getTextBounds(text, START, text.length, rect)
                    val x: Float = width / 2f - rect.width() / 2f - rect.left
                    val y: Float = height / 2f + rect.height() / 2f - rect.bottom
                    canvas.drawText(text, x, y, paint)
                }
            }
        }
    }

    private fun getLetter(value: String) = prepareLetter(value)[FIRST_SIGN].toString().capitalize()

    private fun prepareMinervaFontPaint(context: Context, value: String): Paint = Paint().apply {
        isAntiAlias = true
        textSize = HALF_SIZE
        color = ContextCompat.getColor(context, generateColor(value))
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.roboto_font_family), Typeface.BOLD)
    }

    private fun prepareBackgroundPaint(context: Context, value: String): Paint = Paint().apply {
        isAntiAlias = true
        color = ContextCompat.getColor(context, generateColor(value, true))
        style = Paint.Style.FILL_AND_STROKE
    }

    private fun prepareLetter(value: String): String =
        if (value.isBlank()) String.Space
        else value


    private const val FIRST_SIGN = 0
    private const val START = 0
    private const val SIZE = 512
    private const val HALF_SIZE = SIZE / 2f
}