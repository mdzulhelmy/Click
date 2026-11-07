package ${ANDROID_PACKAGE_NAME}

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AutoClickAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AutoClickAccessibilityService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var target1: Pair<Float, Float>? = null
    private var target2: Pair<Float, Float>? = null
    private var targetPrice: Double = 0.0
    private var maxPrice: Double = 0.0
    private var operator: String = "range"
    private var intervalMs: Long = 250L
    private var onFinishedCallback: ((String) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Toast.makeText(this, "AutoSnipe Accessibility Service Connected!", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can optionally listen to live window changes to extract instant price updates
    }

    override fun onInterrupt() {
        stopSniper()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopSniper()
    }

    fun startSniper(
        target1Coord: Pair<Float, Float>,
        target2Coord: Pair<Float, Float>,
        targetPrice: Double,
        maxPrice: Double = targetPrice,
        operator: String,
        intervalMs: Long,
        onFinished: (String) -> Unit
    ) {
        this.target1 = target1Coord
        this.target2 = target2Coord
        this.targetPrice = targetPrice
        this.maxPrice = maxPrice
        this.operator = operator
        this.intervalMs = intervalMs
        this.onFinishedCallback = onFinished
        this.isRunning = true

        loopClickAndCheck()
    }

    fun stopSniper() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun loopClickAndCheck() {
        if (!isRunning) return

        // 1. Check if the screen currently contains the target price
        val detectedPrice = scanScreenForPrice()
        if (detectedPrice != null && matchesCondition(detectedPrice, targetPrice, maxPrice, operator)) {
            // TRIGGER MET! Execute instant click on Target 2 (Place Order)
            isRunning = false
            target2?.let { (x, y) ->
                dispatchClick(x, y) {
                    onFinishedCallback?.invoke("SUCCESS: Placed order at $$detectedPrice")
                }
            }
            return
        }

        // 2. Otherwise click Target 1 (Refresh / Clicker) and schedule next check
        target1?.let { (x, y) ->
            dispatchClick(x, y) {
                if (isRunning) {
                    handler.postDelayed({ loopClickAndCheck() }, intervalMs)
                }
            }
        }
    }

    private fun dispatchClick(x: Float, y: Float, onComplete: () -> Unit) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete()
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete()
            }
        }, null)
    }

    private fun scanScreenForPrice(): Double? {
        val rootNode = rootInActiveWindow ?: return null
        return findPriceInNode(rootNode)
    }

    private fun findPriceInNode(node: AccessibilityNodeInfo): Double? {
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            val price = extractPrice(text)
            if (price != null) return price
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findPriceInNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun extractPrice(text: String): Double? {
        // Matches $123.45, RM 50.00, 120.00 etc.
        val regex = Regex("""[0-9]+([.,][0-9]{2})?""")
        val match = regex.find(text) ?: return null
        val clean = match.value.replace(",", ".")
        return clean.toDoubleOrNull()
    }

    private fun matchesCondition(current: Double, target: Double, maxTarget: Double, op: String): Boolean {
        return when (op) {
            "range" -> current >= Math.min(target, maxTarget) && current <= Math.max(target, maxTarget)
            "<=" -> current <= target
            "<" -> current < target
            "==" -> Math.abs(current - target) < 0.01
            ">=" -> current >= target
            ">" -> current > target
            else -> false
        }
    }
}