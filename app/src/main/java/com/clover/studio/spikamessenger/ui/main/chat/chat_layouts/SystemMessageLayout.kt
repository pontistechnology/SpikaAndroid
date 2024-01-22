package com.clover.studio.spikamessenger.ui.main.chat.chat_layouts

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.utils.Const
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class SystemMessageLayout(private var context: Context) {
    fun bindSystemMessage(msg: MessageAndRecords, users: List<User>): SpannableStringBuilder {
        // We check if the current list of users contains users who are in the objectIds field:
        // if it contains (eg no user has been "removed" from the group) show the names as written in the database,
        // otherwise show the entire message from the server
        Timber.d("MSG BODY objids: ${msg.message.body?.objectIds}")
        Timber.d("MSG BODY obj: ${msg.message.body?.objects}")

        val objectUsers = msg.message.body?.let { body ->
            val usersObject = users.filter { userId ->
                userId.id in body.objectIds.orEmpty()
            }.map { user ->
                user.displayName
            }

            if (usersObject.size == body.objectIds?.size) {
                usersObject.toString()
            } else {
                body.objects?.toString()
            }
        }

        Timber.d("User object: $objectUsers")

        // We check the same for the subject
        val userSubject = users.firstOrNull { it.id == msg.message.body?.subjectId }?.displayName
            ?: msg.message.body?.subject

        // We need to make a string like: message time + subject + action + object + (if any) additional action
        val spannableStringBuilder = SpannableStringBuilder()

        spannableStringBuilder.append(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                msg.message.createdAt
            ).toString()
        )
        spannableStringBuilder.append(" ")

        val systemMessage = formatSystemMessage(
            context = context,
            subjectName = userSubject.toString(),
            objectNames = objectUsers.toString().replace("[", "").replace("]", ""),
            systemObject = msg.message.body?.objects.toString(),
            type = msg.message.body?.type.toString()
        )

        spannableStringBuilder.append(systemMessage)

        return spannableStringBuilder
    }

    private fun formatSystemMessage(
        context: Context,
        subjectName: String,
        objectNames: String?,
        systemObject: String,
        type: String
    ): SpannableString {

        val spannableSubject = SpannableString(subjectName)
        val spannableObjectNames = SpannableString(objectNames)
        val spannableSystemObject = SpannableString(systemObject)

        Timber.d("Object users: $spannableObjectNames")

        val text = when (type) {
            // Subject + action + object
            Const.SystemMessages.UPDATED_NOTE, Const.SystemMessages.DELETED_NOTE,
            Const.SystemMessages.CREATED_NOTE, Const.SystemMessages.CREATED_GROUP,
            Const.SystemMessages.UPDATED_GROUP_NAME, Const.SystemMessages.UPDATED_GROUP_MEMBERS ->
                SpannableString(
                    "$spannableSubject ${
                        getAction(
                            context = context,
                            type = type
                        )
                    } $spannableSystemObject"
                )

            // Subject + action
            Const.SystemMessages.UPDATED_GROUP_AVATAR, Const.SystemMessages.USER_LEFT_GROUP,
            Const.SystemMessages.UPDATED_GROUP, Const.SystemMessages.UPDATED_GROUP_ADMINS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                }"
            )

            // Subject + action + object + additional action
            Const.SystemMessages.REMOVED_GROUP_MEMBERS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                } $spannableObjectNames ${context.getString(R.string.from_the_group)}"
            )

            Const.SystemMessages.ADDED_GROUP_MEMBERS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                } $spannableObjectNames ${context.getString(R.string.to_the_group)}"
            )

            Const.SystemMessages.ADDED_GROUP_ADMINS, Const.SystemMessages.REMOVED_GROUP_ADMINS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                } $spannableObjectNames ${context.getString(R.string.as_group_admin)}"
            )

            else -> SpannableString("Error")
        }

        applyStyleSpan(text, subjectName, Typeface.BOLD)
        applyStyleSpan(text, objectNames ?: "", Typeface.BOLD)

        return text
    }

    private fun applyStyleSpan(text: SpannableString, target: String, style: Int) {
        val index = text.indexOf(target)
        if (index != -1) {
            text.setSpan(
                StyleSpan(style),
                index,
                index + target.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    private fun getAction(context: Context, type: String): String {
        return when (type) {
            // Notes
            Const.SystemMessages.UPDATED_NOTE -> context.getString(R.string.updated_note)
            Const.SystemMessages.CREATED_NOTE -> context.getString(R.string.created_note)
            Const.SystemMessages.DELETED_NOTE -> context.getString(R.string.deleted_note)

            // Groups
            Const.SystemMessages.CREATED_GROUP -> context.getString(R.string.created_group)
            Const.SystemMessages.UPDATED_GROUP -> context.getString(R.string.updated_group)
            Const.SystemMessages.UPDATED_GROUP_NAME -> context.getString(R.string.updated_group_name)
            Const.SystemMessages.UPDATED_GROUP_AVATAR -> context.getString(R.string.updated_group_avatar)
            Const.SystemMessages.USER_LEFT_GROUP -> context.getString(R.string.user_left_group)

            // Users
            Const.SystemMessages.ADDED_GROUP_ADMINS -> context.getString(R.string.added_group_admins)
            Const.SystemMessages.ADDED_GROUP_MEMBERS -> context.getString(R.string.added_group_members)
            Const.SystemMessages.REMOVED_GROUP_ADMINS, Const.SystemMessages.REMOVED_GROUP_MEMBERS ->
                context.getString(R.string.removed_group_admins)

            Const.SystemMessages.UPDATED_GROUP_ADMINS -> context.getString(R.string.updated_group_admins)
            Const.SystemMessages.UPDATED_GROUP_MEMBERS -> context.getString(R.string.updated_group_members)

            else -> context.getString(R.string.error)
        }
    }
}
