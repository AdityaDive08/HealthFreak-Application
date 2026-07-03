package com.aivoiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class FirstAidActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_aid)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        tts = TextToSpeech(this, this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        txtResult = findViewById(R.id.txtResult)

        val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        fun updateHelp(text: String) {
            txtResult.text = text
            txtResult.startAnimation(anim)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        findViewById<Button>(R.id.btnCut).setOnClickListener {
            updateHelp("Rinse the wound with clean water, apply antiseptic, and bandage.")
        }
        findViewById<Button>(R.id.btnBurn).setOnClickListener {
            updateHelp("Cool the burn under running water for twenty minutes.")
        }
        findViewById<Button>(R.id.btnHeadache).setOnClickListener {
            updateHelp("Drink water and rest in a dark room. Avoid stress.")
        }
        findViewById<Button>(R.id.btnSprain).setOnClickListener {
            updateHelp("Use Rest, Ice, Compression, Elevation.")
        }
        findViewById<Button>(R.id.btnNosebleed).setOnClickListener {
            updateHelp("Lean forward and pinch soft part of nose for ten minutes.")
        }

        // Emergency Button Calls
        findViewById<Button>(R.id.btnAmbulance).setOnClickListener { callEmergency("108") }
        findViewById<Button>(R.id.btnPolice).setOnClickListener { callEmergency("100") }
        findViewById<Button>(R.id.btnFire).setOnClickListener { callEmergency("101") }

        // SOS Send GPS Location
        findViewById<Button>(R.id.btnSOS).setOnClickListener {
            getLocationAndSendSOS()
        }
    }

    private fun callEmergency(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }

    private fun getLocationAndSendSOS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 200)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val message = "🚨 SOS!! I need help!! My Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"

                val phone = "YOUR_EMERGENCY_NUMBER" // <- Add family number here
                val sms = SmsManager.getDefault()
                sms.sendTextMessage(phone, null, message, null, null)

                Toast.makeText(this, "SOS Sent!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Unable to fetch location!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(1.0f)
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
