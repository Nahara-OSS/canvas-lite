package io.github.naharaoss.canvaslite.engine

import android.os.Build
import kotlinx.serialization.Serializable

@Serializable
data class Preferences(
    val general: General = General(),
    val layout: Layout = Layout(),
    val graphics: Graphics = Graphics(),
) {
    @Serializable
    data class General(
        val touchDrawing: Boolean = true,
    )

    @Serializable
    data class Layout(
        val pin: Boolean = false,
        val leftHand: Boolean = false
    )

    @Serializable
    data class Graphics(
        /**
         * Low latency canvas rendering mode, which may helps reducing "lags" while drawing or
         * sketching (especially when the action is fast). May introduces graphical glitches on some
         * devices. The following devices are tested:
         *
         * | Model    | Brand   | Name           | Graphical glitches |
         * |----------|---------|----------------|--------------------|
         * | SM-N975F | Samsung | Galaxy Note10+ | **Yes**            |
         *
         * Low latency rendering mode only available on Android Q and higher.
         */
        val lowLatency: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    )

    companion object {
        fun createDefaults(): Preferences {
            val compat = Compatibility.lookup()

            return Preferences(
                general = General(
                    touchDrawing = compat.stylus != Compatibility.Accessory.Included
                ),
                graphics = Graphics(
                    lowLatency = when (compat.lowLatency) {
                        Compatibility.Status.Unknown -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        Compatibility.Status.Bad -> false
                        Compatibility.Status.Good -> true
                    }
                )
            )
        }
    }
}