package com.abu00salman.novadrive.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.abu00salman.novadrive.ui.theme.tr

/** True when the experimental Nova AI is switched on; otherwise the voice button is «أمرني». */
val LocalNovaAi = staticCompositionLocalOf { false }

/** Name of the voice button in every interface. */
@Composable
fun voiceName(): String = if (LocalNovaAi.current) "Nova AI" else tr("Ask me", "أمرني")

@Composable
fun voiceIcon(): ImageVector = if (LocalNovaAi.current) Icons.Rounded.AutoAwesome else Icons.Rounded.Mic
