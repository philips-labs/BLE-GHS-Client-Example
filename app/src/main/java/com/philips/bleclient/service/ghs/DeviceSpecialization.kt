package com.philips.bleclient.service.ghs

import com.philips.btserver.generichealthservice.ObservationType
import java.lang.IllegalArgumentException

// Partition 8
enum class DeviceSpecialization(val value: Int) {
    // TODO Add remaining device specializations
    MDC_DEV_SPEC_PROFILE_HYDRA(528384),
    MDC_DEV_SPEC_PROFILE_INFUS(528385),
    MDC_DEV_SPEC_PROFILE_VENT(528386),
    MDC_DEV_SPEC_PROFILE_VS_MON(528387),
    MDC_DEV_SPEC_PROFILE_PULS_OXIM(528388),
    MDC_DEV_SPEC_PROFILE_DEFIB(528389),
    MDC_DEV_SPEC_PROFILE_ECG(528390),
    MDC_DEV_SPEC_PROFILE_BP(528391),
    MDC_DEV_SPEC_PROFILE_TEMP(528392),
    UNKNOWN_DEV_SPEC(-1);


    companion object {
        @Suppress("unused")
        fun fromValue(value: Int?): DeviceSpecialization {
            for( devspec in DeviceSpecialization.values()) {
                if (devspec.value == value){
                    return devspec
                }
            }
            return UNKNOWN_DEV_SPEC
        }

        @Suppress("unused")
        fun fromString(string: String): DeviceSpecialization {
            return try {
                DeviceSpecialization.valueOf(string)
            } catch (exception: IllegalArgumentException) {
                DeviceSpecialization.UNKNOWN_DEV_SPEC
            }
        }
    }
}