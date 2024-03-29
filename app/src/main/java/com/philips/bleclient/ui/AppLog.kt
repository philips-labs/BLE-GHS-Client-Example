package com.philips.bleclient.ui

import android.text.Spanned
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.philips.bleclient.BR

object AppLog: BaseObservable() {
    private const val MAX_LOG_CHARACTERS: Int = 8000

    @get:Bindable
    var log: String = ""
        set(value) {
            field = value
            notifyPropertyChanged(BR.log)
        }

    fun log(message: String) {
        log = (log + "$message\n").last(MAX_LOG_CHARACTERS)
    }

    fun log(message: Spanned) {
        log = (log + "$message\n").last(MAX_LOG_CHARACTERS)
    }

    fun clear() {
        log = ""
    }
}
