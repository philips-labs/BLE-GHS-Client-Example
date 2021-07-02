/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * TODO: document the WaveformView
 */
class WaveformView : View {

    private var _lineColor: Int = resources.getColor(R.color.design_default_color_on_primary, null)
    private var _lineWidth: Float = 1.5f

    private var _waveform: IntArray? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lines: FloatArray = FloatArray(0)

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
            lines = FloatArray((value?.size ?: 0) * 4)
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
        this.waveform = ppgArray.foldIndexed(IntArray(ppgArray.size)) { i, a, v ->
            a.apply {
                set(
                    i,
                    v.toInt()
                )
            }
        }
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


    // Because waveforms being drawn may have different sizes
    @SuppressLint("DrawAllocation")
    @ExperimentalUnsignedTypes
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val verticalOffset = (contentHeight - 255f) / 2f

        _waveform?.let {
            var lineIndex = 0
            for (i in 0..(it.size - 4)) {
                lines[lineIndex++] = (i / it.size.toFloat() * contentWidth) + paddingLeft
                lines[lineIndex++] =
                    (it[i].toUByte().toFloat() + verticalOffset) + paddingTop + paddingBottom
                lines[lineIndex++] = ((i + 1) / it.size.toFloat() * contentWidth) + paddingLeft
                lines[lineIndex++] =
                    (it[i + 1].toUByte().toFloat() + verticalOffset) + paddingTop + paddingBottom
            }
            canvas.drawLines(lines, paint)
        }
    }
}