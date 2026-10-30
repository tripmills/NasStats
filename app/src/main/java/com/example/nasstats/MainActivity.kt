package com.example.nasstats

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.Socket

data class TheRoom(val temp: Double, val humidity: Double)

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)

        val mutableUserList: MutableList<TheRoom> = mutableListOf()


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

                    val socket = Socket("nas", 5000)

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


    private fun updateUI(data: String) {
        val drives = data.split("|")

        val formatted = SpannableStringBuilder()

        for (drive in drives) {
            val parts = drive.split(":")
            var myColor = 0xFF1DFFEC.toInt()
            if (parts.size == 3) {
                when (parts[0]) {
                    "3rdfront" -> {
                        var start = formatted.length

                        formatted.append("\n3rd Front: ${parts[1]}°f \n")
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "3rd Front:".length+1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        
                        start = formatted.length
                        myColor = 0xFF1DFFEC.toInt()
                        formatted.append("Humidity: ${parts[2]}% \n")
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "Humidity:".length+1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                    }
                    "CPUTEMP" -> {
                        val start = formatted.length

                        formatted.append("\ncpuTemp: ${parts[1]}°f \n")
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "cpuTemp".length+1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    "TEMP" -> {
                        val start = formatted.length
                        formatted.append("Temp: ${parts[1]}°f  (Max: ${parts[2]}°f)\n")
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "Temp".length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    "HUMIDITY" -> {
                        val start = formatted.length
                        val text = "Humidity: ${parts[1]}\n"
                        formatted.append(text)
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "Humidity".length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    "AMBIENTTEMP" -> {
                        val start = formatted.length
                        formatted.append("\nRoom Temp: ${parts[1]}°f (Max: ${parts[2]}°f) \n")
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "Room Temp:".length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    "MBIENTHUMIDITY" -> {
                        val start = formatted.length
                        val text = "Room Humidity: ${parts[1]}\n"
                        formatted.append(text)
                        formatted.setSpan(
                            ForegroundColorSpan(myColor),
                            start,
                            start + "Room Humidity:".length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    else -> {
                        val start = formatted.length
                        val drive = parts[0]
                        formatted.append("$drive -> ${parts[1]} used, ${parts[2]} free\n")

                        formatted.setSpan(
                            ForegroundColorSpan(Color.YELLOW),
                            start,
                            start + drive.toString().length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            } else {
                formatted.append("${parts[0]} -> NOT MOUNTED\n")
            }
        }
        textView.text = formatted
    }

    override fun onDestroy() {
        super.onDestroy()
        keepRunning = false
    }

}