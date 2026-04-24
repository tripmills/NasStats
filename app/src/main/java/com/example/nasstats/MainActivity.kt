package com.example.nasstats

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        //button = findViewById(R.id.sendButton)

       // button.setOnClickListener {
      //      sendMessage("Hello from Android")
       // }

        startListening()
    }

    @Volatile
    private var keepRunning = true

    private fun startListening() {
        Thread {
            while (keepRunning) {
                try {
                    runOnUiThread {
                        textView.text = "Connecting..."
                    }

                    val socket = Socket("nas", 12345)

                    runOnUiThread {
                        textView.text = "Connected"
                    }

                    val reader = BufferedReader(
                        InputStreamReader(socket.getInputStream())
                    )

                    // Read until connection drops
                    while (keepRunning) {
                        val line = reader.readLine() ?: break

                        runOnUiThread {
                            updateUI(line)
                        }
                    }

                    socket.close()

                    runOnUiThread {
                        textView.text = "Disconnected"
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        textView.text = "Connection failed... retrying"
                    }
                }

                // wait before retrying
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.start()
    }

/*
    private fun startListening() {
        Thread {
            try {
                val socket = Socket("nas", 12345)

                val reader = BufferedReader(
                    InputStreamReader(socket.getInputStream())
                )

                while (true) {
                    val line = reader.readLine() ?: break

                    runOnUiThread {
                        updateUI(line)
                    }
                }

                socket.close()

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    textView.text = "Disconnected: ${e.message}"
                }
            }
        }.start()
    }

 */

    private fun updateUI(data: String) {
        val drives = data.split("|")

        val formatted = StringBuilder()

        for (drive in drives) {
            val parts = drive.split(":")

            if (parts.size == 3) {
                formatted.append("${parts[0]} -> ${parts[1]} used, ${parts[2]} free\n")
            } else {
                formatted.append("${parts[0]} -> NOT MOUNTED\n")
            }
        }

        textView.text = formatted.toString()
    }
/*
    private fun sendMessage(message: String) {
        Thread {
            try {
                val socket = Socket("nas", 12345)

                val writer = BufferedWriter(
                    OutputStreamWriter(socket.getOutputStream())
                )
                val reader = BufferedReader(
                    InputStreamReader(socket.getInputStream())
                )

                // Send message
                writer.write(message)
                writer.newLine()
                writer.flush()

                // Receive response
                val response = reader.readLine()

                runOnUiThread {
                    textView.text = "Server says: $response"
                }

                socket.close()

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    textView.text = "Error: ${e.message}"
                }
            }
        }.start()
    }
    */

    override fun onDestroy() {
        super.onDestroy()
        keepRunning = false
    }

}