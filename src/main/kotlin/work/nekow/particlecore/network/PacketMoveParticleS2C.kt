package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.ParticleCore.Companion.MOD_ID

class PacketMoveParticleS2C(
    val id: Long,
    val vec: Vec3d
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "move_particle")
        val PAYLOAD_ID = CustomPayload.Id<PacketMoveParticleS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketMoveParticleS2C> = PacketCodec.of(
            { packet, buf ->
                buf.writeLong(packet.id)
                buf.writeVec3d(packet.vec)
            }, { buf ->
                val id = buf.readLong()
                val vec = buf.readVec3d()
                PacketMoveParticleS2C(id, vec)
            }
        )

        fun init() {
            PayloadTypeRegistry.playS2C().register(PAYLOAD_ID, PACKET_CODEC)
        }
    }

    override fun getId(): CustomPayload.Id<out CustomPayload?>? {
        return PAYLOAD_ID
    }
}