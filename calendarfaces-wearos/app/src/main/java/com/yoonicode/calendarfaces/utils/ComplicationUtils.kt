/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yoonicode.calendarfaces.utils

import android.content.Context
import android.graphics.RectF
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.yoonicode.calendarfaces.R

// Creates bounds for the locations of both right and left complications. (This is the
// location from 0.0 - 1.0.)
private const val COMPLICATION_TOP_BOUND = 0.75f
private const val COMPLICATION_BOTTOM_BOUND = 0.95f

private const val COMPLICATION_LEFT_BOUND = 0.4f
private const val COMPLICATION_RIGHT_BOUND = 0.6f

private const val DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID = R.drawable.complication_red_style

// Unique IDs for each complication. The settings activity that supports allowing users
// to select their complication data provider requires numbers to be >= 0.
internal const val BOTTOM_COMPLICATION_ID = 100

/**
 * Represents the unique id associated with a complication and the complication types it supports.
 */
sealed class ComplicationConfig(val id: Int, val supportedTypes: List<ComplicationType>) {
    object Bottom : ComplicationConfig(
        BOTTOM_COMPLICATION_ID,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        )
    )
}

// Utility function that initializes default complication slots (left and right).
fun createComplicationSlotManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository,
    drawableId: Int = DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID
): ComplicationSlotsManager {
    val defaultCanvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(context, drawableId)!!,
                watchState,
                listener
            )
        }

    val bottomComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.Bottom.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.Bottom.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                COMPLICATION_LEFT_BOUND,
                COMPLICATION_TOP_BOUND,
                COMPLICATION_RIGHT_BOUND,
                COMPLICATION_BOTTOM_BOUND
            )
        )
    )
        .build()

    return ComplicationSlotsManager(
        listOf(bottomComplication),
        currentUserStyleRepository
    )
}
