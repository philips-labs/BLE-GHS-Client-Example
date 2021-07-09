/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.acom

import com.philips.bleclient.extensions.getObservations
import com.welie.blessed.BluetoothBytesParser
import java.nio.ByteOrder

/*
 * The ACOM Object represents the ACOM content information model from IEEE 11073-10206 (Sec. 7). The PHD class acts as a
 * container for all other classes that make up the device. The systeminfo class contains technical and descriptive
 * information about the device. A particular device model may include the power source and a clock classes to
 * model power and time information.
 *
 * NOTE: For now the PHD Class (along with System Info and Clock) are not being implemented as they are TBD
 *
 * The device model will contain at least one generalization of the observation class that describes the
 * observations that the modeled device generates. The observation class is a base class that defines the common
 * attributes used in reporting all observations. In general, numeric classes are used to represent episodic
 * measurements. The Sample Array class is used to model frequent periodic samples or waveforms, and
 * discrete observation classes are used to represent events and status. Two types of numeric observation are
 * supported: a simple observation and one with multiple components. There are three generalizations of the
 * discrete observation class.
 */
@Suppress("unused")
class AcomObject(bytes: ByteArray) {

    /*
     * id: used to reference the object. The id is of type referenceType, the structure of which is not further specified.
     */
    var id: Any? = null

    /*
     * modelVersion: identifies the version of this specification used to model the device
     */
    var modelVersion: Any? = null

    /*
     * nomenclatureVersion:  identifies the version of the nomenclature terms used by this device
     */
    var nomenclatureVersion: Any? = null

    /*
     * objectClass:  identifies the class of the object based on IEEE Std 11073-10101[B10] nomenclature.
     */
    var objectClass: Any? = null

    var observations: List<Observation> = emptyList()

    /*
     * Any errors in the bytes causes the observation to be skipped... and potentially an issue parsing
     * from that point forward in which case the observations could end up empty. An optional improvment
     * would be to throw and catch exceptions with the details of where in the bytes an error occured
     */
    init {
        readObservations(BluetoothBytesParser(bytes, 0, ByteOrder.BIG_ENDIAN))
    }

    private fun readObservations(bytesParser: BluetoothBytesParser) {
        observations = bytesParser.getObservations()
    }

}
