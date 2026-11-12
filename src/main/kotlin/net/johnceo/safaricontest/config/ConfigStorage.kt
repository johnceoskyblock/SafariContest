package net.johnceo.safaricontest.config

import net.fabricmc.loader.api.FabricLoader
object ConfigStorage {
    val directory get() = FabricLoader.getInstance().configDir.resolve("safari-contest")
}
object ConfigManager {
    val honeyHives get() = ContestConfig.honeyHives
}