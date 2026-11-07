package com.autosnipe.autoclicker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusOverlay: TextView
    private lateinit var statusAccessibility: TextView
    private lateinit var btnGrantOverlay: Button
    private lateinit var btnGrantAccessibility: Button
    private lateinit var btnStartFloating: Button
    private lateinit var etTargetPrice: EditText
    private lateinit var etInterval: EditText
    private lateinit var rgOperator: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusOverlay = findViewById(R.id.tvOverlayStatus)
        statusAccessibility = findViewById(R.id.tvAccessibilityStatus)
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)
        btnGrantAccessibility = findViewById(R.id.btnGrantAccessibility)
        btnStartFloating = findViewById(R.id.btnStartFloating)
        etTargetPrice = findViewById(R.id.etTargetPrice)
        etInterval = findViewById(R.id.etInterval)
        rgOperator = findViewById(R.id.rgOperator)

        btnGrantOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
            }
        }

        btnGrantAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Locate 'AutoSnipe Service' and toggle ON", Toast.LENGTH_LONG).show()
        }

        btnStartFloating.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Overlay Permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isAccessibilityServiceEnabled(this, AutoClickAccessibilityService::class.java)) {
                Toast.makeText(this, "Please enable AutoSnipe Accessibility Service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val priceStr = etTargetPrice.text.toString().trim()
            val targetPrice = priceStr.toDoubleOrNull() ?: 50.0
            val maxPrice = findViewById<EditText>(R.id.etMaxPrice)?.text?.toString()?.toDoubleOrNull() ?: 100.0
            val interval = etInterval.text.toString().toLongOrNull() ?: 250L

            val operator = when (rgOperator.checkedRadioButtonId) {
                R.id.rbRange -> "range"
                R.id.rbLessEqual -> "<="
                R.id.rbExact -> "=="
                R.id.rbGreaterEqual -> ">="
                else -> "<="
            }

            val serviceIntent = Intent(this, FloatingOverlayService::class.java).apply {
                putExtra("TARGET_PRICE", targetPrice)
                putExtra("MAX_PRICE", maxPrice)
                putExtra("INTERVAL_MS", interval)
                putExtra("OPERATOR", operator)
            }
            startService(serviceIntent)
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        statusOverlay.text = if (hasOverlay) "Overlay: GRANTED ✓" else "Overlay: REQUIRED ✗"
        statusOverlay.setTextColor(if (hasOverlay) 0xFF10B981.toInt() else 0xFFEF4444.toInt())

        val hasAccess = isAccessibilityServiceEnabled(this, AutoClickAccessibilityService::class.java)
        statusAccessibility.text = if (hasAccess) "Accessibility: ACTIVE ✓" else "Accessibility: DISABLED ✗"
        statusAccessibility.setTextColor(if (hasAccess) 0xFF10B981.toInt() else 0xFFEF4444.toInt())

        btnStartFloating.isEnabled = hasOverlay && hasAccess
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}