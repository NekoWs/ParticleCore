package work.nekow.particlecore.math

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec

data class FourierTerm(
    val radius: Number,
    val speed: Number,
    val rotate: Number
) {
    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, FourierTerm> = PacketCodec.of(
            { packet, buf ->
                buf.writeDouble(packet.radius.toDouble())
                buf.writeDouble(packet.speed.toDouble())
                buf.writeDouble(packet.rotate.toDouble())
            }, { buf ->
                val radius = buf.readDouble()
                val speed = buf.readDouble()
                val phase = buf.readDouble()
                return@of FourierTerm(radius, speed, phase)
            }
        )
    }
}