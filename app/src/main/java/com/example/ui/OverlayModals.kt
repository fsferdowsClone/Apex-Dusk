package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.game.model.BikeLivery
import com.example.game.model.DailyMissionItem
import com.example.game.model.DailyMissionType
import com.example.game.model.DailyMissionsState
import com.example.game.model.GameUiState
import com.example.ui.theme.CrimsonBrake
import com.example.ui.theme.DuskVoid
import com.example.ui.theme.GlassBorderAmber
import com.example.ui.theme.GlassBorderLight
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
fun StartScreenOverlay(
    state: GameUiState,
    onRideNow: () -> Unit,
    onSelectLivery: (BikeLivery) -> Unit,
    onToggleMute: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        // Top Header Bar: Live Weather Pill + Info & Audio Toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeatherPill(weather = state.weatherCondition)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.cd_about_privacy),
                    testTag = "about_privacy_button",
                    onClick = onOpenAbout,
                )
                GlassIconButton(
                    icon = if (state.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (state.isMuted) stringResource(R.string.cd_unmute_audio) else stringResource(R.string.cd_mute_audio),
                    testTag = "start_mute_button",
                    onClick = onToggleMute,
                )
            }
        }

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp),
        ) {
            val isWide = maxWidth >= 720.dp

            if (isWide) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 920.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        StartMainCard(
                            state = state,
                            onRideNow = onRideNow,
                            onSelectLivery = onSelectLivery,
                            modifier = Modifier.weight(1f),
                        )
                        DailyMissionsCard(
                            missions = state.dailyMissions,
                            modifier = Modifier.weight(0.9f),
                        )
                    }
                    DemoShowcaseCard(
                        onCardClick = onOpenAbout,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    StartMainCard(
                        state = state,
                        onRideNow = onRideNow,
                        onSelectLivery = onSelectLivery,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DailyMissionsCard(
                        missions = state.dailyMissions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DemoShowcaseCard(
                        onCardClick = onOpenAbout,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StartMainCard(
    state: GameUiState,
    onRideNow: () -> Unit,
    onSelectLivery: (BikeLivery) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        borderColor = GlassBorderAmber,
        backgroundColor = GlassSurfaceElevated,
        cornerRadius = 26.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_favicon),
                    contentDescription = stringResource(R.string.cd_game_emblem),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(SunsetAmber, HorizonMagenta)),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .testTag("start_favicon_emblem"),
                )

                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(Locale.US),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = 2.5.sp,
                        color = TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(R.string.subtitle_tagline),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.8.sp,
                        color = SunsetAmber,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // High Score Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = SunsetGold,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.label_high_score),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Text(
                    text = String.format(Locale.US, "%,d", state.highScore),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SunsetGold,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bike Color Selector
            Text(
                text = "${stringResource(R.string.label_bike_livery)} • ${state.selectedLivery.label.uppercase(Locale.US)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BikeLivery.entries.forEach { livery ->
                    val selected = livery == state.selectedLivery
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("livery_${livery.id}")
                            .semantics {
                                contentDescription = livery.label
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(livery.primaryColor, livery.accentColor)
                                )
                            )
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else GlassBorderLight,
                                shape = CircleShape,
                            )
                            .clickable { onSelectLivery(livery) },
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = DuskVoid,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ride Now CTA
            Button(
                onClick = onRideNow,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("ride_now_button"),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(SunsetAmber, HorizonMagenta)
                            )
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_ride_now),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            letterSpacing = 1.5.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.hint_controls),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun DailyMissionsCard(
    missions: DailyMissionsState,
    modifier: Modifier = Modifier,
) {
    val activeMult = missions.scoreMultiplier
    GlassPanel(
        borderColor = if (activeMult > 1.0f) NitroCyan.copy(alpha = 0.65f) else GlassBorderLight,
        backgroundColor = GlassSurfaceElevated,
        cornerRadius = 22.dp,
        modifier = modifier.testTag("daily_missions_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = NitroCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.label_daily_missions),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeMult > 1.0f) {
                                Brush.horizontalGradient(listOf(NitroCyan.copy(alpha = 0.35f), HorizonMagenta.copy(alpha = 0.35f)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0x26FFFFFF), Color(0x26FFFFFF)))
                            }
                        )
                        .border(
                            1.dp,
                            if (activeMult > 1.0f) NitroCyan else GlassBorderLight,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = String.format(Locale.US, "%.2fx %s", activeMult, stringResource(R.string.label_score)),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (activeMult > 1.0f) NitroCyan else TextSecondary,
                    )
                }
            }

            missions.items.forEach { item ->
                DailyMissionRow(item = item)
            }
        }
    }
}

