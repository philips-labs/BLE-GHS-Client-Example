package com.philips.bleclient.ui

import android.os.Bundle
import android.os.UserManager
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import com.philips.bleclient.BR
import com.philips.bleclient.R
import com.philips.bleclient.ServiceHandlerManager
import com.philips.bleclient.service.user.UserDataServiceHandler
import com.philips.bleclient.service.user.UserDataServiceHandlerListener
import timber.log.Timber

class UsersActivity : AppCompatActivity(), UserDataServiceHandlerListener {

    private val ghsServiceHandler get() = ServiceHandlerManager.instance?.getGhsServiceHandler()
    private val usersLogView get() = findViewById<TextView>(R.id.usersLog)

    private var currentUserIndex = 1
    private var consentCode = 1234

    val logCallback = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            if (propertyId == BR.log) updateLogView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)
        supportActionBar?.let {
            it.setTitle(R.string.title_activity_users)
            it.setDisplayHomeAsUpEnabled(true)
        }
        UserDataServiceHandler.instance?.addListener(this)
        //ObservationSyncer.addListener(this)
        // Make the observation log scrollable
        usersLogView.setMovementMethod(ScrollingMovementMethod())
        setupUserIndexField()
        setupConsentCodeField()
    }

    override fun onResume() {
        super.onResume()
        UsersLog.addOnPropertyChangedCallback(logCallback)
    }

    override fun onDestroy() {
        // ObservationSyncer.removeListener(this)
        UsersLog.removeOnPropertyChangedCallback(logCallback)
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

    override fun onReceiveCurrentUserIndex(userIndex: Int) {
        UsersLog.log("Current user index is $userIndex")
    }

    private fun setupUserIndexField() {
        currentUserIndex = 1
        val textField = findViewById<EditText>(R.id.txtUserIndex)
        textField.setText(currentUserIndex.toString())
        findViewById<EditText>(R.id.txtUserIndex).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                currentUserIndex = if (s.length > 0) s.toString().toInt() else 0
            }
        })
    }

    private fun setupConsentCodeField() {
        consentCode = 1234
        val textField = findViewById<EditText>(R.id.txtConsentCode)
        textField.setText(consentCode.toString())
        findViewById<EditText>(R.id.txtConsentCode).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                consentCode = if (s.length > 0) s.toString().toInt() else 0
            }
        })
    }

    @Suppress("UNUSED_PARAMETER")
    fun newUser(view: View) {
        UsersLog.log("Create new user with consent code $consentCode")
        UserDataServiceHandler.instance?.newUserWithConsentCode(consentCode)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setUser(view: View) {
        UsersLog.log("Set current user $currentUserIndex consent code: $consentCode")
        UserDataServiceHandler.instance?.setUserWithConsentCode(currentUserIndex, consentCode)
    }

    @Suppress("UNUSED_PARAMETER")
    fun deleteUser(view: View) {
        if (currentUserIndex == 255) {
            UsersLog.log("Delete all users")
        } else {
            UsersLog.log("Delete user $currentUserIndex")
        }
        UserDataServiceHandler.instance?.deleteUser(currentUserIndex)
    }

    @Suppress("UNUSED_PARAMETER")
    fun deleteUserData(view: View) {
        UsersLog.log("Delete user data $currentUserIndex")
        UserDataServiceHandler.instance?.deleteUserData()
    }

    @Suppress("UNUSED_PARAMETER")
    fun readCurrentUser(view: View) {
        UserDataServiceHandler.instance?.getUserIndex() ?:  Timber.i("No UserDataServiceHandler instance")
    }

    @Suppress("UNUSED_PARAMETER")
    fun readCurrentUserFirstName(view: View) {
        UserDataServiceHandler.instance?.getFirstName()
    }

    @Suppress("UNUSED_PARAMETER")
    fun readCurrentUserLastName(view: View) {
        UserDataServiceHandler.instance?.getLastName()
    }

    private val textFieldValue get() = findViewById<EditText>(R.id.txtText).text.toString()
    private val dbIncValue get() = findViewById<EditText>(R.id.txtDbInc).text.toString().toInt()

    private fun doTextFieldEmptyMessage() {
        Toast.makeText(applicationContext, "Text field is empty", Toast.LENGTH_SHORT).show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun writeCurrentUserFirstName(view: View) {
        if (textFieldValue.isEmpty()) {
            doTextFieldEmptyMessage()
        } else {
            UserDataServiceHandler.instance?.setFirstName(textFieldValue)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun writeCurrentUserLastName(view: View) {
        if (textFieldValue.isEmpty()) {
            doTextFieldEmptyMessage()
        } else {
            UserDataServiceHandler.instance?.setLastName(textFieldValue)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun readDatabaseChangeInc(view: View) {
        UserDataServiceHandler.instance?.getDatabaseChangeInc()
    }

    @Suppress("UNUSED_PARAMETER")
    fun writeDatabaseChangeInc(view: View) {
        val dbInc = dbIncValue
        UsersLog.log("Write Db Change Increment current user $currentUserIndex inc value: $dbIncValue")
        UserDataServiceHandler.instance?.setDatabaseChangeInc(dbInc)
    }

    private fun updateLogView() {
        usersLogView.setText(UsersLog.log)
        usersLogView.scrollToBottom()
    }

}