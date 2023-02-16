package com.philips.bleclient.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import com.philips.bleclient.BR
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.observations.Observation
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class RacpActivity : AppCompatActivity(), ObservationSyncerListener {

    private val ghsServiceHandler get() = ServiceHandlerManager.instance?.getGhsServiceHandler()

    private var isGetRecordsAll = false
    private var useIndications = false

    private var queryByDate = false

    private val racpLogView get() = findViewById<TextView>(R.id.racpLog)
    private val progressBarView get() = findViewById<ProgressBar>(R.id.obsSyncProgress)

    private val queryCalendar = Calendar.getInstance()

    private var startRecordNumber = 5
        set(value) {
            field = value
            updateQueryButtons()
        }

    val logCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (propertyId == BR.log) updateLogView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_racp)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_racp)
            it.setDisplayHomeAsUpEnabled(true)
        }
        ObservationSyncer.addListener(this)
        setupRecordEntryField()
        findViewById<Switch>(R.id.useDateSwitch).visibility = View.INVISIBLE
        setupDatePicker()
        // Make the observation log scrollable
        racpLogView.setMovementMethod(ScrollingMovementMethod())
    }

    override fun onResume() {
        super.onResume()
        RacpLog.addOnPropertyChangedCallback(logCallback)
    }

    override fun onDestroy() {
        ObservationSyncer.removeListener(this)
        RacpLog.removeOnPropertyChangedCallback(logCallback)
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

    private fun setupRecordEntryField() {
        startRecordNumber = 5
        val textField = findViewById<EditText>(R.id.txtStartRecord)
        textField.setText(startRecordNumber.toString())
        findViewById<EditText>(R.id.txtStartRecord).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                startRecordNumber = if (s.length > 0) s.toString().toInt() else 0
            }
        })
    }

    lateinit var dateEdt: EditText
    lateinit var timeEdt: EditText

    private fun setupDatePicker() {
        val dv = findViewById<EditText>(R.id.txtDate)
        val sr = findViewById<EditText>(R.id.txtStartRecord)
        dateEdt = dv
        dateEdt.setOnClickListener {
            val year = queryCalendar.get(Calendar.YEAR)
            val month = queryCalendar.get(Calendar.MONTH)
            val day = queryCalendar.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                this,
                { view, newYear, monthOfYear, dayOfMonth ->
                    queryCalendar.set(Calendar.YEAR, newYear)
                    queryCalendar.set(Calendar.MONTH, monthOfYear)
                    queryCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + newYear)
                    dateEdt?.setText(dat)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
        timeEdt = findViewById(R.id.txtTime)
        timeEdt.setOnClickListener {
            val hour = queryCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = queryCalendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(
                this,
                { view, hr, min ->
                    queryCalendar.set(Calendar.HOUR, hr)
                    queryCalendar.set(Calendar.MINUTE, min)
                    val dat = (hr.toString() + ":" + (min + 1).toString())
                    timeEdt.setText(dat)
                },
                hour,
                minute,
                true
            )
            timePickerDialog.show()
        }
    }

    private fun updateQueryButtons() {
        findViewById<Button>(R.id.queryRecordsButton).text = "${getString(R.string.aboveRecords)} $startRecordNumber"
        findViewById<Button>(R.id.getRecordsAbove).text = "${getString(R.string.getRecordsAbove)} $startRecordNumber"
    }

    @Suppress("UNUSED_PARAMETER")
    fun numberOfRecords(view: View) {
        isGetRecordsAll = true
//        ghsServiceHandlerManager?.getNumberOfRecords()
        ObservationSyncer.getNumberOfRecords()
//        ObservationSyncer.getNumberOfRecordsGreaterThanId(startRecordNumber)
    }


    @Suppress("UNUSED_PARAMETER")
    fun deleteAllRecords(view: View) {
        isGetRecordsAll = true
        ObservationSyncer.deleteAllRecords()
    }


    @Suppress("UNUSED_PARAMETER")
    fun deleteRecordsAbove(view: View) {
        isGetRecordsAll = false
        ObservationSyncer.deleteNumberOfRecordsGreaterThanId(startRecordNumber)
    }

    @Suppress("UNUSED_PARAMETER")
    fun numberRecordsAbove(view: View) {
        isGetRecordsAll = false
//        ghsServiceHandlerManager?.getNumberOfRecordsGreaterThan(startRecordNumber)
        if (queryByDate) {
            ObservationSyncer.getNumberOfRecordsGreaterThanDate(queryCalendar.time)
        } else {
            ObservationSyncer.getNumberOfRecordsGreaterThanId(startRecordNumber)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleIndications(view: View) {
        useIndications = (view as Switch).isChecked

        val notifyProperty = if (useIndications) {
            ObservationLog.log("Using Indications for RACP")
            Timber.i("Using Indications for RACP")
            PROPERTY_INDICATE
        } else {
            ObservationLog.log("Using Notificiations for RACP")
            Timber.i("Using Notificiations for RACP")
            PROPERTY_NOTIFY
        }
        ghsServiceHandler?.racpHandler?.useIndicationsForRACP(notifyProperty)

    }

    @Suppress("UNUSED_PARAMETER")
    fun toggleDateQuery(view: View) {
        queryByDate = (view as Switch).isChecked
        updateDatePicker()
    }

    private fun updateDatePicker() {
        val dateVisibility = if (queryByDate) View.VISIBLE else View.INVISIBLE
        val recNumVisibility = if (queryByDate) View.INVISIBLE else View.VISIBLE
        findViewById<TextView>(R.id.lblQueryStartRecord).visibility = recNumVisibility
        findViewById<EditText>(R.id.txtStartRecord).visibility = recNumVisibility
        findViewById<EditText>(R.id.txtDate).visibility = dateVisibility
        findViewById<EditText>(R.id.txtTime).visibility = dateVisibility
    }

    @Suppress("UNUSED_PARAMETER")
    fun getAllRecords(view: View) {
        isGetRecordsAll = true
        ObservationSyncer.retrieveStoredObservations()
    }

    @Suppress("UNUSED_PARAMETER")
    fun getFirstRecord(view: View) {
        isGetRecordsAll = false
        ObservationSyncer.retrieveFirstStoredObservation()
    }

    fun getLastRecord(view: View) {
        isGetRecordsAll = false
        ObservationSyncer.retrieveLastStoredObservation()
    }

    @Suppress("UNUSED_PARAMETER")
    fun getRecordsAbove(view: View) {
        isGetRecordsAll = false
//        ghsServiceHandlerManager?.getRecordsAbove(startRecordNumber)
        ObservationSyncer.retrieveStoredObservationsAboveId(startRecordNumber)
//        ObservationSyncer.retrieveStoredObservations()
    }

    @Suppress("UNUSED_PARAMETER")
    fun abort(view: View) { ObservationSyncer.abort() }

    private fun updateLogView() {
        racpLogView.setText(RacpLog.log)
        racpLogView.scrollToBottom()
    }

    /*
     * ObservationSyncerListener interface methods
     */

    override fun onNumberOfStoredRecordsReceived(deviceAddress: String, numberOfRecords: Int) {
        val viewId = if (isGetRecordsAll) R.id.txtObservationStoreCount else R.id.txtObservationGEStoreCount
        findViewById<TextView>(viewId).text = numberOfRecords.toString()
        RacpLog.log("# Stored Records Response: $numberOfRecords")
    }

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
        RacpLog.log("Retrieved #$numberOfObsActuallyRetrieved $observation")
        updateProgressBar(numberOfObsActuallyRetrieved, numberOfObsToRetrieve)
    }

    override fun onCompleteStoredRetrieve(
        deviceAddress: String,
        numberOfObsToRetrieve: Int,
        numberOfObsActuallyRetrieved: Int,
        numberOfRecordsSent: Int
    ) {
        Timber.i("Retrieved $numberOfObsActuallyRetrieved of $numberOfObsToRetrieve. $numberOfRecordsSent sent")
        RacpLog.log("Completed: $numberOfObsActuallyRetrieved retrieved")
        clearProgressBar()
    }

    override fun onAbortCompleted(deviceAddress: String) {
        Timber.i("RACP Abort completed successfully")
        clearProgressBar()
    }

    override fun onAbortError(deviceAddress: String, code: Byte) {
        Timber.i("RACP Abort completed with error $code")
        clearProgressBar()
    }

    private fun updateProgressBar(numberOfObsActuallyRetrieved: Int, numberOfObsToRetrieve: Int) {
        progressBarView.progress =
            ((numberOfObsActuallyRetrieved.toFloat() / numberOfObsToRetrieve.toFloat()) * 100).toInt()
    }

    private fun clearProgressBar() {
        Executors.newSingleThreadScheduledExecutor()
            .schedule({ progressBarView.progress = 0 }, 5, TimeUnit.SECONDS)
    }

}

fun TextView.scrollToBottom() {
    val scrollAmount = getLayout().getLineTop(getLineCount()) - getHeight();
    // if there is no need to scroll, scrollAmount will be <=0
    scrollTo(0, if (scrollAmount > 0) scrollAmount else 0)
}
