package com.abu00salman.novadrive.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Makes a small element (a clock, a glyph) tappable with an automotive-size touch area: the tap area
 * is at least [minWidth] × [minHeight] while the content keeps its own size, centred inside it.
 */
fun Modifier.tapTarget(minWidth: Dp = 44.dp, minHeight: Dp = 44.dp, onClick: () -> Unit): Modifier =
    this.defaultMinSize(minWidth, minHeight).clickable(onClick = onClick).wrapContentSize(Alignment.Center)
