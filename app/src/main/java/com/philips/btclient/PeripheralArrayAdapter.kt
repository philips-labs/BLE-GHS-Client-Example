package com.philips.btclient

import android.R
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.welie.blessed.BluetoothPeripheral


class PeripheralArrayAdapter(context: Context, resource: Int) :
    ArrayAdapter<BluetoothPeripheral>(context, resource, R.id.text1, mutableListOf()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.findViewById<TextView>(R.id.text1).text = getItem(position)?.name
        return view
    }
}
