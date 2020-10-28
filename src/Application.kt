package com.coletz

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    MainScope().launch {
        val localAddress = withContext(Dispatchers.IO) {
            localAddressRetriever()
        }

        MulticastReceiver(localAddress).launch(this)
    }

    sysTray()

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


private fun sysTray() {
    val trayIcon: TrayIcon

    if (SystemTray.isSupported()) {
        val tray = SystemTray.getSystemTray()
        val image: Image = Toolkit.getDefaultToolkit().getImage("yt.png")
        val mouseListener: MouseListener = object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                println("Tray Icon - Mouse clicked!")
            }

            override fun mouseEntered(e: MouseEvent?) {
                println("Tray Icon - Mouse entered!")
            }

            override fun mouseExited(e: MouseEvent?) {
                println("Tray Icon - Mouse exited!")
            }

            override fun mousePressed(e: MouseEvent?) {
                println("Tray Icon - Mouse pressed!")
            }

            override fun mouseReleased(e: MouseEvent?) {
                println("Tray Icon - Mouse released!")
            }
        }
        val exitListener = ActionListener {
            println("Exiting...")
            exitProcess(0)
        }
        val popup = PopupMenu()
        val defaultItem = MenuItem("Exit")
        defaultItem.addActionListener(exitListener)
        popup.add(defaultItem)
        trayIcon = TrayIcon(image, "Tray Demo", popup)
        val actionListener = ActionListener {
            trayIcon.displayMessage(
                "Action Event",
                "An Action Event Has Been Performed!",
                TrayIcon.MessageType.INFO
            )
        }
        trayIcon.isImageAutoSize = true
        trayIcon.addActionListener(actionListener)
        trayIcon.addMouseListener(mouseListener)
        try {
            tray.add(trayIcon)
            println("Done")
        } catch (e: AWTException) {
            println("TrayIcon could not be added.")
        }
    } else {
        println("Unsupported")
        //  System Tray is not supported
    }
}