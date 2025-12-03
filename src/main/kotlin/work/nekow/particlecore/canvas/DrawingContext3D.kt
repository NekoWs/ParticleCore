package work.nekow.particlecore.canvas

import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import work.nekow.particlecore.canvas.utils.Point3d
import work.nekow.particlecore.utils.ParticleBuilder
import kotlin.math.*

@Suppress("unused")
class DrawingContext3D {
    private val pointStyle = ParticleBuilder()
    private val points = mutableListOf<ParticleBuilder>()
    private var position = Point3d.ZERO
    private var matrix = Matrix4f()
    private val matrixStack = mutableListOf<Matrix4f>()

    // ====== 变换操作 ======

    fun pushMatrix(): DrawingContext3D {
        matrixStack.add(Matrix4f())
        matrix = Matrix4f()
        return this
    }

    fun popMatrix(): DrawingContext3D {
        if (matrixStack.isNotEmpty())
            matrix = matrixStack.removeLast()
        return this
    }

    fun translate(x: Double, y: Double, z: Double): DrawingContext3D {
        matrix.translate(x.toFloat(), y.toFloat(), z.toFloat())
        return this
    }

    fun translate(vec: Point3d): DrawingContext3D = translate(vec.x, vec.y, vec.z)

    fun scale(x: Double, y: Double, z: Double): DrawingContext3D {
        matrix.scale(x.toFloat(), y.toFloat(), z.toFloat())
        return this
    }

    fun scale(vec: Point3d): DrawingContext3D = scale(vec.x, vec.y, vec.z)

    fun rotateX(angle: Double): DrawingContext3D {
        matrix.rotateX(angle.toFloat())
        return this
    }

    fun rotateY(angle: Double): DrawingContext3D {
        matrix.rotateY(angle.toFloat())
        return this
    }

    fun rotateZ(angle: Double): DrawingContext3D {
        matrix.rotateZ(angle.toFloat())
        return this
    }

    fun rotate(axis: Vector3f, angle: Double): DrawingContext3D {
        matrix.rotate(angle.toFloat(), axis)
        return this
    }

    fun rotate(quaternion: Quaternionf): DrawingContext3D {
        matrix.rotate(quaternion)
        return this
    }

    fun lookAt(eye: Point3d, target: Point3d, up: Vector3f = Vector3f(0f, 1f, 0f)): DrawingContext3D {
        val eyeVec = Vector3f(eye.x.toFloat(), eye.y.toFloat(), eye.z.toFloat())
        val targetVec = Vector3f(target.x.toFloat(), target.y.toFloat(), target.z.toFloat())

        matrix = Matrix4f().lookAt(eyeVec, targetVec, up)
        return this
    }

    /**
     * 设置位置
     */
    fun setPosition(x: Double, y: Double, z: Double): DrawingContext3D {
        // 保存旋转和缩放，只重置平移
        val translation = Vector3f()
        val rotation = Quaternionf()
        val scale = Vector3f()

        matrix.getTranslation(translation)
        matrix.getUnnormalizedRotation(rotation)
        matrix.getScale(scale)

        matrix = Matrix4f()
            .translate(x.toFloat(), y.toFloat(), z.toFloat())
            .rotate(rotation)
            .scale(scale)

        return this
    }
    /**
     * 设置旋转
     */
    fun setRotation(pitch: Double, yaw: Double, roll: Double): DrawingContext3D {
        val translation = Vector3f()
        val scale = Vector3f()

        matrix.getTranslation(translation)
        matrix.getScale(scale)

        matrix = Matrix4f()
            .translate(translation)
            .rotateXYZ(pitch.toFloat(), yaw.toFloat(), roll.toFloat())
            .scale(scale)

        return this
    }

    /**
     * 设置缩放
     */
    fun setScale(x: Double, y: Double = x, z: Double = x): DrawingContext3D {
        val translation = Vector3f()
        val rotation = Quaternionf()

        matrix.getTranslation(translation)
        matrix.getUnnormalizedRotation(rotation)

        matrix = Matrix4f()
            .translate(translation)
            .rotate(rotation)
            .scale(x.toFloat(), y.toFloat(), z.toFloat())

        return this
    }

    // ====== 点操作 ======

