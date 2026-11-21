package com.example.game.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class Mesh(private val floats: FloatArray) {
    private val vertexCount = floats.size / STRIDE_FLOATS
    private var vboId: Int = 0

    fun uploadIfNeeded() {
        if (vboId != 0) return
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        val buf = ByteBuffer.allocateDirect(floats.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(floats)
                position(0)
            }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, floats.size * 4, buf, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun draw(shader: DuskLitShader) {
        uploadIfNeeded()
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glEnableVertexAttribArray(shader.aPosition)
        GLES20.glEnableVertexAttribArray(shader.aNormal)
        GLES20.glEnableVertexAttribArray(shader.aColor)

        GLES20.glVertexAttribPointer(shader.aPosition, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0)
        GLES20.glVertexAttribPointer(shader.aNormal, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 3 * 4)
        GLES20.glVertexAttribPointer(shader.aColor, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 6 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(shader.aPosition)
        GLES20.glDisableVertexAttribArray(shader.aNormal)
        GLES20.glDisableVertexAttribArray(shader.aColor)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    companion object {
        private const val STRIDE_FLOATS = 9
        private const val STRIDE_BYTES = STRIDE_FLOATS * 4
    }
}

class MeshAccumulator {
    private val data = ArrayList<Float>(2048)

    fun addBox(
        cx: Float,
        cy: Float,
        cz: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        r: Float,
        g: Float,
        b: Float,
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        // Top (+Y)
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, r, g, b)
        // Bottom (-Y)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r * 0.7f, g * 0.7f, b * 0.7f)
        // Front (-Z)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r * 0.92f, g * 0.92f, b * 0.92f)
        // Back (+Z)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r * 0.95f, g * 0.95f, b * 0.95f)
        // Left (-X)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r * 0.85f, g * 0.85f, b * 0.85f)
        // Right (+X)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r * 0.85f, g * 0.85f, b * 0.85f)
    }

    fun addQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
    ) {
        addTri(x0, y0, z0, x1, y1, z1, x2, y2, z2, nx, ny, nz, r, g, b)
        addTri(x0, y0, z0, x2, y2, z2, x3, y3, z3, nx, ny, nz, r, g, b)
    }

    fun addTri(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
    ) {
        addVertex(x0, y0, z0, nx, ny, nz, r, g, b)
        addVertex(x1, y1, z1, nx, ny, nz, r, g, b)
        addVertex(x2, y2, z2, nx, ny, nz, r, g, b)
    }

    private fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b)
    }

    fun build(): Mesh = Mesh(data.toFloatArray())
}

object MeshFactory {

    const val SEGMENT_LENGTH = 20f

