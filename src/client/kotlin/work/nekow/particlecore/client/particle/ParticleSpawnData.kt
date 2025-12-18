package work.nekow.particlecore.client.particle

import com.ezylang.evalex.data.EvaluationValue
import net.minecraft.particle.ParticleEffect
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.utils.FinalValues
import work.nekow.particlecore.utils.ParticleBuilder
import work.nekow.particlecore.utils.ParticleRotation

data class ParticleSpawnData(
    val type: ParticleEffect,
    val pos: Vec3d,
    val velocity: Vec3d,
    val offset: Vec3d,
    val age: Int,
    val expression: String,
    val args: HashMap<String, EvaluationValue> = hashMapOf(),
    val rotation: ParticleRotation = ParticleRotation.identity(),
    val scale: Double = 1.0,
    val final: FinalValues = FinalValues.identity()
) {
    companion object {
        fun fromBuilder(
            builder: ParticleBuilder,
            args: HashMap<String, EvaluationValue> = hashMapOf()
        ): ParticleSpawnData {
            return ParticleSpawnData(
                type = builder.type,
                pos = builder.pos,
                velocity = builder.velocity,
                offset = builder.offset,
                age = builder.age,
                expression = builder.expression.build(),
                args = args,
                rotation = builder.rotation,
                scale = builder.scale,
                final = builder.final
            )
        }
    }
}