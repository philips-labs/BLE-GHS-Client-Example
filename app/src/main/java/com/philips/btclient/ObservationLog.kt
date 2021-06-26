package com.philips.btclient

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.philips.btclient.acom.Observation

object ObservationLog: BaseObservable() {
    @get:Bindable
    var log: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.log)
        }

    fun log(observation: Observation) {
        log = (log + "$observation\n").last(400)
    }

    fun clear() {
        log = ""
    }
}

fun String.last(numChars: Int): String {
    return if (this.length < numChars) this else this.drop(this.length - numChars)
}