    fun buildRoadSegment(): Mesh {
        val acc = MeshAccumulator()
        val halfLen = SEGMENT_LENGTH * 0.5f

        // Ground shoulder terrain extending outward
        acc.addBox(-19f, -0.08f, 0f, 24f, 0.10f, SEGMENT_LENGTH, 0.08f, 0.05f, 0.14f)
        acc.addBox(19f, -0.08f, 0f, 24f, 0.10f, SEGMENT_LENGTH, 0.08f, 0.05f, 0.14f)

        // Main dark asphalt highway
        acc.addBox(0f, -0.04f, 0f, 13.4f, 0.08f, SEGMENT_LENGTH, 0.12f, 0.11f, 0.17f)

        // Subtle tire wear tracks per lane
        for (laneX in floatArrayOf(-4.5f, -1.5f, 1.5f, 4.5f)) {
            acc.addBox(laneX - 0.55f, 0.002f, 0f, 0.36f, 0.004f, SEGMENT_LENGTH, 0.09f, 0.08f, 0.14f)
            acc.addBox(laneX + 0.55f, 0.002f, 0f, 0.36f, 0.004f, SEGMENT_LENGTH, 0.09f, 0.08f, 0.14f)
        }

        // Outer amber-yellow edge lines
        acc.addBox(-6.05f, 0.008f, 0f, 0.16f, 0.01f, SEGMENT_LENGTH, 0.98f, 0.68f, 0.18f)
        acc.addBox(6.05f, 0.008f, 0f, 0.16f, 0.01f, SEGMENT_LENGTH, 0.98f, 0.68f, 0.18f)

        // Dashed lane markings (2 dashes per 20m segment on each of the 3 inner dividers)
        for (divX in floatArrayOf(-3.0f, 0.0f, 3.0f)) {
            val colR = if (divX == 0f) 0.98f else 0.90f
            val colG = if (divX == 0f) 0.72f else 0.90f
            val colB = if (divX == 0f) 0.24f else 0.96f
            acc.addBox(divX, 0.008f, -halfLen * 0.5f, 0.14f, 0.01f, 4.4f, colR, colG, colB)
            acc.addBox(divX, 0.008f, halfLen * 0.5f, 0.14f, 0.01f, 4.4f, colR, colG, colB)
        }

        // Reflective metallic guardrails left & right
        for (side in floatArrayOf(-6.65f, 6.65f)) {
            acc.addBox(side, 0.58f, 0f, 0.16f, 0.24f, SEGMENT_LENGTH, 0.72f, 0.75f, 0.84f)
            acc.addBox(side, 0.58f, 0f, 0.20f, 0.08f, SEGMENT_LENGTH, 0.92f, 0.65f, 0.42f)
            // Guardrail support posts every 5m
            for (pz in floatArrayOf(-7.5f, -2.5f, 2.5f, 7.5f)) {
                acc.addBox(side, 0.34f, pz, 0.14f, 0.68f, 0.14f, 0.45f, 0.46f, 0.55f)
            }
        }

        return acc.build()
    }

    fun buildStreetlampPole(): Mesh {
        val acc = MeshAccumulator()
        // Vertical pole
        acc.addBox(0f, 3.2f, 0f, 0.16f, 6.4f, 0.16f, 0.28f, 0.28f, 0.38f)
        // Base flange
        acc.addBox(0f, 0.15f, 0f, 0.34f, 0.30f, 0.34f, 0.22f, 0.22f, 0.30f)
        // Cantilever arm reaching toward road (+X direction when placed on left, flipped via scale on right)
        acc.addBox(1.1f, 6.35f, 0f, 2.3f, 0.14f, 0.14f, 0.30f, 0.30f, 0.40f)
        // Lamp housing
        acc.addBox(2.15f, 6.26f, 0f, 0.55f, 0.14f, 0.32f, 0.18f, 0.18f, 0.24f)
        return acc.build()
    }

    fun buildStreetlampBulb(): Mesh {
        val acc = MeshAccumulator()
        acc.addBox(2.15f, 6.17f, 0f, 0.45f, 0.08f, 0.24f, 1.0f, 0.74f, 0.32f)
        return acc.build()
    }

    fun buildLightPoolQuad(): Mesh {
        val acc = MeshAccumulator()
        acc.addQuad(
            -2.2f, 0.015f, 2.4f,
            2.2f, 0.015f, 2.4f,
            2.2f, 0.015f, -2.4f,
            -2.2f, 0.015f, -2.4f,
            0f, 1f, 0f,
            1.0f, 0.58f, 0.24f,
        )
        return acc.build()
    }

    fun buildSkylineCluster(): Mesh {
        val acc = MeshAccumulator()
        val heights = floatArrayOf(18f, 32f, 24f, 44f, 20f, 38f, 27f, 48f, 22f, 35f)
        val widths = floatArrayOf(7f, 9f, 6f, 10f, 8f, 9f, 7f, 11f, 6f, 8f)
        var cursorZ = -45f
        for (i in heights.indices) {
            val h = heights[i]
            val w = widths[i]
            acc.addBox(0f, h * 0.5f, cursorZ, w, h, w * 0.9f, 0.09f, 0.06f, 0.16f)
            cursorZ += w + 2.5f
        }
        return acc.build()
    }

