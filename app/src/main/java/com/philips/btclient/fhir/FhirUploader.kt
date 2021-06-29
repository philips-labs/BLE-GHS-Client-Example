package com.philips.btclient.fhir

import com.philips.btclient.acom.Observation
import com.philips.btclient.util.asFhir
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import okhttp3.Response

object FhirUploader {
    var usePublicHapiServer = true
    val localHapiServer = "http://192.168.1.47:8080/fhir/Observation"
    val publicHapiServer = "http://hapi.fhir.org/baseR4/Observation"

    val urlString: String get() {return if (usePublicHapiServer) publicHapiServer else localHapiServer
    }

    private val client = OkHttpClient()

    fun postObservation(observation: Observation): Response {
        return postFhir(observation.asFhir())
    }

    fun postFhir(fhir: String): Response {
        val body: RequestBody = fhir.toRequestBody("application/json".toMediaType())

        val hapiUrl = if (usePublicHapiServer) publicHapiServer else localHapiServer
        val request = Request.Builder()
            .url(hapiUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            println("FHIR post to $urlString ${if (response.isSuccessful) "SUCCESS" else "FAILED ${response.code}"}")
            println(response.body?.string())
            return response
        }
    }
}