package work.nekow.particlecore.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import work.nekow.particlecore.client.listeners.FunctionParticlesHandler
import work.nekow.particlecore.client.listeners.ParticlesHandler
import work.nekow.particlecore.client.listeners.RemoveTickParticlesHandler
import work.nekow.particlecore.client.particle.ParticleManager
import work.nekow.particlecore.network.ClearDelayParticlesS2C
import work.nekow.particlecore.network.FunctionParticlesS2C
import work.nekow.particlecore.network.ParticlesS2C

class ParticlecoreClient : ClientModInitializer {
    companion object {
        val client: MinecraftClient = MinecraftClient.getInstance()
        const val MAX_PARTICLES = 16384 * 4
    }

    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ParticlesS2C.PAYLOAD_ID,
            ParticlesHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(ClearDelayParticlesS2C.PAYLOAD_ID,
            RemoveTickParticlesHandler()
        )
        ClientPlayNetworking.registerGlobalReceiver(FunctionParticlesS2C.PAYLOAD_ID,
            FunctionParticlesHandler()
        )

        ClientTickEvents.START_CLIENT_TICK.register { client ->
            ParticleManager.tick(client)
        }
        ClientTickEvents.START_WORLD_TICK.register { world ->
            ParticleManager.worldTick(world)
        }
    }
}
