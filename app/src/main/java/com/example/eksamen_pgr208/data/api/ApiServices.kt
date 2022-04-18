package com.example.eksamen_pgr208.data.api

import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.example.eksamen_pgr208.MainActivity
import com.example.eksamen_pgr208.common.Constants
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors


class ApiServices {
    companion object {

        private val logger = KotlinLogging.logger {}

        fun uploadImage(mainActivity: MainActivity, filePath: String) {

            CoroutineScope(Dispatchers.IO).launch {

            val okHttpClient = OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()

                AndroidNetworking.upload(Constants.API_UPLOAD_URL)
                    .addMultipartFile("image", File(filePath))
                    .addMultipartParameter("content-type", "image/png")
                    .setPriority(Priority.HIGH)
                    .setOkHttpClient(okHttpClient)
                    .setExecutor(Executors.newSingleThreadExecutor())
                    .build()
                    .setUploadProgressListener { bytesUploaded, _ ->
                        println("bytesUploaded: $bytesUploaded")
                    }
                    .getAsString(object : StringRequestListener {
                        override fun onResponse(response: String) {
                            println("From POST response: $response")
                            mainActivity.liveDataUploadImage.postValue(response)
                        }
                        override fun onError(error: ANError) {
                            println("From POST error: ${error.errorBody}")
                        }
                    })
            }
        }

        fun getImages(mainActivity: MainActivity) {

            // parsing response
            val gson: Gson
            val gsonBuilder = GsonBuilder()
            gson = gsonBuilder.create()

            mainActivity.liveDataUploadImage.observe(mainActivity) { res ->

                if (res.isEmpty()) {
                    Toast.makeText(mainActivity, "No images found OR ERROR!", Toast.LENGTH_SHORT).show()
                } else {

                    CoroutineScope(Dispatchers.IO).launch {

                        println("Starting GET request at Google...")
                        AndroidNetworking.get(Constants.API_GET_GOOGLE)
                            .addQueryParameter("url", res)
                            .setTag("image")
                            .setPriority(Priority.HIGH)
                            .setExecutor(Executors.newSingleThreadExecutor())
                            .build()
                            .getAsString(object : StringRequestListener {
                                override fun onResponse(response: String?) {
                                    responseHandler(gson, response, mainActivity, Constants.API_GET_GOOGLE)
                                }

                                override fun onError(anError: ANError?) {
                                    println("ErrorBody from GET request at Google: ${anError?.errorBody}")
                                    println("ErrorCode from GET request at Google: ${anError?.errorCode}")
                                    println("ErrorDetail from GET request at Google: ${anError?.errorDetail}")
                                }
                            })
                        }

                    CoroutineScope(Dispatchers.IO).launch {

                        println("Starting GET request at Tineye...")
                        AndroidNetworking.get(Constants.API_GET_TINEYE)
                            .addQueryParameter("url", res)
                            .setTag("image")
                            .setPriority(Priority.HIGH)
                            .setExecutor(Executors.newSingleThreadExecutor())
                            .build()
                            .getAsString(object : StringRequestListener {
                                override fun onResponse(response: String?) {
                                    responseHandler(gson, response, mainActivity, Constants.API_GET_TINEYE)
                                }

                                override fun onError(anError: ANError?) {
                                    println("ErrorBody from GET request at Tineye: ${anError?.errorBody}")
                                    println("ErrorCode from GET request at Tineye: ${anError?.errorCode}")
                                    println("ErrorDetail from GET request at Tineye: ${anError?.errorDetail}")
                                }
                            })
                        }

                    CoroutineScope(Dispatchers.IO).launch {

                        println("Starting GET request at Bing...")
                        AndroidNetworking.get(Constants.API_GET_BING)
                            .addQueryParameter("url", res)
                            .setTag("image")
                            .setPriority(Priority.HIGH)
                            .setExecutor(Executors.newSingleThreadExecutor())
                            .build()
                            .getAsString(object : StringRequestListener {
                                override fun onResponse(response: String?) {
                                    responseHandler(gson, response, mainActivity, Constants.API_GET_BING)
                                }

                                override fun onError(anError: ANError?) {
                                    println("ErrorBody from GET request at Bing: ${anError?.errorBody}")
                                    println("ErrorCode from GET request at Bing: ${anError?.errorCode}")
                                    println("ErrorDetail from GET request at Bing: ${anError?.errorDetail}")
                                }
                            })
                        }
                }
            }
        }

        private fun responseHandler(
            gson: Gson,
            response: String?,
            mainActivity: MainActivity,
            apiEndPoint: String,
        ) {

            val endPointName = apiEndPoint.substring(apiEndPoint.lastIndexOf("/") + 1)
            val upperCaseOnFirstLetterEndPointName = endPointName.substring(0, 1).uppercase() + endPointName.substring(1)

            try {
                val convertedResponse = gson.fromJson(
                    response,
                    ImageModelResult::class.java
                )
                if (convertedResponse == null) {
                    println("Response from $apiEndPoint is $convertedResponse")
                    throw NullPointerException("Can't handle null arrays")
                }
                if (convertedResponse.size == 0) {
                    logger.warn("Warning from google API line 92")
                    println("Response from $apiEndPoint is $convertedResponse")
                    throw IllegalArgumentException("Can't handle zero-length arrays")
                } else {
                    println("Response from $apiEndPoint is $convertedResponse")
                    println("Using $apiEndPoint")
                    mainActivity.liveDataGetImages.postValue(
                        convertedResponse
                    )
                }
            } catch (e: Exception) {
                e.stackTraceToString()
            } finally {
                println("GET request from API: '$upperCaseOnFirstLetterEndPointName' done")
            }
        }
    }
}