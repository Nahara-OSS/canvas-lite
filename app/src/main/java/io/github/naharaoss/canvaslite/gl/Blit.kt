package io.github.naharaoss.canvaslite.gl

import android.opengl.GLES20
import androidx.compose.ui.graphics.Matrix

/**
 * Blit entire texture to framebuffer.
 */
class Blit : AutoCloseable {
    private val vertex = Shader(
        label = "Vertex shader",
        type = Shader.Type.Vertex,
        source = """
            #version 320 es
            precision mediump float;
            layout(location = 0) uniform mat4 transform;
            layout(location = 0) out vec2 uv;
            
            const vec4 positions[] = vec4[](
                vec4(-1,  1, 0, 0),
                vec4( 1,  1, 1, 0),
                vec4(-1, -1, 0, 1),
                vec4( 1, -1, 1, 1)
            );
            
            void main() {
                gl_Position = transform * vec4(positions[gl_VertexID].xy, 0, 1);
                uv = positions[gl_VertexID].zw;
            }
        """.trimIndent()
    )
    private val fragment = Shader(
        label = "Fragment shader",
        type = Shader.Type.Fragment,
        source = """
            #version 320 es
            precision mediump float;
            layout(location = 1) uniform sampler2D tex;
            layout(location = 0) in vec2 uv;
            layout(location = 0) out vec4 color;
            
            void main() {
                color = texture(tex, uv);
            }
        """.trimIndent()
    )
    private val program = Program(vertex, fragment, label = "Blit")

    fun blit(
        texture: Texture,
        transform: Matrix = Matrix()
    ) {
        texture.bind(Texture.BindTarget.Texture2D, unit = 0).use {
            program.use()
            GLES20.glUniformMatrix4fv(0, 1, false, transform.values, 0)
            GLES20.glUniform1i(1, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        }
    }

    override fun close() {
        program.close()
        vertex.close()
        fragment.close()
    }
}