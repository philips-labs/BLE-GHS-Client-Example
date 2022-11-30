package com.philips.bleclient.service.user

import android.bluetooth.BluetoothGattCharacteristic
import com.philips.bleclient.ServiceHandler
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.ServiceHandlerManagerListener
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.GattStatus
import timber.log.Timber
import java.util.*

/**
 * Listener interface for User Data Service
 */
interface UserDataServiceHandlerListener {
}

class UserDataServiceHandler : ServiceHandler(), ServiceHandlerManagerListener {
    private val peripherals = mutableSetOf<BluetoothPeripheral>()
    private val listeners = mutableListOf<UserDataServiceHandlerListener>()

    var controlPointHandler = UserDataServiceControlPointHandler(this)

    override val name: String
        get() = "UserDataServiceHandler"

    override fun onCharacteristicsDiscovered(
        peripheral: BluetoothPeripheral,
        characteristics: List<BluetoothGattCharacteristic>
    ) {
        Timber.i("Characteristics discovered: ${characteristics.size}")
        super.onCharacteristicsDiscovered(peripheral, characteristics)
        enableAllNotifications(peripheral, characteristics)
        getUserIndex(peripheral)
    }

    override fun onCharacteristicUpdate(
        peripheral: BluetoothPeripheral,
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: GattStatus
    ) {
        super.onCharacteristicUpdate(peripheral, value, characteristic, status)
        if (status == GattStatus.SUCCESS) {
            when (characteristic.uuid) {
                USER_INDEX_CHARACTERISTIC_UUID -> currentUserIndex(value.first())
                UDS_CONTROL_POINT_CHARACTERISTIC_UUID -> controlPointHandler.handleBytes(peripheral, value)
            }
        } else {
            Timber.e("Error in onCharacteristicUpdate()  for peripheral: $peripheral characteristic: <${characteristic.uuid}> error: ${status}")
        }
    }

    private fun currentUserIndex(userIndex: Byte) {
        Timber.i("Current User Index is: $userIndex")
    }

    private fun read(peripheral: BluetoothPeripheral, characteristicUUID: UUID) {
        peripheral.getCharacteristic(SERVICE_UUID, characteristicUUID)?.let {
            if (!peripheral.readCharacteristic(it)) {
                Timber.e("Error on characteristic read peripheral ${peripheral.address} uuid: $characteristicUUID")
            }
        }
    }

    fun write(characteristicUUID: UUID, value: ByteArray) {
        if (peripherals.isNotEmpty()) {
            write(peripherals.first(), characteristicUUID, value)
        } else {
            Timber.e("Error on write characteristic uuid: $characteristicUUID - No peripherals connected")
        }
    }


    /*
     * Public methods
     */

    fun newUserWithConsentCode(consentCode: Int) {
        controlPointHandler.newUserWithConsentCode(consentCode)
    }

    fun setUserWithConsentCode(userIndex: Int, consentCode: Int) {
        controlPointHandler.setUserWithConsentCode(userIndex, consentCode)
    }

    fun deleteUser(userIndex: Int) {
        controlPointHandler.deleteUser(userIndex)
    }

    fun getUserIndex() {
        if (peripherals.isNotEmpty()) {
            read(peripherals.first(), USER_INDEX_CHARACTERISTIC_UUID)
        }
    }

    /*
     * ServiceHandlerManagerListener methods
     */
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral) {}

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.add(peripheral)
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral) {
        peripherals.remove(peripheral)
    }

    fun getUserIndex(peripheral: BluetoothPeripheral) {
        read(peripheral, USER_INDEX_CHARACTERISTIC_UUID)
    }

    /*
     * GenericHealthSensorHandler Listener methods (add/remove)
     */

    fun addListener(listener: UserDataServiceHandlerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: UserDataServiceHandlerListener) = listeners.remove(listener)

    init {
        serviceUUID = SERVICE_UUID
        supportedCharacteristics.addAll(arrayOf(
            USER_DATABASE_CHANGE_INCREMENT,
            USER_INDEX_CHARACTERISTIC_UUID,
            UDS_CONTROL_POINT_CHARACTERISTIC_UUID,
            UDS_AGE_CHARACTERISTIC_UUID,
            UDS_FIRST_NAME_CHARACTERISTIC_UUID,
            UDS_LAST_NAME_CHARACTERISTIC_UUID
        ))
        ServiceHandlerManager.instance?.addListener(this)
    }

    companion object {
        val SERVICE_UUID = UUID.fromString("0000181C-0000-1000-8000-00805f9b34fb")
        val USER_DATABASE_CHANGE_INCREMENT = UUID.fromString("00002a99-0000-1000-8000-00805f9b34fb")
        val USER_INDEX_CHARACTERISTIC_UUID = UUID.fromString("00002a9a-0000-1000-8000-00805f9b34fb")
        val UDS_CONTROL_POINT_CHARACTERISTIC_UUID = UUID.fromString("00002a9f-0000-1000-8000-00805f9b34fb")

        val UDS_AGE_CHARACTERISTIC_UUID = UUID.fromString("00002a80-0000-1000-8000-00805f9b34fb")
        val UDS_FIRST_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002a8a-0000-1000-8000-00805f9b34fb")
        val UDS_LAST_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002a90-0000-1000-8000-00805f9b34fb")

        private const val USER_DATABASE_CHANGE_DESCRIPTION = "User database change increment"

        private const val USER_INDEX_DESCRIPTION = "User index characteristic"
        private const val UDS_CONTROL_POINT_DESCRIPTION = "Control Point characteristic"

        val instance: UserDataServiceHandler? get() {
            return ServiceHandlerManager.instance?.serviceHandlerForUUID(SERVICE_UUID)?.let { it as UserDataServiceHandler }
        }

    }

}
