package work.nekow.particlecore.utils

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

@Suppress("unused")
sealed interface ParticlePath {
    fun apply(t: Double): Vec3d

    enum class Axis {
        X, Y, Z
    }

    enum class PathTypes {
        Ellipse,
        Spiral,
        Lissajous,
        Empty
    }

    /**
     * 椭圆路径
     */
    data class EllipsePath(
        val a: Double = 1.0,
        val b: Double = 0.5,
        val axis: Axis = Axis.Y
    ): ParticlePath {
        override fun apply(t: Double): Vec3d {
            return when (axis) {
                Axis.Y -> Vec3d(cos(t) * a, 0.0, sin(t) * b)
                Axis.X -> Vec3d(0.0, cos(t) * a, sin(t) * b)
                Axis.Z -> Vec3d(cos(t) * a, sin(t) * b, 0.0)
            }
        }
    }

    /**
     * 螺旋路径
     */
    data class SpiralPath(
        val radius: Double = 1.0,
        val pitch: Double = 0.05
    ): ParticlePath {
        override fun apply(t: Double): Vec3d {
            return Vec3d(cos(t) * radius, t * pitch, sin(t) * radius)
        }
    }

    /**
     * 利萨如曲线路径
     */
    data class LissajousPath(
        val a: Double = 1.0,
        val b: Double = 1.0,
        val delta: Double = Math.PI / 2
    ): ParticlePath {
        override fun apply(t: Double): Vec3d {
            return Vec3d(sin(a * t), sin(b * t + delta), 0.0)
        }
    }

    class EmptyPath(): ParticlePath {
        override fun apply(t: Double): Vec3d = Vec3d.ZERO
    }

    companion object {
        operator fun Vec3d.plus(other: Vec3d) = Vec3d(x + other.x, y + other.y, z + other.z)
        operator fun Vec3d.times(scalar: Double) = Vec3d(x * scalar, y * scalar, z * scalar)

        fun getType(path: ParticlePath): PathTypes {
            return when (path) {
                is EllipsePath -> PathTypes.Ellipse
                is SpiralPath -> PathTypes.Spiral
                is LissajousPath -> PathTypes.Lissajous
                is EmptyPath -> PathTypes.Empty
            }
        }

        val PACKET_CODEC: PacketCodec<RegistryByteBuf, ParticlePath> = PacketCodec.of<RegistryByteBuf, ParticlePath>(
            { packet, buf ->
                val type = getType(packet)
                buf.writeString(type.name)
                when (type) {
                    PathTypes.Ellipse -> {
                        packet as EllipsePath
                        buf.writeDouble(packet.a)
                        buf.writeDouble(packet.b)
                        buf.writeString(packet.axis.name)
                    }
                    PathTypes.Spiral -> {
                        packet as SpiralPath
                        buf.writeDouble(packet.radius)
                        buf.writeDouble(packet.pitch)
                    }
                    PathTypes.Lissajous -> {
                        packet as LissajousPath
                        buf.writeDouble(packet.a)
                        buf.writeDouble(packet.b)
                        buf.writeDouble(packet.delta)
                    }

                    PathTypes.Empty -> {}
                }
            }, { buf ->
                val type = PathTypes.valueOf(buf.readString())
                println("Unpack type: ${type.name.uppercase()}")
                when (type) {
                    PathTypes.Ellipse -> {
                        EllipsePath(
                            a = buf.readDouble(),
                            b = buf.readDouble(),
                            axis = Axis.valueOf(buf.readString()),
                        )
                    }
                    PathTypes.Spiral -> {
                        SpiralPath(
                            radius = buf.readDouble(),
                            pitch = buf.readDouble(),
                        )
                    }
                    PathTypes.Lissajous -> {
                        LissajousPath(
                            a = buf.readDouble(),
                            b = buf.readDouble(),
                            delta = buf.readDouble()
                        )
                    }
                    PathTypes.Empty -> {
                        EmptyPath()
                    }
                }
            }
        )

        fun circle(radius: Double = 1.0, axis: Axis = Axis.Y) = EllipsePath(radius, radius, axis)
        fun ellipse(a: Double = 1.0, b: Double = 0.5, axis: Axis = Axis.Y) = EllipsePath(a, b, axis)
        fun spiral(radius: Double = 1.0, pitch: Double = 0.05) = SpiralPath(radius, pitch)
        fun lissajous(a: Double = 1.0, b: Double = 1.0, delta: Double = Math.PI / 2) = LissajousPath(a, b, delta)
    }
}