@Composable
private fun DailyMissionRow(item: DailyMissionItem) {
    val title = when (item.type) {
        DailyMissionType.NEAR_MISSES -> stringResource(R.string.mission_near_miss_title)
        DailyMissionType.DISTANCE_METERS -> stringResource(R.string.mission_distance_title)
        DailyMissionType.TOP_SPEED_KMH -> stringResource(R.string.mission_speed_title)
    }

    val progressLabel = when (item.type) {
        DailyMissionType.NEAR_MISSES -> "${item.currentValue} / ${item.type.targetValue}"
        DailyMissionType.DISTANCE_METERS -> String.format(
            Locale.US,
            "%.1f / %.1f %s",
            item.currentValue / 1000f,
            item.type.targetValue / 1000f,
            stringResource(R.string.label_km),
        )
        DailyMissionType.TOP_SPEED_KMH -> "${item.currentValue} / ${item.type.targetValue} ${stringResource(R.string.label_kmh)}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mission_${item.type.id}")
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22FFFFFF))
            .border(
                width = 1.dp,
                color = if (item.isCompleted) NitroCyan.copy(alpha = 0.6f) else GlassBorderLight,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 13.sp,
                color = if (item.isCompleted) SunsetGold else TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (item.isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = NitroCyan,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = if (item.isCompleted) {
                        stringResource(R.string.label_mission_completed)
                    } else {
                        String.format(Locale.US, "+%.2fx", item.type.bonusMultiplier)
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (item.isCompleted) NitroCyan else SunsetAmber,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0x2FFFFFFF)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressFraction)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                if (item.isCompleted) {
                                    listOf(NitroCyan, SunsetGold)
                                } else {
                                    listOf(SunsetAmber, HorizonMagenta)
                                }
                            )
                        ),
                )
            }

            Text(
                text = progressLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }
    }
}

