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
package com.yoonicode.calendarfaces

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.wear.watchface.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.yoonicode.calendarfaces.activities.PermissionsRequestActivity
import com.yoonicode.calendarfaces.utils.CalendarEntry
import com.yoonicode.calendarfaces.utils.CalendarUtils
import com.yoonicode.calendarfaces.utils.createComplicationSlotManager
import com.yoonicode.calendarfaces.utils.createUserStyleSchema
import java.util.*

/**
 * Handles much of the boilerplate needed to implement a watch face (minus rendering code; see
 * [AnalogWatchCanvasRenderer]) including the complications and settings (styles user can change on
 * the watch face).
 */
class EventfulService : WatchFaceService() {
    var calendarEntry: CalendarEntry? = null
    var hasPermission = false

    // Used by Watch Face APIs to construct user setting options and repository.
    override fun createUserStyleSchema(): UserStyleSchema =
        createUserStyleSchema(context = applicationContext)

    // Creates all complication user settings and adds them to the existing user settings
    // repository.
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = createComplicationSlotManager(
        context = applicationContext,
        currentUserStyleRepository = currentUserStyleRepository
    )

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = AnalogWatchCanvasRenderer(
            this,
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )

        checkForPermission()
        startListener()
        updateCalendarEntry()

        val face = WatchFace(
            watchFaceType = WatchFaceType.ANALOG,
            renderer = renderer
        )
        face.setTapListener(renderer)
        return face
    }

    private fun checkForPermission() {
        hasPermission = (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }

    fun launchPermissionGrantActivity() {
        val intent = Intent(this, PermissionsRequestActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun startListener() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateCalendarEntry()
            }
        }, 0, 1000 * 5)
    }

    private fun updateCalendarEntry() {
        if(hasPermission) {
            calendarEntry = CalendarUtils.getFirstEvent(applicationContext)
        } else {
            checkForPermission()
        }
    }

    companion object {
        const val TAG = "EventfulService"
    }
}
