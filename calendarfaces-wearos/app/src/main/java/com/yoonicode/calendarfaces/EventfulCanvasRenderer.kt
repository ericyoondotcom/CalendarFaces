package com.yoonicode.calendarfaces

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.text.format.DateUtils
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.WatchFaceLayer
import com.yoonicode.calendarfaces.data.watchface.ColorStyleIdAndResourceIds
import com.yoonicode.calendarfaces.data.watchface.WatchFaceColorPalette.Companion.convertToWatchFaceColorPalette
import com.yoonicode.calendarfaces.data.watchface.WatchFaceData
import com.yoonicode.calendarfaces.utils.COLOR_STYLE_SETTING
import com.yoonicode.calendarfaces.utils.SHOW_TIME_STYLE_SETTING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 16L

/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class AnalogWatchCanvasRenderer(
    private val service: EventfulService,
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<AnalogWatchCanvasRenderer.AnalogSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {
    class AnalogSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var watchFaceData: WatchFaceData = WatchFaceData()

    private var watchFaceColors = convertToWatchFaceColorPalette(
        context,
        watchFaceData.highlightColorStyle,
        watchFaceData.ambientColorStyle
    )

    private val timeTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.time_text_size).toFloat()
    }
    private val headerTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.header_text_size).toFloat()
    }
    private val eventNameTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.event_name_size).toFloat()
    }
    private val eventTimeTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.event_time_size).toFloat()
    }

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
    }

    override suspend fun createSharedAssets(): AnalogSharedAssets {
        return AnalogSharedAssets()
    }

    /*
     * Triggered when the user makes changes to the watch face through the settings activity. The
     * function is called by a flow.
     */
    private fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")

        var newWatchFaceData: WatchFaceData = watchFaceData

        // Loops through user style and applies new values to watchFaceData.
        for (options in userStyle) {
            when (options.key.id.toString()) {
                COLOR_STYLE_SETTING -> {
                    val listOption = options.value as
                        UserStyleSetting.ListUserStyleSetting.ListOption

                    newWatchFaceData = newWatchFaceData.copy(
                        highlightColorStyle = ColorStyleIdAndResourceIds.getColorStyleConfig(
                            listOption.id.toString()
                        )
                    )
                }
                SHOW_TIME_STYLE_SETTING -> {
                    val booleanValue = options.value as
                        UserStyleSetting.BooleanUserStyleSetting.BooleanOption

                    newWatchFaceData = newWatchFaceData.copy(
                        showTime = booleanValue.value
                    )
                }
            }
        }

        // Only updates if something changed.
        if (watchFaceData != newWatchFaceData) {
            watchFaceData = newWatchFaceData

            // Recreates Color and ComplicationDrawable from resource ids.
            watchFaceColors = convertToWatchFaceColorPalette(
                context,
                watchFaceData.highlightColorStyle,
                watchFaceData.ambientColorStyle
            )

            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                ComplicationDrawable.getDrawable(
                    context,
                    watchFaceColors.complicationStyleDrawableId
                )?.let {
                    (complication.renderer as CanvasComplicationDrawable).drawable = it
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("AnalogWatchCanvasRenderer scope clear() request")
        super.onDestroy()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: AnalogSharedAssets
    ) {
        val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            watchFaceColors.ambientBackgroundColor
        } else {
            watchFaceColors.activeBackgroundColor
        }
        canvas.drawColor(backgroundColor)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            drawTextDisplays(canvas, bounds, zonedDateTime);
        }

        drawComplications(canvas, zonedDateTime);
    }

    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawTextDisplays(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        if(watchFaceData.showTime) {
            val formatter = DateTimeFormatter.ofPattern("h:mm")
            val timeContent = zonedDateTime.toLocalDateTime().format(formatter)
            timeTextPaint.color = watchFaceColors.activeForegroundColor
            val timePosOffset = context.resources.getDimension(R.dimen.time_pos)
            val timeBounds = Rect() // timeBounds is like an out parameter
            timeTextPaint.getTextBounds(timeContent, 0, timeContent.length, timeBounds)
            canvas.drawText(
                timeContent,
                (bounds.centerX() - timeBounds.width() / 2).toFloat(),
                bounds.top + timePosOffset + timeBounds.height() / 2,
                timeTextPaint
            )
        }

        if(service.calendarEntry == null) {
            return
        }

        val contentArea = Rect(
            (context.resources.getFraction(R.fraction.content_area_padding_x, bounds.width(), 0) + bounds.left).toInt(),
            (context.resources.getFraction(R.fraction.content_area_padding_y, bounds.height(), 0) + bounds.top).toInt(),
            (bounds.right - context.resources.getFraction(R.fraction.content_area_padding_x, bounds.width(), 0)).toInt(),
            (bounds.bottom - context.resources.getFraction(R.fraction.content_area_padding_y, bounds.height(), 0)).toInt()
        )
        val paddingBetweenText = context.resources.getDimension(R.dimen.padding_between_text)

        val xOrigin = contentArea.left.toFloat()
        val yOrigin = contentArea.centerY().toFloat()

        eventNameTextPaint.color = watchFaceColors.activeForegroundColor
        val eventName = service.calendarEntry!!.title
        val eventNameBounds = Rect();
        eventNameTextPaint.getTextBounds(eventName, 0, eventName.length, eventNameBounds)
        canvas.drawText(eventName, xOrigin, yOrigin, eventNameTextPaint)

        headerTextPaint.color = watchFaceColors.activeHighlightColor
        val headerContent = context.resources.getString(R.string.header_text_content)
        val headerBounds = Rect();
        headerTextPaint.getTextBounds(headerContent, 0, headerContent.length, headerBounds);
        canvas.drawText(
            headerContent,
            xOrigin,
            yOrigin - (eventNameBounds.height() / 2) - paddingBetweenText - (headerBounds.height() / 2),
            headerTextPaint
        )

        eventTimeTextPaint.color = watchFaceColors.activeForegroundColor
        val eventTime = DateUtils.getRelativeTimeSpanString(
            service.calendarEntry!!.startTime.timeInMillis,
            Calendar.getInstance().timeInMillis,
            DateUtils.SECOND_IN_MILLIS
        ).toString()
        val eventTimeBounds = Rect();
        eventTimeTextPaint.getTextBounds(eventTime, 0, eventTime.length, eventTimeBounds)
        canvas.drawText(
            eventTime,
            xOrigin,
            yOrigin + (eventNameBounds.height() / 2) + paddingBetweenText + (eventTimeBounds.height() / 2),
            eventTimeTextPaint
        )
    }

    companion object {
        private const val TAG = "AnalogWatchCanvasRenderer"
    }
}
