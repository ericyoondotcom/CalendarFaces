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
package com.yoonicode.calendarfaces.editor

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.yoonicode.calendarfaces.data.watchface.*
import com.yoonicode.calendarfaces.databinding.ActivityWatchFaceConfigBinding
import com.yoonicode.calendarfaces.utils.BOTTOM_COMPLICATION_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchFaceConfigActivity : ComponentActivity() {
    private val stateHolder: WatchFaceConfigStateHolder by lazy {
        WatchFaceConfigStateHolder(
            lifecycleScope,
            this@WatchFaceConfigActivity
        )
    }

    private lateinit var binding: ActivityWatchFaceConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        binding = ActivityWatchFaceConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable widgets until data loads and values are set.
        binding.colorStylePickerButton.isEnabled = false
        binding.showTimeSwitch.isEnabled = false

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            stateHolder.uiState
                .collect { uiState: WatchFaceConfigStateHolder.EditWatchFaceUiState ->
                    when (uiState) {
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Loading -> {
                            Log.d(TAG, "StateFlow Loading: ${uiState.message}")
                        }
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Success -> {
                            Log.d(TAG, "StateFlow Success.")
                            updateWatchFacePreview(uiState.userStylesAndPreview)
                        }
                        is WatchFaceConfigStateHolder.EditWatchFaceUiState.Error -> {
                            Log.e(TAG, "Flow error: ${uiState.exception}")
                        }
                    }
                }
        }
    }

    private fun updateWatchFacePreview(
        userStylesAndPreview: WatchFaceConfigStateHolder.UserStylesAndPreview
    ) {
        Log.d(TAG, "updateWatchFacePreview: $userStylesAndPreview")

        val colorStyleId: String = userStylesAndPreview.colorStyleId
        Log.d(TAG, "\tselected color style: $colorStyleId")

        binding.showTimeSwitch.isChecked = userStylesAndPreview.showTime
        binding.preview.watchFaceBackground.setImageBitmap(userStylesAndPreview.previewImage)

        enabledWidgets()
    }

    private fun enabledWidgets() {
        binding.colorStylePickerButton.isEnabled = true
        binding.showTimeSwitch.isEnabled = true
    }

    fun onClickColorStylePickerButton(view: View) {
        val colorIds = arrayOf(WHITE_COLOR_STYLE_ID, BLUE_COLOR_STYLE_ID, GREEN_COLOR_STYLE_ID, RED_COLOR_STYLE_ID)
        val currentIndex = colorIds.indexOf(stateHolder.getColorStyleId())
        val newIndex = (currentIndex + 1) % colorIds.size
        stateHolder.setColorStyle(colorIds[newIndex])
    }

    fun onClickBottomComplicationButton(view: View) {
        stateHolder.setComplication(BOTTOM_COMPLICATION_ID)
    }

    fun onClickShowTimeSwitch(view: View) {
        stateHolder.setShowTime(binding.showTimeSwitch.isChecked)
    }

    companion object {
        const val TAG = "WatchFaceConfigActivity"
    }
}
