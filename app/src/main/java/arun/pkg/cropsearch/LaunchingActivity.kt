package arun.pkg.cropsearch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import arun.pkg.cropsearch.service.ChatHeadService
import arun.pkg.cropsearch.service.ScreenshotService


class LaunchingActivity : AppCompatActivity() {
    private val DRAW_OVER_OTHER_APP_PERMISSION = 55555

    private var isPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(
                this
            )
        ) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION)
        } else {
            isPermissionGranted = true
            startService(Intent(this, ChatHeadService::class.java))
            finish()
        }
    }
}