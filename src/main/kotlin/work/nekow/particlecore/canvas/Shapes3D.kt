package work.nekow.particlecore.canvas

import work.nekow.particlecore.canvas.utils.Point3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Suppress("unused")
object Shapes3D {
    fun draw(block: DrawingContext3D.() -> Unit): List<Point3d> {
        return DrawingContext3D().apply(block).build()
    }

    fun createCircle(
        radius: Double,
        normal: Point3d = Point3d(0.0, 1.0, 0.0),
        center: Point3d = Point3d.ZERO,
        segments: Int = 32,
        extent: Double = 2 * Math.PI,
        startAngle: Double = 0.0
    ): List<Point3d> = draw {
        translate(center)
        circle(radius, normal, segments = segments, extent = extent, startAngle = startAngle)
    }

    fun createArcOnPlane(
        center: Point3d,
        radius: Double,
        planeNormal: Point3d,
        upDirection: Point3d,
        startAngle: Double = 0.0,
        extent: Double = Math.PI / 2,
        segments: Int = 16
    ): List<Point3d> = draw {
        translate(center)
        circle(
            radius = radius,
            normal = planeNormal,
            up = upDirection,
            segments = segments,
            extent = extent,
            startAngle = startAngle
        )
    }

    fun createOrbitPath(
        center: Point3d,
        radius: Double,
        orbitNormal: Point3d,
        startAngle: Double = 0.0,
        revolutions: Double = 1.0,
        pointsPerRevolution: Int = 32
    ): List<Point3d> = draw {
        val totalPoints = (pointsPerRevolution * revolutions).toInt()
        val (u, v) = computePlaneBasis(orbitNormal, Point3d(0.0, 0.0, 1.0))

        for (i in 0..totalPoints) {
            val t = i.toDouble() / totalPoints
            val angle = startAngle + t * 2 * Math.PI * revolutions

            val localX = radius * cos(angle)
            val localZ = radius * sin(angle)

            val point = Point3d(
                center.x + localX * u.x + localZ * v.x,
                center.y + localX * u.y + localZ * v.y,
                center.z + localX * u.z + localZ * v.z
            )

            addPoint(point)
        }
    }

    private fun computePlaneBasis(normal: Point3d, upHint: Point3d): Pair<Point3d, Point3d> {
        var up = upHint
        if (abs(up.dot(normal)) > 0.99) {
            up = if (abs(normal.y) < 0.9) Point3d(0.0, 1.0, 0.0) else Point3d(1.0, 0.0, 0.0)
        }

        val u = up.cross(normal).normalize()
        val v = normal.cross(u).normalize()

        return Pair(u, v)
    }
}