package com.example.pipkeypet

import android.accessibilityservice.AccessibilityService
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView

class KeyPetService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var petView: ImageView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var isLeftPaw = true

    private lateinit var idleBitmap: Bitmap
    private lateinit var tapLeftBitmap: Bitmap
    private lateinit var tapRightBitmap: Bitmap

    private val resetToIdleRunnable = Runnable {
        petView?.setImageBitmap(idleBitmap)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        initSprites()
        createOverlayWindow()
    }

    private fun createOverlayWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        petView = ImageView(this).apply {
            setImageBitmap(idleBitmap)
        }

        params = WindowManager.LayoutParams(
            220,
            220,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        petView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        triggerPetAnimation()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(petView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(petView, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            triggerPetAnimation()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            triggerPetAnimation()
        }
        return false
    }

    private fun triggerPetAnimation() {
        handler.removeCallbacks(resetToIdleRunnable)
        isLeftPaw = !isLeftPaw
        petView?.setImageBitmap(if (isLeftPaw) tapLeftBitmap else tapRightBitmap)
        handler.postDelayed(resetToIdleRunnable, 120)
    }

    private fun initSprites() {
        idleBitmap = drawPet(pawState = 0)
        tapLeftBitmap = drawPet(pawState = 1)
        tapRightBitmap = drawPet(pawState = 2)
    }

    private fun drawPet(pawState: Int): Bitmap {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.parseColor("#14FF62")
        canvas.drawRoundRect(40f, 50f, 160f, 150f, 30f, 30f, paint)

        val path = Path().apply {
            moveTo(45f, 60f); lineTo(40f, 20f); lineTo(75f, 50f); close()
            moveTo(155f, 60f); lineTo(160f, 20f); lineTo(125f, 50f); close()
        }
        canvas.drawPath(path, paint)

        paint.color = Color.parseColor("#0A2E16")
        canvas.drawCircle(75f, 95f, 10f, paint)
        canvas.drawCircle(125f, 95f, 10f, paint)

        paint.color = Color.parseColor("#14FF62")
        val leftY = if (pawState == 1) 175f else 145f
        val rightY = if (pawState == 2) 175f else 145f
        canvas.drawCircle(60f, leftY, 18f, paint)
        canvas.drawCircle(140f, rightY, 18f, paint)

        return bmp
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (petView != null) windowManager?.removeView(petView)
    }
}
