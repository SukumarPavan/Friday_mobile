package com.example.friday

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.VideoView
import androidx.core.app.NotificationCompat

class FridayOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        showOverlay()
        startForegroundServiceWithNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    private fun showOverlay() {
        if (overlayView != null) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_friday, null)

        val params = WindowManager.LayoutParams(
            dpToPx(200),
            dpToPx(200),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = dpToPx(32) // margin from bottom

        windowManager?.addView(overlayView, params)

        // Setup VideoView
        val videoView = overlayView!!.findViewById<VideoView>(R.id.overlayVideo)
        val resId = resources.getIdentifier("circle", "raw", packageName)
        if (resId != 0) {
            videoView.setVideoPath("android.resource://$packageName/$resId")
            videoView.start()
        }

        // Close button
        val closeBtn = overlayView!!.findViewById<ImageButton>(R.id.overlayCloseBtn)
        closeBtn.setOnClickListener { stopSelf() }
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "friday_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Friday Overlay", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Friday Assistant")
            .setContentText("Friday is listening...")
            .setSmallIcon(R.drawable.ic_apps)
            .build()
        startForeground(1, notification)
    }

    companion object {
        fun show(context: Context) {
            val intent = Intent(context, FridayOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        fun hide(context: Context) {
            context.stopService(Intent(context, FridayOverlayService::class.java))
        }
    }
} 