    fun point(x: Double, y: Double, z: Double): DrawingContext3D {
        val vec = Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), 1.0f)
        vec.mul(matrix)
        val vec3d = Vec3d(vec.x.toDouble(), vec.y.toDouble(), vec.z.toDouble())
        points.add(pointStyle.clone().pos(vec3d))
        return this
    }

    fun point(point: Point3d): DrawingContext3D = point(point.x, point.y, point.z)

    // ====== 基础绘图 =====

    fun begin(): DrawingContext3D {
        points.clear()
        return this
    }

    fun moveTo(x: Double, y: Double, z: Double): DrawingContext3D {
        position = Point3d(x, y, z)
        return this
    }

    fun moveTo(point: Point3d): DrawingContext3D = moveTo(point.x, point.y, point.z)

    fun lineTo(point: Point3d, stepSize: Double = 0.1): DrawingContext3D {
        interpolateLine(position, point, stepSize).forEach {
            point(it)
        }
        position = point
        return this
    }

    fun lineTo(x: Double, y: Double, z: Double): DrawingContext3D {
        return lineTo(Point3d(x, y, z))
    }

    // ===== 绘制形状 =====

    /**
     * 设置点样式
     */
    fun setStyle(block: ParticleBuilder.() -> Unit): DrawingContext3D {
        pointStyle.apply(block)
        return this
    }

    /**
     * 绘制圆
     */
    fun circle(
        radius: Double,
        normal: Point3d = Point3d(0.0, 1.0, 0.0),
        up: Point3d = Point3d(0.0, 0.0, 1.0),
        segments: Int = 32,
        extent: Double = 2 * PI,
        startAngle: Double = 0.0,
    ): DrawingContext3D {
        val center = position

        val n = normal.normalize()
        val (u, v) = computePlaneBasis(n, up)

        for (i in 0..segments) {
            val t = i.toDouble() / segments
            val angle = startAngle + t * extent

            // 在圆平面上的坐标
            val localX = radius * cos(angle)
            val localZ = radius * sin(angle)

            // 转换到世界空间
            val worldPoint = Point3d(
                localX * u.x + localZ * v.x,
                localX * u.y + localZ * v.y,
                localX * u.z + localZ * v.z
            ) + center
            point(worldPoint)
        }
        return this
    }

    /**
     * 绘制圆环
     */
    fun torus(
        majorRadius: Double,
        minorRadius: Double,
        majorSegments: Int = 32,
        minorSegments: Int = 16
    ): DrawingContext3D {
        for (i in 0..majorSegments) {
            val majorAngle = 2 * Math.PI * i / majorSegments

            for (j in 0..minorSegments) {
                val minorAngle = 2 * Math.PI * j / minorSegments

                val x = (majorRadius + minorRadius * cos(minorAngle)) * cos(majorAngle)
                val y = minorRadius * sin(minorAngle)
                val z = (majorRadius + minorRadius * cos(minorAngle)) * sin(majorAngle)

                point(x, y, z)
            }
        }
        return this
    }

    /**
     * 绘制立方体
     */
    fun cube(size: Double, wireframe: Boolean = true, points: Int = 10): DrawingContext3D {
        val half = size / 2

        val vertices = listOf(
            Point3d(-half, -half, -half),
            Point3d(half, -half, -half),
            Point3d(half, half, -half),
            Point3d(-half, half, -half),
            Point3d(-half, -half, half),
            Point3d(half, -half, half),
            Point3d(half, half, half),
            Point3d(-half, half, half)
        )

        if (wireframe) {
            // 绘制12条边
            val edges = listOf(
                intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0), // 前面
                intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4), // 后面
                intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)  // 连接线
            )

            edges.forEach { edge ->
                val start = vertices[edge[0]]
                val end = vertices[edge[1]]

                Shapes3D.line(start, end, points)
            }
        } else {
            // 填充模式：添加所有顶点
            vertices.forEach { vertex ->
                point(vertex)
            }
        }

        return this
    }

    /**
     * 绘制圆柱
     */
    fun cylinder(
        radius: Double,
        height: Double,
        segments: Int = 32,
        caps: Boolean = true
    ): DrawingContext3D {
        // 侧面
        for (i in 0..segments) {
            val angle = 2 * Math.PI * i / segments
            val x = radius * cos(angle)
            val z = radius * sin(angle)
            
            // 底部点
            point(x, 0.0, z)
            // 顶部点
            point(x, height, z)

            // 侧面垂直线
            for (j in 0..3) {
                val y = height * j / 3.0
                point(x, y, z)
            }
        }
        // 顶部和底部圆盘
        if (caps) {
            pushMatrix()
            circle(radius, segments = segments)
            popMatrix()

            pushMatrix()
            translate(0.0, height, 0.0)
            circle(radius, segments = segments)
            popMatrix()
        }
        return this
    }
    
    /**
     * 绘制球体
     */
    fun sphere(
        radius: Double,
        rings: Int = 8,
        segments: Int = 16
    ): DrawingContext3D {
        for (i in 0..rings) {
            val phi = Math.PI * i / rings
            for (j in 0..segments) {
                val theta = 2 * Math.PI * j / segments

                val x = radius * sin(phi) * cos(theta)
                val y = radius * cos(phi)
                val z = radius * sin(phi) * sin(theta)

                point(x, y, z)
            }
        }
        return this
    }

    // ====== 贝塞尔曲线 ======

    fun bezierCurve(
        points: List<Point3d>,
        stepSize: Double = 0.1
    ): DrawingContext3D {
        Companion.bezierCurve(points, stepSize).forEach {
            point(it)
        }
        return this
    }

    fun build(): List<ParticleBuilder> = points.toList()

    fun clear(): DrawingContext3D {
        points.clear()
        return this
    }

    companion object {
        /**
         * 直线插值
         *
         * @param start 起点
         * @param end 终点
         * @param stepSize 精度
         */
        fun interpolateLine(
            start: Point3d,
            end: Point3d,
            stepSize: Double
        ): List<Point3d> {
            val distance = start.distanceTo(end)
            val steps = max(2, ceil(distance / stepSize).toInt())

            return (0 until steps).map { i ->
                val t = i.toDouble() / (steps - 1)
                start * (1 - t) + end * t
            }
        }

        /**
         * 多段线插值
         *
         * @param points 点列表
         * @param stepSize 精度
         * @param closed 是否闭合
         */
        fun interpolatePolyline(
            points: List<Point3d>,
            stepSize: Double,
            closed: Boolean = false
        ): List<Point3d> {
            val result = mutableListOf<Point3d>()

            val segments = if (closed) {
                points.indices.map { i ->
                    points[i] to points[(i + 1) % points.size]
                }
            } else {
                points.windowed(2) { (p1, p2) -> p1 to p2 }
            }

            segments.forEach { (p1, p2) ->
                val segmentPoints = interpolateLine(p1, p2, stepSize)
                result.addAll(segmentPoints.dropLast(1)) // 避免重复点
            }

            if (points.isNotEmpty()) {
                result.add(points.last())
            }

            return result
        }

        /**
         * 贝塞尔曲线
         *
         * @param controlPoints 控制点列表
         * @param stepSize 精度
         */
        fun bezierCurve(
            controlPoints: List<Point3d>,
            stepSize: Double
        ): List<Point3d> {
            if (controlPoints.size < 2) return emptyList()

            // 估算曲线长度
            val estimatedLength = controlPoints
                .windowed(2)
                .sumOf { (p1, p2) -> p1.distanceTo(p2) }

            val steps = max(10, ceil(estimatedLength / stepSize).toInt())

            return (0..steps).map { i ->
                val t = i.toDouble() / steps
                bezierPoint(controlPoints, t)
            }
        }

        private fun bezierPoint(
            controlPoints: List<Point3d>,
            t: Double
        ): Point3d {
            var result = Point3d(0.0, 0.0, 0.0)
            val n = controlPoints.size - 1

            for (i in controlPoints.indices) {
                val binomial = combination(n, i)
                val weight = binomial * t.pow(i) * (1 - t).pow(n - i)
                result += controlPoints[i] * weight
            }

            return result
        }

        private fun combination(n: Int, k: Int): Double {
            if (k !in 0..n) return 0.0
            var result = 1.0
            for (i in 1..k) {
                result = result * (n - k + i) / i
            }
            return result
        }

        fun computePlaneBasis(normal: Point3d, upHint: Point3d): Pair<Point3d, Point3d> {
            // 如果upHint与normal平行，需要选择一个不同的up
            var up = upHint
            if (abs(up.dot(normal)) > 0.99) {
                // 选择另一个向量作为up
                up = if (abs(normal.y) < 0.9)
                    Point3d(0.0, 1.0, 0.0)
                else Point3d(0.0, 0.0, 1.0)
            }

            // 计算第一个基向量（垂直于normal和up）
            val u = up.cross(normal).normalize()
            // 计算第二个基向量（垂直于normal和u）
            val v = normal.cross(u).normalize()

            return Pair(u, v)
        }
    }
}