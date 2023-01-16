package com.philips.bleclient.ui

import com.philips.bleclient.observations.Observation
import com.philips.bleclient.service.ghs.GenericHealthSensorHandlerListener
import com.philips.btserver.generichealthservice.ObservationType

object GHSDeviceInfoMap: GenericHealthSensorHandlerListener {
    val deviceInfoMap = mutableMapOf<String, MutableMap<String, Any>>()

    fun getAddress(deviceAddress: String): Map<String, Any>? {
        return deviceInfoMap.get(deviceAddress)
    }


    fun getSupportedObservationTypes(deviceAddress: String): List<ObservationType>? {
        return getAddress(deviceAddress)?.get("supportedObservationTypes")?.let { it as List<ObservationType> }
    }

    // GenericHealthSensorHandlerListener methods

    override fun onDisconnected(deviceAddress: String) {
        deviceInfoMap.remove(deviceAddress)
    }

    override fun onSupportedObservationTypes(
        deviceAddress: String,
        observationTypes: List<ObservationType>
    ) {
        deviceInfoMap.getOrPut(deviceAddress) { mutableMapOf() }.put("supportedObservationTypes", observationTypes)
    }
}