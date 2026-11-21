package com.example.game.engine

import com.example.game.model.BikeLivery
import com.example.game.model.DailyMissionsState
import com.example.game.model.GamePhase
import com.example.game.model.GameUiState
import com.example.game.model.InputState
import com.example.game.model.NearMissEvent
import com.example.game.model.Particle
import com.example.game.model.TrafficVehicle
import com.example.game.model.VehicleType
import com.example.game.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

interface SimulationEvents {
    fun onNearMiss(combo: Int, bonusPoints: Int)
    fun onCrash(finalScore: Int, isNewRecord: Boolean)
    fun onDailyMissionsChanged(missions: DailyMissionsState) {}
}

class GameSimulation(
    private val random: Random = Random.Default,
) {
    companion object {
        const val ROAD_HALF_WIDTH = 6.2f
        val LANE_CENTERS = floatArrayOf(-4.5f, -1.5f, 1.5f, 4.5f)
        const val MAX_TRAFFIC = 18
        const val MAX_PARTICLES = 96

        const val MIN_SPEED_MPS = 18f       // ~65 km/h
        const val CRUISE_SPEED_MPS = 34.7f  // ~125 km/h
        const val MAX_SPEED_MPS = 68.0f     // ~245 km/h
        const val NITRO_MAX_MPS = 94.5f     // ~340 km/h

        const val BIKE_HALF_WIDTH = 0.42f
        const val BIKE_HALF_LENGTH = 1.08f
        const val COMBO_WINDOW_SEC = 4.5f
        const val NEAR_MISS_SPEED_THRESHOLD_MPS = 33f // ~119 km/h
        const val NEAR_MISS_LATERAL_MARGIN = 1.28f
        const val WEATHER_MIN_HOLD_SEC = 16f
        const val WEATHER_MAX_HOLD_SEC = 26f
    }

    var phase: GamePhase = GamePhase.MENU
        private set

    var playerX: Float = 1.5f
        private set
    var playerVx: Float = 0f
        private set
    var leanAngleDeg: Float = 0f
        private set
    var pitchAngleDeg: Float = 0f
        private set
    var speedMps: Float = 32f
        private set
    var worldScrollZ: Float = 0f
        private set
    var bikeWheelAngleDeg: Float = 0f
        private set

    var nitroPercent: Float = 100f
        private set
    var isNitroActive: Boolean = false
        private set
    var isBraking: Boolean = false
        private set

    var distanceMeters: Float = 0f
        private set
    var scoreFloat: Float = 0f
        private set
    var topSpeedKmh: Int = 0
        private set
    var nearMissCount: Int = 0
        private set
    var comboMultiplier: Int = 1
        private set
    var comboTimerSec: Float = 0f
        private set
    var highScore: Int = 0
        private set
    var isNewRecord: Boolean = false
        private set

    var cameraShake: Float = 0f
        private set
    var currentFovDeg: Float = 58f
        private set

    var selectedLivery: BikeLivery = BikeLivery.SOLAR_EMBER
    var isMuted: Boolean = false

    // Random Weather Generator state
    var targetWeather: WeatherCondition = WeatherCondition.CLEAR_DUSK
        private set
    var weatherHoldTimerSec: Float = 18f
        private set
    var mistIntensity: Float = 0f
        private set
    var rainIntensity: Float = 0f
        private set
    var currentFogDensity: Float = WeatherCondition.CLEAR_DUSK.fogDensity
        private set
    var currentFogR: Float = WeatherCondition.CLEAR_DUSK.fogR
        private set
    var currentFogG: Float = WeatherCondition.CLEAR_DUSK.fogG
        private set
    var currentFogB: Float = WeatherCondition.CLEAR_DUSK.fogB
        private set

    // Daily Missions state
    private var dailyBaseMissions: DailyMissionsState = DailyMissionsState()
    private var lastCompletedMask: Int = 0

    private var nearMissIdSeq: Long = 0L
    private var activeNearMissEvent: NearMissEvent? = null
    private var nearMissPopupTimer: Float = 0f

    private var spawnCooldownDist: Float = 0f
    private var showcaseTime: Float = 0f
    private var nitroParticleAccumulator: Float = 0f

    val trafficPool: Array<TrafficVehicle> = Array(MAX_TRAFFIC) { TrafficVehicle() }
    val particlePool: Array<Particle> = Array(MAX_PARTICLES) { Particle() }

    private val trafficPalette = arrayOf(
        floatArrayOf(0.82f, 0.85f, 0.90f), // Silver
        floatArrayOf(0.16f, 0.22f, 0.34f), // Midnight blue
        floatArrayOf(0.78f, 0.18f, 0.24f), // Crimson
        floatArrayOf(0.22f, 0.62f, 0.72f), // Teal
        floatArrayOf(0.92f, 0.56f, 0.16f), // Amber orange
        floatArrayOf(0.32f, 0.30f, 0.38f), // Gunmetal
        floatArrayOf(0.94f, 0.92f, 0.86f), // Pearl white
    )

    init {
        seedShowcaseTraffic()
    }

    fun applyPersistedHighScore(savedHighScore: Int) {
        if (savedHighScore > highScore) {
            highScore = savedHighScore
        }
    }

    fun applyPersistedDailyMissions(saved: DailyMissionsState) {
        if (phase == GamePhase.MENU || phase == GamePhase.GAME_OVER) {
            dailyBaseMissions = saved
            lastCompletedMask = computeCompletedMask(saved)
        }
    }

    fun forceWeather(condition: WeatherCondition, immediate: Boolean = false) {
        targetWeather = condition
        weatherHoldTimerSec = WEATHER_MIN_HOLD_SEC + random.nextFloat() * (WEATHER_MAX_HOLD_SEC - WEATHER_MIN_HOLD_SEC)
        if (immediate) {
            mistIntensity = condition.mistAmount
            rainIntensity = condition.rainAmount
            currentFogDensity = condition.fogDensity
            currentFogR = condition.fogR
            currentFogG = condition.fogG
            currentFogB = condition.fogB
        }
    }

    fun currentDailyMissions(): DailyMissionsState {
        return if (phase == GamePhase.PLAYING || phase == GamePhase.PAUSED) {
            DailyMissionsState(
                dateKey = dailyBaseMissions.dateKey,
                totalNearMisses = dailyBaseMissions.totalNearMisses + nearMissCount,
                totalDistanceMeters = dailyBaseMissions.totalDistanceMeters + distanceMeters.toInt(),
                bestSpeedKmh = maxOf(dailyBaseMissions.bestSpeedKmh, topSpeedKmh),
            )
        } else {
            dailyBaseMissions
        }
    }

    private fun computeCompletedMask(missions: DailyMissionsState): Int {
        var mask = 0
        missions.items.forEachIndexed { idx, item ->
            if (item.isCompleted) mask = mask or (1 shl idx)
        }
        return mask
    }

    fun startNewRun() {
        if (phase == GamePhase.GAME_OVER) {
            // Ensure base already includes previous run
            dailyBaseMissions = currentDailyMissions()
        }
        phase = GamePhase.PLAYING
        playerX = 1.5f
        playerVx = 0f
        leanAngleDeg = 0f
        pitchAngleDeg = 0f
        speedMps = CRUISE_SPEED_MPS
        distanceMeters = 0f
        scoreFloat = 0f
        topSpeedKmh = (speedMps * 3.6f).toInt()
        nearMissCount = 0
        comboMultiplier = 1
        comboTimerSec = 0f
        nitroPercent = 100f
        isNitroActive = false
        isBraking = false
        isNewRecord = false
        cameraShake = 0f
        currentFovDeg = 58f
        activeNearMissEvent = null
        nearMissPopupTimer = 0f
        spawnCooldownDist = 18f
        lastCompletedMask = computeCompletedMask(dailyBaseMissions)

        for (v in trafficPool) v.active = false
        for (p in particlePool) p.active = false

        // Seed initial traffic nicely staggered ahead
        spawnTrafficVehicle(-95f, preferredLane = 0)
        spawnTrafficVehicle(-135f, preferredLane = 2)
        spawnTrafficVehicle(-175f, preferredLane = 3)
        spawnTrafficVehicle(-215f, preferredLane = 1)
    }

    fun pauseGame() {
        if (phase == GamePhase.PLAYING) {
            phase = GamePhase.PAUSED
            isNitroActive = false
        }
    }

    fun resumeGame() {
        if (phase == GamePhase.PAUSED) {
            phase = GamePhase.PLAYING
        }
    }

    fun returnToMenu() {
        if (phase == GamePhase.PLAYING || phase == GamePhase.PAUSED || phase == GamePhase.GAME_OVER) {
            dailyBaseMissions = currentDailyMissions()
        }
        phase = GamePhase.MENU
        isNitroActive = false
        isBraking = false
        cameraShake = 0f
        activeNearMissEvent = null
        for (p in particlePool) p.active = false
        seedShowcaseTraffic()
    }

    private fun seedShowcaseTraffic() {
        for (v in trafficPool) v.active = false
        spawnTrafficVehicle(-65f, preferredLane = 0)
        spawnTrafficVehicle(-120f, preferredLane = 3)
        spawnTrafficVehicle(-185f, preferredLane = 1)
    }

    fun step(
        dtRaw: Float,
        input: InputState,
        events: SimulationEvents? = null,
    ) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)

        if (phase == GamePhase.PAUSED) {
            return
        }

        stepWeather(dt)

        if (phase == GamePhase.MENU) {
            stepMenuShowcase(dt)
            return
        }

        if (phase == GamePhase.GAME_OVER) {
            stepGameOverCooldown(dt)
            return
        }

        // === PLAYING ===
        val wantsNitro = input.nitro && nitroPercent > 1.5f && !input.brake
        isNitroActive = wantsNitro
        isBraking = input.brake && !wantsNitro

        // Speed dynamics
        val targetMax = if (isNitroActive) NITRO_MAX_MPS else MAX_SPEED_MPS
        val accelRate = when {
            isNitroActive -> 46f
            input.accelerate -> 24f
            isBraking -> -42f
            else -> if (speedMps < CRUISE_SPEED_MPS) 14f else 4.5f // Gentle auto-cruise roll
        }

        speedMps = (speedMps + accelRate * dt).coerceIn(MIN_SPEED_MPS, targetMax)
        if (!isNitroActive && speedMps > MAX_SPEED_MPS) {
            // Smooth aerodynamic drag back from nitro top speed
            speedMps = (speedMps - 22f * dt).coerceAtLeast(MAX_SPEED_MPS)
        }

        // Nitro consumption / passive trickling regen
        if (isNitroActive) {
            nitroPercent = (nitroPercent - 26f * dt).coerceAtLeast(0f)
            if (nitroPercent <= 0.1f) isNitroActive = false
        } else {
            nitroPercent = (nitroPercent + 3.2f * dt).coerceAtMost(100f)
        }

        // Steering & realistic bike banking
        val steerDir = (if (input.steerRight) 1f else 0f) - (if (input.steerLeft) 1f else 0f)
        val steerAccel = 44f
        val maxLatSpeed = 11.2f
        if (steerDir != 0f) {
            playerVx = (playerVx + steerDir * steerAccel * dt).coerceIn(-maxLatSpeed, maxLatSpeed)
        } else {
            // Crisp damping back to straight line
            val damping = 12f * dt
            playerVx = if (abs(playerVx) < 0.15f) 0f else playerVx * (1f - damping).coerceAtLeast(0f)
        }

        playerX += playerVx * dt
        val maxBikeX = ROAD_HALF_WIDTH - 0.85f
        if (playerX < -maxBikeX) {
            playerX = -maxBikeX
            playerVx = 0f
        } else if (playerX > maxBikeX) {
            playerX = maxBikeX
            playerVx = 0f
        }

        // Banking lean & accel/brake pitch
        val targetLean = -(playerVx / maxLatSpeed) * 28f
        leanAngleDeg += (targetLean - leanAngleDeg) * (10f * dt).coerceAtMost(1f)

        val targetPitch = when {
            isNitroActive -> 4.5f
            input.accelerate -> 2.2f
            isBraking -> -4.0f
            else -> 0f
        }
        pitchAngleDeg += (targetPitch - pitchAngleDeg) * (8f * dt).coerceAtMost(1f)

        // Camera FOV boost
        val targetFov = if (isNitroActive) 74f else 58f + ((speedMps - MIN_SPEED_MPS) / (MAX_SPEED_MPS - MIN_SPEED_MPS)) * 5f
        currentFovDeg += (targetFov - currentFovDeg) * (6f * dt).coerceAtMost(1f)

        // World scrolling & telemetry
        val deltaDist = speedMps * dt
        worldScrollZ = (worldScrollZ + deltaDist) % 2000f
        distanceMeters += deltaDist
        bikeWheelAngleDeg = (bikeWheelAngleDeg + (deltaDist / 0.34f) * (180f / Math.PI.toFloat())) % 360f

        val currentKmh = (speedMps * 3.6f).toInt()
        if (currentKmh > topSpeedKmh) topSpeedKmh = currentKmh

        // Score accrues with speed, active combo multiplier, and Daily Missions bonus multiplier
        val missionMult = currentDailyMissions().scoreMultiplier
        scoreFloat += deltaDist * 1.4f * comboMultiplier * missionMult

        val updatedMissions = currentDailyMissions()
        val completedMask = computeCompletedMask(updatedMissions)
        if (completedMask != lastCompletedMask) {
            lastCompletedMask = completedMask
            events?.onDailyMissionsChanged(updatedMissions)
        }

        // Combo decay
        if (comboMultiplier > 1) {
            comboTimerSec -= dt
            if (comboTimerSec <= 0f) {
                comboMultiplier = 1
                comboTimerSec = 0f
            }
        }

        if (nearMissPopupTimer > 0f) {
            nearMissPopupTimer -= dt
            if (nearMissPopupTimer <= 0f) {
                activeNearMissEvent = null
            }
        }

        // Nitro exhaust particles
        if (isNitroActive) {
            nitroParticleAccumulator += dt
            while (nitroParticleAccumulator >= 0.016f) {
                nitroParticleAccumulator -= 0.016f
                spawnNitroParticle()
            }
        }

        // Spawn & update traffic
        spawnCooldownDist -= deltaDist
        if (spawnCooldownDist <= 0f) {
            val densityGap = (34f - (distanceMeters / 160f)).coerceIn(15f, 34f)
            spawnCooldownDist = densityGap + random.nextFloat() * 14f
            spawnTrafficVehicle(-225f - random.nextFloat() * 25f)
        }

        stepTrafficAndCheckInteractions(dt, events)
        stepParticles(dt)
    }

    private fun stepMenuShowcase(dt: Float) {
        showcaseTime += dt
        speedMps = 32f
        val deltaDist = speedMps * dt
        worldScrollZ = (worldScrollZ + deltaDist) % 2000f
        bikeWheelAngleDeg = (bikeWheelAngleDeg + (deltaDist / 0.34f) * (180f / Math.PI.toFloat())) % 360f

        val desiredX = sin(showcaseTime * 0.55f) * 2.8f
        val prevX = playerX
        playerX += (desiredX - playerX) * (2.5f * dt).coerceAtMost(1f)
        val vx = (playerX - prevX) / dt
        leanAngleDeg = (-vx * 3.5f).coerceIn(-18f, 18f)
        currentFovDeg += (58f - currentFovDeg) * (4f * dt).coerceAtMost(1f)

        for (v in trafficPool) {
            if (!v.active) continue
            v.z += (speedMps - v.speedMps) * dt
            v.wheelAngleDeg = (v.wheelAngleDeg + (v.speedMps * dt / 0.36f) * 57.2958f) % 360f
            if (v.z > 22f) {
                v.z = -215f - random.nextFloat() * 35f
            }
        }
    }

    private fun stepGameOverCooldown(dt: Float) {
        speedMps = (speedMps - 28f * dt).coerceAtLeast(0f)
        val deltaDist = speedMps * dt
        worldScrollZ = (worldScrollZ + deltaDist) % 2000f
        cameraShake = (cameraShake - 1.9f * dt).coerceAtLeast(0f)

        for (v in trafficPool) {
            if (!v.active) continue
            v.z += (speedMps - v.speedMps) * dt
            v.wheelAngleDeg = (v.wheelAngleDeg + (v.speedMps * dt / 0.36f) * 57.2958f) % 360f
            if (v.z > 28f || v.z < -260f) v.active = false
        }
        stepParticles(dt)
    }

    private fun stepTrafficAndCheckInteractions(dt: Float, events: SimulationEvents?) {
        for (v in trafficPool) {
            if (!v.active) continue

            // Smooth lane shifting for cars occasionally
            val targetX = LANE_CENTERS[v.targetLane]
            val dxLane = targetX - v.x
            if (abs(dxLane) > 0.02f) {
                v.x += dxLane.coerceIn(-2.2f * dt, 2.2f * dt)
            } else {
                v.x = targetX
                v.lane = v.targetLane
            }

            // Relative movement toward +Z camera as player overtakes
            v.z += (speedMps - v.speedMps) * dt
            v.wheelAngleDeg = (v.wheelAngleDeg + (v.speedMps * dt / 0.36f) * 57.2958f) % 360f

            if (v.z > 22f) {
                v.active = false
                continue
            }

            // Collision & Near-Miss bounds
            val halfW = v.type.width * 0.48f
            val halfL = v.type.length * 0.48f
            val dx = abs(playerX - v.x)
            val dz = abs(v.z)

            if (dx < (BIKE_HALF_WIDTH + halfW) && dz < (BIKE_HALF_LENGTH + halfL)) {
                // Crash!
                triggerCrash(events)
                return
            }

            // Near-Miss check: passing beside the vehicle at high speed
            if (!v.nearMissAwarded &&
                speedMps >= NEAR_MISS_SPEED_THRESHOLD_MPS &&
                dz < (BIKE_HALF_LENGTH + halfL + 0.9f) &&
                dx >= (BIKE_HALF_WIDTH + halfW) &&
                dx < (BIKE_HALF_WIDTH + halfW + NEAR_MISS_LATERAL_MARGIN)
            ) {
                v.nearMissAwarded = true
                nearMissCount++
                comboMultiplier = (comboMultiplier + 1).coerceAtMost(10)
                comboTimerSec = COMBO_WINDOW_SEC
                val missionMult = currentDailyMissions().scoreMultiplier
                val bonus = (100 * comboMultiplier * missionMult).toInt()
                scoreFloat += bonus
                nitroPercent = (nitroPercent + 22f).coerceAtMost(100f)

                nearMissIdSeq++
                activeNearMissEvent = NearMissEvent(
                    id = nearMissIdSeq,
                    bonusPoints = bonus,
                    combo = comboMultiplier,
                )
                nearMissPopupTimer = 1.35f
                events?.onNearMiss(comboMultiplier, bonus)
                events?.onDailyMissionsChanged(currentDailyMissions())
            }
        }
    }

    private fun stepWeather(dt: Float) {
        weatherHoldTimerSec -= dt
        if (weatherHoldTimerSec <= 0f) {
            val candidates = WeatherCondition.entries.filter { it != targetWeather }
            targetWeather = candidates[random.nextInt(candidates.size)]
            weatherHoldTimerSec =
                WEATHER_MIN_HOLD_SEC + random.nextFloat() * (WEATHER_MAX_HOLD_SEC - WEATHER_MIN_HOLD_SEC)
        }

        val blend = (dt * 0.45f).coerceAtMost(1f)
        mistIntensity += (targetWeather.mistAmount - mistIntensity) * blend
        rainIntensity += (targetWeather.rainAmount - rainIntensity) * blend
        currentFogDensity += (targetWeather.fogDensity - currentFogDensity) * blend
        currentFogR += (targetWeather.fogR - currentFogR) * blend
        currentFogG += (targetWeather.fogG - currentFogG) * blend
        currentFogB += (targetWeather.fogB - currentFogB) * blend
    }

    private fun triggerCrash(events: SimulationEvents?) {
        phase = GamePhase.GAME_OVER
        isNitroActive = false
        isBraking = false
        cameraShake = 1.0f

        val finalScore = scoreFloat.toInt()
        if (finalScore > highScore) {
            highScore = finalScore
            isNewRecord = true
        }

        // Burst of 52 impact sparks
        for (i in 0 until 52) {
            val p = particlePool.firstOrNull { !it.active } ?: break
            val angle = random.nextFloat() * 6.283f
            val speed = 4f + random.nextFloat() * 14f
            p.active = true
            p.x = playerX + (random.nextFloat() - 0.5f) * 0.5f
            p.y = 0.45f + random.nextFloat() * 0.6f
            p.z = -0.3f + (random.nextFloat() - 0.5f) * 0.8f
            p.vx = cos(angle) * speed
            p.vy = 2.5f + random.nextFloat() * 8.5f
            p.vz = sin(angle) * speed + speedMps * 0.3f
            p.maxLife = 0.45f + random.nextFloat() * 0.55f
            p.life = p.maxLife
            p.size = 0.09f + random.nextFloat() * 0.14f
            if (i % 3 == 0) {
                p.r = 1.0f; p.g = 0.92f; p.b = 0.55f
            } else {
                p.r = 1.0f; p.g = 0.35f; p.b = 0.08f
            }
        }

        val finalMissions = DailyMissionsState(
            dateKey = dailyBaseMissions.dateKey,
            totalNearMisses = dailyBaseMissions.totalNearMisses + nearMissCount,
            totalDistanceMeters = dailyBaseMissions.totalDistanceMeters + distanceMeters.toInt(),
            bestSpeedKmh = maxOf(dailyBaseMissions.bestSpeedKmh, topSpeedKmh),
        )
        dailyBaseMissions = finalMissions
        lastCompletedMask = computeCompletedMask(finalMissions)
        events?.onDailyMissionsChanged(finalMissions)
        events?.onCrash(finalScore, isNewRecord)
    }

    private fun spawnNitroParticle() {
        val p = particlePool.firstOrNull { !it.active } ?: return
        val side = if (random.nextBoolean()) -0.17f else 0.17f
        p.active = true
        p.x = playerX + side + (random.nextFloat() - 0.5f) * 0.06f
        p.y = 0.44f + (random.nextFloat() - 0.5f) * 0.05f
        p.z = 1.08f
        p.vx = playerVx * 0.15f + (random.nextFloat() - 0.5f) * 0.9f
        p.vy = 0.3f + random.nextFloat() * 0.8f
        p.vz = 14f + random.nextFloat() * 8f
        p.maxLife = 0.20f + random.nextFloat() * 0.16f
        p.life = p.maxLife
        p.size = 0.10f + random.nextFloat() * 0.08f
        if (random.nextFloat() < 0.45f) {
            p.r = 0.15f; p.g = 0.88f; p.b = 1.0f
        } else {
            p.r = 1.0f; p.g = 0.52f; p.b = 0.10f
        }
    }

    private fun stepParticles(dt: Float) {
        for (p in particlePool) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            if (phase == GamePhase.GAME_OVER) {
                p.vy -= 18f * dt
                if (p.y < 0.06f) {
                    p.y = 0.06f
                    p.vy = -p.vy * 0.4f
                }
            }
        }
    }

    fun spawnTrafficVehicle(spawnZ: Float, preferredLane: Int? = null): Boolean {
        val slot = trafficPool.firstOrNull { !it.active } ?: return false

        // Count occupied lanes near spawnZ to guarantee at least 1 passable lane
        val laneBlocked = BooleanArray(4)
        var occupiedCount = 0
        for (other in trafficPool) {
            if (!other.active) continue
            if (abs(other.z - spawnZ) < 19f) {
                if (!laneBlocked[other.lane]) {
                    laneBlocked[other.lane] = true
                    occupiedCount++
                }
            }
        }
        if (occupiedCount >= 3) return false

        val candidateLane = preferredLane ?: run {
            val openLanes = (0..3).filter { !laneBlocked[it] }
            if (openLanes.isEmpty()) return false
            openLanes[random.nextInt(openLanes.size)]
        }

        // Prevent same-lane overlap
        for (other in trafficPool) {
            if (other.active && other.lane == candidateLane && abs(other.z - spawnZ) < 18f) {
                return false
            }
        }

        val typeRoll = random.nextFloat()
        val type = when {
            typeRoll < 0.50f -> VehicleType.SEDAN
            typeRoll < 0.80f -> VehicleType.SPORTS_CAR
            else -> VehicleType.BOX_TRUCK
        }

        val color = trafficPalette[random.nextInt(trafficPalette.size)]
        val baseVehSpeed = when (type) {
            VehicleType.BOX_TRUCK -> 17f + random.nextFloat() * 5f
            VehicleType.SEDAN -> 20f + random.nextFloat() * 7f
            VehicleType.SPORTS_CAR -> 24f + random.nextFloat() * 9f
        }

        slot.active = true
        slot.type = type
        slot.lane = candidateLane
        slot.targetLane = candidateLane
        slot.x = LANE_CENTERS[candidateLane]
        slot.z = spawnZ
        slot.speedMps = baseVehSpeed
        slot.r = color[0]
        slot.g = color[1]
        slot.b = color[2]
        slot.wheelAngleDeg = 0f
        slot.nearMissAwarded = false
        return true
    }

    private var TrafficVehicle.wheelAngleDeg: Float
        get() = wheelAngleRad
        set(value) {
            wheelAngleRad = value
        }

    fun toUiState(): GameUiState {
        return GameUiState(
            phase = phase,
            speedKmh = (speedMps * 3.6f).toInt(),
            distanceKm = distanceMeters / 1000f,
            score = scoreFloat.toInt(),
            highScore = maxOf(highScore, scoreFloat.toInt()),
            topSpeedKmh = topSpeedKmh,
            nearMissCount = nearMissCount,
            comboMultiplier = comboMultiplier,
            comboTimerProgress = if (comboMultiplier > 1) (comboTimerSec / COMBO_WINDOW_SEC).coerceIn(0f, 1f) else 0f,
            nitroPercent = nitroPercent,
            isNitroActive = isNitroActive,
            isBraking = isBraking,
            isNewRecord = isNewRecord,
            isMuted = isMuted,
            selectedLivery = selectedLivery,
            activeNearMiss = activeNearMissEvent,
            weatherCondition = targetWeather,
            dailyMissions = currentDailyMissions(),
        )
    }
}
