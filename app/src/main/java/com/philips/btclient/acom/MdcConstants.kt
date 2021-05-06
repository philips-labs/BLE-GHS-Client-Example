/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.btclient.acom

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

        // Measurement - Cardio/Heart
        const val MDC_ECG_HEART_RATE =              0x00024182 // Rate | Beats | Heart | CV
        // Measurement - PPG (It's the best we have right now
        const val MDC_PPG_TIME_PD_PP =              0x00024840 // Period | Plethysmography | Pulse | Blood, CVS
        const val MDC_PLETH =                       0x00024BB4 // Plethysmography
        const val MDC_PULS_OXIM_PLETH =             0x00024BB4 // Blood pressure
        // Measurement - Blood Pressure
        const val MDC_PRESS_BLD =                   0x000204A00 // Plethysmography

        const val MDC_TEMP_BODY =                   0x00024B5C // Temperature - Body

        const val MDC_SPO2_OXYGENATION_RATIO =      0x00024C90 // SpO2 (for now the one sent by the simulator)

        // Unit Codes
        const val MDC_DIM_BEAT_PER_MIN =            0x00040AA0 // Beats per minute
    }

    enum class MdcMeasurements(val value: Int) {
        // Measurement - Cardio/Heart
        MDC_ECG_HEART_RATE(0x00024182),  // Rate | Beats | Heart | CVS
        // Measurement - PPG (It's the best we have right now
        MDC_PPG_TIME_PD_PP(0x00024840), // Period | Plethysmography | Pulse | Blood, CVS
        MDC_PLETH(0x00024BB4), // Plethysmography
        MDC_PULS_OXIM_PLETH(0x00024BB4),
        // Measurement - Blood Pressure
        MDC_PRESS_BLD(0x000204A00), // Blood Pressure

        // Temperature measurements
        MDC_TEMP_BODY(150364),
        MDC_TEMP_ORAL(188424),
        MDC_TEMP_EAR(188428),
        MDC_TEMP_FINGER(188432);

        companion object {
            fun valueOf(value: Int): MdcMeasurements? = values().find { it.value == value }
        }

    }

    // TODO: Note moving everything to MdcMeasurements
    enum class MDC_PRESS_BLD(val value: Int) {
        DIA(0x000204A02), // Pressure of the blood at the diastolic phase
        AORT_DIA (0x000204A0E), // Diastolic pressure of the blood in the aorta
        ART_DIA (0x000204A12), // Diastolic pressure of the blood in an artery
        ART_ABP_DIA(0x000204A16), // Diastolic pressure of the blood in an artery measured ambulatory

        SYS(0x000204A01), // Pressure of the blood at the systolic phase
        AORT_SYS (0x000204A0D), // Systolic pressure of the blood in the aorta
    }

    // TODO: Note moving everything to MdcMeasurements
    enum class MDC_TEMP(val value: Int) {
        BODY(150364),
        TYMP(150392),
        RECT(188420),
        ORAL(188424),
        EAR(188428),
        FINGER(188432),
        TOE(188448),
        AXILLA(188452),
        GIT(188456)
    }

    enum class MDC_BODY(val value: Int) {
        BODY(0x70840), // Body as whole
        LEFT_BODY(0x70841), // Left side of body as whole
        RIGHT_BODY(0x70841), // Right side of body as whole
        UPEXT_FOREARM(0x706E8), // Upper extremity, Forearm,
        UPEXT_FOREARM_L(0x706E9), // Upper extremity, Forearm, Left
        UPEXT_FOREARM_R(0x706EA), // Upper extremity, Forearm, Left
        UPEXT_ARM_UPPER(0x706F4), // Upper extremity, Forearm,
        UPEXT_ARM_UPPER_L(0x706F5), // Upper extremity, Forearm, Left
        UPEXT_ARM_UPPER_R(0x706F6), // Upper extremity, Forearm, Left
    }

    enum class MDC_DEV_ANALY(val value: Int) {
        GENERIC( 0x11004), // Instrument that analyzes acquired patient information (Generic analyzer)
        SAT_O2( 0x11008), // SpO2 monitor
        PRESS_BLD( 0x1104C), // Blood pressure
    }
}