    fun buildSkylineWindows(): Mesh {
        val acc = MeshAccumulator()
        val heights = floatArrayOf(18f, 32f, 24f, 44f, 20f, 38f, 27f, 48f, 22f, 35f)
        val widths = floatArrayOf(7f, 9f, 6f, 10f, 8f, 9f, 7f, 11f, 6f, 8f)
        var cursorZ = -45f
        for (i in heights.indices) {
            val h = heights[i]
            val w = widths[i]
            val isAmber = i % 2 == 0
            val r = if (isAmber) 1.0f else 0.25f
            val g = if (isAmber) 0.58f else 0.82f
            val b = if (isAmber) 0.25f else 1.0f
            var wy = 6f
            while (wy < h - 3f) {
                acc.addBox(0f, wy, cursorZ, w + 0.12f, 0.35f, w * 0.65f, r, g, b)
                wy += 4.2f
            }
            cursorZ += w + 2.5f
        }
        return acc.build()
    }

    fun buildBikeBodyPainted(): Mesh {
        val acc = MeshAccumulator()
        // Sculpted front aero nose & upper fairing
        acc.addBox(0f, 0.72f, -0.56f, 0.42f, 0.32f, 0.64f, 1f, 1f, 1f)
        // Side aerodynamic fairings
        acc.addBox(-0.22f, 0.50f, -0.20f, 0.10f, 0.36f, 0.78f, 0.95f, 0.95f, 0.95f)
        acc.addBox(0.22f, 0.50f, -0.20f, 0.10f, 0.36f, 0.78f, 0.95f, 0.95f, 0.95f)
        // Sculpted fuel tank
        acc.addBox(0f, 0.80f, -0.10f, 0.38f, 0.24f, 0.52f, 1f, 1f, 1f)
        // Rear upswept tail cowling
        acc.addBox(0f, 0.78f, 0.68f, 0.30f, 0.18f, 0.62f, 0.95f, 0.95f, 0.95f)
        // Front mudguard
        acc.addBox(0f, 0.48f, -0.78f, 0.18f, 0.10f, 0.34f, 0.9f, 0.9f, 0.9f)
        return acc.build()
    }

    fun buildBikeChassisAndRider(): Mesh {
        val acc = MeshAccumulator()
        // Frame & engine block
        acc.addBox(0f, 0.44f, 0.02f, 0.30f, 0.34f, 0.74f, 0.18f, 0.19f, 0.25f)
        // Front forks
        acc.addBox(-0.11f, 0.58f, -0.62f, 0.06f, 0.56f, 0.08f, 0.75f, 0.76f, 0.82f)
        acc.addBox(0.11f, 0.58f, -0.62f, 0.06f, 0.56f, 0.08f, 0.75f, 0.76f, 0.82f)
        // Dark tinted windscreen
        acc.addBox(0f, 0.94f, -0.58f, 0.32f, 0.22f, 0.30f, 0.08f, 0.10f, 0.18f)
        // Clip-on handlebars
        acc.addBox(0f, 0.88f, -0.44f, 0.68f, 0.05f, 0.06f, 0.22f, 0.22f, 0.28f)
        // Rider saddle
        acc.addBox(0f, 0.74f, 0.30f, 0.28f, 0.10f, 0.44f, 0.10f, 0.10f, 0.14f)
        // Twin angled exhaust cans
        acc.addBox(-0.18f, 0.44f, 0.68f, 0.09f, 0.10f, 0.56f, 0.62f, 0.65f, 0.74f)
        acc.addBox(0.18f, 0.44f, 0.68f, 0.09f, 0.10f, 0.56f, 0.62f, 0.65f, 0.74f)

        // === Leaning Sportbike Rider ===
        // Lower legs / boots
        acc.addBox(-0.20f, 0.54f, 0.16f, 0.12f, 0.34f, 0.18f, 0.14f, 0.14f, 0.18f)
        acc.addBox(0.20f, 0.54f, 0.16f, 0.12f, 0.34f, 0.18f, 0.14f, 0.14f, 0.18f)
        // Tucked rider torso (armored jacket)
        acc.addBox(0f, 1.04f, 0.10f, 0.38f, 0.34f, 0.52f, 0.16f, 0.15f, 0.22f)
        // Jacket racing spine stripe
        acc.addBox(0f, 1.19f, 0.14f, 0.12f, 0.08f, 0.44f, 0.92f, 0.92f, 0.98f)
        // Rider arms gripping bars
        acc.addBox(-0.23f, 0.95f, -0.22f, 0.10f, 0.11f, 0.42f, 0.18f, 0.17f, 0.24f)
        acc.addBox(0.23f, 0.95f, -0.22f, 0.10f, 0.11f, 0.42f, 0.18f, 0.17f, 0.24f)
        // Full-face racing helmet
        acc.addBox(0f, 1.30f, -0.14f, 0.27f, 0.27f, 0.31f, 0.94f, 0.94f, 0.98f)
        // Tinted visor
        acc.addBox(0f, 1.30f, -0.27f, 0.24f, 0.11f, 0.08f, 0.04f, 0.06f, 0.12f)
        return acc.build()
    }

