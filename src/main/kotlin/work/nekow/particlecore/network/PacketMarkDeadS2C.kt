package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import work.nekow.particlecore.ParticleCore.Companion.MOD_ID

class PacketMarkDeadS2C(
    val id: Long
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "mark_dead_id")
        val PAYLOAD_ID = CustomPayload.Id<PacketMarkDeadS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketMarkDeadS2C> = CustomPayload.codecOf(
            { packet, buf ->
                buf.writeLong(packet.id)
            }, { buf ->
                val id = buf.readLong()
                PacketMarkDeadS2C(id)
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