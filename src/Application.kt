package com.coletz

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.*
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL


fun main(args: Array<String>) {

    MainScope().launch {
        val localAddress = withContext(Dispatchers.IO) {
            localAddressRetriever()
        }

        MulticastReceiver(localAddress).launch(this)
    }

    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation)

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/share") {
            val link = call.parameters["link"]
            if (link != null) {
                launch(Dispatchers.IO) {
                    runCatching { Desktop.getDesktop().browse(URL(link).toURI()) }

                }
            }
            call.respondText(link ?: "Missing link parameter", contentType = ContentType.Text.Plain)
        }
    }
}

private suspend fun localAddressRetriever(): InetAddress {
    var address: InetAddress? = null
    while (address == null) {
        address = Socket().runCatching {
            connect(InetSocketAddress("google.com", 80))
            localAddress
        }.getOrNull()

        delay(5000)
    }
    return address
}