package com.aivoiceassistant

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

class UserProfileActivity : AppCompatActivity() {

    private lateinit var imgProfilePreview: ImageView
    private var photoUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { processImage(it) }
        } else {
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val txtUserName = findViewById<TextView>(R.id.txtUserName)
        val txtUserEmail = findViewById<TextView>(R.id.txtUserEmail)
        val btnEditProfileImg = findViewById<ImageView>(R.id.btnEditProfileImg)
        imgProfilePreview = findViewById(R.id.imgProfilePreview)

        // Ensure SessionManager is initialized
        SessionManager.init(applicationContext)

        // Populate User Data
        txtUserName.text = SessionManager.userName
        txtUserEmail.text = SessionManager.userEmail

        // Load existing profile picture
        val existingDp = SessionManager.getProfilePicture()
        if (existingDp != null) {
            try {
                val imageBytes = Base64.decode(existingDp, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imgProfilePreview.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                // Ignore if decode fails
            }
        }

        btnEditProfileImg.setOnClickListener {
            showImagePickDialog()
        }
        
        imgProfilePreview.setOnClickListener {
            val dp = SessionManager.getProfilePicture()
            if (dp != null) {
                viewProfileImage(dp)
            } else {
                Toast.makeText(this, "No profile picture set", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnLogout.setOnClickListener {
            SessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showImagePickDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Profile Picture")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> launchCamera()
                1 -> pickImage.launch("image/*")
                2 -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun launchCamera() {
        try {
            val photoFile = File(cacheDir, "camera_image_${UUID.randomUUID()}.jpg")
            photoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", photoFile)
            takePicture.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processImage(uri: Uri) {
        try {
            // First pass: get the bitmap
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Second pass: correct orientation based on EXIF
            inputStream = contentResolver.openInputStream(uri)
            var bitmap = originalBitmap
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matrix = android.graphics.Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                        matrix.preScale(1.0f, -1.0f)
                        matrix.postRotate(180f)
                    }
                }
                
                // Extra safety: If someone takes a selfie and wants it mirrored on Android, 
                // the camera hardware flips it. But usually EXIF handles what's needed.
                if (!matrix.isIdentity) {
                    bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                }
                inputStream.close()
            }

            imgProfilePreview.setImageBitmap(bitmap)
            saveProfileImageToSession(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImageToSession(bitmap: Bitmap) {
        try {
            // Resize bitmap to avoid giant base64 strings
            val maxDim = 300
            val width = bitmap.width
            val height = bitmap.height
            val ratio = width.toFloat() / height.toFloat()
            var newWidth = maxDim
            var newHeight = maxDim
            if (width > height) {
                newHeight = (maxDim / ratio).toInt()
            } else {
                newWidth = (maxDim * ratio).toInt()
            }
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            SessionManager.saveProfilePicture(base64Image)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun viewProfileImage(base64Image: String) {
        try {
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val imageView = ImageView(this).apply {
                setImageBitmap(bitmap)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                setPadding(16, 16, 16, 16)
            }

            AlertDialog.Builder(this)
                .setView(imageView)
                .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to view image", Toast.LENGTH_SHORT).show()
        }
    }
}
