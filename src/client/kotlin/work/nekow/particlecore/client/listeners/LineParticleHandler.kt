package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.client.particle.ParticleSpawnData
import work.nekow.particlecore.network.PacketLineParticlesS2C
import work.nekow.particlecore.utils.ParticleBuilder
import kotlin.math.floor
import kotlin.math.round

class LineParticleHandler: ClientPlayNetworking.PlayPayloadHandler<PacketLineParticlesS2C> {
    override fun receive(
        payload: PacketLineParticlesS2C,
        context: ClientPlayNetworking.Context
    ) {
        line(
            payload.particle,
            payload.from,
            payload.to,
            payload.id,
            payload.step,
            payload.delay
        )
    }
    companion object {
        fun line(
            particle: ParticleBuilder,
            from: Vec3d,
            to: Vec3d,
            id: Long,
            step: Double = 0.1,
            delay: Double = 0.0
        ) {
            val distance = to.distanceTo(from)
            val direction = to.subtract(from).normalize()

            var pos = from
            var particleDelay = 0.0

            repeat(round(distance / step).toInt()) {
                ParticleManager.addTickParticle(
                    ParticleSpawnData.fromBuilder(particle, id),
                    floor(delay).toInt()
                )
                particleDelay += delay
                pos = pos.add(direction.multiply(step))
            }
        }
    }
}