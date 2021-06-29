package com.philips.btclient

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


/**
 * TODO: document the WaveformView
 */
class WaveformView : View {

    private var _lineColor: Int = Color.BLUE // TODO: use a default from R.color...
    private var _lineWidth: Float = 2f // TODO: use a default from R.dimen...

    private var _waveform: IntArray? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var lineColor: Int
        get() = _lineColor
        set(value) {
            _lineColor = value
            paint.color = _lineColor
            postInvalidate()
        }

    var waveform: IntArray?
        get() = _waveform
        set(value) {
            _waveform = value
            postInvalidate()
        }

    var lineWidth: Float
        get() = _lineWidth
        set(value) {
            _lineWidth = value
            paint.strokeWidth = _lineWidth
            postInvalidate()
        }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    fun setWaveform(ppgArray: ByteArray) {
        this.waveform = ppgArray.foldIndexed(IntArray(ppgArray.size)) { i, a, v -> a.apply { set(i, v.toInt()) } }
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.WaveformView, defStyle, 0
        )

        this.lineColor = a.getColor(
            R.styleable.WaveformView_lineColor,
            lineColor
        )

        paint.style = Paint.Style.STROKE

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        this.lineWidth = a.getDimension(
            R.styleable.WaveformView_lineWidth,
            lineWidth
        )

        a.recycle()
    }

    var lines: FloatArray = FloatArray(0)

    @ExperimentalUnsignedTypes
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val signedArray = _waveform
        _waveform?.let {
            var lineIndex = 0
            val arraySize = it.size * 4
            if (lines.size != arraySize) lines = FloatArray(arraySize)
            for ( i in 0..(it.size - 4)) {
                lines[lineIndex++] = i / it.size.toFloat() * width
                lines[lineIndex++] = it[i].toUByte().toFloat() + ((height - 255f) / 2f)
                lines[lineIndex++] = (i + 1) / it.size.toFloat() * width;
                lines[lineIndex++] = it[i + 1].toUByte().toFloat() + ((height - 255) / 2f)
            }
            canvas.drawLines(lines, paint);
        }
    }
}