package com.clover.studio.spikamessenger.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import kotlin.math.abs

const val SHOW_LIMIT = 30
const val MAX_ALPHA = 255
const val MAX_SCALE = 1F
const val MIN_ALPHA = 0
const val MIN_SCALE = 0f
const val SMALL_MESSAGE_POSITIVE_SIZE_1 = 80
const val SMALL_MESSAGE_POSITIVE_SIZE_2 = 250
const val SMALL_MESSAGE_NEGATIVE_SIZE_1 = -10
const val SMALL_MESSAGE_NEGATIVE_SIZE_2 = -200

class MessageSwipeController(
    private val context: Context,
    private val messageRecords: MutableList<MessageAndRecords>,
    private val onSwipeAction: ((action: String, position: Int) -> Unit)
) :
    Callback() {

    private var replyImage: Drawable? = null
    private var infoImage: Drawable? = null

    private var currentItemViewHolder: ViewHolder? = null
    private lateinit var mView: View
    private var swipeBack = false
    private var isVibrate = false
    private var startTracking = false
    private var halfScreen = 0

    private var action: String = ""

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
    ): Int {
        mView = viewHolder.itemView
        replyImage = AppCompatResources.getDrawable(context, R.drawable.img_reply_item)
        infoImage = AppCompatResources.getDrawable(context, R.drawable.img_info_item)

        val width = Resources.getSystem().displayMetrics.widthPixels
        halfScreen = width / 3

        return if (messageRecords[viewHolder.absoluteAdapterPosition].message.canDelete()) {
            // Disable swipe for deleted messages
            makeMovementFlags(ACTION_STATE_IDLE, 0)
        } else {
            // Enable swipe for non-deleted messages
            makeMovementFlags(ACTION_STATE_IDLE, RIGHT or LEFT)
        }
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
        // Detect side of swipe
        if (actionState == ACTION_STATE_SWIPE) {
            action = if (dX > 0) {
                Const.UserActions.ACTION_RIGHT
            } else {
                Const.UserActions.ACTION_LEFT
            }
            // Detect when swipe is released
            setTouchListener(recyclerView, viewHolder)
        }

        var newDx = dX
        if (Const.UserActions.ACTION_LEFT == action && dX <= -halfScreen / 2) {
            newDx = (-halfScreen / 2).toFloat()
        } else if (Const.UserActions.ACTION_RIGHT == action && dX >= halfScreen / 2) newDx =
            (halfScreen / 2).toFloat()

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            newDx,
            dY,
            actionState,
            isCurrentlyActive
        )
        // Move the section header in the opposite direction to counter the container's movement
        mView.findViewById<View>(R.id.tv_section_header).translationX = -dX
        startTracking = true
        currentItemViewHolder = viewHolder

        if (Const.UserActions.ACTION_RIGHT == action) {
            drawIcon(c, action, replyImage)
        } else {
            drawIcon(c, action, infoImage)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack =
                event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                // Adjust for small messages:
                if (abs(mView.translationX) >= halfScreen
                    || (mView.translationX > SMALL_MESSAGE_POSITIVE_SIZE_1 && mView.translationX < SMALL_MESSAGE_POSITIVE_SIZE_2)
                    || (mView.translationX < SMALL_MESSAGE_NEGATIVE_SIZE_1 && mView.translationX > SMALL_MESSAGE_NEGATIVE_SIZE_2)
                ) {
                    onSwipeAction.invoke(action, viewHolder.absoluteAdapterPosition)
                    resetPosition()
                }
            }
            false
        }
    }

    private fun resetPosition() {
        if (currentItemViewHolder != null) {
            val view = currentItemViewHolder!!.itemView
            view.translationX = 0f
        }
    }

    /* A method used to draw and show/hide the reply icon on the left side of messages
    * If we swipe the message from left to right, we take positive values of the variable x, otherwise negative values
    */
    private fun drawIcon(canvas: Canvas, action: String, icon: Drawable?) {
        if (currentItemViewHolder == null) {
            return
        }

        var alpha = 0
        var scale = 0f
        val showing: Boolean
        val translationX = mView.translationX

        showing = if (Const.UserActions.ACTION_RIGHT == action) {
            translationX >= convertToDp(SHOW_LIMIT)
        } else {
            translationX <= -convertToDp(SHOW_LIMIT)
        }

        // Show icon if translationX is bigger than 30px
        if (showing) {
            // Icon moving forward - show it
            scale = MAX_SCALE
            alpha = MAX_ALPHA
            // If translationX (icon) is not on the screen
        } else if (translationX <= 0.0f) {
            startTracking = false
            isVibrate = false
        } else {
            // Icon is returning - hide it
            scale = MIN_SCALE
            alpha = MIN_ALPHA
        }

        replyImage?.alpha = alpha
        if (startTracking) {
            // On half of screen vibrate
            if (!isVibrate && (mView.translationX >= halfScreen
                        || mView.translationX >= -halfScreen)
            ) {
                mView.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP
                )
                isVibrate = true
            }
        }

        // Calculate where to show icon (x,y)
        val x: Int = if (action == Const.UserActions.ACTION_LEFT) {
            (mView.measuredWidth + mView.translationX / 2).toInt() - convertToDp(16)
        } else {
            if (mView.translationX > halfScreen) {
                halfScreen / 2
            } else {
                (mView.translationX / 2).toInt()
            }
        }
        val y = (mView.top + mView.measuredHeight / 2).toFloat()

        // Draw icon
        val iconSize = (convertToDp(12) * scale)
        icon?.setBounds(
            (x - iconSize).toInt(),
            (y - iconSize).toInt(),
            (x + iconSize).toInt(),
            (y + iconSize).toInt()
        )
        icon?.draw(canvas)
        icon?.alpha = MAX_ALPHA
    }

    private fun convertToDp(pixel: Int): Int {
        return Tools.dp(pixel.toFloat(), context)
    }
}
