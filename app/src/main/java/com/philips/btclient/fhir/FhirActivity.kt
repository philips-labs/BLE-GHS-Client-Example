package com.philips.btclient.fhir

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Switch
import com.philips.btclient.R

class FhirActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fhir)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_fhir)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()
        val s = findViewById<View>(R.id.publicHapiSwitch) as Switch
        s.isChecked = FhirUploader.usePublicHapiServer
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

    fun togglePublicHapi(view: View) {
        FhirUploader.usePublicHapiServer = (view as Switch).isChecked
    }

}