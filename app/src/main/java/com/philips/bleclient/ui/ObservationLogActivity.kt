/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import com.philips.bleclient.BR
import com.philips.bleclient.R
import com.philips.bleclient.observations.BundledObservationValue
import com.philips.bleclient.observations.Observation

class ObservationLogActivity : AppCompatActivity() {

    val logCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (propertyId == BR.log) updateLogView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_observation_log)
        supportActionBar?.let {
            it.setTitle(R.string.observations)
            it.setDisplayHomeAsUpEnabled(true)
        }
        // Make the observation log scrollable
        findViewById<TextView>(R.id.observationsLog).setMovementMethod(ScrollingMovementMethod())
        updateLogView()
    }

    override fun onResume() {
        super.onResume()
        ObservationLog.addOnPropertyChangedCallback(logCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        ObservationLog.removeOnPropertyChangedCallback(logCallback)
    }

    @Suppress("UNUSED_PARAMETER")
    fun clearObservationLog(view: View) {
        ObservationLog.clear()
    }

    @Suppress("UNUSED_PARAMETER")
    fun parseTestObservation(view: View) {
        val obs = Observation.fromBytes(testObservationBytes())
        val obsValue = obs?.value
        if (obsValue is BundledObservationValue) {
            ObservationLog.log("Test parse bundle observations:")
            obsValue.observations.forEach { ObservationLog.log("$it") }
        } else {
            ObservationLog.log("Test parse observation: $obs")
        }
    }

    private fun testObservationBytes(): ByteArray {
        return byteArrayOf( 0xFF.toByte(), 0x2A, 0x00, 0x02, 0x00, 0x46, 0xD1.toByte(), 0x70, 0x79, 0x50,
            0x9A.toByte(), 0x00, 0x07, 0x80.toByte(), 0x02, 0x00, 0x0C, 0x00, 0x01, 0x00, 0x0C,
            0xE0.toByte(), 0x02, 0x00, 0x40, 0x11, 0x19, 0x27, 0x00, 0xFE.toByte(), 0x00, 0x0C,
            0x00, 0x01, 0x00, 0x5C, 0xE0.toByte(), 0x02, 0x00, 0x40, 0x11, 0xF1.toByte(), 0x1C, 0x00, 0xFE.toByte()
        )
    }

    private fun updateLogView() {
        findViewById<TextView>(R.id.observationsLog).setText(ObservationLog.log)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

}