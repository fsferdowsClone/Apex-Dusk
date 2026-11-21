package com.example.game.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        val log = GLES20.glGetShaderInfoLog(shader)
        GLES20.glDeleteShader(shader)
        throw RuntimeException("Shader compile error: $log")
    }
    return shader
}

private fun linkProgram(vertexSource: String, fragmentSource: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vs)
    GLES20.glAttachShader(program, fs)
    GLES20.glLinkProgram(program)
    val linked = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
    if (linked[0] == 0) {
        val log = GLES20.glGetProgramInfoLog(program)
        GLES20.glDeleteProgram(program)
        throw RuntimeException("Program link error: $log")
    }
    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)
    return program
}

private fun createFullscreenQuadBuffer(): FloatBuffer =
    ByteBuffer.allocateDirect(6 * 2 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    -1f, -1f,
                    1f, -1f,
                    1f, 1f,
                    -1f, -1f,
                    1f, 1f,
                    -1f, 1f,
                )
            )
            position(0)
        }

class SkyDomeShader {
    private val program = linkProgram(VERTEX_SRC, FRAGMENT_SRC)
    private val aPos = GLES20.glGetAttribLocation(program, "aPos")
    private val uAspect = GLES20.glGetUniformLocation(program, "uAspect")
    private val uTime = GLES20.glGetUniformLocation(program, "uTime")
    private val uCameraShiftX = GLES20.glGetUniformLocation(program, "uCameraShiftX")
    private val uMistIntensity = GLES20.glGetUniformLocation(program, "uMistIntensity")
    private val uRainIntensity = GLES20.glGetUniformLocation(program, "uRainIntensity")

    private val quadBuffer: FloatBuffer = createFullscreenQuadBuffer()

    fun draw(
        aspect: Float,
        timeSec: Float,
        cameraShiftX: Float,
        mistIntensity: Float,
        rainIntensity: Float,
    ) {
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uAspect, aspect)
        GLES20.glUniform1f(uTime, timeSec)
        GLES20.glUniform1f(uCameraShiftX, cameraShiftX)
        GLES20.glUniform1f(uMistIntensity, mistIntensity)
        GLES20.glUniform1f(uRainIntensity, rainIntensity)

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 8, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    companion object {
        private const val VERTEX_SRC = """
            attribute vec2 aPos;
            varying vec2 vNdc;
            void main() {
                vNdc = aPos;
                gl_Position = vec4(aPos, 0.9999, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;
            varying vec2 vNdc;
            uniform float uAspect;
            uniform float uTime;
            uniform float uCameraShiftX;
            uniform float uMistIntensity;
            uniform float uRainIntensity;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            void main() {
                float y = vNdc.y;
                // Dusk gradient stops
                vec3 zenith = vec3(0.035, 0.024, 0.11);
                vec3 midSky = vec3(0.165, 0.075, 0.28);
                vec3 magentaBand = vec3(0.54, 0.14, 0.34);
                vec3 horizonAmber = vec3(0.98, 0.45, 0.20);
                vec3 lowerMist = vec3(0.28, 0.10, 0.24);

                vec3 sky;
                if (y > 0.42) {
                    sky = mix(midSky, zenith, smoothstep(0.42, 1.0, y));
                } else if (y > 0.12) {
                    sky = mix(magentaBand, midSky, smoothstep(0.12, 0.42, y));
                } else if (y > -0.05) {
                    sky = mix(horizonAmber, magentaBand, smoothstep(-0.05, 0.12, y));
                } else {
                    sky = mix(lowerMist, horizonAmber, smoothstep(-0.45, -0.05, y));
                }

                // Glowing low-hanging sun (softened under mist/rain cloud cover)
                vec2 sunUv = vec2((vNdc.x + uCameraShiftX * 0.04) * uAspect, vNdc.y - 0.10);
                float dist = length(sunUv);

                float sunVisibility = clamp(1.0 - uRainIntensity * 0.58 - uMistIntensity * 0.28, 0.25, 1.0);
                float halo = (exp(-dist * 3.1) * 0.65 + exp(-dist * 8.5) * 0.55) * (1.0 - uRainIntensity * 0.35);
                vec3 sunHaloColor = mix(vec3(1.0, 0.35, 0.45), vec3(1.0, 0.78, 0.38), exp(-dist * 4.5));
                sky += sunHaloColor * halo;

                float sunDisc = (1.0 - smoothstep(0.115, 0.128 + uMistIntensity * 0.04, dist)) * sunVisibility;
                vec3 discColor = mix(vec3(1.0, 0.45, 0.22), vec3(1.0, 0.94, 0.72), smoothstep(-0.12, 0.12, sunUv.y));
                sky = mix(sky, discColor, sunDisc);

                // Subtle twilight stars near the zenith (fade out in rain/mist)
                float starVisibility = clamp(1.0 - uRainIntensity * 0.9 - uMistIntensity * 0.5, 0.0, 1.0);
                if (y > 0.28 && starVisibility > 0.05) {
                    vec2 starGrid = floor(vec2(vNdc.x * uAspect * 36.0, vNdc.y * 36.0));
                    float rnd = hash(starGrid);
                    if (rnd > 0.986) {
                        float twinkle = 0.55 + 0.45 * sin(uTime * 2.5 + rnd * 40.0);
                        float starFade = smoothstep(0.28, 0.75, y);
                        sky += vec3(0.85, 0.88, 1.0) * twinkle * starFade * 0.65 * starVisibility;
                    }
                }

                // Mist horizon veil
                if (uMistIntensity > 0.01) {
                    float mistBand = exp(-abs(y - 0.02) * 5.0);
                    vec3 mistTint = vec3(0.48, 0.29, 0.45);
                    sky = mix(sky, mistTint, mistBand * uMistIntensity * 0.58);
                }

                // Rain overcast storm tint
                if (uRainIntensity > 0.01) {
                    vec3 stormTint = vec3(0.16, 0.13, 0.26);
                    sky = mix(sky, stormTint, uRainIntensity * 0.44);
                }

                gl_FragColor = vec4(sky, 1.0);
            }
        """
    }
}

