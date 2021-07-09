/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.welie.blessed.BluetoothPeripheral

class PeripheralInfoActivity : AppCompatActivity() {
    private var peripheral: BluetoothPeripheral? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        var title = "No peripheral '$deviceAddress' found"
        deviceAddress?.let { addr ->
            peripheral = ServiceHandlerManager.getInstance(this).getConnectedPeripheral(addr)
            peripheral?.let {
                setupPeripheral(it)
                title = "${it.name} information"
            }
        }

        supportActionBar?.let {
            it.title = title
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                goBack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    private fun goBack() {
        finish()
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
    }

    private fun setupPeripheral(periph: BluetoothPeripheral) {
        findViewById<TextView>(R.id.peripheralMacAddress).text = "MAC address: ${periph.address}"
        findViewById<TextView>(R.id.serviceUUIDs).text =
            periph.services.fold("Service UUIDs:\n", { acc, service -> "$acc ${service.uuid}\n" })
        findViewById<TextView>(R.id.charUUIDs).text = periph.notifyingCharacteristics.fold(
            "Notifying char UUIDs:\n",
            { acc, char -> "$acc ${char.uuid}\n" })
    }

    @Suppress("UNUSED_PARAMETER")
    fun disconnectPeripheral(view: View) {
        peripheral?.cancelConnection()
        goBack()
    }

}