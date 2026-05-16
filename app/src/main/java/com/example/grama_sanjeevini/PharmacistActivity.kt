package com.example.grama_sanjeevini
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class PharmacistActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacist)

        db = FirebaseFirestore.getInstance()

        val medNameInput = findViewById<EditText>(R.id.etPharmMedName)
        val pharmNameInput = findViewById<EditText>(R.id.etPharmName)
        val stockInput = findViewById<EditText>(R.id.etPharmStock)
        val expiryInput = findViewById<EditText>(R.id.etPharmExpiry)
        val emergencyCheck = findViewById<CheckBox>(R.id.cbIsEmergency)
        val latInput = findViewById<EditText>(R.id.etPharmLat)
        val lngInput = findViewById<EditText>(R.id.etPharmLng)
        val updateBtn = findViewById<Button>(R.id.btnUpdateStock)
        val backBtn = findViewById<Button>(R.id.btnBack)

        updateBtn.setOnClickListener {
            val medName = medNameInput.text.toString().trim()
            val pharmName = pharmNameInput.text.toString().trim()
            val stockStr = stockInput.text.toString().trim()
            val expiryStr = expiryInput.text.toString().trim()
            val latStr = latInput.text.toString().trim()
            val lngStr = lngInput.text.toString().trim()
            val isEmergency = emergencyCheck.isChecked

            if (medName.isEmpty() || pharmName.isEmpty() || stockStr.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val stock = stockStr.toIntOrNull() ?: 0
            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            val newMedicine = hashMapOf(
                "name" to medName,
                "pharmacy" to pharmName,
                "stock" to stock,
                "expiryDate" to expiryStr,
                "isEmergency" to isEmergency,
                "latitude" to lat,
                "longitude" to lng
            )

            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()

            db.collection("medicines")
                .add(newMedicine)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Added to database!", Toast.LENGTH_LONG).show()
                    medNameInput.text.clear()
                    stockInput.text.clear()
                    expiryInput.text.clear()
                    emergencyCheck.isChecked = false
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        backBtn.setOnClickListener {
            finish()
        }
    }
}
