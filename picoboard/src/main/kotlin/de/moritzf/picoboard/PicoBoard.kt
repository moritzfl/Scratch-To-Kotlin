package de.moritzf.picoboard

import com.fazecast.jSerialComm.SerialPort
import de.moritzf.picoboard.internal.PicoBoardPortSelector
import de.moritzf.picoboard.internal.SerialPicoBoardPacketTransport

public object PicoBoard {
    @JvmStatic
    public fun listPorts(): List<PicoBoardPort> {
        return SerialPort.getCommPorts()
            .map { port ->
                PicoBoardPort(
                    systemPortPath = port.systemPortPath,
                    systemPortName = port.systemPortName,
                    descriptivePortName = port.descriptivePortName.orEmpty(),
                    portDescription = port.portDescription.orEmpty(),
                )
            }
    }

    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun open(
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardConnection {
        return open(requireAutoSelectedPort(), options)
    }

    @JvmStatic
    public fun findAutoSelectedPort(): PicoBoardPort? {
        return PicoBoardPortSelector.autoSelect(listPorts())
    }

    @JvmStatic
    @Throws(PicoBoardPortSelectionException::class)
    public fun requireAutoSelectedPort(): PicoBoardPort {
        return PicoBoardPortSelector.requireAutoSelected(listPorts())
    }

    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun open(
        systemPortPath: String,
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardConnection {
        val transport = SerialPicoBoardPacketTransport.open(systemPortPath, options)
        return PicoBoardConnection(
            transport = transport,
            portIdentifier = transport.identifier,
            options = options,
        )
    }

    @JvmStatic
    @JvmOverloads
    @Throws(PicoBoardException::class)
    public fun open(
        port: PicoBoardPort,
        options: PicoBoardOptions = PicoBoardOptions(),
    ): PicoBoardConnection {
        return open(port.systemPortPath, options)
    }
}
