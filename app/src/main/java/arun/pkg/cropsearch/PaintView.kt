package arun.pkg.cropsearch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class PaintView : View {
    private val mFingerPaths: Array<Path?> = arrayOfNulls(MAX_FINGERS)
    private lateinit var mFingerPaint: Paint
    private lateinit var mCompletedPaths: ArrayList<Path?>
    private val mPathBounds = RectF()
    private var top = 0f
    private var left = Float.MAX_VALUE
    private var right = 0f
    private var bottom = Float.MAX_VALUE

    private var isDone = false
    private var circleDrawnListener: CircleDrawnListener? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun setCircleDrawnListener(circleDrawnListener: CircleDrawnListener) {
        this.circleDrawnListener = circleDrawnListener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mCompletedPaths = ArrayList()
        mFingerPaint = Paint()
        mFingerPaint.isAntiAlias = true
        mFingerPaint.setColor(Color.WHITE)
        mFingerPaint.style = Paint.Style.STROKE
        mFingerPaint.strokeWidth = 24f
        mFingerPaint.strokeCap = Paint.Cap.BUTT

        left = Float.MAX_VALUE
        bottom = Float.MAX_VALUE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if(isDone) {
            //canvas.drawRect(left, top, right, bottom, mSquarePaint)
            circleDrawnListener?.onCircleDrawn(left, top, right, bottom)
        } else {
            for (completedPath in mCompletedPaths) {
                completedPath?.let { canvas.drawPath(it, mFingerPaint) }
            }

            for (fingerPath in mFingerPaths) {
                if (fingerPath != null) {
                    canvas.drawPath(fingerPath, mFingerPaint)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount
        val cappedPointerCount = if (pointerCount > MAX_FINGERS) MAX_FINGERS else pointerCount
        val actionIndex = event.actionIndex
        val action = event.actionMasked
        val id = event.getPointerId(actionIndex)

        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && id < MAX_FINGERS) {
            mCompletedPaths = ArrayList()
            mFingerPaths[id] = Path()
            mFingerPaths[id]?.moveTo(event.getX(actionIndex), event.getY(actionIndex))

            if(isDone) {
                isDone = false
                top = 0f
                left = Float.MAX_VALUE
                bottom = Float.MAX_VALUE
                right = 0f
            }
        } else if ((action == MotionEvent.ACTION_MOVE) && id < MAX_FINGERS) {
            updateFrameValues(event.getX(actionIndex), event.getY(actionIndex))
        } else if ((action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) && id < MAX_FINGERS) {
            mFingerPaths[id]?.setLastPoint(event.getX(actionIndex), event.getY(actionIndex))
            mCompletedPaths.add(mFingerPaths[id])
            mFingerPaths[id]?.computeBounds(mPathBounds, true)
            invalidate(
                mPathBounds.left.toInt(), mPathBounds.top.toInt(),
                mPathBounds.right.toInt(), mPathBounds.bottom.toInt()
            )
            mFingerPaths[id] = null

            isDone = true
        }

        for (i in 0 until cappedPointerCount) {
            if (mFingerPaths[i] != null) {
                val index = event.findPointerIndex(i)
                mFingerPaths[i]?.lineTo(event.getX(index), event.getY(index))
                mFingerPaths[i]?.computeBounds(mPathBounds, true)
                invalidate(
                    mPathBounds.left.toInt(), mPathBounds.top.toInt(),
                    mPathBounds.right.toInt(), mPathBounds.bottom.toInt()
                )
            }
        }

        return true
    }

    private fun updateFrameValues(x: Float, y: Float) {
        if(x > right) {
            right = x
            if(left == Float.MAX_VALUE) {
                left = x
            }
        }
        if(x < left){
            left = x
        }

        if(y > top) {
            top = y
        }
        if(y < bottom) {
            bottom = y
            if(bottom == Float.MAX_VALUE) {
                bottom = y
            }
        }
    }

    companion object {
        const val MAX_FINGERS: Int = 1
    }
}