package io.budge.trackme.utils

import android.util.Log.e
import io.budge.trackme.data.Result
import io.budge.trackme.utils.Constants.AUTHORIZE_COMMAND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class SocketManager(private val responseCallbacks: ResponseCallbacks) {

    private var isConnectionOpen = false
    private var outWriter: PrintWriter? = null
    private var inReader: BufferedReader? = null

    suspend fun openConnection(ipAddress: String, port: Int) {
        isConnectionOpen = true
        e("openConnection", isConnectionOpen.toString())
        withContext(Dispatchers.IO) {
            while (isConnectionOpen) {
                try {
                    val socket = Socket(ipAddress, port)
                    try {
                        val outputStreamWriter =
                            OutputStreamWriter(socket.getOutputStream())
                        val inputStreamReader = InputStreamReader(socket.getInputStream())
                        outWriter = PrintWriter(BufferedWriter(outputStreamWriter), true)
                        inReader = BufferedReader(inputStreamReader)
                        sendMessage(AUTHORIZE_COMMAND)
                        while (isConnectionOpen) {
                            inReader?.let {
                                val response = it.readLine()
                            responseCallbacks.onResponse(Result.Success(response.trim()))
                            }
                        }
                    } catch (e: Exception) {
                        e("sendMessage", "2 $e")
                        socket.close()
                        responseCallbacks.onResponse(Result.Error(e))
                        continue
                    }
                    socket.close()
                    break
                } catch (e: Exception) {
                    e("sendMessage", "1 $e")
                    responseCallbacks.onResponse(Result.Error(e))
                }
            }
        }
    }


    fun closeConnection() {
        e("closeConnection", "close")
        isConnectionOpen = false
        outWriter?.apply {
            flush()
            close()
        }
        outWriter = null
        inReader = null
    }


    private fun sendMessage(message: String) {
        e("sendMessage", message)
        if (outWriter?.checkError() != false) return
        outWriter?.apply {
            println(message)
            flush()
        }
    }

    interface ResponseCallbacks {
        fun onResponse(result: Result<Any>)
    }
}