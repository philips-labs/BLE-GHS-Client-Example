package com.philips.bleclient.ui

import android.view.View
import com.philips.bleclient.observations.Observation
import com.philips.bleclient.service.ghs.GenericHealthSensorHandlerRacpListener
import com.philips.bleclient.service.ghs.GenericHealthSensorServiceHandler
import timber.log.Timber


interface ObservationSyncerListener {
    fun onNumberOfStoredRecordsReceived(deviceAddress: String, numberOfRecords: Int)
    fun onStartStoredRetrieve(deviceAddress: String, numberOfObsToRetrieve: Int)
    fun onStoredObservationRetrieved(deviceAddress: String, observation: Observation, numberOfObsActuallyRetrieved: Int, numberOfObsToRetrieve: Int)
    fun onCompleteStoredRetrieve(deviceAddress: String, numberOfObsToRetrieve: Int, numberOfObsActuallyRetrieved: Int, numberOfRecordsSent: Int)
    fun onAbortCompleted(deviceAddress: String)
    fun onAbortError(deviceAddress: String, code: Byte)
}

// TODO This needs to suppport multiple devices... currently assumes one GHS peripheral
object ObservationSyncer: GenericHealthSensorHandlerRacpListener {

    private val listeners = mutableListOf<ObservationSyncerListener>()
    private val ghsServiceHandler get() = GenericHealthSensorServiceHandler.instance

    private var isRetrieving = false
    private var numberOfObsToRetrieve = 0
    private var numberOfObsRetrieved = 0

    // Connect to the service handler to start receving and handling RACP callbacks
    fun connect() {
        ghsServiceHandler?.addRacpListener(this)
    }

    // Connect to the service handler to stop receving and handling RACP callbacks
    fun disconnect() {
        ghsServiceHandler?.removeRacpListener(this)
    }

    fun abort() {
        if (isRetrieving) {
            ghsServiceHandler?.abortGetRecords()
        }
    }


    fun deleteAllRecords() {
        isRetrieving = false
        ghsServiceHandler?.deleteAllRecords()
    }

    fun deleteNumberOfRecordsGreaterThanId(startRecordNumber: Int) {
        isRetrieving = false
        ghsServiceHandler?.deleteRecordsAbove(startRecordNumber)
    }

    fun getNumberOfRecords() {
        isRetrieving = false
        ghsServiceHandler?.getNumberOfRecords()
    }

    fun getNumberOfRecordsGreaterThanId(startRecordNumber: Int) {
        isRetrieving = false
        ghsServiceHandler?.getNumberOfRecordsGreaterThan(startRecordNumber)
    }

    fun retrieveStoredObservationsAboveId(recordId: Int) {
        isRetrieving = true
        // Kick off by requesting number of records, then continue in response
        ghsServiceHandler?.getRecordsAbove(recordId)
    }

    fun retrieveStoredObservations() {
        isRetrieving = true
        // Kick off by requesting number of records, then continue in response
        ghsServiceHandler?.getNumberOfRecords()
    }

    fun retrieveFirstStoredObservation() {
        ghsServiceHandler?.getFirstRecord()
    }

    fun retrieveLastStoredObservation() {
        ghsServiceHandler?.getLastRecord()
    }

    /*
     * ObservationSyncer Listener methods (add/remove)
     */

    fun addListener(listener: ObservationSyncerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: ObservationSyncerListener) = listeners.remove(listener)

    /*
     * GenericHealthSensorHandlerRacpListener methods (add/remove)
     */

    override fun onNumberOfStoredRecordsResponse(deviceAddress: String, numberOfRecords: Int) {
        if (isRetrieving) {
            // We're starting a records retrieval, so insure state is fresh
            numberOfObsRetrieved = 0
            numberOfObsToRetrieve = numberOfRecords
            listeners.forEach { it.onStartStoredRetrieve(deviceAddress, numberOfObsToRetrieve) }
            ghsServiceHandler?.getAllRecords()
        } else {
            listeners.forEach { it.onNumberOfStoredRecordsReceived(deviceAddress, numberOfRecords) }
        }
    }

    override fun onReceivedStoredObservation(deviceAddress: String, observation: Observation) {
        if (isRetrieving) {
            listeners.forEach { it.onStoredObservationRetrieved(deviceAddress, observation, ++numberOfObsRetrieved, numberOfObsToRetrieve) }
        }
    }

    override fun onNumberOfStoredRecordsRetrieved(deviceAddress: String, numberOfRecords: Int) {
        if (isRetrieving) {
            isRetrieving = false
            listeners.forEach { it.onCompleteStoredRetrieve(deviceAddress, numberOfObsToRetrieve, numberOfObsRetrieved, numberOfRecords) }
            numberOfObsToRetrieve = 0
            numberOfObsRetrieved = 0
        }
    }

    override fun onRacpAbortCompleted(deviceAddress: String) {
        listeners.forEach { it.onAbortCompleted(deviceAddress) }
    }

    override fun onRacpAbortError(deviceAddress: String, code: Byte) {
        listeners.forEach { it.onAbortError(deviceAddress, code) }
    }

}
