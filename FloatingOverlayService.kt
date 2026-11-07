package ${ANDROID_PACKAGE_NAME}

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager

    // Floating Target 1 (Clicker / Refresh tapper)
    private lateinit var target1View: View
    private lateinit var target1Params: WindowManager.LayoutParams

    // Floating Target 2 (Order / Checkout button)
    private lateinit var target2View: View
    private lateinit var target2Params: WindowManager.LayoutParams

    // Floating Control HUD
    private lateinit var hudView: View
    private lateinit var hudParams: WindowManager.LayoutParams

    private var targetPrice: Double = 50.0
    private var maxPrice: Double = 100.0
    private var intervalMs: Long = 250L
    private var operator: String = "range"
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            targetPrice = it.getDoubleExtra("TARGET_PRICE", 50.0)
            maxPrice = it.getDoubleExtra("MAX_PRICE", 100.0)
            intervalMs = it.getLongExtra("INTERVAL_MS", 250L)
            operator = it.getStringExtra("OPERATOR") ?: "range"
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Create Target 1 (Price Check / Refresh)
        target1View = inflater.inflate(R.layout.floating_target1, null)
        target1Params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120
            y = 350
        }
        setupDraggable(target1View, target1Params, "Target 1 (Clicker)")
        windowManager.addView(target1View, target1Params)

        // 2. Create Target 2 (Place Order)
        target2View = inflater.inflate(R.layout.floating_target2, null)
        target2Params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 120
            y = 700
        }
        setupDraggable(target2View, target2Params, "Target 2 (Order)")
        windowManager.addView(target2View, target2Params)

        // 3. Create Control HUD
        hudView = inflater.inflate(R.layout.floating_hud, null)
        hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 60
        }
        setupHud()
        windowManager.addView(hudView, hudParams)

        Toast.makeText(this, "AutoSnipe Floating Buttons Ready! Drag anywhere.", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggable(view: View, params: WindowManager.LayoutParams, label: String) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupHud() {
        val btnToggle = hudView.findViewById<Button>(R.id.btnToggleBot)
        val btnClose = hudView.findViewById<Button>(R.id.btnCloseHud)
        val tvStatus = hudView.findViewById<TextView>(R.id.tvHudStatus)

        tvStatus.text = if (operator == "range") {
            "Range: $$targetPrice - $$maxPrice"
        } else {
            "Target: $operator $$targetPrice"
        }

        btnToggle.setOnClickListener {
            val accessibilityService = AutoClickAccessibilityService.instance
            if (accessibilityService == null) {
                Toast.makeText(this, "Accessibility Service is not active!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isRunning) {
                isRunning = true
                btnToggle.text = "STOP"
                btnToggle.setBackgroundColor(0xFFDC2626.toInt())

                // Calculate center coordinates of Target 1 & Target 2
                val t1X = target1Params.x + (target1View.width / 2f)
                val t1Y = target1Params.y + (target1View.height / 2f)
                val t2X = target2Params.x + (target2View.width / 2f)
                val t2Y = target2Params.y + (target2View.height / 2f)

                accessibilityService.startSniper(
                    target1Coord = Pair(t1X, t1Y),
                    target2Coord = Pair(t2X, t2Y),
                    targetPrice = targetPrice,
                    maxPrice = maxPrice,
                    operator = operator,
                    intervalMs = intervalMs
                ) { resultMsg ->
                    isRunning = false
                    btnToggle.text = "START"
                    btnToggle.setBackgroundColor(0xFF16A34A.toInt())
                    tvStatus.text = resultMsg
                }
            } else {
                isRunning = false
                accessibilityService.stopSniper()
                btnToggle.text = "START"
                btnToggle.setBackgroundColor(0xFF16A34A.toInt())
                tvStatus.text = "Stopped"
            }
        }

        btnClose.setOnClickListener {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::target1View.isInitialized) windowManager.removeView(target1View)
        if (::target2View.isInitialized) windowManager.removeView(target2View)
        if (::hudView.isInitialized) windowManager.removeView(hudView)
        AutoClickAccessibilityService.instance?.stopSniper()
    }
}