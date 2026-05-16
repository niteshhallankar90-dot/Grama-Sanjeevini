package com.example.grama_sanjeevini
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Pharmacy(
    val name: String = "",
    val pharmacy: String = "",
    val stock: Int = 0,
    val emergency: Boolean = false,
    val expiryDate: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class PharmacyDisplayItem(
    val pharmacy: Pharmacy,
    val distanceKm: Float
)

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentUserLocation: Location? = null

    private lateinit var etSearch: EditText
    private lateinit var tvResult: TextView
    private val LOCATION_PERMISSION_CODE = 100

    private var closestPharmacyLat: Double = 0.0
    private var closestPharmacyLng: Double = 0.0
    private var closestPharmacyName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etSearch = findViewById(R.id.etSearchMedicine)
        tvResult = findViewById(R.id.tvResult)

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val nearbyButton = findViewById<Button>(R.id.btnNearbyPharmacies)
        val emergencyButton = findViewById<Button>(R.id.btnEmergency)
        val pharmacistButton = findViewById<Button>(R.id.btnPharmacistLogin)
        val logoutButton = findViewById<Button>(R.id.btnLogout)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.email == "pharmacist@grama.com") {
            pharmacistButton.visibility = View.VISIBLE
        } else {
            pharmacistButton.visibility = View.GONE
        }

        forceUploadEmergencyMedicines()

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        requestLocationPermission()

        btnSearch.setOnClickListener {
            val medicineName = etSearch.text.toString().trim()
            if (medicineName.isEmpty()) tvResult.text = "Please enter a medicine name"
            else fetchAndDisplay(db.collection("medicines").whereEqualTo("name", medicineName), false, false)
        }

        nearbyButton.setOnClickListener {
            fetchAndDisplay(db.collection("medicines"), false, true)
        }

        emergencyButton.setOnClickListener {
            fetchAndDisplay(db.collection("medicines").whereEqualTo("emergency", true), true, false)
        }

        pharmacistButton.setOnClickListener {
            startActivity(Intent(this, PharmacistActivity::class.java))
        }

        tvResult.setOnClickListener {
            if (closestPharmacyName.isNotEmpty()) {
                try {
                    val gmmIntentUri = Uri.parse("geo:$closestPharmacyLat,$closestPharmacyLng?q=$closestPharmacyLat,$closestPharmacyLng($closestPharmacyName)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                } catch (e: Exception) {
                    val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$closestPharmacyLat,$closestPharmacyLng")
                    startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
            }
        }
    }

    private fun forceUploadEmergencyMedicines() {
        db.collection("medicines").whereEqualTo("emergency", true).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val m1 = Pharmacy("Snake Anti-Venom", "Village Pharmacy", 10, true, "2025-10-01", 12.9715, 77.5945)
                    val m2 = Pharmacy("Oxygen Cylinders", "City Health Store", 5, true, "2026-05-15", 13.0827, 80.2707)
                    val m3 = Pharmacy("Rabies Vaccine", "Rural Care Meds", 20, true, "2024-12-31", 12.2958, 76.6394)
                    val m4 = Pharmacy("Asthma Inhalers", "Village Pharmacy", 50, true, "2025-08-20", 12.9715, 77.5945)

                    db.collection("medicines").add(m1)
                    db.collection("medicines").add(m2)
                    db.collection("medicines").add(m3)
                    db.collection("medicines").add(m4)
                    Toast.makeText(this, "✅ Emergency Medicines Auto-Uploaded!", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        } else getUserLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) getUserLocation()
    }

    // FIX #1: Added Fake GPS Fallback for the Emulator!
    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentUserLocation = location
                } else {
                    // If the emulator's GPS is turned off, pretend we are standing in Central Bangalore!
                    val fakeLocation = Location("")
                    fakeLocation.latitude = 12.9300
                    fakeLocation.longitude = 77.6000
                    currentUserLocation = fakeLocation
                }
            }
        }
    }

    private fun fetchAndDisplay(query: Query, isEmergencyMode: Boolean, isNearbyMode: Boolean) {
        tvResult.text = "Calculating GPS Distance..."
        closestPharmacyName = ""

        query.get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                tvResult.text = "❌ No results found"
            } else {
                val displayItems = mutableListOf<PharmacyDisplayItem>()
                for (document in documents) {
                    val pharmacy = document.toObject(Pharmacy::class.java)
                    var distanceKm = 0f
                    if (currentUserLocation != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            currentUserLocation!!.latitude, currentUserLocation!!.longitude,
                            pharmacy.latitude, pharmacy.longitude, results
                        )
                        distanceKm = results[0] / 1000f
                    }
                    displayItems.add(PharmacyDisplayItem(pharmacy, distanceKm))
                }

                displayItems.sortBy { it.distanceKm }

                if (displayItems.isNotEmpty()) {
                    val closest = displayItems[0].pharmacy
                    closestPharmacyLat = closest.latitude
                    closestPharmacyLng = closest.longitude
                    closestPharmacyName = closest.pharmacy
                }

                var finalString = ""

                if (isEmergencyMode) {
                    finalString += "🚨 EMERGENCY MEDICINES (Available 24/7):\n\n"
                    for ((index, item) in displayItems.withIndex()) {
                        finalString += "${index + 1}. ${item.pharmacy.name} (${item.pharmacy.pharmacy}) - %.1f km away\n".format(item.distanceKm)
                    }
                } else if (isNearbyMode) {
                    finalString += "🏥 NEARBY PHARMACIES:\n\n"

                    // FIX #2: Filter out duplicates so each Pharmacy only shows up once!
                    val uniquePharmacies = displayItems.distinctBy { it.pharmacy.pharmacy }

                    for ((index, item) in uniquePharmacies.withIndex()) {
                        finalString += "${index + 1}. ${item.pharmacy.pharmacy} - %.1f km away\n".format(item.distanceKm)
                    }
                } else {
                    val item = displayItems[0]
                    finalString += "✅ Found: ${item.pharmacy.name}\n📍 Pharmacy: ${item.pharmacy.pharmacy}\n📦 Stock Available: ${item.pharmacy.stock}\n📏 Distance: %.1f km away".format(item.distanceKm)
                    if (item.pharmacy.expiryDate.isNotEmpty()) finalString += "\n\n⚠️ ALERT: Medicine expires soon (${item.pharmacy.expiryDate})!"
                }

                finalString += "\n\n🗺️ Tap this green box to open Google Maps!"
                tvResult.text = finalString
            }
        }.addOnFailureListener {
            tvResult.text = "Error connecting to database"
        }
    }
}
