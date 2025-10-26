package work.nekow.particlecore.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.FourierTerm
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.Particlecore.Companion.MOD_ID

class PacketFourierParticleS2C(
    val particle: ParticleBuilder,
    val duration: Double,
    val timeStep: Double,
    val length: Int,
    val terms: List<FourierTerm>,
    val id: Long,
    val delay: Double,
    val fscale: FPScale,
    val rotate: FPRotate,
    val particleDelay: Int = 0
): CustomPayload {
    companion object {
        val ID: Identifier = Identifier.of(MOD_ID, "fourier_particle")
        val PAYLOAD_ID = CustomPayload.Id<PacketFourierParticleS2C>(ID)
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, PacketFourierParticleS2C> = CustomPayload.codecOf(
            { packet, buf ->
                ParticleBuilder.PACKET_CODEC.encode(buf, packet.particle)
                buf.writeDouble(packet.duration)
                buf.writeDouble(packet.timeStep)
                buf.writeInt(packet.length)
                packet.terms.forEach { FourierTerm.PACKET_CODEC.encode(buf, it) }
                buf.writeLong(packet.id)
                buf.writeDouble(packet.delay)
                FPScale.PACKET_CODEC.encode(buf, packet.fscale)
                FPRotate.PACKET_CODEC.encode(buf, packet.rotate)
                buf.writeInt(packet.particleDelay)
            }, { buf ->
                var length: Int
                PacketFourierParticleS2C(
                    ParticleBuilder.PACKET_CODEC.decode(buf),
                    buf.readDouble(),
                    buf.readDouble(),
                    let { length = buf.readInt(); length },
                    ArrayList<FourierTerm>().let {
                        repeat(length) { i ->
                            it.add(FourierTerm.PACKET_CODEC.decode(buf))
                        }
                        it
                    },
                    buf.readLong(),
                    buf.readDouble(),
                    FPScale.PACKET_CODEC.decode(buf),
                    FPRotate.PACKET_CODEC.decode(buf),
                    buf.readInt()
                )
            }
        )
        fun init() {
            PayloadTypeRegistry.playS2C().register(PAYLOAD_ID, PACKET_CODEC)
        }
    }
    override fun getId(): CustomPayload.Id<out CustomPayload> {
        return PAYLOAD_ID
    }

    data class FPScale(
        val fscale: Double = 1.0,
        val fscaleTo: Double = 1.0,
        val fscaleSteps: Int = 1
    ) {
        companion object {
            val PACKET_CODEC: PacketCodec<RegistryByteBuf, FPScale> = PacketCodec.of(
                { packet, buf ->
                    buf.writeDouble(packet.fscale)
                    buf.writeDouble(packet.fscaleTo)
                    buf.writeInt(packet.fscaleSteps)
                }, { buf -> FPScale(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readInt()
                )}
            )
        }
    }
    data class FPRotate(
        val rotate: Vec3d = Vec3d(0.0, 0.0, 0.0),
        val rotateTo: Vec3d = Vec3d(0.0, 0.0, 0.0),
        val rotateDelay: Double = 0.0,
        val rotateVelocity: Boolean = true,
        val rotateDirection: Int = 1
    ) {
        companion object {
            val PACKET_CODEC: PacketCodec<RegistryByteBuf, FPRotate> = PacketCodec.of(
                { packet, buf ->
                    buf.writeVec3d(packet.rotate)
                    buf.writeVec3d(packet.rotateTo)
                    buf.writeDouble(packet.rotateDelay)
                    buf.writeBoolean(packet.rotateVelocity)
                    buf.writeInt(packet.rotateDirection)
                }, { buf -> FPRotate(
                    buf.readVec3d(),
                    buf.readVec3d(),
                    buf.readDouble(),
                    buf.readBoolean(),
                    buf.readInt()
                )}
            )
        }
    }
}