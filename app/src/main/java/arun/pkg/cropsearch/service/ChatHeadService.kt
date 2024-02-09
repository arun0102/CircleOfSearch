package arun.pkg.cropsearch.service


import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.ImageView
import arun.pkg.cropsearch.InvisibleActivity


class ChatHeadService : Service() {
    private var mgr: MediaProjectionManager? = null
    private var wmgr: WindowManager? = null
    private var mChatHeadView: View? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        wmgr = getSystemService(WINDOW_SERVICE) as WindowManager
        mChatHeadView = LayoutInflater.from(this)
            .inflate(arun.pkg.cropsearch.R.layout.view_capture_chathead, null)
        val LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        //Add the view to the window.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        //params.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG
        //Specify the chat head position
        //Initially view will be added to top-left corner
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 0
        params.y = 100

        //Add the view to the window
        wmgr?.addView(mChatHeadView, params)

        val closeButton =
            mChatHeadView?.findViewById<View>(arun.pkg.cropsearch.R.id.close_btn) as ImageView
        closeButton.setOnClickListener { //close the service and remove the chat head from the window
            stopSelf()
        }


//Drag and move chat head using user's touch action.
        val chatHeadImage =
            mChatHeadView?.findViewById<View>(arun.pkg.cropsearch.R.id.chat_head_profile_iv) as ImageView

        chatHeadImage.setOnTouchListener(object : OnTouchListener {
            private var lastAction = 0
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        //remember the initial position.
                        initialX = params.x
                        initialY = params.y

                        //get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY

                        lastAction = event.action
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        //As we implemented on touch listener with ACTION_MOVE,
                        //we have to check if the previous action was ACTION_DOWN
                        //to identify if the user clicked the view or not.
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            //Open the chat conversation click.
                            val intent = Intent(
                                this@ChatHeadService,
                                InvisibleActivity::class.java
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                            //close the service and remove the chat heads
                            stopSelf()
                        }
                        lastAction = event.action
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()

                        //Update the layout with new X & Y coordinate
                        wmgr?.updateViewLayout(mChatHeadView, params)
                        lastAction = event.action
                        return true
                    }
                }
                return false
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()

        if (mChatHeadView != null) wmgr?.removeView(mChatHeadView);
    }

    override fun onBind(intent: Intent?): IBinder {
        throw IllegalStateException("Binding not supported. Go away.")
    }
}