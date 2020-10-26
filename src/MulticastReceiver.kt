package com.coletz

import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.*
import java.util.*
import kotlin.text.toByteArray


class MulticastReceiver(private val serverAddress: InetAddress) {

    private var socket: MulticastSocket? = null

    private val buffer = ByteArray(256)

    fun launch(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            with(MulticastSocket(5894)) {
                socket = this
                val group = InetAddress.getByName("228.152.49.25")
                setCorrectIpVersionInterface(group)
                joinGroup(group)
                while (true) {
                    val rxPacket = DatagramPacket(buffer, buffer.size)
                    receive(rxPacket)
                    val rxString = rxPacket.let { String(it.data, length = it.length) }
                    if (rxString == "§reqaddress") {
                        val txString = "${serverAddress.hostName}:8954".toByteArray()
                        val txPacket = DatagramPacket(txString, txString.size, rxPacket.address, rxPacket.port)
                        send(txPacket)
                    }
                    if (rxString == "§stop") {
                        break
                    }
                }
                leaveGroup(group)
                close()
            }
        }
    }

    private fun MulticastSocket.setCorrectIpVersionInterface(multicastAddress: InetAddress) {
        var interfaceSet = false
        val ipV6 = multicastAddress is Inet6Address
        val interfaces: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val i = interfaces.nextElement() as NetworkInterface
            val addresses: Enumeration<*> = i.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement() as InetAddress
                if (ipV6 && address is Inet6Address) {
                    setInterface(address)
                    interfaceSet = true
                    break
                } else if (!ipV6 && address is Inet4Address) {
                    setInterface(address)
                    interfaceSet = true
                    break
                }
            }
            if (interfaceSet) {
                break
            }
        }
    }
}