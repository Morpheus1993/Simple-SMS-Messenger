package com.simplemobiletools.smsmessenger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.simplemobiletools.commons.extensions.isNumberBlocked
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.SimpleContact
import com.simplemobiletools.smsmessenger.extensions.*
import com.simplemobiletools.smsmessenger.helpers.refreshMessages
import com.simplemobiletools.smsmessenger.misc.BackendClient
import com.simplemobiletools.smsmessenger.models.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random


class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        var address = ""
        var body = ""
        var subject = ""
        var date = 0L
        var threadId = 0L
        var status = Telephony.Sms.STATUS_NONE
        val type = Telephony.Sms.MESSAGE_TYPE_INBOX
        val read = 0
        val subscriptionId = intent.getIntExtra("subscription", -1)

        ensureBackgroundThread {
            messages.forEach {
                address = it.originatingAddress ?: ""
                subject = it.pseudoSubject
                status = it.status
                body += it.messageBody
                date = Math.min(it.timestampMillis, System.currentTimeMillis())
                threadId = context.getThreadId(address)
            }

            Handler(Looper.getMainLooper()).post {
                if (!context.isNumberBlocked(address)) {
                    ensureBackgroundThread {
                        val newMessageId = context.insertNewSMS(address, subject, body, date, read, threadId, type, subscriptionId)

                        val conversation = context.getConversations(threadId).firstOrNull() ?: return@ensureBackgroundThread
                        try {
                            context.conversationsDB.insertOrUpdate(conversation)
                        } catch (ignored: Exception) {
                        }

                        context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversations())
                        val participant = SimpleContact(0, 0, address, "", arrayListOf(address), ArrayList(), ArrayList())
                        val participants = arrayListOf(participant)
                        val messageDate = (date / 1000).toInt()

                        val message = context.messagesDB.getLatestMessageInThread(threadId).let { latestMessage ->
                            val rsaHeader = Message.isHeader(body)
                            if (latestMessage != null) {
                                if (latestMessage.headerRSA) {
                                    val verified = runBlocking {
                                           isAuthenticMessage(latestMessage.body, body)
                                        }
                                    return@let Message(newMessageId, body, type, status, participants, messageDate, false, threadId, false, null, address, "", subscriptionId, rsaHeader, verified)
                                } else {
                                    return@let Message(newMessageId, body, type, status, participants, messageDate, false, threadId, false, null, address, "", subscriptionId, rsaHeader, false)
                                }
                            } else {
                                return@let Message(newMessageId, body, type, status, participants, messageDate, false, threadId, false, null, address, "", subscriptionId, rsaHeader, false)

                            }
                        }

                        context.messagesDB.insertOrUpdate(message)
                        refreshMessages()
                    }
                }

                context.showReceivedMessageNotification(address, body, threadId, null)
            }
        }
    }


    private suspend fun isAuthenticMessage(header: String, message: String): Boolean {
        return BackendClient().verify(header, message)
    }

}
