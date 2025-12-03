package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.ParticleAore.Companion.MOD_ID

class PacketFunctionParticlesS2C(
    val particle: ParticleBuilder,
    val function: String,
    val range: Pair<Double, Double>,
    val step: Double,
    val delay: Double,
    val id: Long
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "function_particles")
        val PAYLOAD_ID = CustomPayload.Id<PacketFunctionParticlesS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketFunctionParticlesS2C> = PacketCodec.of<RegistryByteBuf, PacketFunctionParticlesS2C>(
            { packet, buf ->
                ParticleBuilder.PACKET_CODEC.encode(buf, packet.particle)
                buf.writeString(packet.function)
                buf.writeDouble(packet.range.first)
                buf.writeDouble(packet.range.second)
                buf.writeDouble(packet.step)
                buf.writeDouble(packet.delay)
                buf.writeLong(packet.id)
            }, { buf ->
                PacketFunctionParticlesS2C(
                    ParticleBuilder.PACKET_CODEC.decode(buf),
                    buf.readString(),
                    Pair(buf.readDouble(), buf.readDouble()),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readLong()
                )
            }
        )

        fun init() {
            PayloadTypeRegistry.playS2C().register(PAYLOAD_ID, PACKET_CODEC)
        }
    }
    override fun getId(): CustomPayload.Id<out CustomPayload?> {
        return PAYLOAD_ID
    }
}