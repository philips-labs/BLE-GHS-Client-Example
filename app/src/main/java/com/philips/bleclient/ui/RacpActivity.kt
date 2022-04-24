package com.philips.bleclient.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.observations.Observation
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RacpActivity : AppCompatActivity(), ObservationSyncerListener {

    private val ghsServiceHandlerManager get() = ServiceHandlerManager.instance?.getGhsServiceHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_racp)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_racp)
            it.setDisplayHomeAsUpEnabled(true)
        }
        ObservationSyncer.addListener(this)
    }

    override fun onDestroy() {
        ObservationSyncer.removeListener(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

    @Suppress("UNUSED_PARAMETER")
    fun numberOfRecords(view: View) = ghsServiceHandlerManager?.getNumberOfRecords()

    @Suppress("UNUSED_PARAMETER")
    fun numberRecordsAboveFour(view: View)  = ghsServiceHandlerManager?.getNumberOfRecordsGreaterThan(5)

    @Suppress("UNUSED_PARAMETER")
    fun getAllRecords(view: View) = ObservationSyncer.retrieveStoredObservations()

    @Suppress("UNUSED_PARAMETER")
    fun getRecordsAboveFour(view: View) = ghsServiceHandlerManager?.getRecordsAbove(4)

    @Suppress("UNUSED_PARAMETER")
    fun abort(view: View) = ghsServiceHandlerManager?.abortGetRecords()

    /*
     * ObservationSyncerListener interface methods
     */
    override fun onStartStoredRetrieve(deviceAddress: String, numberOfObsToRetrieve: Int) {
        Timber.i("Start stored retrieve of $numberOfObsToRetrieve records")
    }

    override fun onStoredObservationRetrieved(
        deviceAddress: String,
        observation: Observation,
        numberOfObsActuallyRetrieved: Int,
        numberOfObsToRetrieve: Int
    ) {
        Timber.i("Retrieved ${observation.type} observation, $numberOfObsActuallyRetrieved of $numberOfObsToRetrieve received")
        updateProgressBar(numberOfObsActuallyRetrieved, numberOfObsToRetrieve)
    }

    override fun onCompleteStoredRetrieve(
        deviceAddress: String,
        numberOfObsToRetrieve: Int,
        numberOfObsActuallyRetrieved: Int,
        numberOfRecordsSent: Int
    ) {
        Timber.i("Retrieved $numberOfObsActuallyRetrieved of $numberOfObsToRetrieve. $numberOfRecordsSent sent")
        clearProgressBar()
    }

    override fun onAbortCompleted(deviceAddress: String) {
        Timber.i("RACP Abort completed successfully")
        clearProgressBar()
    }

    override fun onAbortError(deviceAddress: String, code: Byte) {
        Timber.i("RACP Abort completed with error $code")
    }

    private fun getProgressBar(): ProgressBar {
        return findViewById(R.id.obsSyncProgress) as ProgressBar
    }
    private fun updateProgressBar(numberOfObsActuallyRetrieved: Int, numberOfObsToRetrieve: Int) {
        getProgressBar().progress = ((numberOfObsActuallyRetrieved.toFloat() / numberOfObsToRetrieve.toFloat()) * 100).toInt()
    }

    private fun clearProgressBar() {
        Executors.newSingleThreadScheduledExecutor()
            .schedule({ getProgressBar().progress = 0 }, 5, TimeUnit.SECONDS)
    }
}