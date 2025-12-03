package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.Point3d
import work.nekow.particlecore.math.ParticleColor

@Suppress("unused")
class ShapeBuilder3D {
    private val context = DrawingContext3D()

    infix fun at(position: Point3d): ShapeBuilder3D {
        context.translate(position)
        return this
    }

    infix fun withRadius(radius: Double): CircleBuilder = CircleBuilder(context, radius)
    infix fun withSize(size: Double): CubeBuilder = CubeBuilder(context, size)
    infix fun withColor(color: ParticleColor): ShapeBuilder3D {
        context.color(color)
        return this
    }

    fun build(): List<Point3d> = context.build()

    class CircleBuilder(private val context: DrawingContext3D, private val radius: Double) {
        infix fun inDirection(normal: Point3d): CircleBuilder2 {
            return CircleBuilder2(context, radius, normal)
        }

        fun build(): List<Point3d> {
            context.circle(radius)
            return context.build()
        }
    }

    class CircleBuilder2(
        private val context: DrawingContext3D,
        private val radius: Double,
        private val normal: Point3d
    ) {
        infix fun segments(count: Int): CircleBuilder3 {
            return CircleBuilder3(context, radius, normal, count)
        }

        fun build(): List<Point3d> {
            context.circle(radius, normal)
            return context.build()
        }
    }

    class CircleBuilder3(
        private val context: DrawingContext3D,
        private val radius: Double,
        private val normal: Point3d,
        private val segments: Int
    ) {
        infix fun extent(angle: Double): CircleBuilder4 {
            return CircleBuilder4(context, radius, normal, segments, angle)
        }

        fun build(): List<Point3d> {
            context.circle(radius, normal, segments = segments)
            return context.build()
        }
    }

    class CircleBuilder4(
        private val context: DrawingContext3D,
        private val radius: Double,
        private val normal: Point3d,
        private val segments: Int,
        private val extent: Double
    ) {
        fun build(): List<Point3d> {
            context.circle(radius, normal, segments = segments, extent = extent)
            return context.build()
        }
    }

    class CubeBuilder(private val context: DrawingContext3D, private val size: Double) {
        fun build(): List<Point3d> {
            context.cube(size)
            return context.build()
        }
    }
}