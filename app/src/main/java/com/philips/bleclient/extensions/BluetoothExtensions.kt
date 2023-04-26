/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.bleclient

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE

fun BluetoothGattCharacteristic.isRead(): Boolean = properties and PROPERTY_READ > 0

fun BluetoothGattCharacteristic.isWrite(): Boolean = properties and PROPERTY_WRITE > 0

fun BluetoothGattCharacteristic.isNotify(): Boolean = properties and PROPERTY_NOTIFY > 0

fun BluetoothGattCharacteristic.isIndicate(): Boolean = properties and PROPERTY_INDICATE > 0
