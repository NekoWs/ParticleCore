package work.nekow.particlecore.canvas.utils

enum class FillMethod3D {
    FACE_BY_FACE,  // 逐个面填充
    VOXEL,         // 体素化填充
    CONTOUR        // 轮廓切片填充
}