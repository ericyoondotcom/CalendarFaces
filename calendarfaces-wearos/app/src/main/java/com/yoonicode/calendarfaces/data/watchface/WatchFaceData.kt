/*
 * Copyright 2021 The Android Open Source Project
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
package com.yoonicode.calendarfaces.data.watchface

// Defaults for the watch face. All private values aren't editable by the user, so they don't need
// to be exposed as settings defaults.
const val SHOW_TIME_DEFAULT = true

/**
 * Represents all data needed to render an analog watch face.
 */
data class WatchFaceData(
    val highlightColorStyle: ColorStyleIdAndResourceIds = ColorStyleIdAndResourceIds.RED,
    val ambientColorStyle: ColorStyleIdAndResourceIds = ColorStyleIdAndResourceIds.AMBIENT,
    val showTime: Boolean = SHOW_TIME_DEFAULT
)
