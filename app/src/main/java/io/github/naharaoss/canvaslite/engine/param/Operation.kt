package io.github.naharaoss.canvaslite.engine.param

enum class Operation {
    Add { override fun apply(src: Float, value: Float): Float = src + value },
    Subtract { override fun apply(src: Float, value: Float): Float = src - value },
    Multiply { override fun apply(src: Float, value: Float): Float = src * value },
    Divide { override fun apply(src: Float, value: Float): Float = src / value };

    abstract fun apply(src: Float, value: Float): Float
}