    fun buildBikeTaillight(): Mesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0.78f, 0.99f, 0.24f, 0.06f, 0.05f, 1.0f, 0.10f, 0.18f)
        return acc.build()
    }

    fun buildBikeHeadlightCone(): Mesh {
        val acc = MeshAccumulator()
        // Translucent warm-white light fan on asphalt ahead of bike
        acc.addQuad(
            -0.28f, 0.04f, -1.0f,
            0.28f, 0.04f, -1.0f,
            1.85f, 0.04f, -12.5f,
            -1.85f, 0.04f, -12.5f,
            0f, 1f, 0f,
            1.0f, 0.92f, 0.76f,
        )
        return acc.build()
    }

    fun buildSpokedWheel(radius: Float = 0.34f, width: Float = 0.16f): Mesh {
        val acc = MeshAccumulator()
        val segments = 12
        val hw = width * 0.5f
        val innerR = radius * 0.66f

        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * Math.PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * Math.PI.toFloat())
            val y0 = cos(a0) * radius; val z0 = sin(a0) * radius
            val y1 = cos(a1) * radius; val z1 = sin(a1) * radius
            val iy0 = cos(a0) * innerR; val iz0 = sin(a0) * innerR
            val iy1 = cos(a1) * innerR; val iz1 = sin(a1) * innerR

            val ny = (y0 + y1) * 0.5f
            val nz = (z0 + z1) * 0.5f
            // Outer tire tread
            acc.addQuad(-hw, y0, z0, hw, y0, z0, hw, y1, z1, -hw, y1, z1, 0f, ny, nz, 0.08f, 0.08f, 0.11f)
            // Sidewalls (metallic rim ring)
            acc.addQuad(-hw, iy0, iz0, -hw, y0, z0, -hw, y1, z1, -hw, iy1, iz1, -1f, 0f, 0f, 0.20f, 0.22f, 0.28f)
            acc.addQuad(hw, iy1, iz1, hw, y1, z1, hw, y0, z0, hw, iy0, iz0, 1f, 0f, 0f, 0.20f, 0.22f, 0.28f)
        }

        // Hub
        acc.addBox(0f, 0f, 0f, width * 0.9f, radius * 0.28f, radius * 0.28f, 0.82f, 0.85f, 0.92f)
        // 3 crossing diameter spokes (= 6 radial spokes) so rotation is crisp and unmistakable!
        acc.addBox(0f, 0f, 0f, width * 0.65f, radius * 1.42f, 0.045f, 0.88f, 0.90f, 0.96f)
        acc.addBox(0f, 0f, 0f, width * 0.65f, 0.045f, radius * 1.42f, 0.88f, 0.90f, 0.96f)

        return acc.build()
    }

    fun buildNitroFlameCone(): Mesh {
        val acc = MeshAccumulator()
        // Tapered 4-sided exhaust flame extending toward +Z
        val len = 0.95f
        val r = 0.085f
        acc.addTri(-r, 0f, 0f, r, 0f, 0f, 0f, 0f, len, 0f, 1f, 0f, 0.20f, 0.88f, 1.0f)
        acc.addTri(r, 0f, 0f, -r, 0f, 0f, 0f, 0f, len, 0f, -1f, 0f, 1.0f, 0.56f, 0.12f)
        acc.addTri(0f, -r, 0f, 0f, r, 0f, 0f, 0f, len, 1f, 0f, 0f, 0.20f, 0.88f, 1.0f)
        acc.addTri(0f, r, 0f, 0f, -r, 0f, 0f, 0f, len, -1f, 0f, 0f, 1.0f, 0.56f, 0.12f)
        return acc.build()
    }

    fun buildSedanBody(): Mesh {
        val acc = MeshAccumulator()
        // Lower chassis
        acc.addBox(0f, 0.50f, 0f, 1.82f, 0.62f, 4.28f, 1f, 1f, 1f)
        // Cabin roof
        acc.addBox(0f, 1.04f, 0.12f, 1.50f, 0.52f, 2.15f, 0.94f, 0.94f, 0.96f)
        return acc.build()
    }

    fun buildSedanDetails(): Mesh {
        val acc = MeshAccumulator()
        // Dark tinted windows
        acc.addBox(0f, 1.04f, 0.12f, 1.54f, 0.36f, 1.92f, 0.08f, 0.10f, 0.18f)
        // Bumpers & 4 wheels
        acc.addBox(0f, 0.28f, 0f, 1.86f, 0.20f, 4.34f, 0.12f, 0.12f, 0.16f)
        for (wx in floatArrayOf(-0.84f, 0.84f)) {
            for (wz in floatArrayOf(-1.32f, 1.32f)) {
                acc.addBox(wx, 0.28f, wz, 0.22f, 0.54f, 0.54f, 0.08f, 0.08f, 0.10f)
            }
        }
        return acc.build()
    }

    fun buildSedanLights(): Mesh {
        val acc = MeshAccumulator()
        // Rear glowing red taillights (+Z)
        acc.addBox(-0.66f, 0.62f, 2.16f, 0.36f, 0.14f, 0.06f, 1.0f, 0.10f, 0.16f)
        acc.addBox(0.66f, 0.62f, 2.16f, 0.36f, 0.14f, 0.06f, 1.0f, 0.10f, 0.16f)
        // Front headlights (-Z)
        acc.addBox(-0.66f, 0.54f, -2.16f, 0.32f, 0.12f, 0.06f, 1.0f, 0.94f, 0.78f)
        acc.addBox(0.66f, 0.54f, -2.16f, 0.32f, 0.12f, 0.06f, 1.0f, 0.94f, 0.78f)
        return acc.build()
    }

    fun buildSportsCarBody(): Mesh {
        val acc = MeshAccumulator()
        // Wide low chassis
        acc.addBox(0f, 0.42f, 0f, 1.94f, 0.48f, 4.20f, 1f, 1f, 1f)
        // Sleek coupe canopy
        acc.addBox(0f, 0.84f, 0.18f, 1.48f, 0.40f, 1.85f, 0.96f, 0.96f, 0.98f)
        // Rear aerodynamic wing
        acc.addBox(0f, 0.92f, 1.86f, 1.78f, 0.08f, 0.30f, 0.14f, 0.14f, 0.18f)
        acc.addBox(-0.55f, 0.76f, 1.86f, 0.08f, 0.28f, 0.18f, 0.14f, 0.14f, 0.18f)
        acc.addBox(0.55f, 0.76f, 1.86f, 0.08f, 0.28f, 0.18f, 0.14f, 0.14f, 0.18f)
        return acc.build()
    }

    fun buildSportsCarDetails(): Mesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0.84f, 0.18f, 1.52f, 0.28f, 1.65f, 0.06f, 0.08f, 0.15f)
        for (wx in floatArrayOf(-0.90f, 0.90f)) {
            for (wz in floatArrayOf(-1.30f, 1.30f)) {
                acc.addBox(wx, 0.27f, wz, 0.24f, 0.54f, 0.54f, 0.07f, 0.07f, 0.10f)
            }
        }
        return acc.build()
    }

    fun buildSportsCarLights(): Mesh {
        val acc = MeshAccumulator()
        // Full-width cyber LED rear taillight bar
        acc.addBox(0f, 0.54f, 2.12f, 1.66f, 0.09f, 0.06f, 1.0f, 0.08f, 0.20f)
        // Front headlights
        acc.addBox(-0.68f, 0.46f, -2.12f, 0.36f, 0.10f, 0.06f, 0.92f, 0.98f, 1.0f)
        acc.addBox(0.68f, 0.46f, -2.12f, 0.36f, 0.10f, 0.06f, 0.92f, 0.98f, 1.0f)
        return acc.build()
    }

    fun buildTruckBody(): Mesh {
        val acc = MeshAccumulator()
        // Tall cargo box
        acc.addBox(0f, 1.48f, 0.65f, 2.22f, 2.15f, 4.95f, 0.92f, 0.92f, 0.96f)
        // Front cab
        acc.addBox(0f, 1.12f, -2.42f, 2.08f, 1.48f, 1.45f, 1f, 1f, 1f)
        return acc.build()
    }

    fun buildTruckDetails(): Mesh {
        val acc = MeshAccumulator()
        // Cab windshield
        acc.addBox(0f, 1.44f, -3.14f, 1.88f, 0.58f, 0.08f, 0.08f, 0.10f, 0.16f)
        // Chassis & wheels
        acc.addBox(0f, 0.36f, 0f, 2.04f, 0.24f, 6.50f, 0.14f, 0.14f, 0.18f)
        for (wx in floatArrayOf(-0.98f, 0.98f)) {
            for (wz in floatArrayOf(-2.25f, 1.25f, 2.25f)) {
                acc.addBox(wx, 0.38f, wz, 0.28f, 0.76f, 0.76f, 0.07f, 0.07f, 0.09f)
            }
        }
        return acc.build()
    }

    fun buildTruckLights(): Mesh {
        val acc = MeshAccumulator()
        // Rear bumper taillights + upper clearance markers
        acc.addBox(-0.82f, 0.55f, 3.16f, 0.36f, 0.18f, 0.06f, 1.0f, 0.10f, 0.14f)
        acc.addBox(0.82f, 0.55f, 3.16f, 0.36f, 0.18f, 0.06f, 1.0f, 0.10f, 0.14f)
        acc.addBox(-0.95f, 2.45f, 3.14f, 0.14f, 0.10f, 0.06f, 1.0f, 0.20f, 0.12f)
        acc.addBox(0.95f, 2.45f, 3.14f, 0.14f, 0.10f, 0.06f, 1.0f, 0.20f, 0.12f)
        // Front headlights
        acc.addBox(-0.78f, 0.62f, -3.16f, 0.34f, 0.16f, 0.06f, 1.0f, 0.92f, 0.76f)
        acc.addBox(0.78f, 0.62f, -3.16f, 0.34f, 0.16f, 0.06f, 1.0f, 0.92f, 0.76f)
        return acc.build()
    }

    fun buildUnitCube(): Mesh {
        val acc = MeshAccumulator()
        acc.addBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f)
        return acc.build()
    }
}
