package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
data class ParticleColor(
    val red: Float,
    val green: Float,
    val blue: Float
) {
    constructor(color: Float) : this(color, color, color)

    fun fixed(): ParticleColor = ParticleColor(
        max(min(red, 255F), 0F),
        max(min(green, 255F), 0F),
        max(min(blue, 255F), 0F)
    )

    operator fun plus(other: ParticleColor): ParticleColor =
        ParticleColor(red + other.red, green + other.green, blue + other.blue)
    operator fun minus(other: ParticleColor): ParticleColor =
        ParticleColor(red - other.red, green - other.green, blue - other.blue)
    operator fun times(other: ParticleColor): ParticleColor =
        ParticleColor(red * other.red, green * other.green, blue * other.blue)
    operator fun div(other: ParticleColor): ParticleColor =
        ParticleColor(red / other.red, green / other.green, blue / other.blue)

    operator fun plus(num: Number): ParticleColor =
        ParticleColor(red + num.toFloat(), green + num.toFloat(), blue + num.toFloat())
    operator fun minus(num: Number): ParticleColor =
        ParticleColor(red - num.toFloat(), green - num.toFloat(), blue - num.toFloat())
    operator fun times(num: Number): ParticleColor =
        ParticleColor(red * num.toFloat(), green * num.toFloat(), blue * num.toFloat())
    operator fun div(num: Number): ParticleColor =
        ParticleColor(red / num.toFloat(), green / num.toFloat(), blue / num.toFloat())

    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticleColor> = PacketCodec.of(
            { packet, buf ->
                buf.writeFloat(packet.red)
                buf.writeFloat(packet.green)
                buf.writeFloat(packet.blue)
            }, { buf ->
                val red = buf.readFloat()
                val green = buf.readFloat()
                val blue = buf.readFloat()
                ParticleColor(red, green, blue)
            }
        )
        val UNSET = ParticleColor(-1f, -1f, -1f)

        /**
         * 获取线性渐变颜色列表
         *
         * @param a 起始颜色
         * @param b 目标颜色
         * @param steps 渐变步数
         *
         * @return 颜色列表，长度 = steps
         */
        fun linearGradient(a: ParticleColor, b: ParticleColor, steps: Int): List<ParticleColor> {
            val result = mutableListOf<ParticleColor>()
            for (i in 0 until steps) {
                result.add(a + (b - a) * i / steps)
            }
            return result
        }

        fun fromRGB(red: Int, green: Int, blue: Int): ParticleColor {
            return ParticleColor(red.toFloat(), green.toFloat(), blue.toFloat())
        }
        val BLACK = fromRGB(0, 0, 0)
        val DARK_BLUE = fromRGB(0, 0, 170)
        val DARK_GREEN = fromRGB(0, 170, 0)
        val DARK_AQUA = fromRGB(0, 170, 170)
        val DARK_RED = fromRGB(170, 0, 0)
        val DARK_PURPLE = fromRGB(170, 0, 170)
        val GOLD = fromRGB(255, 170, 0)
        val GRAY = fromRGB(170, 170, 170)
        val DARK_GRAY = fromRGB(85, 85, 85)
        val BLUE = fromRGB(85, 85, 255)
        val GREEN = fromRGB(85, 255, 85)
        val AQUA = fromRGB(85, 255, 255)
        val RED = fromRGB(255, 85, 85)
        val LIGHT_PURPLE = fromRGB(255, 85, 255)
        val YELLOW = fromRGB(255, 255, 85)
        val WHITE = fromRGB(255, 255, 255)
    }
}