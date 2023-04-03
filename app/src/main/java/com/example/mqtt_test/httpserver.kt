package com.example.mqtt_test
import android.content.Context
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status.OK
import java.io.IOException
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

typealias ClientGenKeyCallbackFunction = () -> String
typealias ClientCertCallbackFunction = (String) -> String
typealias submitUrlFunction = (String) -> String

@Serializable
data class JsonRequest(val command: String, val data: String)

class AndroidHttpServer(private val context: Context,
                        private val gCallback:ClientGenKeyCallbackFunction,
                        private val cCallback:ClientCertCallbackFunction,
                        private val sCallback:submitUrlFunction,
                        port: Int) : NanoHTTPD(port) {



    @Throws(IOException::class)
    fun startServer() {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    }

    fun stopServer() {
        stop()
    }

    private fun executeGenKeyCallback(): String {
        return gCallback()
    }

    private fun executeCertCallback(Cert:String): String {
        return cCallback(Cert)
    }

    private fun executeUrlSubmitCallback(Url:String): String {
        return sCallback(Url)
    }

    private fun readRequestBody(session: IHTTPSession): String {
        val bodySize = session.headers["content-length"]?.toIntOrNull() ?: return ""
        val buffer = ByteArray(bodySize)
        session.inputStream.read(buffer, 0, bodySize)
        return String(buffer)
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        Log.d("TAG", session.method.toString())
        if (session.method == Method.GET) {
            return try {
                when (uri) {
                    "/material-min.js" -> {
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "application/javascript",
                            context.assets.open("material-min.js").reader().readText()
                        )
                    }
                    "/jquery-min.js" -> {
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "application/javascript",
                            context.assets.open("jquery-min.js").reader().readText()
                        )
                    }
                    "/material.indigo-pink.min.css" -> {
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/css",
                            context.assets.open("material.indigo-pink.min.css").reader().readText()
                        )
                    }
                    else -> { // Default to serving index.html
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "text/html",
                            context.assets.open("index.html").reader().readText()
                        )
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    "Internal Server Error: ${e.message}"
                )
            }
        }
        else if (session.method == Method.POST && uri == "/req")
        {

            val requestBody = readRequestBody(session)
            Log.d("TAG", requestBody.toString())

            val jsonRequest: JsonRequest = Json.decodeFromString(requestBody)
            return when (jsonRequest.command) {
                "GetPublicKey" -> {
                    newFixedLengthResponse(OK, MIME_PLAINTEXT, executeGenKeyCallback())
                }
                "PutCertificate" -> {
                        newFixedLengthResponse(OK, MIME_PLAINTEXT, executeCertCallback(jsonRequest.data))
                }
                "UrlSubmit" -> {
                    newFixedLengthResponse(OK, MIME_PLAINTEXT, executeUrlSubmitCallback(jsonRequest.data))
                }
                else -> newFixedLengthResponse(OK, MIME_PLAINTEXT, "Invalid command")
            }
        }
        return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR,
            MIME_PLAINTEXT,
            "Internal Server Error"
        )
    }
}