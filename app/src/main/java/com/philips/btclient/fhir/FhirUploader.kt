/*
 * Copyright (c) Koninklijke Philips N.V. 2021.
 * All rights reserved.
 */
package com.philips.btclient.fhir

import com.philips.btclient.acom.Observation
import com.philips.btclient.util.asFhir
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object FhirUploader {
    var usePublicHapiServer = true
    var localHapiServer = "http://192.168.1.47:8080/fhir/Observation"
    private const val publicHapiServer = "http://hapi.fhir.org/baseR4/Observation"

    val urlString: String get() { return if (usePublicHapiServer) publicHapiServer else localHapiServer }

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
        try {
            client.newCall(request).execute().use { response ->
                println("FHIR post to $urlString ${if (response.isSuccessful) "SUCCESS" else "FAILED ${response.code}"}")
                println(response.body?.string())
                return response
            }
        } catch (e: Exception) {
            println("Exception in FHIR post to $urlString: ${e.message}")
            // 420 Method Failure (Spring Framework) - deprecated response used by the Spring Framework when a method has failed
            return Response.Builder().request(request).message(e.message ?: "Excepetion occurred").code(420).build()
        }
    }
}