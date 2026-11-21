package com.example.game.model

import androidx.compose.ui.graphics.Color

enum class GamePhase {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
}

enum class WeatherCondition(
    val label: String,
    val fogDensity: Float,
    val fogR: Float,
    val fogG: Float,
    val fogB: Float,
    val mistAmount: Float,
    val rainAmount: Float,
) {
    CLEAR_DUSK(
        label = "CLEAR DUSK",
        fogDensity = 0.0046f,
        fogR = 0.62f,
        fogG = 0.24f,
        fogB = 0.36f,
        mistAmount = 0.0f,
        rainAmount = 0.0f,
    ),
    LIGHT_MIST(
        label = "LIGHT MIST",
        fogDensity = 0.0115f,
        fogR = 0.48f,
        fogG = 0.29f,
        fogB = 0.45f,
        mistAmount = 1.0f,
        rainAmount = 0.0f,
    ),
    LIGHT_RAIN(
        label = "LIGHT RAIN",
        fogDensity = 0.0082f,
        fogR = 0.27f,
        fogG = 0.19f,
        fogB = 0.36f,
        mistAmount = 0.35f,
        rainAmount = 1.0f,
    ),
}

enum class DailyMissionType(
    val id: String,
    val targetValue: Int,
    val bonusMultiplier: Float,
) {
    NEAR_MISSES(
        id = "near_misses_5",
        targetValue = 5,
        bonusMultiplier = 0.25f,
    ),
    DISTANCE_METERS(
        id = "distance_3000m",
        targetValue = 3000,
        bonusMultiplier = 0.25f,
    ),
    TOP_SPEED_KMH(
        id = "top_speed_280",
        targetValue = 280,
        bonusMultiplier = 0.50f,
    ),
}

data class DailyMissionItem(
    val type: DailyMissionType,
    val currentValue: Int,
    val isCompleted: Boolean,
) {
    val progressFraction: Float
        get() = (currentValue.toFloat() / type.targetValue.toFloat()).coerceIn(0f, 1f)
}

data class DailyMissionsState(
    val dateKey: String = "",
    val totalNearMisses: Int = 0,
    val totalDistanceMeters: Int = 0,
    val bestSpeedKmh: Int = 0,
) {
    val items: List<DailyMissionItem>
        get() = listOf(
            DailyMissionItem(
                type = DailyMissionType.NEAR_MISSES,
                currentValue = totalNearMisses.coerceAtMost(DailyMissionType.NEAR_MISSES.targetValue),
                isCompleted = totalNearMisses >= DailyMissionType.NEAR_MISSES.targetValue,
            ),
            DailyMissionItem(
                type = DailyMissionType.DISTANCE_METERS,
                currentValue = totalDistanceMeters.coerceAtMost(DailyMissionType.DISTANCE_METERS.targetValue),
                isCompleted = totalDistanceMeters >= DailyMissionType.DISTANCE_METERS.targetValue,
            ),
            DailyMissionItem(
                type = DailyMissionType.TOP_SPEED_KMH,
                currentValue = bestSpeedKmh.coerceAtMost(DailyMissionType.TOP_SPEED_KMH.targetValue),
                isCompleted = bestSpeedKmh >= DailyMissionType.TOP_SPEED_KMH.targetValue,
            ),
        )

    val scoreMultiplier: Float
        get() {
            var bonus = 0f
            for (item in items) {
                if (item.isCompleted) bonus += item.type.bonusMultiplier
            }
            return 1.0f + bonus
        }
}

enum class BikeLivery(
    val id: String,
    val label: String,
    val primaryColor: Color,
    val accentColor: Color,
    val r: Float,
    val g: Float,
    val b: Float,
    val accentR: Float,
    val accentG: Float,
    val accentB: Float,
) {
    SOLAR_EMBER(
        id = "solar_ember",
        label = "Solar Ember",
        primaryColor = Color(0xFFFF5E1A),
        accentColor = Color(0xFFFFC400),
        r = 1.0f,
        g = 0.37f,
        b = 0.10f,
        accentR = 1.0f,
        accentG = 0.77f,
        accentB = 0.0f,
    ),
    CYBER_CYAN(
        id = "cyber_cyan",
        label = "Cyber Cyan",
        primaryColor = Color(0xFF00D4FF),
        accentColor = Color(0xFF64FFDA),
        r = 0.0f,
        g = 0.83f,
        b = 1.0f,
        accentR = 0.39f,
        accentG = 1.0f,
        accentB = 0.85f,
    ),
    NEON_MAGENTA(
        id = "neon_magenta",
        label = "Dusk Pulse",
        primaryColor = Color(0xFFE6247E),
        accentColor = Color(0xFFFF9E00),
        r = 0.90f,
        g = 0.14f,
        b = 0.49f,
        accentR = 1.0f,
        accentG = 0.62f,
        accentB = 0.0f,
    ),
    VALKYRIE_GOLD(
        id = "valkyrie_gold",
        label = "Apex Gold",
        primaryColor = Color(0xFFF5B800),
        accentColor = Color(0xFFFF4D4D),
        r = 0.96f,
        g = 0.72f,
        b = 0.0f,
        accentR = 1.0f,
        accentG = 0.30f,
        accentB = 0.30f,
    );

    companion object {
        fun fromId(id: String): BikeLivery =
            entries.firstOrNull { it.id == id } ?: SOLAR_EMBER
    }
}

data class InputState(
    val steerLeft: Boolean = false,
    val steerRight: Boolean = false,
    val accelerate: Boolean = false,
    val brake: Boolean = false,
    val nitro: Boolean = false,
)

enum class VehicleType(
    val width: Float,
    val height: Float,
    val length: Float,
) {
    SEDAN(width = 1.85f, height = 1.38f, length = 4.35f),
    SPORTS_CAR(width = 1.94f, height = 1.18f, length = 4.25f),
    BOX_TRUCK(width = 2.24f, height = 2.65f, length = 6.75f),
}

class TrafficVehicle {
    var active: Boolean = false
    var type: VehicleType = VehicleType.SEDAN
    var lane: Int = 0
    var targetLane: Int = 0
    var x: Float = 0f
    var z: Float = 0f
    var speedMps: Float = 20f
    var r: Float = 0.5f
    var g: Float = 0.5f
    var b: Float = 0.6f
    var wheelAngleRad: Float = 0f
    var nearMissAwarded: Boolean = false
}

class Particle {
    var active: Boolean = false
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var vz: Float = 0f
    var life: Float = 0f
    var maxLife: Float = 1f
    var size: Float = 0.1f
    var r: Float = 1f
    var g: Float = 0.6f
    var b: Float = 0.1f
}

data class NearMissEvent(
    val id: Long,
    val bonusPoints: Int,
    val combo: Int,
)

data class GameUiState(
    val phase: GamePhase = GamePhase.MENU,
    val speedKmh: Int = 115,
    val distanceKm: Float = 0f,
    val score: Int = 0,
    val highScore: Int = 0,
    val topSpeedKmh: Int = 0,
    val nearMissCount: Int = 0,
    val comboMultiplier: Int = 1,
    val comboTimerProgress: Float = 0f,
    val nitroPercent: Float = 100f,
    val isNitroActive: Boolean = false,
    val isBraking: Boolean = false,
    val isNewRecord: Boolean = false,
    val isMuted: Boolean = false,
    val selectedLivery: BikeLivery = BikeLivery.SOLAR_EMBER,
    val activeNearMiss: NearMissEvent? = null,
    val weatherCondition: WeatherCondition = WeatherCondition.CLEAR_DUSK,
    val dailyMissions: DailyMissionsState = DailyMissionsState(),
)
