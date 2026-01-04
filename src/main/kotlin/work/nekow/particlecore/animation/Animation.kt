package work.nekow.particlecore.animation

import kotlinx.serialization.Serializable
import net.minecraft.particle.SimpleParticleType
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d

@Serializable
class Animation {
    @Serializable
    data class Timing(
        val duration: Int,
        val groups: ArrayList<Group>
    ) {
    }
    @Serializable
    data class Group(
        val name: String,
        val particles: ArrayList<Particle>
    ) {
    }
    @Serializable
    data class Particle(
        val type: String
    ) {
        @Transient val effect = Registries.PARTICLE_TYPE.get(Identifier.of(type))
        init {
            if (effect !is SimpleParticleType) {
                TODO("非 SimpleParticleType 支持")
            }

        }
        @Serializable
        data class Status(
            val velocity: Vec3d? = null,
            val pos: Vec3d? = null,

        )
        @Serializable
        data class Timing(
            val duration: Int,
            val status: Status,
        )
    }
}