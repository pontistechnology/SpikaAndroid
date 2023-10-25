package com.clover.studio.spikamessenger.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.clover.studio.spikamessenger.R

const val HALF_SCREEN = 100

class NotesSwipeHelper(
    private val context: Context,
    private val onSwipeAction: ((position: Int) -> Unit)
) :
    Callback() {

    private var deleteImage: Drawable? = null

    private lateinit var mView: View
    private var dX = 0f
    private var swipeBack = false
    private var isVibrate = false
    private var startTracking = false
    private val background = ColorDrawable()

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
    ): Int {
        mView = viewHolder.itemView
        deleteImage = AppCompatResources.getDrawable(context, R.drawable.img_delete_note)

        return makeMovementFlags(ACTION_STATE_IDLE, LEFT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = false
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        setTouchListener(recyclerView, viewHolder)

        if (mView.translationX < convertToDp(HALF_SCREEN) || dX < this.dX) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            this.dX = dX
            startTracking = true
        }

        drawIcon(c, viewHolder.itemView)
    }

    private fun drawIcon(canvas: Canvas, itemView: View) {
        var scale = 0f
        var alpha = 0
        val showing: Boolean
        val translationX = mView.translationX

        showing = translationX <= -convertToDp(SHOW_LIMIT)
        if (showing) {
            scale = MAX_SCALE
            alpha = MAX_ALPHA
        } else if (translationX <= 0.0f) {
            startTracking = false
            isVibrate = false
        } else {
            scale = MIN_SCALE
            alpha = MIN_ALPHA
        }

        deleteImage?.alpha = alpha

        if (startTracking) {
            if (!isVibrate && (mView.translationX >= convertToDp(HALF_SCREEN)
                        || mView.translationX >= -convertToDp(HALF_SCREEN))
            ) {
                mView.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                )
                isVibrate = true
            }
        }

        val x: Int =
            if (mView.translationX < -convertToDp(SHOW_LIMIT)) {
                mView.measuredWidth - convertToDp(SHOW_LIMIT) / 2 - convertToDp(16)
            } else {
                (mView.measuredWidth - mView.translationX / 2).toInt() - convertToDp(16)
            }

        val y = (mView.top + mView.measuredHeight / 2).toFloat()

        background.color = context.getColor(R.color.style_red)
        background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
        )
        background.draw(canvas)

        deleteImage?.setBounds(
            (x - convertToDp(12) * scale).toInt(),
            (y - convertToDp(11) * scale).toInt(),
            (x + convertToDp(12) * scale).toInt(),
            (y + convertToDp(10) * scale).toInt()
        )
        deleteImage?.draw(canvas)
    }

    private fun convertToDp(pixel: Int): Int {
        return Tools.dp(pixel.toFloat(), context)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack =
                event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                onSwipeAction.invoke(viewHolder.absoluteAdapterPosition)
            }
            false
        }
    }
}
