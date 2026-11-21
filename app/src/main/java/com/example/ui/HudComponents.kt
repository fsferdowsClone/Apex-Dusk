package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.GameUiState
import com.example.game.model.WeatherCondition
import com.example.ui.theme.CrimsonBrake
import com.example.ui.theme.GlassBorderAmber
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.HorizonMagenta
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NitroCyan
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SunsetAmber
import com.example.ui.theme.SunsetGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorderLight,
    backgroundColor: Color = GlassSurfaceDark,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        content = content,
    )
}

@Composable
fun InGameHud(
    state: GameUiState,
    onPauseClick: () -> Unit,
    onToggleMute: () -> Unit,
    onSteerLeft: (Boolean) -> Unit,
    onSteerRight: (Boolean) -> Unit,
    onAccelerate: (Boolean) -> Unit,
    onBrake: (Boolean) -> Unit,
    onNitro: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Top HUD bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // Left: Score, Combo & Distance
            GlassPanel(
                borderColor = if (state.comboMultiplier > 1) GlassBorderAmber else GlassBorderLight,
                cornerRadius = 16.dp,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.label_score),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        if (state.comboMultiplier > 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(HorizonMagenta, SunsetAmber)
                                        )
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "x${state.comboMultiplier}",
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "%,d", state.score),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextPrimary,
                    )

                    if (state.comboMultiplier > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(108.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(state.comboTimerProgress)
                                    .height(3.dp)
                                    .background(
                                        Brush.horizontalGradient(listOf(SunsetAmber, HorizonMagenta))
                                    ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f %s", state.distanceKm, stringResource(R.string.label_km)),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = SunsetGold,
                        )
                        val missionMult = state.dailyMissions.scoreMultiplier
                        if (missionMult > 1.0f) {
                            Text(
                                text = String.format(Locale.US, "%s %.2fx", stringResource(R.string.label_mission_multiplier), missionMult),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NitroCyan,
                            )
                        }
                    }
                }
            }

            // Right: Weather Pill + Mute & Pause buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val muteDesc = if (state.isMuted) {
                        stringResource(R.string.cd_unmute_audio)
                    } else {
                        stringResource(R.string.cd_mute_audio)
                    }
                    GlassIconButton(
                        icon = if (state.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = muteDesc,
                        testTag = "mute_button",
                        onClick = onToggleMute,
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Pause,
                        contentDescription = stringResource(R.string.cd_pause_game),
                        testTag = "pause_button",
                        onClick = onPauseClick,
                    )
                }
                WeatherPill(weather = state.weatherCondition)
            }
        }

        // Near-Miss Floating Popup
        AnimatedVisibility(
            visible = state.activeNearMiss != null,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            ) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 84.dp),
        ) {
            state.activeNearMiss?.let { event ->
                GlassPanel(
                    borderColor = SunsetAmber,
                    backgroundColor = Color(0xCC220B38),
                    cornerRadius = 16.dp,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.label_near_miss_popup),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SunsetGold,
                        )
                        Text(
                            text = "+${event.bonusPoints}  •  COMBO x${event.combo}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NitroCyan,
                        )
                    }
                }
            }
        }

        // Bottom Telemetry & Ergonomic Touch Controls
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            val isWide = maxWidth >= 600.dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Telemetry Bar (Speed + Nitro)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Speedometer
                    GlassPanel(
                        borderColor = when {
                            state.isNitroActive -> NitroCyan
                            state.isBraking -> CrimsonBrake
                            else -> GlassBorderLight
                        },
                        cornerRadius = 14.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "${state.speedKmh}",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isWide) 28.sp else 24.sp,
                                color = when {
                                    state.isNitroActive -> NitroCyan
                                    state.isBraking -> CrimsonBrake
                                    else -> TextPrimary
                                },
                            )
                            Text(
                                text = stringResource(R.string.label_kmh),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 3.dp),
                            )
                        }
                    }

                    // Glowing Nitro Bar
                    val animatedNitro by animateFloatAsState(
                        targetValue = (state.nitroPercent / 100f).coerceIn(0f, 1f),
                        label = "nitroFill",
                    )
                    GlassPanel(
                        borderColor = if (state.isNitroActive) NitroCyan else GlassBorderLight,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = if (state.isNitroActive) NitroCyan else SunsetAmber,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.label_nitro),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.isNitroActive) NitroCyan else TextSecondary,
                                    )
                                }
                                Text(
                                    text = "${state.nitroPercent.toInt()}%",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedNitro)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NitroCyan, HorizonMagenta, SunsetAmber)
                                            )
                                        ),
                                )
                            }
                        }
                    }
                }

                // Touch Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // Left Steering Cluster
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HoldControlButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            label = null,
                            contentDescription = stringResource(R.string.cd_steer_left),
                            testTag = "steer_left_button",
                            accentColor = SunsetAmber,
                            size = 64.dp,
                            onPressedChanged = onSteerLeft,
                        )
                        HoldControlButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            label = null,
                            contentDescription = stringResource(R.string.cd_steer_right),
                            testTag = "steer_right_button",
                            accentColor = SunsetAmber,
                            size = 64.dp,
                            onPressedChanged = onSteerRight,
                        )
                    }

                    // Right Pedals + Nitro Cluster
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        HoldControlButton(
                            icon = Icons.Rounded.KeyboardArrowDown,
                            label = "BRK",
                            contentDescription = stringResource(R.string.cd_brake),
                            testTag = "brake_button",
                            accentColor = CrimsonBrake,
                            size = 56.dp,
                            onPressedChanged = onBrake,
                        )
                        HoldControlButton(
                            icon = Icons.Rounded.KeyboardArrowUp,
                            label = "GAS",
                            contentDescription = stringResource(R.string.cd_accelerate),
                            testTag = "accelerate_button",
                            accentColor = SunsetGold,
                            size = 60.dp,
                            onPressedChanged = onAccelerate,
                        )
                        HoldControlButton(
                            icon = Icons.Rounded.Bolt,
                            label = "NOS",
                            contentDescription = stringResource(R.string.cd_nitro),
                            testTag = "nitro_button",
                            accentColor = NitroCyan,
                            size = 66.dp,
                            onPressedChanged = onNitro,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherPill(
    weather: WeatherCondition,
    modifier: Modifier = Modifier,
) {
    val (icon, labelRes, tint) = when (weather) {
        WeatherCondition.CLEAR_DUSK -> Triple(Icons.Rounded.WbTwilight, R.string.weather_clear_dusk, SunsetGold)
        WeatherCondition.LIGHT_MIST -> Triple(Icons.Rounded.Cloud, R.string.weather_light_mist, TextSecondary)
        WeatherCondition.LIGHT_RAIN -> Triple(Icons.Rounded.Grain, R.string.weather_light_rain, NitroCyan)
    }
    GlassPanel(
        cornerRadius = 12.dp,
        borderColor = GlassBorderLight,
        modifier = modifier.testTag("weather_pill"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(labelRes),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextPrimary,
            )
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = GlassSurfaceDark,
        border = BorderStroke(1.dp, GlassBorderLight),
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun HoldControlButton(
    icon: ImageVector,
    label: String?,
    contentDescription: String,
    testTag: String,
    accentColor: Color,
    size: Dp,
    onPressedChanged: (Boolean) -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isPressed) accentColor.copy(alpha = 0.36f) else GlassSurfaceElevated
            )
            .border(
                width = if (isPressed) 1.5.dp else 1.dp,
                color = if (isPressed) accentColor else GlassBorderLight,
                shape = RoundedCornerShape(18.dp),
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (anyPressed != isPressed) {
                            isPressed = anyPressed
                            onPressedChanged(anyPressed)
                        }
                    }
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPressed) accentColor else TextPrimary,
                modifier = Modifier.size(if (label == null) 30.dp else 22.dp),
            )
            if (label != null) {
                Text(
                    text = label,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = if (isPressed) accentColor else TextSecondary,
                )
            }
        }
    }
}
