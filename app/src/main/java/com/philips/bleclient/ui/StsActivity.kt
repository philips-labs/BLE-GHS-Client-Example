package com.philips.bleclient.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.asFormattedHexString
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import com.philips.bleclient.service.sts.SimpleTimeServiceHandler
import com.welie.blessed.BluetoothPeripheral
import timber.log.Timber

class StsActivity : AppCompatActivity() {
    private var peripheral: BluetoothPeripheral? = null
    private var stsServiceHandler = ServiceHandlerManager.getInstance(this).getStsServiceHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sts)
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        deviceAddress?.let { addr ->
            peripheral = ServiceHandlerManager.getInstance(this).getConnectedPeripheral(addr)
        }

        supportActionBar?.let {
            it.title = "Elapsed Time Service"
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun getStsBytes(view: View) {
    }

}

fun ServiceHandlerManager.getStsServiceHandler(): SimpleTimeServiceHandler? {
    return serviceHandlerForUUID(SimpleTimeServiceHandler.SERVICE_UUID)?.let {it as SimpleTimeServiceHandler }
}