class RainOverlayShader {
    private val program = linkProgram(VERTEX_SRC, FRAGMENT_SRC)
    private val aPos = GLES20.glGetAttribLocation(program, "aPos")
    private val uTime = GLES20.glGetUniformLocation(program, "uTime")
    private val uAspect = GLES20.glGetUniformLocation(program, "uAspect")
    private val uRainIntensity = GLES20.glGetUniformLocation(program, "uRainIntensity")
    private val uWindLean = GLES20.glGetUniformLocation(program, "uWindLean")
    private val uSpeedFactor = GLES20.glGetUniformLocation(program, "uSpeedFactor")

    private val quadBuffer: FloatBuffer = createFullscreenQuadBuffer()

    fun draw(
        aspect: Float,
        timeSec: Float,
        rainIntensity: Float,
        windLean: Float,
        speedFactor: Float,
    ) {
        if (rainIntensity <= 0.01f) return
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uTime, timeSec)
        GLES20.glUniform1f(uAspect, aspect)
        GLES20.glUniform1f(uRainIntensity, rainIntensity)
        GLES20.glUniform1f(uWindLean, windLean)
        GLES20.glUniform1f(uSpeedFactor, speedFactor)

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 8, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    companion object {
        private const val VERTEX_SRC = """
            attribute vec2 aPos;
            varying vec2 vUv;
            void main() {
                vUv = aPos * 0.5 + 0.5;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;
            varying vec2 vUv;
            uniform float uTime;
            uniform float uAspect;
            uniform float uRainIntensity;
            uniform float uWindLean;
            uniform float uSpeedFactor;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float rainLayer(vec2 uv, float cols, float speed, float slant) {
                vec2 p = vec2(uv.x * uAspect * cols + uv.y * slant, uv.y * 5.2 + uTime * speed);
                vec2 cell = floor(p);
                vec2 f = fract(p);
                float rnd = hash(cell);
                if (rnd < 0.72) return 0.0;
                float xOff = fract(rnd * 13.7) * 0.7 + 0.15;
                float dx = abs(f.x - xOff);
                float streak = smoothstep(0.055, 0.0, dx) * smoothstep(0.0, 0.25, f.y) * smoothstep(1.0, 0.55, f.y);
                return streak * (0.45 + 0.55 * fract(rnd * 29.3));
            }

            void main() {
                float slant = 0.22 + uWindLean * 0.35;
                float speedBoost = 1.0 + uSpeedFactor * 0.85;
                float r1 = rainLayer(vUv, 26.0, 3.6 * speedBoost, slant);
                float r2 = rainLayer(vUv + vec2(0.37, 0.19), 42.0, 2.7 * speedBoost, slant * 1.15) * 0.65;

                float rainAlpha = clamp((r1 + r2) * uRainIntensity * 0.42, 0.0, 0.55);
                vec3 rainColor = mix(vec3(0.62, 0.76, 0.96), vec3(0.95, 0.68, 0.85), 1.0 - vUv.y);
                gl_FragColor = vec4(rainColor, rainAlpha);
            }
        """
    }
}

