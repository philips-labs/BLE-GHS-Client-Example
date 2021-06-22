/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.mjolnir.services.handlers.generichealthsensor.acom

/*
 * Constants from the IEEE P11073-10101 Specification
 */
class MdcConstants {
    // TODO: Note moving measurements to MdcMeasurements, (and value types to another enum, etc, etc)
    companion object {

        const val MDC_ATTR_ID_TYPE =                0x0001092F  // Type, VMO and derived objects
        const val MDC_ATTR_ID_HANDLE =              0x00010921  // Handle, VMO and derived objects, etc.
        const val MDC_ATTR_UNIT_CODE =              0x00010996  // Unit code for objects



        const val MDC_ATTR_NU_VAL_OBS_SIMP =        0x00010A56  // Simple-Nu-Observed-Value
        const val MDC_ATTR_NU_CMPD_VAL_OBS =        0x0001094B  // Compound-Nu-Observed-Value, Numeric and derived objects
        const val MDC_ATTR_NU_VAL_OBS =             0x00010950  // Nu-Observed-Value, Numeric and derived objects
        const val MDC_ATTR_SA_VAL_OBS =             0x0001096E  // Sa-Observed-Value, Sample Array and derived objects
        const val MDC_ATTR_SA_CMPD_VAL_OBS =        0x00010967  // Compound-Sa-Observed-Value, Sample Array and derived objects
        const val MDC_ATTR_VAL_ENUM_OBS =           0x0001099E  // Enum-Observed-Value, Enumeration
        const val MDC_ATTR_VAL_ENUM_OBS_CMPD =      0x0001099F  // Compound-Enum-Observed-Value, Enumeration
        const val MDC_ATTR_CMPLX_VAL_OBS =          0x00010A3C  // Cmplx-Observed-Value, Complex Metric

        const val MDC_ATTR_TIME_STAMP_ABS =         0x00010990  // Absolute-Time-Stamp, Numeric, Time Sample Array, Distribution Sample Array, Enumeration and derived objects

        const val MDC_ATTR_SUPPLEMENTAL_TYPES =     0x00010A61  // Supplemental-Types (being used for MDC_ATTR_SUPPLEMENTAL_INFO)

        fun isTypeAttribute(value: Int): Boolean {
            return value == MDC_ATTR_ID_TYPE
        }
    }
}