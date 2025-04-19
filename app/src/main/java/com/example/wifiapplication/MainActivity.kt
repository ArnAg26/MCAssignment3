package com.example.wifiapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var tvSummary: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnSummary: Button
    private lateinit var btnSave: Button
    private lateinit var radioGroup: RadioGroup

    private var currentLocation = "Location A"
    private val dataMap = mutableMapOf(
        "Location A" to mutableListOf<Int>(),
        "Location B" to mutableListOf<Int>(),
        "Location C" to mutableListOf<Int>()
    )

    private var scanReceiver: BroadcastReceiver? = null
    private var isLogging = false
    private val scanIntervalMillis = 5000L
    private val handler = Handler()

    companion object {
        private const val LOCATION_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        tvSummary = findViewById(R.id.tvSummary)
        btnStart = findViewById(R.id.btnStartLogging)
        btnStop = findViewById(R.id.btnStopLogging)
        btnSummary = findViewById(R.id.btnShowSummary)
        btnSave = findViewById(R.id.btnSaveToCSV)
        radioGroup = findViewById(R.id.radioGroupLocation)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentLocation = when (checkedId) {
                R.id.radioLocationA -> "Location A"
                R.id.radioLocationB -> "Location B"
                R.id.radioLocationC -> "Location C"
                else -> "Location A"
            }
            Toast.makeText(this, "Selected $currentLocation", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener { startLogging() }
        btnStop.setOnClickListener { stopLogging() }
        btnSummary.setOnClickListener { loadDataAndShowSummary() }
        btnSave.setOnClickListener { saveMatrixToCSV() }

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission required for WiFi scanning", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun scheduleNextScan() {
        handler.postDelayed({
            if (isLogging) {
                wifiManager.startScan()
            }
        }, scanIntervalMillis)
    }

    private fun startLogging() {
        if (isLogging) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Please grant location permission", Toast.LENGTH_SHORT).show()
            requestLocationPermission()
            return
        }

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please enable Location Services", Toast.LENGTH_LONG).show()
            return
        }

        isLogging = true

        scanReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                val results: List<ScanResult> = wifiManager.scanResults
                val strongestSignal = results.maxByOrNull { it.level }?.level
                Log.d("Values received", strongestSignal.toString())

                if (strongestSignal != null) {
                    val list = dataMap[currentLocation] ?: mutableListOf()
                    Log.d("Values received ${list.size}", list.toString())
                    if (list.size < 100) {
                        list.add(strongestSignal)
                        dataMap[currentLocation] = list
                        tvSummary.text = "Logging $currentLocation: ${list.size} / 100"
                        scheduleNextScan()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "$currentLocation already has 100 samples",
                            Toast.LENGTH_SHORT
                        ).show()
                        stopLogging()
                    }
                }
            }
        }

        registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    private fun stopLogging() {
        if (scanReceiver != null) {
            unregisterReceiver(scanReceiver)
            scanReceiver = null
        }
        handler.removeCallbacksAndMessages(null)
        isLogging = false
        Toast.makeText(this, "Stopped logging", Toast.LENGTH_SHORT).show()
    }

    private fun calculateStdDev(list: List<Int>, mean: Double): Double {
        val variance = list.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    private fun getRssiDistribution(list: List<Int>): Map<String, Int> {
        val distribution = mutableMapOf(
            "📶 Excellent (-40 to -50)" to 0,
            "👍 Good (-51 to -60)" to 0,
            "😐 Fair (-61 to -70)" to 0,
            "⚠️ Weak (< -70)" to 0
        )
        for (rssi in list) {
            when {
                rssi >= -50 -> distribution["📶 Excellent (-40 to -50)"] = distribution["📶 Excellent (-40 to -50)"]!! + 1
                rssi in -60..-51 -> distribution["👍 Good (-51 to -60)"] = distribution["👍 Good (-51 to -60)"]!! + 1
                rssi in -70..-61 -> distribution["😐 Fair (-61 to -70)"] = distribution["😐 Fair (-61 to -70)"]!! + 1
                else -> distribution["⚠️ Weak (< -70)"] = distribution["⚠️ Weak (< -70)"]!! + 1
            }
        }
        return distribution
    }

    private fun saveMatrixToCSV() {
        val locA = dataMap["Location A"] ?: emptyList()
        val locB = dataMap["Location B"] ?: emptyList()
        val locC = dataMap["Location C"] ?: emptyList()

        if (locA.size < 100 || locB.size < 100 || locC.size < 100) {
            Toast.makeText(this, "Collect 100 samples at all locations before saving.", Toast.LENGTH_LONG).show()
            return
        }

        val file = File(filesDir, "rss_matrix.csv")
        try {
            file.printWriter().use { out ->
                out.println("Index,Location A,Location B,Location C")
                for (i in 0 until 100) {
                    out.println("${i + 1},${locA[i]},${locB[i]},${locC[i]}")
                }
            }
            Toast.makeText(this, "Saved to rss_matrix.csv", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save matrix", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDataAndShowSummary() {
        val file = File(filesDir, "rss_matrix.csv")
        if (!file.exists()) {
            Toast.makeText(this, "No matrix file found", Toast.LENGTH_SHORT).show()
            return
        }

        val locA = mutableListOf<Int>()
        val locB = mutableListOf<Int>()
        val locC = mutableListOf<Int>()

        try {
            val reader = file.bufferedReader()
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size == 4) {
                    locA.add(parts[1].toInt())
                    locB.add(parts[2].toInt())
                    locC.add(parts[3].toInt())
                }
            }

            val sb = StringBuilder()
            listOf("Location A" to locA, "Location B" to locB, "Location C" to locC).forEach { (name, list) ->
                if (list.isNotEmpty()) {
                    val min = list.minOrNull() ?: 0
                    val max = list.maxOrNull() ?: 0
                    val avg = list.average()
                    val stdDev = calculateStdDev(list, avg)

                    sb.append("📍 $name\n")
                    sb.append("Min: $min dBm, Max: $max dBm\n")
                    sb.append("Average: %.2f dBm\n".format(avg))
                    sb.append("Std Dev: %.2f\n".format(stdDev))
                    sb.append("Signal Strength Distribution:\n")
                    val distribution = getRssiDistribution(list)
                    distribution.forEach { (range, count) ->
                        sb.append("$range: $count times\n")
                    }
                    sb.append("--------------\n")
                } else {
                    sb.append("📍 $name\nNo data\n--------------\n")
                }
            }

            tvSummary.text = sb.toString()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read matrix", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLogging()
    }
}
