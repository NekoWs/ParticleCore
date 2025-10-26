package work.nekow.particlecore.client.listeners

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import work.nekow.particlecore.client.particle.ParticleManager.Companion.addParticle
import work.nekow.particlecore.client.particle.ParticleSpawnData
import work.nekow.particlecore.network.PacketParticleS2C

class PacketParticleHandler: ClientPlayNetworking.PlayPayloadHandler<PacketParticleS2C> {
    override fun receive(
        payload: PacketParticleS2C,
        context: ClientPlayNetworking.Context
    ) {
        val particle = payload.particle
        repeat(particle.count) {
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
            addParticle(data)
        }
    }
}