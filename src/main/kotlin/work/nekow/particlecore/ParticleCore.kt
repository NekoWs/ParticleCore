package work.nekow.particlecore

import net.fabricmc.api.ModInitializer
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.Vec3d
import work.nekow.particlecore.network.*
import work.nekow.particlecore.utils.ParticleUtils

class ParticleCore : ModInitializer {
    companion object {
        const val MOD_ID = "particlecore"

        /**
         * 获取 NbtCompound 中的 Vec3d
         *
         * @param key 键
         * @param defaultValue 默认值，若不存在该键则返回默认值
         * @return 获取到的 Vec3d
         * @throws Exception 获取失败
         */
        fun NbtCompound.getVec3d(key: String, defaultValue: Vec3d = Vec3d.ZERO): Vec3d {
            return this.getListOrEmpty(key).let {
                if (it.isEmpty) return defaultValue

                if (it.size != 3) throw Exception("failed to get $key because list size is not 3")
                Vec3d(
                    it[0].asDouble().get(),
                    it[1].asDouble().get(),
                    it[2].asDouble().get()
                )
            }
        }
    }

    override fun onInitialize() {
        PacketParticlesS2C.init()
        PacketMarkDeadS2C.init()
        PacketMoveParticleS2C.init()
        PacketVelocityParticleS2C.init()
        PacketRemoveTickParticlesS2C.init()
        PacketFunctionParticlesS2C.init()

        ParticleUtils.init()

        ParticleCommands.init()
    }
}
