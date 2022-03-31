package com.philips.bleclient.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.fhir.FhirUploader

class RacpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_racp)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_racp)
            it.setDisplayHomeAsUpEnabled(true)
        }
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

    @Suppress("UNUSED_PARAMETER")
    fun numberOfRecords(view: View) {
        ServiceHandlerManager.instance?.let {
            it.getGhsServiceHandler()?.racpHandler?.requestNumberOfRecords()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun numberRecordsAboveFour(view: View) {
        ServiceHandlerManager.instance?.let {
            it.getGhsServiceHandler()?.racpHandler?.requestNumberOfRecordsGreaterThan(5)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getAllRecords(view: View) {
        ServiceHandlerManager.instance?.let {
            it.getGhsServiceHandler()?.racpHandler?.getAllRecords()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getRecordsAboveFour(view: View) {
        ServiceHandlerManager.instance?.let {
            it.getGhsServiceHandler()?.racpHandler?.getRecordsAbove(4)
        }
    }

}