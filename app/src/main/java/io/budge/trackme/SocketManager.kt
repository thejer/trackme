package io.budge.trackme

import android.util.Log.e
import io.budge.trackme.data.Result
import java.io.*
import java.net.Socket

class SocketManager(private val responseCallbacks: ResponseCallbacks) {

    private var isConnectionOpen = false
    private var outWriter: PrintWriter? = null
    private var inReader: BufferedReader? = null

    fun openConnection(ipAddress: String, port: Int, emailAddress: String) {
        isConnectionOpen = true
        e("openConnection", isConnectionOpen.toString())
        while (isConnectionOpen) {
            try {
                val socket = Socket(ipAddress, port)
                try {
                    val outputStreamWriter =
                        OutputStreamWriter(socket.getOutputStream())
                    val inputStreamReader = InputStreamReader(socket.getInputStream())
                    outWriter = PrintWriter(BufferedWriter(outputStreamWriter), true)
                    inReader = BufferedReader(inputStreamReader)
                    authenticate(emailAddress)
                    while (isConnectionOpen) {
                        e("sendMessage", "message")
                        inReader?.let {
                            val response = it.readLine()
                            e("sendMessage", response)
//                            responseCallbacks.onResponse(Result.Success(response.trim()))
                        }
                    }
                } catch (e: Exception) {
                    socket.close()
                    responseCallbacks.onResponse(Result.Error(e))
                    continue
                }
                socket.close()
                break
            } catch (e: Exception) {
                responseCallbacks.onResponse(Result.Error(e))
            }
        }
    }


    fun closeConnection() {
        isConnectionOpen = false
        outWriter?.apply {
            flush()
            close()
        }
        outWriter = null
        inReader = null
    }

    private fun authenticate(emailAddress: String) {
        sendMessage("AUTHORIZE $emailAddress")
    }

    private fun sendMessage(message: String) {
        e("sendMessage", message)
        if (outWriter?.checkError() != false) return
        outWriter?.apply {
            kotlin.io.println(message)
            flush()
        }
    }

    interface ResponseCallbacks {
        fun onResponse(result: Result<Any>)
    }
}