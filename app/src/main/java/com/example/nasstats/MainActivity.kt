package com.example.nasstats

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.*
import java.net.Socket
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TreeMap



data class TheRoom(val temp: Double, val humidity: Double)
// Maps "SensorName" -> TextView instance
private val sensorViews = TreeMap<String, TextView>()
// Tracks the pending removal runnable for each sensor
private val sensorTimeoutRunnables = HashMap<String, Runnable>()
private val timeoutHandler = Handler(Looper.getMainLooper())

// Adjust this to how long a sensor can go silent before being removed (e.g., 10 seconds)
private val SENSOR_TIMEOUT_MS = 10_000L

class SensorViewModel : ViewModel() {
    // Survives rotation: Maps "SensorName" -> Latest formatted CharSequence
    //val sensorDataMap = LinkedHashMap<String, CharSequence>()
    val sensorDataMap = TreeMap<String, CharSequence>()
}


class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private val viewModel: SensorViewModel by viewModels()
    private val sensorViews = TreeMap<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)

        for ((sensorName, savedText) in viewModel.sensorDataMap) {
            updateOrCreateSensorView(sensorName, savedText)
        }
        //val mutableUserList: MutableList<TheRoom> = mutableListOf()
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

                    val socket = Socket("nas", 5010)

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
                    Thread.sleep(5000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.start()
    }

    private fun sendMessage(clientSocket: Socket, message: String) {
        try {
            val writer = PrintWriter(clientSocket.getOutputStream(), true)
            writer.println(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun String.reformatDate(
        fromPattern: String = "yyyy-MM-dd HH:mm:ss", // Adjust to match your incoming string
//        toPattern: String = "MMM dd, h:mm a"
        toPattern: String = "MM-dd-yy h:mm a"
    ): String {
        return try {
            val input = SimpleDateFormat(fromPattern, Locale.getDefault())
            val output = SimpleDateFormat(toPattern, Locale.getDefault())
            val date = input.parse(this)
            if (date != null) output.format(date) else this
        } catch (e: Exception) {
            this // Returns the original string safely if parsing fails
        }
    }

    fun SpannableStringBuilder.appendColored(text: String, color: Int): SpannableStringBuilder {
        val start = length
        append(text)
        setSpan(
            ForegroundColorSpan(color),
            start,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }

    fun handleServerMessage(rawMessage: String) {
        // 1. Split the pipe-delimited string
        val parts = rawMessage.trim().split("|")
        Log.d("incomingstring", "Incoming message parts size: $parts.size - $parts[0] ")

        // 2. Validate that it's a 'temp' message and has enough data
        if (parts.size >= 3 && parts[0] == "TEMP") {
            val roomname = parts.getOrNull(1) ?: "error"
            val roomtemp = parts.getOrNull(2) ?: "error"
            val roomhum = parts.getOrNull(3) ?: "error"
            val maxtempdate = parts.getOrNull(4) ?: "error"
            val maxtemp = parts.getOrNull(5) ?: "error"
            val mintempdate = parts.getOrNull(6) ?: "error"
            val mintemp = parts.getOrNull(7) ?: "error"
            val maxhumdate = parts.getOrNull(8) ?: "error"
            val maxhum = parts.getOrNull(9) ?: "error"
            val minhumdate = parts.getOrNull(10) ?: "error"
            val minhum = parts.getOrNull(11) ?: "error"
            val formatted = SpannableStringBuilder()

            formatted.appendColored("${roomname}\n", ContextCompat.getColor(this, R.color.gold))
            formatted.appendColored("${roomtemp}°F",ContextCompat.getColor(this, R.color.lightblue))
            formatted.append(" - Hum ")
            formatted.appendColored("${roomhum}%\n\n",ContextCompat.getColor(this, R.color.purple))
            formatted.append("Max: ")
            formatted.appendColored("${maxtemp}°F", ContextCompat.getColor(this, R.color.lightblue))
            formatted.append(" on ${maxtempdate.reformatDate()}\n")
            formatted.append("Min: ")
            formatted.appendColored("${mintemp}°F", ContextCompat.getColor(this, R.color.lightblue))
            formatted.append(" on ${mintempdate.reformatDate()}\n")
            formatted.append("Max: ")
            formatted.appendColored("${maxhum}%", ContextCompat.getColor(this, R.color.purple))
            formatted.append(" on ${maxhumdate.reformatDate()}\n")
            formatted.append("Min: ")
            formatted.appendColored("${minhum}%", ContextCompat.getColor(this, R.color.purple))
            formatted.append(" on ${minhumdate.reformatDate()}\n")

            runOnUiThread {
                updateOrCreateSensorView(roomname, formatted)
            }
        }
    }

    fun createSensorBorderDrawable(
        borderColor: Int = Color.WHITE,
        borderWidthDp: Int = 2,
        cornerRadiusDp: Float = 8f
    ): GradientDrawable {
        val density = resources.displayMetrics.density
        val strokeWidthPx = (borderWidthDp * density).toInt()
        val cornerRadiusPx = cornerRadiusDp * density

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.BLACK) // Keeps your existing black background
            setStroke(strokeWidthPx, borderColor) // Adds the border line
            cornerRadius = cornerRadiusPx // Optional: set to 0f for sharp square corners
        }
    }

    private fun updateOrCreateSensorView(sensorName: String, displayText: CharSequence) {
        // 1. Save the latest formatted text so it survives rotation
        viewModel.sensorDataMap[sensorName] = displayText

        val listContainer = findViewById<LinearLayout>(R.id.sensor_list_container)
        val statusTextView = findViewById<TextView>(R.id.textView)

        if (statusTextView.visibility == View.VISIBLE) {
            statusTextView.visibility = View.GONE
        }

        val existingTextView = sensorViews[sensorName]

        if (existingTextView != null) {
            existingTextView.text = displayText
        } else {
            val paddingPx = (12 * resources.displayMetrics.density).toInt()

            val newTextView = TextView(this).apply {
                text = displayText
                textSize = 18f
                setTextColor(Color.WHITE)

                background = createSensorBorderDrawable(
                    borderColor = Color.WHITE,
                    borderWidthDp = 2,
                    cornerRadiusDp = 8f
                )

                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(20, 0, 0, 20)
                }

                setOnClickListener {
                   // Toast.makeText(this@MainActivity, "$sensorName clicked", Toast.LENGTH_SHORT).show()
                resethighs(sensorName)
                }
               // resethighs()
            }

            listContainer.addView(newTextView)
            sensorViews[sensorName] = newTextView
        }
        resetSensorTimeout(sensorName)
    }

    private fun resetSensorTimeout(sensorName: String) {
        // Cancel the previous timer for this sensor if it exists
        sensorTimeoutRunnables[sensorName]?.let { existingRunnable ->
            timeoutHandler.removeCallbacks(existingRunnable)
        }

        // Create a new timer that removes the sensor if it expires
        val timeoutRunnable = Runnable {
            removeInactiveSensor(sensorName)
        }

        sensorTimeoutRunnables[sensorName] = timeoutRunnable
        timeoutHandler.postDelayed(timeoutRunnable, SENSOR_TIMEOUT_MS)
    }

    private fun removeInactiveSensor(sensorName: String) {
        val listContainer = findViewById<LinearLayout>(R.id.sensor_list_container)
        val statusTextView = findViewById<TextView>(R.id.textView)

        // Remove the TextView from the screen
        sensorViews[sensorName]?.let { viewToRemove ->
            listContainer.removeView(viewToRemove)
        }

        // Clean up all references so it doesn't reappear on rotation
        sensorViews.remove(sensorName)
        viewModel.sensorDataMap.remove(sensorName)
        sensorTimeoutRunnables.remove(sensorName)

        // If all sensors timed out, bring back the default status TextView
        if (sensorViews.isEmpty()) {
            statusTextView.visibility = View.VISIBLE
        }
    }


    private fun resethighs(thename:String = "") {
        ConfirmDialogs.showClearHighsDialog(this,"$thename") {
            Thread {
                try {
                    Socket("nas", 5011).use { clientSocket ->
                        sendMessage(clientSocket, "CLEAR|$thename")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun updateUI(data: String) {
        handleServerMessage(data.toString())
        textView.text = "Loading..."
    }

    override fun onDestroy() {
        super.onDestroy()
        keepRunning = false
    }

}