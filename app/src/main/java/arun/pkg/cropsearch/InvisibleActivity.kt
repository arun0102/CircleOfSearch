package arun.pkg.cropsearch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import arun.pkg.cropsearch.service.ScreenshotService

class InvisibleActivity : AppCompatActivity() {

    private var mgr: MediaProjectionManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        applicationContext?.let { context ->
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
                    mgr?.createScreenCaptureIntent()?.let {
                        startActivityForResult(
                            it,
                            REQUEST_SCREENSHOT
                        )
                    }
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    // In an educational UI, explain to the user why your app requires this
                    // permission for a specific feature to behave as expected, and what
                    // features are disabled if it's declined. In this UI, include a
                    // "cancel" or "no thanks" button that lets the user continue
                    // using your app without granting the permission.
                    //showInContextUI(...)
                }

                else -> {
                    // You can directly ask for the permission.
                    // The registered ActivityResultCallback gets the result of this request.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        TODO("VERSION.SDK_INT < TIRAMISU")
                    }
                }
            }
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                mgr?.createScreenCaptureIntent()?.let {
                    startActivityForResult(
                        it,
                        REQUEST_SCREENSHOT
                    )
                }
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                val i =
                    Intent(this, ScreenshotService::class.java)
                        .putExtra(EXTRA_RESULT_CODE, resultCode)
                        .putExtra(EXTRA_RESULT_INTENT, data)

                startService(i)
            }
        }
        finish()
    }

    companion object {
        private const val REQUEST_SCREENSHOT = 59706
        private const val EXTRA_RESULT_CODE: String = "resultCode"
        private const val EXTRA_RESULT_INTENT: String = "resultIntent"
    }
}