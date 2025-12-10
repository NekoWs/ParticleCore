package work.nekow.particlecore

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f

fun main() {
    val pos1 = Vector4f(1f, 1f, 1f, 1f)
    val pos2 = Vector4f(1f, 1f, 1f, 1f)

    val rx = Math.toRadians(45.0).toFloat()
    val ry = Math.toRadians(10.0).toFloat()

    val matrix = Matrix4f()
        .rotateX(rx)

    val rotate = matrix.getNormalizedRotation(Quaternionf())

//    val rotate = Quaternionf()
//        .rotateX(rx)
//        .rotateY(ry)

    val matrix2 = Matrix4f()
        .rotate(rotate)

//    println(pos1.mul(matrix))
//    println(pos2.mul(matrix2))
    println(matrix)
    println(matrix2)
}