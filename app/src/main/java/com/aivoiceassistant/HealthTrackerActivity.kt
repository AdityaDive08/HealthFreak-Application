package com.aivoiceassistant

import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aivoiceassistant.data.api.ApiClient
import com.aivoiceassistant.data.api.HealthRecordRequest
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.*

class HealthTrackerActivity : AppCompatActivity() {

    private lateinit var edtBP: EditText
    private lateinit var edtSugar: EditText
    private lateinit var edtHR: EditText
    private lateinit var txtBP: TextView
    private lateinit var txtSugar: TextView
    private lateinit var txtHR: TextView
    private lateinit var btnAddEntry: Button
    private lateinit var chartBP: LineChart
    private lateinit var chartSugar: LineChart
    private lateinit var chartHR: LineChart

    private val bpList    = arrayListOf<Entry>()
    private val sugarList = arrayListOf<Entry>()
    private val hrList    = arrayListOf<Entry>()
    private val dateLabels = arrayListOf<String>()

    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val userId  get() = SessionManager.userId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_tracker)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        SessionManager.init(applicationContext)

        edtBP    = findViewById(R.id.edtBP)
        edtSugar = findViewById(R.id.edtSugar)
        edtHR    = findViewById(R.id.edtHR)
        txtBP    = findViewById(R.id.txtBP)
        txtSugar = findViewById(R.id.txtSugar)
        txtHR    = findViewById(R.id.txtHR)
        btnAddEntry = findViewById(R.id.btnAddEntry)
        chartBP = findViewById(R.id.chartBP)
        chartSugar = findViewById(R.id.chartSugar)
        chartHR = findViewById(R.id.chartHR)

        // Blood Pressure Limits (Normal: 90 - 119)
        setupIndividualChart(chartBP, listOf(
            LimitLine(120f, "High (\u2265120)").apply {
                lineWidth = 1f
                lineColor = Color.parseColor("#EF4444") // Red
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                textSize = 10f
                textColor = Color.parseColor("#EF4444")
            },
            LimitLine(90f, "Low (<90)").apply {
                lineWidth = 1f
                lineColor = Color.parseColor("#EAB308") // Yellow
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                textSize = 10f
                textColor = Color.parseColor("#EAB308")
            }
        ))
        
        // Blood Sugar Limits (Normal: 70 - 99)
        setupIndividualChart(chartSugar, listOf(
            LimitLine(100f, "High (\u2265100)").apply {
                lineWidth = 1f
                lineColor = Color.parseColor("#EF4444") // Red
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                textSize = 10f
                textColor = Color.parseColor("#EF4444")
            },
            LimitLine(70f, "Low (<70)").apply {
                lineWidth = 1f
                lineColor = Color.parseColor("#EAB308") // Yellow
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                textSize = 10f
                textColor = Color.parseColor("#EAB308")
            }
        ))
        
        // Heart Rate Limits (Normal: 60 - 100)
        setupIndividualChart(chartHR, listOf(
            LimitLine(100f, "High (>100)").apply {
                lineWidth = 1f
                lineColor = Color.parseColor("#EF4444") // Red
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                textSize = 10f
                textColor = Color.parseColor("#EF4444")
            },
            LimitLine(60f, "Low (<60)").apply {
                lineWidth = 1f
                lineColor = Color.parseColor("#EAB308") // Yellow
                enableDashedLine(10f, 10f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                textSize = 10f
                textColor = Color.parseColor("#EAB308")
            }
        ))
        loadHistoryFromDB()     // Load previous readings on start

        btnAddEntry.setOnClickListener {
            val bp    = edtBP.text.toString().trim()
            val sugar = edtSugar.text.toString().trim()
            val hr    = edtHR.text.toString().trim()

            if (bp.isEmpty() || sugar.isEmpty() || hr.isEmpty()) {
                Toast.makeText(this, "Please enter all values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bpVal    = bp.toInt()
            val sugarVal = sugar.toInt()
            val hrVal    = hr.toInt()

            updateStatusLabels(bpVal, sugarVal, hrVal)
            addToChart(bpVal, sugarVal, hrVal)
            saveRecordToDB(bpVal, sugarVal, hrVal)

            edtBP.text.clear()
            edtSugar.text.clear()
            edtHR.text.clear()
        }
    }

    // ── Load history from DB on startup ────────────────────────────────────
    private fun loadHistoryFromDB() {
        if (userId <= 0) return
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.phpApi.getHealthRecords(userId)
                }
                if (response.success && response.records.isNotEmpty()) {
                    bpList.clear(); sugarList.clear(); hrList.clear(); dateLabels.clear()

                    // Show history timeline
                    val parsedDates = response.records.map { parseDateString(it.recorded_at) }
                    val formatterDay = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                    val formatterTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

                    val dayOccurrences = mutableMapOf<String, Int>()
                    parsedDates.forEach { date ->
                        if (date != null) {
                            val dayStr = formatterDay.format(date)
                            dayOccurrences[dayStr] = dayOccurrences.getOrDefault(dayStr, 0) + 1
                        }
                    }

                    response.records.forEachIndexed { index, record ->
                        bpList.add(Entry(index.toFloat(), record.blood_pressure.toFloat()))
                        sugarList.add(Entry(index.toFloat(), record.blood_sugar.toFloat()))
                        hrList.add(Entry(index.toFloat(), record.heart_rate.toFloat()))
                        
                        val date = parsedDates[index]
                        var labelStr = record.recorded_at.take(10)
                        if (date != null) {
                            val dayStr = formatterDay.format(date)
                            labelStr = if ((dayOccurrences[dayStr] ?: 0) > 1) {
                                "$dayStr ${formatterTime.format(date)}"
                            } else {
                                dayStr
                            }
                        }
                        dateLabels.add(labelStr)
                    }

                    // Show last entry in labels
                    val last = response.records.last()
                    updateStatusLabels(last.blood_pressure, last.blood_sugar, last.heart_rate)
                    updateChart()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HealthTrackerActivity,
                    "Could not load history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Save new record to DB ───────────────────────────────────────────────
    private fun saveRecordToDB(bp: Int, sugar: Int, hr: Int) {
        if (userId <= 0) {
            Toast.makeText(this, "Not logged in. Data shown locally only.", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.phpApi.saveHealthRecord(
                        HealthRecordRequest(userId, bp, sugar, hr)
                    )
                }
                // Silent success — data already shown in UI
            } catch (e: Exception) {
                Toast.makeText(this@HealthTrackerActivity,
                    "Saved locally. Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── UI helpers ──────────────────────────────────────────────────────────
    private fun updateStatusLabels(bp: Int, sugar: Int, hr: Int) {
        when {
            bp < 90 -> {
                txtBP.text = "Blood Pressure: $bp (Low)"
                txtBP.setTextColor(Color.parseColor("#EAB308"))
            }
            bp >= 120 -> {
                txtBP.text = "Blood Pressure: $bp (High)"
                txtBP.setTextColor(Color.parseColor("#DC2626"))
            }
            else -> {
                txtBP.text = "Blood Pressure: $bp (Normal)"
                txtBP.setTextColor(Color.parseColor("#059669"))
            }
        }
        when {
            sugar < 70 -> {
                txtSugar.text = "Sugar: $sugar (Low)"
                txtSugar.setTextColor(Color.parseColor("#EAB308"))
            }
            sugar >= 100 -> {
                txtSugar.text = "Sugar: $sugar (High)"
                txtSugar.setTextColor(Color.parseColor("#DC2626"))
            }
            else -> {
                txtSugar.text = "Sugar: $sugar (Normal)"
                txtSugar.setTextColor(Color.parseColor("#059669"))
            }
        }
        when {
            hr < 60 -> {
                txtHR.text = "Heart Rate: $hr (Low)"
                txtHR.setTextColor(Color.parseColor("#EAB308"))
            }
            hr > 100 -> {
                txtHR.text = "Heart Rate: $hr (High)"
                txtHR.setTextColor(Color.parseColor("#DC2626"))
            }
            else -> {
                txtHR.text = "Heart Rate: $hr (Normal)"
                txtHR.setTextColor(Color.parseColor("#059669"))
            }
        }
    }

    private fun addToChart(bp: Int, sugar: Int, hr: Int) {
        val index = bpList.size.toFloat()
        bpList.add(Entry(index, bp.toFloat()))
        sugarList.add(Entry(index, sugar.toFloat()))
        hrList.add(Entry(index, hr.toFloat()))
        
        val formatter = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
        dateLabels.add(formatter.format(java.util.Date()))
        
        updateChart()
    }

    private fun setupIndividualChart(chart: LineChart, limitLines: List<LimitLine>) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.axisRight.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.extraBottomOffset = 45f // Increased to accommodate rotated labels
        
        // Style X Axis
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = Color.parseColor("#6B7280")
            labelRotationAngle = -45f // Rotate labels so date/time fits better
            textSize = 9f
            yOffset = 5f
        }
        
        // Style Y Axis
        chart.axisLeft.apply {
            setDrawGridLines(true)
            gridColor = Color.parseColor("#F3F4F6") // Very light gray grid
            textColor = Color.parseColor("#6B7280")
            removeAllLimitLines()
            limitLines.forEach { addLimitLine(it) }
            
            // Add some padding to Y axis so limit lines don't hit the very top/bottom
            spaceTop = 20f
            spaceBottom = 20f
        }

        // Hide Legend for a cleaner, classic look
        chart.legend.isEnabled = false
    }

    private fun updateChart() {
        if (bpList.isEmpty() || sugarList.isEmpty() || hrList.isEmpty()) return

        chartBP.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
        chartSugar.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
        chartHR.xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)

        // Evaluate BP
        val bpCircleColors = bpList.map { 
            if (it.y < 90f) Color.parseColor("#EAB308") 
            else if (it.y >= 120f) Color.parseColor("#EF4444") 
            else Color.parseColor("#10B981")
        }
        val lastBp = bpList.last().y
        val bpColorStr = if (lastBp < 90f) "#EAB308" else if (lastBp >= 120f) "#EF4444" else "#10B981"
        val bpFillStr  = if (lastBp < 90f) "#FEF08A" else if (lastBp >= 120f) "#FCA5A5" else "#D1FAE5"
        
        val bpSet = LineDataSet(bpList, "BP").apply {
            color = Color.parseColor(bpColorStr)
            circleColors = bpCircleColors
            lineWidth = 3f
            circleRadius = 5f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor(bpFillStr)
            fillAlpha = 60
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        chartBP.data = LineData(bpSet)
        chartBP.animateX(1000)
        chartBP.invalidate()

        // Evaluate Sugar
        val sugarCircleColors = sugarList.map { 
            if (it.y < 70f) Color.parseColor("#EAB308") 
            else if (it.y >= 100f) Color.parseColor("#EF4444") 
            else Color.parseColor("#10B981")
        }
        val lastSugar = sugarList.last().y
        val sugarColorStr = if (lastSugar < 70f) "#EAB308" else if (lastSugar >= 100f) "#EF4444" else "#10B981"
        val sugarFillStr  = if (lastSugar < 70f) "#FEF08A" else if (lastSugar >= 100f) "#FCA5A5" else "#D1FAE5"

        val sugarSet = LineDataSet(sugarList, "Sugar").apply {
            color = Color.parseColor(sugarColorStr)
            circleColors = sugarCircleColors
            lineWidth = 3f
            circleRadius = 5f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor(sugarFillStr)
            fillAlpha = 60
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        chartSugar.data = LineData(sugarSet)
        chartSugar.animateX(1000)
        chartSugar.invalidate()

        // Evaluate Heart Rate
        val hrCircleColors = hrList.map { 
            if (it.y < 60f) Color.parseColor("#EAB308") 
            else if (it.y > 100f) Color.parseColor("#EF4444") 
            else Color.parseColor("#10B981")
        }
        val lastHr = hrList.last().y
        val hrColorStr = if (lastHr < 60f) "#EAB308" else if (lastHr > 100f) "#EF4444" else "#10B981"
        val hrFillStr  = if (lastHr < 60f) "#FEF08A" else if (lastHr > 100f) "#FCA5A5" else "#D1FAE5"

        val hrSet = LineDataSet(hrList, "Heart Rate").apply {
            color = Color.parseColor(hrColorStr)
            circleColors = hrCircleColors
            lineWidth = 3f
            circleRadius = 5f
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.parseColor(hrFillStr)
            fillAlpha = 60
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        chartHR.data = LineData(hrSet)
        chartHR.animateX(1000)
        chartHR.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun parseDateString(dateStr: String): java.util.Date? {
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
                if (format.endsWith("'Z'")) {
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(dateStr)
                if (parsed != null) return parsed
            } catch (e: Exception) { }
        }
        return null
    }
}