class DuskLitShader {
    private val program = linkProgram(VERTEX_SRC, FRAGMENT_SRC)

    val aPosition = GLES20.glGetAttribLocation(program, "aPosition")
    val aNormal = GLES20.glGetAttribLocation(program, "aNormal")
    val aColor = GLES20.glGetAttribLocation(program, "aColor")

    private val uMvpMatrix = GLES20.glGetUniformLocation(program, "uMvpMatrix")
    private val uModelMatrix = GLES20.glGetUniformLocation(program, "uModelMatrix")
    private val uViewPos = GLES20.glGetUniformLocation(program, "uViewPos")
    private val uTint = GLES20.glGetUniformLocation(program, "uTint")
    private val uEmissive = GLES20.glGetUniformLocation(program, "uEmissive")
    private val uAlpha = GLES20.glGetUniformLocation(program, "uAlpha")
    private val uFogColor = GLES20.glGetUniformLocation(program, "uFogColor")
    private val uFogDensity = GLES20.glGetUniformLocation(program, "uFogDensity")
    private val uWetness = GLES20.glGetUniformLocation(program, "uWetness")

    private var baseFogR = 0.62f
    private var baseFogG = 0.24f
    private var baseFogB = 0.36f
    private var baseFogDensity = 0.0046f

    fun use(
        viewX: Float,
        viewY: Float,
        viewZ: Float,
        fogR: Float,
        fogG: Float,
        fogB: Float,
        fogDensity: Float,
        wetness: Float,
    ) {
        baseFogR = fogR
        baseFogG = fogG
        baseFogB = fogB
        baseFogDensity = fogDensity

        GLES20.glUseProgram(program)
        GLES20.glUniform3f(uViewPos, viewX, viewY, viewZ)
        GLES20.glUniform3f(uFogColor, fogR, fogG, fogB)
        GLES20.glUniform1f(uFogDensity, fogDensity)
        GLES20.glUniform1f(uWetness, wetness)
    }

    fun setMaterial(
        mvpMatrix: FloatArray,
        modelMatrix: FloatArray,
        tintR: Float = 1f,
        tintG: Float = 1f,
        tintB: Float = 1f,
        emissive: Float = 0f,
        alpha: Float = 1f,
        fogDensityScale: Float = 1.0f,
    ) {
        GLES20.glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(uTint, tintR, tintG, tintB)
        GLES20.glUniform1f(uEmissive, emissive)
        GLES20.glUniform1f(uAlpha, alpha)
        GLES20.glUniform1f(uFogDensity, baseFogDensity * fogDensityScale)
    }

    companion object {
        private const val VERTEX_SRC = """
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec3 aColor;

            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;

            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec3 vColor;

            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vColor = aColor;
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;

            varying vec3 vWorldPos;
            varying vec3 vNormal;
            varying vec3 vColor;

            uniform vec3 uViewPos;
            uniform vec3 uTint;
            uniform float uEmissive;
            uniform float uAlpha;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform float uWetness;

            void main() {
                vec3 baseColor = vColor * uTint;
                vec3 N = normalize(vNormal);
                vec3 V = normalize(uViewPos - vWorldPos);

                // Warm sunset key light from low horizon
                vec3 L = normalize(vec3(0.18, 0.34, -0.92));
                float ndotl = max(dot(N, L), 0.0);

                // Cool fill light from overhead twilight
                float hemi = clamp(N.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 ambient = mix(vec3(0.14, 0.10, 0.24), vec3(0.32, 0.22, 0.42), hemi);
                vec3 sunDiffuse = vec3(1.0, 0.62, 0.36) * ndotl * (0.85 - uWetness * 0.18);

                // Specular dusk glint (intensified on wet rain-slicked surfaces)
                vec3 H = normalize(L + V);
                float shininess = mix(28.0, 56.0, uWetness);
                float specStrength = mix(0.42, 0.85, uWetness);
                float spec = pow(max(dot(N, H), 0.0), shininess) * specStrength;

                vec3 litColor = baseColor * (ambient + sunDiffuse) + vec3(1.0, 0.72, 0.56) * spec;
                vec3 finalColor = mix(litColor, baseColor * (1.0 + uEmissive * 0.6), clamp(uEmissive, 0.0, 1.0));

                float dist = length(uViewPos - vWorldPos);
                float fogFactor = 1.0 - exp(-pow(dist * uFogDensity, 2.0));
                finalColor = mix(finalColor, uFogColor, clamp(fogFactor, 0.0, 1.0));

                gl_FragColor = vec4(finalColor, uAlpha);
            }
        """
    }
}
