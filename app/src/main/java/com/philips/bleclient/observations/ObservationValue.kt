/*
 * Copyright (c) Koninklijke Philips N.V. 2020.
 * All rights reserved.
 */
package com.philips.bleclient.observations

import com.philips.btserver.generichealthservice.UnitCode

abstract class ObservationValue {
    open var unitCode: UnitCode = UnitCode.UNKNOWN_CODE
}