package com.example.game.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.audio.DuskAudioEngine
import com.example.game.engine.GameSimulation
import com.example.game.engine.SimulationEvents
import com.example.game.model.GamePhase
import com.example.game.model.GameUiState
import com.example.game.model.InputState
import com.example.game.model.VehicleType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin
import kotlin.random.Random

class ApexDuskRenderer(
    private val simulation: GameSimulation,
    private val audioEngine: DuskAudioEngine,
    private val simulationEvents: SimulationEvents,
    private val onUiStateSnapshot: (GameUiState) -> Unit,
) : GLSurfaceView.Renderer {

    @Volatile
    var currentInput: InputState = InputState()

    private lateinit var skyShader: SkyDomeShader
    private lateinit var litShader: DuskLitShader
    private lateinit var rainShader: RainOverlayShader

    private lateinit var roadSegmentMesh: Mesh
    private lateinit var lampPoleMesh: Mesh
    private lateinit var lampBulbMesh: Mesh
    private lateinit var lightPoolMesh: Mesh
    private lateinit var skylineClusterMesh: Mesh
    private lateinit var skylineWindowsMesh: Mesh

    private lateinit var bikePaintedMesh: Mesh
    private lateinit var bikeChassisMesh: Mesh
    private lateinit var bikeTaillightMesh: Mesh
    private lateinit var bikeHeadlightConeMesh: Mesh
    private lateinit var spokedWheelMesh: Mesh
    private lateinit var nitroFlameMesh: Mesh

    private lateinit var sedanBodyMesh: Mesh
    private lateinit var sedanDetailsMesh: Mesh
    private lateinit var sedanLightsMesh: Mesh
    private lateinit var sportsBodyMesh: Mesh
    private lateinit var sportsDetailsMesh: Mesh
    private lateinit var sportsLightsMesh: Mesh
    private lateinit var truckBodyMesh: Mesh
    private lateinit var truckDetailsMesh: Mesh
    private lateinit var truckLightsMesh: Mesh
    private lateinit var unitCubeMesh: Mesh

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var aspectRatio: Float = 16f / 9f
    private var lastFrameNs: Long = 0L
    private var elapsedTimeSec: Float = 0f
    private var uiSyncAccumSec: Float = 0f
    private val pendingActions = ArrayDeque<() -> Unit>()
    private val actionLock = Any()

    fun enqueueAction(action: () -> Unit) {
        synchronized(actionLock) {
            pendingActions.addLast(action)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.03f, 0.11f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        skyShader = SkyDomeShader()
        litShader = DuskLitShader()
        rainShader = RainOverlayShader()

        roadSegmentMesh = MeshFactory.buildRoadSegment()
        lampPoleMesh = MeshFactory.buildStreetlampPole()
        lampBulbMesh = MeshFactory.buildStreetlampBulb()
        lightPoolMesh = MeshFactory.buildLightPoolQuad()
        skylineClusterMesh = MeshFactory.buildSkylineCluster()
        skylineWindowsMesh = MeshFactory.buildSkylineWindows()

        bikePaintedMesh = MeshFactory.buildBikeBodyPainted()
        bikeChassisMesh = MeshFactory.buildBikeChassisAndRider()
        bikeTaillightMesh = MeshFactory.buildBikeTaillight()
        bikeHeadlightConeMesh = MeshFactory.buildBikeHeadlightCone()
        spokedWheelMesh = MeshFactory.buildSpokedWheel()
        nitroFlameMesh = MeshFactory.buildNitroFlameCone()

        sedanBodyMesh = MeshFactory.buildSedanBody()
        sedanDetailsMesh = MeshFactory.buildSedanDetails()
        sedanLightsMesh = MeshFactory.buildSedanLights()
        sportsBodyMesh = MeshFactory.buildSportsCarBody()
        sportsDetailsMesh = MeshFactory.buildSportsCarDetails()
        sportsLightsMesh = MeshFactory.buildSportsCarLights()
        truckBodyMesh = MeshFactory.buildTruckBody()
        truckDetailsMesh = MeshFactory.buildTruckDetails()
        truckLightsMesh = MeshFactory.buildTruckLights()
        unitCubeMesh = MeshFactory.buildUnitCube()

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1f
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = if (lastFrameNs == 0L) 0.016f else ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs
        elapsedTimeSec += dt

        // Drain queued UI actions cleanly on the GL thread
        while (true) {
            val action = synchronized(actionLock) {
                if (pendingActions.isEmpty()) null else pendingActions.removeFirst()
            } ?: break
            action.invoke()
        }

        simulation.step(dt, currentInput, simulationEvents)

        audioEngine.updateTelemetry(
            speedKmh = simulation.speedMps * 3.6f,
            isNitroActive = simulation.isNitroActive,
            isMenuPreview = simulation.phase == GamePhase.MENU,
            isPlaying = simulation.phase == GamePhase.PLAYING,
        )

        uiSyncAccumSec += dt
        if (uiSyncAccumSec >= 0.032f) { // ~30Hz crisp HUD telemetry updates
            uiSyncAccumSec = 0f
            onUiStateSnapshot(simulation.toUiState())
        }

        // Camera setup with FOV boost & crash shake
        Matrix.perspectiveM(projMatrix, 0, simulation.currentFovDeg, aspectRatio, 0.25f, 340f)

        val shake = simulation.cameraShake
        val shakeX = if (shake > 0.01f) (Random.nextFloat() - 0.5f) * shake * 0.65f else 0f
        val shakeY = if (shake > 0.01f) (Random.nextFloat() - 0.5f) * shake * 0.45f else 0f

        val camX = simulation.playerX * 0.72f + shakeX
        val camY = if (simulation.phase == GamePhase.MENU) 2.55f else 2.75f + shakeY
        val camZ = if (simulation.phase == GamePhase.MENU) 5.2f else 5.8f
        val lookX = simulation.playerX * 0.85f
        val lookY = 1.20f + shakeY * 0.4f
        val lookZ = -24f

        Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            lookX, lookY, lookZ,
            -simulation.leanAngleDeg * 0.0045f, 1f, 0f,
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 1. Sky Dome & Dusk Sun
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        skyShader.draw(
            aspect = aspectRatio,
            timeSec = elapsedTimeSec,
            cameraShiftX = camX,
            mistIntensity = simulation.mistIntensity,
            rainIntensity = simulation.rainIntensity,
        )
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        litShader.use(
            viewX = camX,
            viewY = camY,
            viewZ = camZ,
            fogR = simulation.currentFogR,
            fogG = simulation.currentFogG,
            fogB = simulation.currentFogB,
            fogDensity = simulation.currentFogDensity,
            wetness = simulation.rainIntensity,
        )

        // 2. Parallax City Skyline Silhouettes
        renderSkyline()

        // 3. Endless Pooled 4-Lane Highway + Streetlamps
        renderHighwayAndLamps()

        // 4. Pooled Traffic Vehicles
        renderTraffic()

        // 5. Custom Player Sportbike & Leaning Rider
        renderPlayerBike()

        // 6. Nitro Exhaust & Crash Particles
        renderParticles()

        // 7. Screen-Space Light Rain Shader Overlay
        if (simulation.rainIntensity > 0.01f) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glDepthMask(false)
            rainShader.draw(
                aspect = aspectRatio,
                timeSec = elapsedTimeSec,
                rainIntensity = simulation.rainIntensity,
                windLean = simulation.leanAngleDeg / 28f,
                speedFactor = ((simulation.speedMps - 18f) / 76f).coerceIn(0f, 1f),
            )
            GLES20.glDepthMask(true)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        }
    }

    private fun renderSkyline() {
        val skylineLen = 105f
        val parallaxOffset = (simulation.worldScrollZ * 0.22f) % skylineLen
        for (side in floatArrayOf(-38f, 38f)) {
            for (seg in -1..2) {
                val z = -seg * skylineLen + parallaxOffset - 60f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, side, -0.5f, z)
                if (side > 0f) Matrix.scaleM(modelMatrix, 0, -1f, 1f, 1f)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

                GLES20.glDisable(GLES20.GL_CULL_FACE)
                litShader.setMaterial(mvpMatrix, modelMatrix, fogDensityScale = 0.68f)
                skylineClusterMesh.draw(litShader)

                litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.0f, fogDensityScale = 0.68f)
                skylineWindowsMesh.draw(litShader)
                GLES20.glEnable(GLES20.GL_CULL_FACE)
            }
        }
    }

    private fun renderHighwayAndLamps() {
        val segLen = MeshFactory.SEGMENT_LENGTH
        val offsetZ = simulation.worldScrollZ % segLen
        for (i in -1..13) {
            val z = -i * segLen + offsetZ
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0f, 0f, z)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            litShader.setMaterial(mvpMatrix, modelMatrix)
            roadSegmentMesh.draw(litShader)
        }

        // Streetlamps spaced every 40m (2 segments)
        val lampSpacing = 40f
        val lampOffset = simulation.worldScrollZ % lampSpacing
        for (i in 0..6) {
            val z = -i * lampSpacing + lampOffset
            // Left lamp
            drawStreetlamp(-7.1f, z, mirrorX = false)
            // Right lamp (staggered by 20m)
            drawStreetlamp(7.1f, z - 20f, mirrorX = true)
        }
    }

    private fun drawStreetlamp(x: Float, z: Float, mirrorX: Boolean) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0f, z)
        if (mirrorX) Matrix.scaleM(modelMatrix, 0, -1f, 1f, 1f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glDisable(GLES20.GL_CULL_FACE)
        litShader.setMaterial(mvpMatrix, modelMatrix)
        lampPoleMesh.draw(litShader)

        litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.4f)
        lampBulbMesh.draw(litShader)

        // Subtle warm light pool on road shoulder
        Matrix.setIdentityM(modelMatrix, 0)
        val poolX = if (mirrorX) x - 2.2f else x + 2.2f
        Matrix.translateM(modelMatrix, 0, poolX, 0.01f, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glDepthMask(false)
        litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.1f, alpha = 0.22f)
        lightPoolMesh.draw(litShader)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
    }

    private fun renderTraffic() {
        for (v in simulation.trafficPool) {
            if (!v.active) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, v.x, 0f, v.z)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

            when (v.type) {
                VehicleType.SEDAN -> {
                    litShader.setMaterial(mvpMatrix, modelMatrix, tintR = v.r, tintG = v.g, tintB = v.b)
                    sedanBodyMesh.draw(litShader)
                    litShader.setMaterial(mvpMatrix, modelMatrix)
                    sedanDetailsMesh.draw(litShader)
                    litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.5f)
                    sedanLightsMesh.draw(litShader)
                }
                VehicleType.SPORTS_CAR -> {
                    litShader.setMaterial(mvpMatrix, modelMatrix, tintR = v.r, tintG = v.g, tintB = v.b)
                    sportsBodyMesh.draw(litShader)
                    litShader.setMaterial(mvpMatrix, modelMatrix)
                    sportsDetailsMesh.draw(litShader)
                    litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.6f)
                    sportsLightsMesh.draw(litShader)
                }
                VehicleType.BOX_TRUCK -> {
                    litShader.setMaterial(mvpMatrix, modelMatrix, tintR = v.r, tintG = v.g, tintB = v.b)
                    truckBodyMesh.draw(litShader)
                    litShader.setMaterial(mvpMatrix, modelMatrix)
                    truckDetailsMesh.draw(litShader)
                    litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.4f)
                    truckLightsMesh.draw(litShader)
                }
            }
        }
    }

    private fun renderPlayerBike() {
        val livery = simulation.selectedLivery
        val bikeBaseMatrix = FloatArray(16)
        Matrix.setIdentityM(bikeBaseMatrix, 0)
        Matrix.translateM(bikeBaseMatrix, 0, simulation.playerX, 0f, 0f)
        Matrix.rotateM(bikeBaseMatrix, 0, simulation.leanAngleDeg, 0f, 0f, 1f)
        Matrix.rotateM(bikeBaseMatrix, 0, simulation.pitchAngleDeg, 1f, 0f, 0f)

        // Bike chassis & rider
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, bikeBaseMatrix, 0)
        litShader.setMaterial(mvpMatrix, bikeBaseMatrix)
        bikeChassisMesh.draw(litShader)

        // Bike livery painted fairings
        litShader.setMaterial(mvpMatrix, bikeBaseMatrix, tintR = livery.r, tintG = livery.g, tintB = livery.b)
        bikePaintedMesh.draw(litShader)

        // Glowing red LED taillight (brightens sharply when braking)
        val brakeBoost = if (simulation.isBraking) 2.4f else 1.25f
        litShader.setMaterial(
            mvpMatrix,
            bikeBaseMatrix,
            tintR = if (simulation.isBraking) 1.3f else 1.0f,
            emissive = brakeBoost,
        )
        bikeTaillightMesh.draw(litShader)

        // Front & Rear spinning spoked wheels
        for (wheelZ in floatArrayOf(-0.74f, 0.72f)) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0f, 0.34f, wheelZ)
            Matrix.rotateM(modelMatrix, 0, -simulation.bikeWheelAngleDeg, 1f, 0f, 0f)
            val worldWheel = FloatArray(16)
            Matrix.multiplyMM(worldWheel, 0, bikeBaseMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, worldWheel, 0)
            litShader.setMaterial(mvpMatrix, worldWheel)
            spokedWheelMesh.draw(litShader)
        }

        // Dual-exhaust nitro flame cones
        if (simulation.isNitroActive) {
            val flicker = 0.85f + 0.35f * sin(elapsedTimeSec * 48f)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            for (exX in floatArrayOf(-0.18f, 0.18f)) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, exX, 0.44f, 0.96f)
                Matrix.scaleM(modelMatrix, 0, 1f, 1f, flicker)
                val worldFlame = FloatArray(16)
                Matrix.multiplyMM(worldFlame, 0, bikeBaseMatrix, 0, modelMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, worldFlame, 0)
                litShader.setMaterial(mvpMatrix, worldFlame, emissive = 2.0f, alpha = 0.92f)
                nitroFlameMesh.draw(litShader)
            }
            GLES20.glEnable(GLES20.GL_CULL_FACE)
        }

        // Front headlight beam cone projected onto asphalt
        GLES20.glDepthMask(false)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, simulation.playerX, 0.01f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        litShader.setMaterial(mvpMatrix, modelMatrix, emissive = 1.2f, alpha = 0.16f)
        bikeHeadlightConeMesh.draw(litShader)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(true)
    }

    private fun renderParticles() {
        GLES20.glDepthMask(false)
        for (p in simulation.particlePool) {
            if (!p.active) continue
            val lifeRatio = (p.life / p.maxLife).coerceIn(0f, 1f)
            val s = p.size * (0.4f + 0.6f * lifeRatio)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.scaleM(modelMatrix, 0, s, s, s * 2.2f)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            litShader.setMaterial(
                mvpMatrix = mvpMatrix,
                modelMatrix = modelMatrix,
                tintR = p.r,
                tintG = p.g,
                tintB = p.b,
                emissive = 1.8f,
                alpha = lifeRatio,
            )
            unitCubeMesh.draw(litShader)
        }
        GLES20.glDepthMask(true)
    }
}
