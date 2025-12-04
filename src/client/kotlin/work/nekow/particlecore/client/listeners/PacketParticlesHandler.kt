package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.client.particle.ParticleSpawnData
import work.nekow.particlecore.network.PacketParticlesS2C
import kotlin.math.roundToInt

class PacketParticlesHandler: ClientPlayNetworking.PlayPayloadHandler<PacketParticlesS2C> {
    override fun receive(
        payload: PacketParticlesS2C,
        context: ClientPlayNetworking.Context
    ) {
        val particles = payload.particles
        var delay: Double = payload.delay.toDouble()
        particles.forEach { particle ->
            val data = ParticleSpawnData(
                type = particle.type,
                pos = particle.pos,
                velocity = particle.velocity,
                offset = particle.offset,
                age = particle.age,
                id = payload.id,
                expression = particle.expression.build(),
                color = particle.color,
                scale = particle.scale
            )
            repeat(particle.count) {
                ParticleManager.addTickParticle(data, delay.roundToInt())
            }
            delay += payload.particleDelay
        }
    }
}