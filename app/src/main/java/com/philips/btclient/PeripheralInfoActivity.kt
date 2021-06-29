package com.philips.btclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.welie.blessed.BluetoothPeripheral

class PeripheralInfoActivity : AppCompatActivity() {
    private var peripheral: BluetoothPeripheral? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_info)
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        var title = "No peripheral '$deviceAddress' found"
        deviceAddress?.let {
            peripheral = BluetoothHandler.getInstance(this).getConnectedPeripheral(it)
            peripheral?.let{
                setupPeripheral(it)
                title = "${it.name} information"
            }
        }

        supportActionBar?.let {
            it.setTitle(title)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
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
        findViewById<TextView>(R.id.serviceUUIDs).text = periph.services.fold("Service UUIDs:\n", {acc, service -> "$acc ${service.uuid}\n"})
        findViewById<TextView>(R.id.charUUIDs).text = periph.notifyingCharacteristics.fold("Notifying char UUIDs:\n", {acc, char -> "$acc ${char.uuid}\n"})
    }

    fun disconnectPeripheral(view: View) {
        peripheral?.cancelConnection()
        goBack()
    }

}