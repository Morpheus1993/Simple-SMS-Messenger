package com.simplemobiletools.smsmessenger.misc

import com.simplemobiletools.commons.extensions.toBoolean
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONObject

class BackendClient {
    suspend fun verify(header: String, message: String): Boolean {
        val client = HttpClient()
        val test = JSONObject(
            mapOf(
                "signature" to header.substring(10, header.length),
                "text" to message,
                "id" to header.substring(4,10)
            )
        )
        val response: HttpResponse = client.post("https://crypto-sms.netlify.app/api/verify") {
            contentType(ContentType.Application.Json)
            body = test.toString()
        }
        val stringBody: String = response.receive()
        println("----------------------------------------------")
        println(response)
        println(JSONObject(stringBody))
        println("----------------------------------------------")
        return JSONObject(stringBody).get("message_valid").toBoolean()
    }

}
