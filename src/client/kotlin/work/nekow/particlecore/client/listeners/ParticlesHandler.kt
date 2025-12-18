package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.client.particle.ParticleSpawnData
import work.nekow.particlecore.network.ParticlesS2C
import kotlin.math.roundToInt

class ParticlesHandler: ClientPlayNetworking.PlayPayloadHandler<ParticlesS2C> {
    override fun receive(
        payload: ParticlesS2C,
        context: ClientPlayNetworking.Context
    ) {
        val particles = payload.particles
        var delay: Double = payload.delay.toDouble()
        particles.forEach { particle ->
            val data = ParticleSpawnData.fromBuilder(particle)
            repeat(particle.count) {
                ParticleManager.spawnParticle(data, delay.roundToInt())
            }
            delay += payload.particleDelay
        }
    }
}