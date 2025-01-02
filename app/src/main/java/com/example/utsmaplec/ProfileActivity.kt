package com.example.utsmaplec


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bottom Navigation View setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Set the default selected item to profile
        bottomNavigationView.selectedItemId = R.id.nav_profile

        // Log out functionality
        val logoutTextView = findViewById<TextView>(R.id.logout_text)
        logoutTextView.setOnClickListener {
            performLogout()
        }

        // Handle Change Account Name Click
        val changeAccountNameLayout = findViewById<LinearLayout>(R.id.change_account_name_layout)
        changeAccountNameLayout.setOnClickListener {
            showChangeAccountNameDialog()
        }

        // Handle Change Account Image Click
        val changeAccountImageLayout = findViewById<LinearLayout>(R.id.change_account_image_layout)
        changeAccountImageLayout.setOnClickListener {
            showChangeAccountImageDialog()
        }

        // Handle Change Account Number Click
        val changeAccountNumberLayout = findViewById<LinearLayout>(R.id.change_account_number_layout)
        changeAccountNumberLayout.setOnClickListener {
            showChangeAccountNumberDialog()
        }

        // Load user name and phone number from Firestore
        loadUserName()
        loadAccountNumber()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // Izin kamera tidak diberikan, tampilkan pesan ke pengguna
            }
        }
    }

    private fun performLogout() {
        // Tampilkan pop-up konfirmasi log out
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_logout_confirmation, null)
        val cancelText = dialogView.findViewById<TextView>(R.id.dialog_cancel)
        val confirmText = dialogView.findViewById<TextView>(R.id.dialog_confirm)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Handle tombol No
        cancelText.setOnClickListener {
            dialog.dismiss() // Tutup dialog
        }

        // Handle tombol Yes
        confirmText.setOnClickListener {
            // Hapus status login dari SharedPreferences
            val sharedPreferences: SharedPreferences =
                getSharedPreferences("USER_PREF", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear() // Hapus semua data
            editor.apply()

            // Arahkan pengguna kembali ke StartActivity
            val intent = Intent(this, StartActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showChangeAccountNameDialog() {
        // Inflate custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_account_name, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_account_name)
        val cancelText = dialogView.findViewById<TextView>(R.id.dialog_cancel)
        val editTextButton = dialogView.findViewById<TextView>(R.id.dialog_edit)

        // Create AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Retrieve the current account name from SharedPreferences
        val sharedPreferences: SharedPreferences = getSharedPreferences("USER_PREF", MODE_PRIVATE)
        val currentName = sharedPreferences.getString("ACCOUNT_NAME", "")
        editText.setText(currentName) // Set the current name in the EditText

        // Handle Cancel button
        cancelText.setOnClickListener {
            dialog.dismiss()
        }

        // Handle Edit button
        editTextButton.setOnClickListener {
            val newName = editText.text.toString()
            if (newName.isNotEmpty()) {
                // Save the new name to SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("ACCOUNT_NAME", newName)
                editor.apply()

                // Update the name in Firestore
                updateAccountNameInFirestore(newName)

                // Update name in the UI if needed
                val profileName = findViewById<TextView>(R.id.profile_name)
                profileName.text = newName

                dialog.dismiss()
            } else {
                editText.error = "Name cannot be empty"
            }
        }

        dialog.show()
    }

    private fun updateAccountNameInFirestore(newName: String) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user document
            val userRef = db.collection("users").document(userId)

            // Update the account name in Firestore
            userRef.update("name", newName) // Change to "name"
                .addOnSuccessListener {
                    Log.d("ProfileActivity", "Account name updated successfully in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error updating account name: ${e.message}")
                }
        }
    }

    private fun showChangeAccountImageDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_account_image, null)
        val takePicture = dialogView.findViewById<TextView>(R.id.dialog_take_picture)
        val chooseFromGallery = dialogView.findViewById<TextView>(R.id.dialog_choose_gallery)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        takePicture.setOnClickListener {
            checkCameraPermission() // Periksa izin kamera sebelum membuka kamera
            dialog.dismiss()
        }

        chooseFromGallery.setOnClickListener {
            openGallery()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoURI = createImageFile()?.let {
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
        }

        if (photoURI != null) {
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun createImageFile(): File? {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("temp_image", ".jpg", storageDir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    // Tampilkan gambar di ImageView atau simpan gambar
                }
                REQUEST_IMAGE_PICK -> {
                    val selectedImageUri = data?.data
                    // Tampilkan gambar dari galeri di ImageView atau simpan URI
                }
            }
        }
    }

    private fun loadUserName() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user document
            val userRef = db.collection("users").document(userId)

            // Get the user data
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userName = document.getString("name") // Change to "name"
                        Log.d("ProfileActivity", "User name retrieved: $userName") // Log the retrieved name
                        val profileNameTextView = findViewById<TextView>(R.id.profile_name)
                        profileNameTextView.text = userName // Set the user's name in the TextView
                    } else {
                        Log.d("ProfileActivity", "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error getting document: ${e.message}")
                }
        }
    }

    private fun showChangeAccountNumberDialog() {
        // Inflate custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_account_number, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_account_number)
        val cancelText = dialogView.findViewById<TextView>(R.id.dialog_cancel)
        val editTextButton = dialogView.findViewById<TextView>(R.id.dialog_edit)

        // Create AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Retrieve the current account number from Firestore
        loadAccountNumber(editText) // Load current number into the EditText

        // Handle Cancel button
        cancelText.setOnClickListener {
            dialog.dismiss()
        }

        // Handle Edit button
        editTextButton.setOnClickListener {
            val newNumber = editText.text.toString()
            if (newNumber.isNotEmpty()) {
                // Update the number in Firestore
                updateAccountNumberInFirestore(newNumber)

                // Update number in the UI if needed
                val profileNumberTextView = findViewById<TextView>(R.id.profile_number) // Assuming you have a TextView for the number
                profileNumberTextView.text = newNumber

                dialog.dismiss()
            } else {
                editText.error = "Number cannot be empty"
            }
        }

        dialog.show()
    }

    private fun loadAccountNumber(editText: EditText) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user document
            val userRef = db.collection("users").document(userId)

            // Get the user data
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userNumber = document.getString("phoneNumber") // Assuming you have a phoneNumber field
                        editText.setText(userNumber) // Set the current number in the EditText
                    } else {
                        Log.d("ProfileActivity", "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error getting document: ${e.message}")
                }
        }
    }

    private fun updateAccountNumberInFirestore(newNumber: String) {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user document
            val userRef = db.collection("users").document(userId)

            // Update the account number in Firestore
            userRef.update("phoneNumber", newNumber) // Change to "phoneNumber"
                .addOnSuccessListener {
                    Log.d("ProfileActivity", "Account number updated successfully in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error updating account number: ${e.message}")
                }
        }
    }

    private fun loadAccountNumber() {
        val user = auth.currentUser
        user?.let {
            val userId = user.uid

            // Reference to the user document
            val userRef = db.collection("users").document(userId)

            // Get the user data
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userNumber = document.getString("phoneNumber") // Assuming you have a phoneNumber field
                        Log.d("ProfileActivity", "User number retrieved: $userNumber") // Log the retrieved number
                        val profileNumberTextView = findViewById<TextView>(R.id.profile_number)
                        profileNumberTextView.text = userNumber // Set the user's phone number in the TextView
                    } else {
                        Log.d("ProfileActivity", "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error getting document: ${e.message}")
                }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

}
