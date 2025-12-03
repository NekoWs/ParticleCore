package work.nekow.particlecore.canvas

import org.joml.Vector3f
import work.nekow.particlecore.canvas.utils.Point3d
import work.nekow.particlecore.utils.ParticleBuilder
import kotlin.math.abs

@Suppress("unused")
object Shapes3D {
    fun draw(block: DrawingContext3D.() -> Unit): List<ParticleBuilder> {
        return DrawingContext3D().apply(block).build()
    }

    // 便捷的工厂方法

    fun circle(
        center: Point3d = Point3d.ZERO,
        radius: Double,
        normal: Point3d = Point3d(0.0, 1.0, 0.0),
        segments: Int = 32,
        extent: Double = 2 * Math.PI,
        startAngle: Double = 0.0
    ): List<ParticleBuilder> = draw {
        translate(center)
        circle(radius, normal, segments = segments, extent = extent, startAngle = startAngle)
    }

    fun sphere(
        center: Point3d = Point3d.ZERO,
        radius: Double,
        rings: Int = 8,
        segments: Int = 16
    ): List<ParticleBuilder> = draw {
        translate(center)
        sphere(radius, rings, segments)
    }

    fun cube(
        center: Point3d = Point3d.ZERO,
        size: Double,
        wireframe: Boolean = true
    ): List<ParticleBuilder> = draw {
        translate(center)
        cube(size, wireframe)
    }

    fun cylinder(
        center: Point3d = Point3d.ZERO,
        radius: Double,
        height: Double,
        segments: Int = 32,
        caps: Boolean = true
    ): List<ParticleBuilder> = draw {
        translate(center)
        cylinder(radius, height, segments, caps)
    }

    fun line(start: Point3d, end: Point3d, points: Int = 10): List<ParticleBuilder> = draw {
        for (i in 0..points) {
            val t = i.toDouble() / points
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            val z = start.z + (end.z - start.z) * t
            point(x, y, z)
        }
    }

    fun grid(
        center: Point3d = Point3d.ZERO,
        size: Double,
        cells: Int,
        normal: Point3d = Point3d(0.0, 1.0, 0.0)
    ): List<ParticleBuilder> = draw {
        translate(center)

        val halfSize = size / 2
        val cellSize = size / cells

        // 计算平面基向量
        val n = Vector3f(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).normalize()
        val up = if (abs(n.y) < 0.9f) Vector3f(0f, 1f, 0f) else Vector3f(0f, 0f, 1f)
        val u = up.cross(n, Vector3f()).normalize()
        val v = n.cross(u, Vector3f()).normalize()

        // 绘制网格线
        for (i in -cells..cells) {
            val offset = halfSize * i / cells

            // u方向线
            for (j in 0..10) {
                val t = j / 10.0
                val pointU = offset * u.x + (-halfSize + t * size) * v.x
                val pointV = offset * u.y + (-halfSize + t * size) * v.y
                val pointW = offset * u.z + (-halfSize + t * size) * v.z
                point(pointU, pointV, pointW)
            }

            // v方向线
            for (j in 0..10) {
                val t = j / 10.0
                val pointU = (-halfSize + t * size) * u.x + offset * v.x
                val pointV = (-halfSize + t * size) * u.y + offset * v.y
                val pointW = (-halfSize + t * size) * u.z + offset * v.z
                point(pointU, pointV, pointW)
            }
        }
    }
}