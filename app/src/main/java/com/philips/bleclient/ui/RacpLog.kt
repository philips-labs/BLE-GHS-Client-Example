package com.philips.bleclient.ui

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import com.philips.bleclient.observations.Observation

object RacpLog: BaseObservable() {
    private const val MAX_LOG_CHARACTERS: Int = 4000

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
