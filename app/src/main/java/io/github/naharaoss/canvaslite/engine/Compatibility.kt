package io.github.naharaoss.canvaslite.engine

import android.os.Build

data class Compatibility(
    val lowLatency: Status = Status.Unknown,
    val stylus: Accessory = Accessory.Unknown
) {
    enum class Status { Unknown, Good, Bad }
    enum class Accessory { Unknown, Included, Optional }

    companion object {
        fun lookup(brand: String = Build.BRAND, model: String = Build.MODEL): Compatibility = when (brand to model) {
            "samsung" to "SM-N975F" -> Compatibility(
                lowLatency = Status.Bad,
                stylus = Accessory.Included
            )
            else -> Compatibility()
        }
    }
}