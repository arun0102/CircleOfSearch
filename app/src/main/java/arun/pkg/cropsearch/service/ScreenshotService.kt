package arun.pkg.cropsearch.service


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.FileProvider
import arun.pkg.cropsearch.CircleDrawnListener
import arun.pkg.cropsearch.PaintView
import arun.pkg.cropsearch.R
import arun.pkg.cropsearch.ScreenCapture
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ScreenshotService : Service() {

    private val CHANNEL_WHATEVER = "channel_whatever"
    private val NOTIFY_ID = 9906
    val EXTRA_RESULT_CODE: String = "resultCode"
    val EXTRA_RESULT_INTENT: String = "resultIntent"
    val VIRT_DISPLAY_FLAGS: Int = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    private var projection: MediaProjection? = null
    private var vdisplay: VirtualDisplay? = null
    private val handlerThread = HandlerThread(
        javaClass.simpleName,
        Process.THREAD_PRIORITY_BACKGROUND
    )
    private var handler: Handler? = null
    private var mgr: MediaProjectionManager? = null
    private var wmgr: WindowManager? = null
    private var screenCapture: ScreenCapture? = null
    private var resultCode: Int? = 0
    private var resultData: Intent? = null
    var notificationManager: NotificationManager? = null
    private var mCropView: View? = null
    private var top = 0f
    private var left = 0f
    private var right = 0f
    private var bottom = 0f
    private var isCapturedOnce = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        wmgr = getSystemService(WINDOW_SERVICE) as WindowManager

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        mCropView = LayoutInflater.from(this)
            .inflate(R.layout.view_draw_sreen, null)
        val LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        //Add the view to the window.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //Add the view to the window
        wmgr?.addView(mCropView, params)

        val paintView =
            mCropView?.findViewById<View>(R.id.cropImageView) as PaintView
        paintView.setCircleDrawnListener(object : CircleDrawnListener {
            override fun onCircleDrawn(left: Float, top: Float, right: Float, bottom: Float) {
                wmgr?.removeView(mCropView)
                this@ScreenshotService.top = top
                this@ScreenshotService.left = left
                this@ScreenshotService.right = right
                this@ScreenshotService.bottom = bottom
                handler!!.postDelayed({ startCapture() }, 100)
            }
        })
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        resultCode = i?.getIntExtra(EXTRA_RESULT_CODE, 1337)
        resultData = i?.getParcelableExtra(EXTRA_RESULT_INTENT)
        foregroundify()
        return (START_NOT_STICKY)
    }

    override fun onBind(intent: Intent?): IBinder {
        throw IllegalStateException("Binding not supported. Go away.")
    }

    fun getWindowManager(): WindowManager {
        return (wmgr!!)
    }

    fun getHandler(): Handler {
        return (handler!!)
    }

    fun processImage(png: ByteArray?) {
        object : Thread() {
            override fun run() {
                if (!isCapturedOnce) {
                    isCapturedOnce = true
                    val output = File(
                        getExternalFilesDir(null),
                        "screenshot.png"
                    )

                    try {
                        val fos = FileOutputStream(output)

                        fos.write(png)
                        fos.flush()
                        fos.fd.sync()
                        fos.close()


                        val croppedImage: File = cropImage(output)
                        notificationManager!!.cancel(NOTIFY_ID)
                        handler!!.postDelayed({
                            shareImage(getContentUri(croppedImage))
                        }, 100)

                        stopCapture()
                        this@ScreenshotService.stopSelf()
                    } catch (e: Exception) {
                        Log.e(javaClass.simpleName, "Exception writing out screenshot", e)
                    }
                }
            }
        }.start()
    }

    private fun shareImage(uri: Uri?) {
        val shareIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("com.google.android.googlequicksearchbox"))
        shareIntent.setAction(Intent.ACTION_SEND)
        shareIntent.setType("image/*")
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(shareIntent)
    }

    private fun getContentUri(imageFile: File): Uri? {
        return FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            imageFile
        )
    }

    private fun cropImage(fullImageFile: File): File {
        val filePath = fullImageFile.path
        val bitmap = BitmapFactory.decodeFile(filePath)

        val tv = TypedValue()
        var actionBarHeight = 0
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight =
                TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
        val croppedBitmap =
            Bitmap.createBitmap(
                bitmap,
                left.toInt(),
                bottom.toInt() + actionBarHeight,
                right.toInt() - left.toInt(),
                top.toInt() - bottom.toInt()
            )
        val croppedFile = File(
            getExternalFilesDir(null),
            "cropped_screenshot" + ".png"
        )
        try {
            FileOutputStream(croppedFile).use { out ->
                croppedBitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    out
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return croppedFile
    }

    private fun stopCapture() {
        if (projection != null) {
            projection!!.stop()
            vdisplay!!.release()
            projection = null
        }
    }

    private fun startCapture() {
        projection = resultCode?.let { it1 -> mgr?.getMediaProjection(it1, resultData!!) }

        screenCapture = ScreenCapture(this)

        val cb: MediaProjection.Callback = object : MediaProjection.Callback() {
            override fun onStop() {
                vdisplay!!.release()
            }
        }
        projection?.registerCallback(cb, handler)
        vdisplay = projection?.createVirtualDisplay(
            "Circle to Search",
            mCropView!!.width, mCropView!!.height,
            resources.displayMetrics.densityDpi,
            VIRT_DISPLAY_FLAGS, screenCapture!!.getSurface(), null, handler
        )
    }

    private fun foregroundify() {
        if (notificationManager!!.getNotificationChannel(CHANNEL_WHATEVER) == null) {
            notificationManager!!.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WHATEVER,
                    "Whatever", NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val b: Notification =
            Notification.Builder(this, CHANNEL_WHATEVER)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(getString(R.string.app_name))
                .build()
        b.flags = Notification.FLAG_AUTO_CANCEL
        startForeground(NOTIFY_ID, b)

    }
}