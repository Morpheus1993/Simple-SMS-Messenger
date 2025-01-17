package com.simplemobiletools.smsmessenger.models

import android.provider.Telephony
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.smsmessenger.helpers.ST_20_PROTOCOL_PREFIX

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "participants") val participants: ArrayList<SimpleContact>,
    @ColumnInfo(name = "date") val date: Int,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "is_mms") val isMMS: Boolean,
    @ColumnInfo(name = "attachment") val attachment: MessageAttachment?,
    @ColumnInfo(name = "sender_name") var senderName: String,
    @ColumnInfo(name = "sender_photo_uri") val senderPhotoUri: String,
    @ColumnInfo(name = "subscription_id") var subscriptionId: Int,
    @ColumnInfo(name = "headerRSA") var headerRSA: Boolean,
    @ColumnInfo(name = "validationFlag") val validationFlag: Boolean,
    ) : ThreadItem() {

    fun isReceivedMessage() = type == Telephony.Sms.MESSAGE_TYPE_INBOX

    fun isVerifiedRegularMessage() = !headerRSA && validationFlag

    companion object {
//        const val MSG_RSA_HEADER =
//        const val MSG_VERIFIED =

        fun isHeader(text: String) = text.startsWith(ST_20_PROTOCOL_PREFIX)
    }
}
