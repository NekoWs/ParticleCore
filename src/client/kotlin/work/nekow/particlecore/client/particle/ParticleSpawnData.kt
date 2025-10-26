package work.nekow.particlecore.client.particle

import com.ezylang.evalex.data.EvaluationValue
import net.minecraft.particle.ParticleEffect
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.math.ParticleColor
import work.nekow.particlecore.utils.ParticleBuilder

data class ParticleSpawnData(
    val type: ParticleEffect,
    val pos: Vec3d,
    val velocity: Vec3d,
    val offset: Vec3d,
    val age: Int,
    val id: Long,
    val expression: String,
    val args: HashMap<String, EvaluationValue> = hashMapOf(),
    val color: ParticleColor = ParticleColor.UNSET,
    val scale: Double = 1.0
) {
    companion object {
        fun fromBuilder(
            builder: ParticleBuilder,
            id: Long,
            args: HashMap<String, EvaluationValue> = hashMapOf()
        ): ParticleSpawnData {
            return ParticleSpawnData(
                type = builder.type,
                pos = builder.pos,
                velocity = builder.velocity,
                offset = builder.offset,
                age = builder.age,
                id = id,
                expression = builder.expression.build(),
                args = args,
                color = builder.color,
                scale = builder.scale
            )
        }
    }
}