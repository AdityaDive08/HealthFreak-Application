package com.aivoiceassistant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Buttons
        val btnConsult = findViewById<Button>(R.id.btnConsult)
        val btnHealthTracker = findViewById<Button>(R.id.btnHealthTracker)
        val btnSOS = findViewById<Button>(R.id.btnSOS)
        val btnHospitals = findViewById<Button>(R.id.btnHospitals)
        val btnFirstAid = findViewById<Button>(R.id.btnFirstAid)
        val btnAmbulance = findViewById<Button>(R.id.btnAmbulance)
        val imgProfile = findViewById<android.widget.ImageView>(R.id.imgProfile)

        // ==== PROFILE BUTTON ====
        imgProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        // ==== CONSULT BUTTON ====
        btnConsult.setOnClickListener {
            startActivity(Intent(this, ConsultActivity::class.java))
        }


        // ==== HEALTH TRACKER BUTTON ====
        btnHealthTracker.setOnClickListener {
            startActivity(Intent(this, HealthTrackerActivity::class.java))
        }

        // ==== SOS BUTTON (IMPORTANT) ====
        btnSOS.setOnClickListener {
            startActivity(Intent(this, SosActivity::class.java))
        }

        // ==== HOSPITALS BUTTON ====
        btnHospitals.setOnClickListener {
            Toast.makeText(this, "Nearby Hospitals Feature Coming Soon", Toast.LENGTH_SHORT).show()
        }

        // ==== FIRST AID BUTTON ====
        btnFirstAid.setOnClickListener {
            startActivity(Intent(this, FirstAidActivity::class.java))
        }


        // ==== AMBULANCE BUTTON ====
        btnAmbulance.setOnClickListener {
            startActivity(Intent(this, AmbulanceActivity::class.java))
        }

    }

    override fun onResume() {
        super.onResume()
        SessionManager.init(applicationContext)
        val imgProfile = findViewById<android.widget.ImageView>(R.id.imgProfile)
        val existingDp = SessionManager.getProfilePicture()
        if (existingDp != null) {
            try {
                val imageBytes = android.util.Base64.decode(existingDp, android.util.Base64.DEFAULT)
                val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imgProfile.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            // Default empty / grey background if empty
            imgProfile.setImageResource(0) // clear image
        }
    }
}
