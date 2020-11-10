package io.budge.trackme.utils

import io.budge.trackme.data.Result
import io.budge.trackme.utils.Constants.AUTHORIZE_COMMAND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class SocketManager(private val onResponse: (Result<Any>) -> Unit) {

    private var isConnectionOpen = false
    private var outWriter: PrintWriter? = null
    private var inReader: BufferedReader? = null

    suspend fun openConnection(ipAddress: String, port: Int) {
        isConnectionOpen = true
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
                        readInputStream()
                    } catch (e: Exception) {
                        socket.close()
                        onResponse(Result.Error(e))
                        continue
                    }
                    break
                } catch (e: Exception) {
                    onResponse(Result.Error(e))
                }
            }
        }
    }

    fun readInputStream(){
        inReader?.let {
            val response = it.readLine()
            onResponse(Result.Success(response.trim()))
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


    private fun sendMessage(message: String) {
        if (outWriter?.checkError() != false) return
        outWriter?.apply {
            println(message)
            flush()
        }
    }
}