@Composable
fun PauseModal(
    state: GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onGarage: () -> Unit,
    onToggleMute: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88070414))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        GlassPanel(
            borderColor = GlassBorderLight,
            backgroundColor = GlassSurfaceElevated,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 390.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.title_paused),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = 2.sp,
                    color = TextPrimary,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunsetAmber,
                        contentColor = DuskVoid,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("resume_button"),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_resume),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GlassBorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("restart_button"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_restart),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onGarage,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorderLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_button"),
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_garage),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextSecondary,
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleMute,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorderLight),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pause_mute_button"),
                    ) {
                        Icon(
                            imageVector = if (state.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (state.isMuted) stringResource(R.string.cd_unmute_audio) else stringResource(R.string.cd_mute_audio),
                            tint = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverModal(
    state: GameUiState,
    onPlayAgain: () -> Unit,
    onGarage: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99070414))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        GlassPanel(
            borderColor = if (state.isNewRecord) SunsetGold else CrimsonBrake.copy(alpha = 0.7f),
            backgroundColor = GlassSurfaceElevated,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                if (state.isNewRecord) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(SunsetAmber, HorizonMagenta)))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.badge_new_record),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = stringResource(R.string.title_game_over),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    letterSpacing = 2.sp,
                    color = if (state.isNewRecord) SunsetGold else CrimsonBrake,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 2x2 Summary Stat Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatTile(
                        label = stringResource(R.string.label_score),
                        value = String.format(Locale.US, "%,d", state.score),
                        accent = SunsetGold,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.label_distance),
                        value = String.format(Locale.US, "%.2f %s", state.distanceKm, stringResource(R.string.label_km)),
                        accent = NitroCyan,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatTile(
                        label = stringResource(R.string.label_top_speed),
                        value = "${state.topSpeedKmh} ${stringResource(R.string.label_kmh)}",
                        accent = SunsetAmber,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.label_near_misses),
                        value = "${state.nearMissCount}",
                        accent = HorizonMagenta,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = SunsetGold,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${stringResource(R.string.label_best_score)}: ${String.format(Locale.US, "%,d", state.highScore)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onGarage,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorderLight),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(52.dp)
                            .testTag("garage_button"),
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_garage),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary,
                        )
                    }

                    Button(
                        onClick = onPlayAgain,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(0.58f)
                            .height(52.dp)
                            .testTag("play_again_button"),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(SunsetAmber, HorizonMagenta))),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Text(
                                    text = stringResource(R.string.action_play_again),
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutPrivacyModal(
    onDismiss: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6070414))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        GlassPanel(
            borderColor = NitroCyan.copy(alpha = 0.55f),
            backgroundColor = GlassSurfaceElevated,
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .testTag("about_privacy_modal"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_favicon),
                            contentDescription = stringResource(R.string.cd_game_emblem),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SunsetAmber.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                        )
                        Text(
                            text = stringResource(R.string.title_about_privacy),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.4.sp,
                            color = TextPrimary,
                        )
                    }
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SunsetAmber,
                    )
                }

                DemoShowcaseStrip(
                    onItemClick = {},
                    clickable = false,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = NitroCyan,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.label_privacy_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = NitroCyan,
                        )
                    }
                    Text(
                        text = stringResource(R.string.body_privacy_disclosure),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_gameplay_tips_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetGold,
                    )
                    Text(
                        text = stringResource(R.string.body_gameplay_tips),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunsetAmber,
                        contentColor = DuskVoid,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_about_button"),
                ) {
                    Text(
                        text = stringResource(R.string.action_close),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

private data class DemoShowcaseItem(
    val drawableRes: Int,
    val captionRes: Int,
    val accent: Color,
)

private val demoShowcaseItems = listOf(
    DemoShowcaseItem(
        drawableRes = R.drawable.img_demo_dusk_highway,
        captionRes = R.string.demo_caption_dusk,
        accent = SunsetAmber,
    ),
    DemoShowcaseItem(
        drawableRes = R.drawable.img_demo_nitro_rain,
        captionRes = R.string.demo_caption_rain,
        accent = NitroCyan,
    ),
    DemoShowcaseItem(
        drawableRes = R.drawable.img_demo_garage_showroom,
        captionRes = R.string.demo_caption_garage,
        accent = HorizonMagenta,
    ),
)

@Composable
private fun DemoShowcaseCard(
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        borderColor = GlassBorderLight,
        backgroundColor = GlassSurfaceElevated,
        cornerRadius = 20.dp,
        modifier = modifier.testTag("demo_showcase_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.label_demo_showcase),
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp,
                color = TextPrimary,
            )
            DemoShowcaseStrip(
                onItemClick = onCardClick,
                clickable = true,
            )
        }
    }
}

@Composable
private fun DemoShowcaseStrip(
    onItemClick: () -> Unit,
    clickable: Boolean,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(demoShowcaseItems) { item ->
            val caption = stringResource(item.captionRes)
            Box(
                modifier = Modifier
                    .width(172.dp)
                    .height(98.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, item.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .then(
                        if (clickable) {
                            Modifier.clickable(onClick = onItemClick)
                        } else {
                            Modifier
                        }
                    ),
            ) {
                Image(
                    painter = painterResource(id = item.drawableRes),
                    contentDescription = caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC070414))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x28FFFFFF))
            .border(1.dp, GlassBorderLight, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = accent,
        )
    }
}
