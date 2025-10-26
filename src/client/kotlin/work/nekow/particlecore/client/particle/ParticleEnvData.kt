package work.nekow.particlecore.client.particle

import net.minecraft.util.math.Vec3d

data class ParticleEnvData(
    var velocity: Vec3d = Vec3d.ZERO,
    var position: Vec3d = Vec3d.ZERO,
    var red: Float = 1F,
    var green: Float = 1F,
    var blue: Float = 1F,
    var alpha: Float = 1F,
    var angle: Float = 0F,
    var prefix: ArrayList<String> = ArrayList(),
    var light: Int = -1,
    var gravity: Float = 0F,
    var scale: Float = 1F,
) {
    fun clone(): ParticleEnvData {
        return ParticleEnvData(
            velocity = velocity,
            position = position,
            red = red,
            green = green,
            blue = blue,
            alpha = alpha,
            angle = angle,
            prefix = prefix,
            light = light,
            gravity = gravity,
            scale = scale,
        )
    }
}
