package work.nekow.particlecore.math

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import kotlin.math.max
import kotlin.math.min

data class ParticleColor(
    val red: Float,
    val green: Float,
    val blue: Float
) {
    constructor(color: Float) : this(color, color, color)

    fun subtract(red: Float, green: Float, blue: Float): ParticleColor {
        return ParticleColor(
            max(red - red, 0F),
            max(green - green, 0F),
            max(blue - blue, 0F)
        )
    }

    fun plus(red: Float, green: Float, blue: Float): ParticleColor {
        return ParticleColor(
            min(red + red, 255F),
            min(green + green, 255F),
            min(blue + blue, 255F)
        )
    }

    companion object {
        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticleColor> = PacketCodec.of<RegistryByteBuf, ParticleColor>(
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