package com.xplay.player.update

data class OtaVersionData(
    val version: Int = 0,
    val version_name: String? = null,
    val enforce: Int = 0,
    val url: String? = null,
    val package_name: String? = null,
    val remark: String? = null
)

