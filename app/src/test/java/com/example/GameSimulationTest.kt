package com.example

import com.example.game.engine.GameSimulation
import com.example.game.engine.SimulationEvents
import com.example.game.model.DailyMissionsState
import com.example.game.model.GamePhase
import com.example.game.model.InputState
import com.example.game.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameSimulationTest {

    @Test
    fun `steering and banking respond smoothly to lateral input`() {
        val sim = GameSimulation(Random(42))
        sim.startNewRun()
        val startX = sim.playerX

        repeat(15) {
            sim.step(0.016f, InputState(steerRight = true))
        }

        assertTrue(sim.playerX > startX)
        assertTrue(sim.leanAngleDeg < -5f)
    }

    @Test
    fun `nitro boost increases speed, widens FOV, and consumes meter`() {
        val sim = GameSimulation(Random(42))
        sim.startNewRun()
        val initialFov = sim.currentFovDeg
        val initialSpeed = sim.speedMps

        repeat(20) {
            sim.step(0.016f, InputState(nitro = true))
        }

        assertTrue(sim.isNitroActive)
        assertTrue(sim.speedMps > initialSpeed)
        assertTrue(sim.currentFovDeg > initialFov)
        assertTrue(sim.nitroPercent < 100f)
    }

    @Test
    fun `near miss triggers combo multiplier and refills nitro`() {
        val sim = GameSimulation(Random(42))
        sim.startNewRun()
        repeat(40) { sim.step(0.016f, InputState(nitro = true)) }
        val nitroBeforeNearMiss = sim.nitroPercent

        for (v in sim.trafficPool) v.active = false
        val spawned = sim.spawnTrafficVehicle(spawnZ = -1.2f, preferredLane = 2)
        assertTrue(spawned)
        val veh = sim.trafficPool.first { it.active }
        veh.x = sim.playerX + 1.55f
        veh.z = 0f

        var nearMissCalls = 0
        val events = object : SimulationEvents {
            override fun onNearMiss(combo: Int, bonusPoints: Int) {
                nearMissCalls++
            }

            override fun onCrash(finalScore: Int, isNewRecord: Boolean) {}
        }

        sim.step(0.016f, InputState(accelerate = true), events)

        assertEquals(1, nearMissCalls)
        assertEquals(1, sim.nearMissCount)
        assertEquals(2, sim.comboMultiplier)
        assertTrue(sim.nitroPercent > nitroBeforeNearMiss)
    }

    @Test
    fun `collision transitions to game over and updates high score`() {
        val sim = GameSimulation(Random(42))
        sim.startNewRun()
        repeat(15) { sim.step(0.016f, InputState(accelerate = true)) }

        for (v in sim.trafficPool) v.active = false
        sim.spawnTrafficVehicle(spawnZ = 0f, preferredLane = 2)
        val veh = sim.trafficPool.first { it.active }
        veh.x = sim.playerX
        veh.z = 0f

        var crashed = false
        val events = object : SimulationEvents {
            override fun onNearMiss(combo: Int, bonusPoints: Int) {}
            override fun onCrash(finalScore: Int, isNewRecord: Boolean) {
                crashed = true
            }
        }

        sim.step(0.016f, InputState(), events)

        assertTrue(crashed)
        assertEquals(GamePhase.GAME_OVER, sim.phase)
        assertTrue(sim.cameraShake > 0.5f)
        assertTrue(sim.highScore >= sim.scoreFloat.toInt())
    }

    @Test
    fun `weather generator transitions smoothly across clear, mist, and rain`() {
        val sim = GameSimulation(Random(7))
        assertEquals(WeatherCondition.CLEAR_DUSK, sim.targetWeather)

        // Advance simulation past max weather hold time (26s) to trigger a random transition
        repeat(600) {
            sim.step(0.05f, InputState())
        }
        assertNotEquals(WeatherCondition.CLEAR_DUSK, sim.targetWeather)

        // Force LIGHT_RAIN and verify atmospheric shader parameters blend smoothly
        sim.forceWeather(WeatherCondition.LIGHT_RAIN)
        repeat(120) {
            sim.step(0.05f, InputState())
        }
        assertTrue(sim.rainIntensity > 0.7f)
        assertTrue(sim.currentFogDensity > WeatherCondition.CLEAR_DUSK.fogDensity)

        // Force LIGHT_MIST and verify mist intensifies while rain fades
        sim.forceWeather(WeatherCondition.LIGHT_MIST)
        repeat(120) {
            sim.step(0.05f, InputState())
        }
        assertTrue(sim.mistIntensity > 0.7f)
        assertTrue(sim.rainIntensity < 0.25f)
    }

    @Test
    fun `completing daily near-miss mission increases score multiplier`() {
        val sim = GameSimulation(Random(99))
        sim.applyPersistedDailyMissions(
            DailyMissionsState(
                dateKey = "2026-11-21",
                totalNearMisses = 4,
                totalDistanceMeters = 0,
                bestSpeedKmh = 120,
            )
        )
        assertEquals(1.0f, sim.currentDailyMissions().scoreMultiplier, 0.001f)

        sim.startNewRun()
        for (v in sim.trafficPool) v.active = false
        sim.spawnTrafficVehicle(spawnZ = 0f, preferredLane = 2)
        val veh = sim.trafficPool.first { it.active }
        veh.x = sim.playerX + 1.55f
        veh.z = 0f

        var missionUpdated: DailyMissionsState? = null
        val events = object : SimulationEvents {
            override fun onNearMiss(combo: Int, bonusPoints: Int) {}
            override fun onCrash(finalScore: Int, isNewRecord: Boolean) {}
            override fun onDailyMissionsChanged(missions: DailyMissionsState) {
                missionUpdated = missions
            }
        }

        sim.step(0.016f, InputState(accelerate = true), events)

        val current = sim.currentDailyMissions()
        assertEquals(5, current.totalNearMisses)
        assertTrue(current.items.first().isCompleted)
        assertEquals(1.25f, current.scoreMultiplier, 0.001f)
        assertEquals(1.25f, missionUpdated?.scoreMultiplier ?: 0f, 0.001f)
    }
}
