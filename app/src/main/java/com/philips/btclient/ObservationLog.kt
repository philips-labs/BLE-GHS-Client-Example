/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.philips.btclient.acom.Observation

object ObservationLog: BaseObservable() {
    private val MAX_LOG_CHARACTERS = 4000

    @get:Bindable
    var log: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.log)
        }

    fun log(message: String) {
        log = (log + "$message\n").last(MAX_LOG_CHARACTERS)
    }

    fun log(observation: Observation) {
        log("$observation")
    }

    fun clear() {
        log = ""
    }
}

fun String.last(numChars: Int): String {
    return if (this.length < numChars) this else this.drop(this.length - numChars)
}
