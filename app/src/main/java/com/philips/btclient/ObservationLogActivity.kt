package com.philips.btclient

import android.os.Bundle
import android.transition.Slide
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable

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

    fun clearObservationLog(view: View) {
        ObservationLog.clear()
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