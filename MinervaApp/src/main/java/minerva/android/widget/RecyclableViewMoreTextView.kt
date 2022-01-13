package minerva.android.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import minerva.android.R

class RecyclableViewMoreTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val ANIMATION_PROPERTY_MAX_HEIGHT = "maxHeight"
        const val ANIMATION_PROPERTY_ALPHA = "alpha"
        const val DEFAULT_ELLIPSIZED_TEXT = "..."
        const val MAX_VALUE_ALPHA = 255
        const val MIN_VALUE_ALPHA = 0
    }

    private var visibleLines: Int? = null
    var isExpanded: Boolean = false
    private var animationDuration: Int? = null
    private var foregroundColor: Int? = null
    private var ellipsizeText: String? = null
    private var initialValue: String? = null
    private var isUnderlined: Boolean? = null
    private var ellipsizeTextColor: Int? = null

    private val visibleText: String get() = visibleText()

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.RecyclableViewMoreTextView)
        visibleLines = attributes.getInt(R.styleable.RecyclableViewMoreTextView_visibleLines, 0)
        animationDuration = attributes.getInt(R.styleable.RecyclableViewMoreTextView_duration, 1000)
        foregroundColor = attributes.getColor(R.styleable.RecyclableViewMoreTextView_foregroundColor, Color.TRANSPARENT)
        ellipsizeText = attributes.getString(R.styleable.RecyclableViewMoreTextView_ellipsizeText)
        isUnderlined = attributes.getBoolean(R.styleable.RecyclableViewMoreTextView_isUnderlined, false)
        ellipsizeTextColor = attributes.getColor(R.styleable.RecyclableViewMoreTextView_ellipsizeTextColor, Color.BLUE)
        isExpanded = attributes.getBoolean(R.styleable.RecyclableViewMoreTextView_isExpanded, false)
        attributes.recycle()
    }

    fun bind(text: String, isExpanded: Boolean) {
        this@RecyclableViewMoreTextView.isExpanded = isExpanded
        setupText(text)
        setMaxLines(isExpanded)
        setForeground(isExpanded)
        setEllipsizedText(isExpanded)
    }

    private fun setupText(value: String) {
        initialValue = value
        text = value
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        setMaxLines(isExpanded)
        setForeground(isExpanded)
        setEllipsizedText(isExpanded)
    }

    fun toggle() {
        if (visibleText.isAllTextVisible()) {
            return
        }

        isExpanded = !isExpanded

        if (isExpanded)
            setEllipsizedText(isExpanded)

        val startHeight = measuredHeight
        setMaxLines(isExpanded)
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val endHeight = measuredHeight

        animationSet(startHeight, endHeight).apply {
            duration = animationDuration?.toLong()!!
            start()

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isExpanded!!)
                        setEllipsizedText(isExpanded!!)
                }

                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
        }
    }

    private fun setEllipsizedText(isExpanded: Boolean) {
        if (initialValue?.isBlank()!! || visibleText.isEmpty()) {
            return
        }

        text = if (isExpanded || visibleText.isAllTextVisible()) {
            initialValue
        } else {
            SpannableStringBuilder(
                 visibleText.substring(
                    0,
                    visibleText.length - (ellipsizeText.orEmpty().length + DEFAULT_ELLIPSIZED_TEXT.length)
                )
            )
                .append(DEFAULT_ELLIPSIZED_TEXT.span())
                .append(ellipsizeText.orEmpty().span())
        }
    }

    private fun visibleText(): String {
        var end = 0


        for (i in 0 until visibleLines!!) {
            if (layout?.getLineEnd(i) ?: 0 != 0)
                end = layout.getLineEnd(i)
        }

        return initialValue?.substring(0, end)!!
    }

    private fun setMaxLines(isExpanded: Boolean) {
        maxLines = if (!isExpanded) {
            visibleLines!!
        } else {
            Integer.MAX_VALUE
        }
    }

    private fun setForeground(isExpanded: Boolean) {
        foreground = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(foregroundColor!!, Color.TRANSPARENT)
        )
        foreground.alpha = if (isExpanded) {
            MIN_VALUE_ALPHA
        } else {
            MAX_VALUE_ALPHA
        }
    }

    private fun animationSet(startHeight: Int, endHeight: Int): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofInt(
                    this@RecyclableViewMoreTextView,
                    ANIMATION_PROPERTY_MAX_HEIGHT,
                    startHeight,
                    endHeight
                ),
                ObjectAnimator.ofInt(
                    this@RecyclableViewMoreTextView.foreground,
                    ANIMATION_PROPERTY_ALPHA,
                    foreground.alpha,
                    MAX_VALUE_ALPHA - foreground.alpha
                )
            )
        }
    }

    private fun String.isAllTextVisible(): Boolean = this == text

    private fun String.span(): SpannableString =
        SpannableString(this).apply {
            setSpan(
                ForegroundColorSpan(ellipsizeTextColor!!),
                0,
                this.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (isUnderlined!!)
                setSpan(
                    UnderlineSpan(),
                    0,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
        }

}