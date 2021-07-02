/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE

// In blessed this is BluetoothPeripheral.doesNotSupportReading(@NotNull final BluetoothGattCharacteristic characteristic)
fun BluetoothGattCharacteristic.isRead(): Boolean {
    return getProperties() and PROPERTY_READ > 0
}

fun BluetoothGattCharacteristic.isWrite(): Boolean {
    return getProperties() and PROPERTY_WRITE > 0
}

fun BluetoothGattCharacteristic.isNotify(): Boolean {
    return getProperties() and PROPERTY_NOTIFY > 0
}

fun BluetoothGattCharacteristic.isIndicate(): Boolean {
    return getProperties() and PROPERTY_INDICATE > 0
}