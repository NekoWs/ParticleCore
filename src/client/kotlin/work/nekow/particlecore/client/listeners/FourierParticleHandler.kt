package work.nekow.particlecore.client.listeners

import com.ezylang.evalex.data.EvaluationValue
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.util.math.Vec3d
import org.joml.Vector2d
import org.joml.times
import work.nekow.particlecore.client.particle.ParticleManager.Companion.addTickParticle
import work.nekow.particlecore.client.particle.ParticleSpawnData
import work.nekow.particlecore.math.FourierUtils
import work.nekow.particlecore.network.PacketFourierParticleS2C
import java.math.BigDecimal
import kotlin.math.floor
import kotlin.math.max

class FourierParticleHandler: ClientPlayNetworking.PlayPayloadHandler<PacketFourierParticleS2C> {
    override fun receive(
        payload: PacketFourierParticleS2C,
        context: ClientPlayNetworking.Context
    ) {
        fun v(number: Number): EvaluationValue {
            return EvaluationValue.numberValue(BigDecimal.valueOf(number.toDouble()))
        }
        val fscale = payload.fscale
        val frotate = payload.rotate
        val particle = payload.particle

        var scale = fscale.fscale

        val points = FourierUtils.fourierPoints(
            payload.terms,
            payload.duration,
            payload.timeStep,
            1.0
        )

        val center = particle.pos
        val args = hashMapOf(
            "cx" to v(center.x),
            "cy" to v(center.y),
            "cz" to v(center.z)
        )
        var delay = 0.0
        fun rotate(vec: Vector2d, rotate: Vec3d): Vec3d {
            return Vec3d(vec.x, 0.0, vec.y).rotateX(Math.toRadians(rotate.x).toFloat())
                .rotateY(Math.toRadians(rotate.y).toFloat())
                .rotateZ(Math.toRadians(rotate.z).toFloat())
        }
        val targetScale = fscale.fscaleTo
        val scaleSteps = fscale.fscaleSteps
        val scaleStep = (targetScale - scale) / scaleSteps
        var scaleTimes = 0

        var current = frotate.rotate
        val target = frotate.rotateTo
        val rotateDelay = max(frotate.rotateDelay, 0.0001)
        val direction = target.subtract(current)
            .normalize()
            .multiply(frotate.rotateDirection.toDouble())
        var rotateTimes = target.distanceTo(current) / frotate.rotateDirection
        var rotating = 0.0
        do {
            val i = floor(rotating).toInt()
            points.forEach { po ->
                val point = po.times(scale)

                val p = rotate(point, current)
                val next = rotate(
                    if (scaleTimes < scaleSteps)
                        po.times(scale + scaleStep)
                    else point,
                    if (frotate.rotateVelocity)
                        current.add(direction.multiply(1 / rotateDelay))
                    else current
                )
                val rotateVelocity = next.subtract(p)

                val pos = center.add(p)

                val velocity = particle.velocity
                val clone = particle.clone()
                    .pos(pos)
                    .velocity(velocity.add(rotateVelocity))
                val data = ParticleSpawnData.fromBuilder(clone, payload.id, args)

                addTickParticle(data, floor(delay).toInt() + payload.particleDelay + i)
                delay += payload.delay
            }
            if (scaleTimes < scaleSteps) {
                scale += scaleStep
                scaleTimes++
            }

            rotating += rotateDelay
            current = current.add(direction)
        } while ((rotateTimes--) > 0 || scaleTimes < scaleSteps)
    }
}