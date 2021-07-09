/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.philips.bleclient.R
import com.philips.bleclient.fhir.FhirUploader

class FhirActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fhir)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_fhir)
            it.setDisplayHomeAsUpEnabled(true)
        }

        initHapiEditText()
        val hapiUrl = findViewById<EditText>(R.id.hapiServerUrl)

        // when we edit text in the hapi url field it will check for a valid URL
        hapiUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (isValidUrl(hapiUrl.text.toString())) {
                    hapiUrl.error = null
                } else {
                    hapiUrl.error = "Invalid URL"
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    override fun onResume() {
        super.onResume()
        (findViewById<View>(R.id.publicHapiSwitch) as Switch).isChecked = FhirUploader.usePublicHapiServer
        (findViewById<View>(R.id.postObservationsSwitch) as Switch).isChecked = FhirUploader.postObservationsToServer
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

    fun togglePostObservations(view: View) {
        FhirUploader.postObservationsToServer = (view as Switch).isChecked
    }

    @Suppress("UNUSED_PARAMETER")
    fun saveUrl(view: View) {
        val hapiUrl = findViewById<EditText>(R.id.hapiServerUrl)
        if (isValidUrl(hapiUrl.text.toString())) {
            FhirUploader.localHapiServer = hapiUrl.text.toString()
            Toast.makeText(this@FhirActivity, "Saved HAPI URL", Toast.LENGTH_SHORT).show()
        } else {
            hapiUrl.error = "Not saved invalid URL"
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun revertUrl(view: View) { initHapiEditText() }

    private fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }

    private fun initHapiEditText() {
        val hapiUrl = findViewById<EditText>(R.id.hapiServerUrl)
        hapiUrl.setText(FhirUploader.localHapiServer, TextView.BufferType.EDITABLE)
        hapiUrl.error = null
    }

}