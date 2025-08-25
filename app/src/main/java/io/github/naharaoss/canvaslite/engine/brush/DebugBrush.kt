package io.github.naharaoss.canvaslite.engine.brush

import android.opengl.GLES20
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import io.github.naharaoss.canvaslite.engine.Blending
import io.github.naharaoss.canvaslite.engine.PenInput
import io.github.naharaoss.canvaslite.engine.lerp
import io.github.naharaoss.canvaslite.gl.Program
import io.github.naharaoss.canvaslite.gl.Shader
import io.github.naharaoss.canvaslite.gpu.GPUBlending
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

class DebugBrush : Brush {
    override fun createGPUInstance() = object : Brush.GPUBrushInstance {
        override val brush = this@DebugBrush
        private val vertex = Shader(
            label = "Vertex shader",
            type = Shader.Type.Vertex,
            source = """
                precision mediump float;
                uniform mat4 boardToClip;
                attribute vec3 data;
                
                void main() {
                    gl_Position = boardToClip * vec4(data.xy, 0, 1);
                    gl_PointSize = data.z;
                }
            """.trimIndent()
        )
        private val fragment = Shader(
            label = "Fragment shader",
            type = Shader.Type.Fragment,
            source = """
                precision mediump float;
                uniform vec4 color;
                
                void main() {
                    gl_FragColor = color;
                }
            """.trimIndent()
        )
        private val program = Program(vertex, fragment, label = "Debug brush program")
        private val boardToClip = program.uniform("boardToClip")
        private val color = program.uniform("color")
        private val data = program.attribute("data")
        private var inputBuffer = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder())

        override fun draw(
            inputs: List<PenInput>,
            boardToClip: Matrix,
            color: Color,
            blending: Blending
        ) {
            var counter = 0

            fun consumeInput(input: PenInput) {
                if (inputBuffer.remaining() < 12) {
                    val newCap = inputBuffer.capacity() * 2
                    val newBuf = ByteBuffer.allocateDirect(newCap).order(ByteOrder.nativeOrder())
                    inputBuffer.flip()
                    newBuf.put(inputBuffer)
                    inputBuffer = newBuf
                }

                inputBuffer.putFloat(input.x)
                inputBuffer.putFloat(input.y)
                inputBuffer.putFloat(input.pressure.pow(2f) * 50f)
                counter++
            }

            inputBuffer.clear()
            var lastInput: PenInput? = null

            for (input in inputs) {
                val last = lastInput

                if (last == null) {
                    consumeInput(input)
                    lastInput = input
                } else {
                    val distance = last distanceTo input
                    val spacing = 1f
                    var travelled = 0f

                    while (travelled < distance) {
                        val interpolated = lerp(last, input, travelled / distance)
                        consumeInput(interpolated)
                        travelled += spacing
                        lastInput = interpolated
                    }
                }
            }

            inputBuffer.flip()
            program.use()

            GPUBlending.enable {
                blending.gpu.applyBlending()

                this.data?.enable().use {
                    this.boardToClip?.matrix(boardToClip)
                    this.color?.uniform4f(color.red, color.green, color.blue, color.alpha)
                    this.data?.setPointer(type = Program.AttribType.Float32x3, src = inputBuffer)
                    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, counter)
                }
            }

            GLES20.glUseProgram(0)
        }

        override fun calculateBox(inputs: List<PenInput>) = super.calculateBox(inputs)?.inflate(50f)

        override fun close() {
            program.close()
            vertex.close()
            fragment.close()
        }
    }
}