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
import com.simplemobiletools.smsmessenger.models.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

import io.ktor.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch


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
                        val message = Message(newMessageId, body, type, status, participants, messageDate, false, threadId, false, null, address, "", subscriptionId)
                        context.messagesDB.insertOrUpdate(message)
                        refreshMessages()
                        checkReceivedSMS(message)
                        GlobalScope.launch (Dispatchers.Main) {
                            sendGetRequest(message)
                        }
                    }

                    context.showReceivedMessageNotification(address, body, threadId, null)
                }
            }
        }
    }

    fun checkReceivedSMS(message: Message) {

        // val url = "https://crypto-sms.netlify.app/api/certListChecker`"
        // I sposob z khttp
//        val response: Response = khttp.post(
//            url = "http://localhost:8888/api/certListChecker",
//            json = mapOf("sender" to message.senderName, "text" to message.body)
//        )

        // II sposob z okhttp
//        val jsonObject = JSONObject()
//        jsonObject.put("sender", message.senderName)
//        jsonObject.put("text", message.body)
//        val client = OkHttpClient()
//        val JSON: MediaType = MediaType.parse("application/json; charset=utf-8")
//        // put your json here
//        // put your json here
//        val body = RequestBody.create(JSON, jsonObject.toString())
//        val request = Request.Builder()
//            .url("http://localhost:8888/api/certListChecker")
//            .post(body)
//            .build()
//
//        val response: Response = client.newCall(request).execute()

        // III sposob: ProcessBuilder???

    }

    suspend fun sendGetRequest(message: Message) {
        val client = HttpClient()
        val response: HttpResponse = client.get("https://ktor.io/")
        println(response.status)
        println("DUPA")
        client.close()
    }
}
