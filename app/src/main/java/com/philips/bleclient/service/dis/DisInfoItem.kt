package com.philips.bleclient.service.dis

import com.philips.bleclient.service.bas.BasServiceHandler
import java.util.*

enum class DisInfoItem(val value : UUID) {
    MANUFACTURER_NAME(  UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")),
    MODEL_NUMBER(  UUID.fromString("000002A24-0000-1000-8000-00805f9b34fb")),
    SERIAL_NUMBER( UUID.fromString("000002A25-0000-1000-8000-00805f9b34fb")),
    HARDWARE_REVISION( UUID.fromString("000002A27-0000-1000-8000-00805f9b34fb")),
    FIRMWARE_REVISION( UUID.fromString("000002A26-0000-1000-8000-00805f9b34fb")),
    SOFTWARE_REVISION(  UUID.fromString("000002A28-0000-1000-8000-00805f9b34fb")),
    SYSTEM_ID(UUID.fromString("000002A23-0000-1000-8000-00805f9b34fb")),
    PNP_ID(UUID.fromString("000002A50-0000-1000-8000-00805f9b34fb")),
    UDI(UUID.fromString("000002bff-0000-1000-8000-00805f9b34fb")),
    BATTERY_LEVEL(BasServiceHandler.BATTERY_LEVEL_CHARACTERISTIC_UUID)

//    fun getUuid(): UUID {
//        return value
//    }

}