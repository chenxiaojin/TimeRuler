package com.cxj.timeruler.indicator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.cxj.timeruler.R
import com.cxj.timeruler.TimeRuler
import com.cxj.timeruler.util.DisplayUtil
import com.cxj.timeruler.util.Utils
import kotlin.math.roundToInt

/**
 *  author : chenxiaojin
 *  date : 2021/4/30 下午 05:50
 *  description : 时间指示器
 *  包含动画移动, 更新, 时间变动回调, 自动计算移动范围
 */
class TimeIndicator {
    private val tag = "TimeIndicator"

    // 显示区域
    private var displayRectF: RectF = RectF()

    // 时间轴
    private var timeRuler: TimeRuler? = null

    // 是否自定义触摸区域
    private var isCustomTouchArea = false

    // 用于判断在普通模式下时间是否变更
    private var oldTimePosition = 0

    // 用于判断在完整模式下时间是否变更
    private var oldMillisecond = 0L

    // 指示器移动动画
    private var animator = ValueAnimator()

    // 指示器背景
    var backgroundBitmap: Bitmap? = null

    // 指示器当前指向的时间刻度
    var timePosition = 0

    // 指示器在当前屏幕的坐标X
    var currentPosition = 0

    // 指示器大小和起始高度
    var width: Int = 0
    var height: Int = 0
    var top: Float = 0f

    // 位置间距
    var offsetX = 0
    var offsetY = 0

    // 是否正在拖拽
    var isDragging = false

    // 时间变更监听器
    var timeListener: TimeListener? = null

    // 自定义指示器触摸区域大小
    var customTouchWidth = 0
    var customTouchHeight = 0

    // 是否可见
    var isVisible = true

    // 完整模式下的时间
    var timePositionInFullMode = 0f
    var currentPositionInFullMode = 0f

    // 指示器背景
    var indicatorBackgroundColor = 0


    constructor(timeRuler: TimeRuler) {
        this.timeRuler = timeRuler
        init(timeRuler.context)
    }

    private fun init(context: Context) {
        top = timeRuler?.rulerStartTop!! + offsetY
        width = DisplayUtil.dip2px(context, 12f)
        height = DisplayUtil.dip2px(context, 33f)
        backgroundBitmap = Utils.drawableToBitmap(
            context,
            width,
            height,
            R.mipmap.light_timeline_slider
        )
        indicatorBackgroundColor = ContextCompat.getColor(context, android.R.color.white)

        displayRectF.left = currentPosition + offsetX.toFloat()
        displayRectF.top = top + offsetY
        displayRectF.right = displayRectF.left + width
        displayRectF.bottom = displayRectF.top + height
    }

    /**
     * 指示器绘制
     * @param paint 画笔
     * @param canvas 画布
     */
    fun onDraw(paint: Paint, canvas: Canvas) {
        // 未显示时, 不绘制
        if (!isVisible) {
            return
        }
        paint.color = indicatorBackgroundColor
        // 正在执行动画时, 直接画指示器, 不做其他操作
        if (animator.isRunning) {
            canvas.drawBitmap(
                backgroundBitmap!!,
                animator.animatedValue as Float - width / 2,
                displayRectF.top,
                paint
            )
            timeRuler?.invalidate()
        } else {
            // 正在拖拽则直接绘制, 不做其他操作
            if (isDragging) {
                canvas.drawBitmap(
                    backgroundBitmap!!,
                    displayRectF.left,
                    displayRectF.top,
                    paint
                )
            }
            // 指示器在屏幕内才绘制, 避免浪费性能
            else if (timePosition - timeRuler?.currentLeft!! < timeRuler?.width!! + width) {
                // 滚动时刷新指示器在当前屏幕中的相对坐标
                currentPosition = (timePosition - timeRuler?.currentLeft!!).toInt()
                refreshRect(currentPosition.toFloat())

                canvas.drawBitmap(
                    backgroundBitmap!!,
                    displayRectF.left,
                    displayRectF.top,
                    paint
                )
            }
        }
    }

    /**
     * 完整模式下的绘制
     * @param paint 画笔
     * @param canvas 画布
     */
    fun onDrawInFullMode(paint: Paint, canvas: Canvas) {
        // 未显示时不绘制
        if (!isVisible) {
            return
        }
        paint.color = indicatorBackgroundColor
        // 正在拖拽则直接绘制, 不做其他操作
        if (isDragging) {
            canvas.drawBitmap(
                backgroundBitmap!!,
                displayRectF.left,
                displayRectF.top,
                paint
            )
        } else {
            // 滚动时刷新指示器在当前屏幕中的相对坐标
            currentPositionInFullMode = timePositionInFullMode + timeRuler!!.getSegmentStartOffset()
            refreshRect(currentPositionInFullMode)
            canvas.drawBitmap(
                backgroundBitmap!!,
                displayRectF.left,
                displayRectF.top,
                paint
            )
        }
    }

    /**
     * 更新指示器位置
     * 自动计算位置, 保证指示器指向对应的刻度
     * @param position 屏幕坐标x
     */
    fun updatePosition(position: Float) {
        if (timeRuler!!.isFullMode()) {
            updatePositionInFullMode(position)
            return
        }
        // 计算当前点击的时间位置,
        var timePos: Int
        val latestIndicatorPos = currentPosition
        val currentLeft = timeRuler?.currentLeft!!
        val segmentWidth = timeRuler?.segmentWidth!!
        // currentLeft<0表示当前时间轴刚开始, 需要减去padding才能得到正确位置
        if (currentLeft < 0) {
            // 指示器位置不能小于刻度起始位置, 小于时设置为刻度起始位置
            timePos = position.toInt() + currentLeft.toInt()
            if (timePos < timeRuler!!.getSegmentStartOffset()) {
                timePos = timeRuler!!.getSegmentStartOffset().toInt()
            }
        } else {
            // 指示器位置不能大于刻度尺结束位置, 大于时设置为刻度结束给位置
            timePos = (currentLeft + position).toInt()
            if (timePos > timeRuler!!.getSegmentMaxWidth()) {
                timePos = timeRuler!!.getSegmentMaxWidth()
            }
        }
        // 计算当前对应的刻度索引
        var timeIndex = timePos / segmentWidth
        // 时间不能整除时, 要根据左右偏移大小选择距离最近的时间刻度作为当前时间
        val offset = timePos % segmentWidth
        if (offset != 0 && offset > segmentWidth / 2) {
            timeIndex += 1
        }
        timePosition = timeIndex * segmentWidth
        currentPosition = timePosition - currentLeft.toInt()

        // 匹配时间是否变更, 变更则回调
        if (oldTimePosition != timePosition) {
            oldTimePosition = timePosition
            timeListener?.onPositionChanged(timePosition.toFloat())
        }

        // 进行位置偏差移动
        doIndicatorAnimate(
            latestIndicatorPos.toFloat() + offsetX,
            currentPosition.toFloat() + offsetX
        )
    }

    /**
     *指示器移动到指定位置
     * @param position 屏幕坐标位置x
     */
    fun moveTo(position: Float) {
        if (timeRuler!!.isFullMode()) {
            moveToInFullMode(position)
            return
        }
        currentPosition = when {
            // 小于最左刻度, 赋值为最左刻度
            position < timeRuler!!.getSegmentStartOffset().toInt() -> {
                timeRuler!!.getSegmentStartOffset().toInt()
            } // 大于最右刻度, 赋值为最右刻度
            position > timeRuler!!.getSegmentMaxWidth() - timeRuler!!.currentLeft -> {
                (timeRuler!!.getSegmentMaxWidth() - timeRuler!!.currentLeft).toInt()
            }
            else -> {
                position.toInt()
            }
        }
        refreshRect(currentPosition.toFloat())
        timeRuler?.invalidate()
    }

    /**
     * 完整模式下的移动指示器操作
     * @param position 屏幕坐标位置x
     */
    private fun moveToInFullMode(position: Float) {
        currentPositionInFullMode = position
        // 计算范围, 不能超过时间轴起始和结束范围
        if (currentPositionInFullMode < timeRuler!!.getSegmentStartOffset()) {
            currentPositionInFullMode = timeRuler!!.getSegmentStartOffset()
        } else if (currentPositionInFullMode > timeRuler!!.getScreenWidth()
            - timeRuler!!.getSegmentEndOffset()
        ) {
            currentPositionInFullMode =
                timeRuler!!.getScreenWidth() - timeRuler!!.getSegmentEndOffset()
        }
        timePositionInFullMode = currentPositionInFullMode - timeRuler!!.getSegmentStartOffset()

        // 计算时间(毫秒, 按200ms整除)
        val millisecond = (((timePositionInFullMode * timeRuler!!.getTimeOfPerPixel()).toInt()
                / 200) * 200).toLong()
        // 时间变动时回调变动函数
        if (oldMillisecond != millisecond) {
            oldMillisecond = millisecond
            timeListener?.onPositionChanged(timePositionInFullMode)
        }
        refreshRect(currentPositionInFullMode)
        timeRuler?.invalidate()
    }

    /**
     * 完整模式下的更新指示器坐标
     * @param position 屏幕坐标x
     */
    private fun updatePositionInFullMode(position: Float) {
        // 计算当前坐标, 不能超过时间轴起始和结束坐标, 超过则强制纠正
        currentPositionInFullMode = when {
            position < timeRuler!!.getSegmentStartOffset() -> {
                timeRuler!!.getSegmentStartOffset()
            }
            position > timeRuler!!.getScreenWidth() - timeRuler!!.getSegmentEndOffset() -> {
                timeRuler!!.getScreenWidth() - timeRuler!!.getSegmentEndOffset()
            }
            else -> {
                position
            }
        }
        // 实际位置=当前位置-时间轴起始位置
        timePositionInFullMode = currentPositionInFullMode - timeRuler!!.getSegmentStartOffset()
        var currentTime = (timePositionInFullMode * timeRuler!!.getTimeOfPerPixel()).roundToInt()
        // 不是刚好被200毫秒整除的情况, 调整到200毫秒
        if (currentTime % 200 != 0) {
            timePositionInFullMode = ((currentTime / 200) * 200 / timeRuler!!.getTimeOfPerPixel())
            currentTime = (timePositionInFullMode * timeRuler!!.getTimeOfPerPixel()).roundToInt()
        }
        currentPositionInFullMode = timePositionInFullMode + timeRuler!!.getSegmentStartOffset()
        refreshRect(currentPositionInFullMode)
        // 时间变动后回调
        if (oldMillisecond != currentTime.toLong()) {
            oldMillisecond = currentTime.toLong()
            timeListener?.onPositionChanged(timePositionInFullMode)
        }
        timeRuler?.invalidate()
    }

    /**
     * 刷新指示器位置
     * @param currentPosition 当前指示器位置
     */
    private fun refreshRect(currentPosition: Float) {
        displayRectF.left = currentPosition - width / 2 + offsetX.toFloat()
        displayRectF.top = top + offsetY
        displayRectF.right = displayRectF.left + width * 2
        displayRectF.bottom = displayRectF.top + height * 2
    }

    /**
     * 执行动画滑动
     * @param startValue 开始滑动坐标
     * @param endValue 停止滑动坐标
     */
    private fun doIndicatorAnimate(startValue: Float, endValue: Float) {
        animator.setFloatValues(
            startValue,
            endValue
        )
        animator.start()
        timeRuler?.invalidate()
    }

    /**
     * 是否按住了指示器
     * @param x 屏幕坐标x
     * @param y 屏幕坐标y
     */
    fun isTouched(x: Int, y: Int): Boolean {
        // 自定义了触摸区域, 则只校验触摸区域, 否则根据指示器大小校验
        return if (!isCustomTouchArea) {
            ((x >= displayRectF.left - offsetX && x <= displayRectF.right - offsetX)
                    && (y > displayRectF.top - offsetY && y <= displayRectF.bottom - offsetY))
        } else {
            ((x >= displayRectF.left - offsetX && x <= displayRectF.left - offsetX + customTouchWidth)
                    && (y > displayRectF.top - offsetY && y <= displayRectF.top - offsetY + customTouchHeight))
        }
    }

    /**
     * 自定义触摸区域
     * @param width 触摸宽度(px)
     * @param height 触摸高度(px)
     */
    fun setTouchArea(width: Int, height: Int) {
        isCustomTouchArea = true
        customTouchWidth = width
        customTouchHeight = height
    }

    /**
     * 时间变动监听器
     */
    interface TimeListener {
        /**
         * 时间位置变动
         * @param timePosition 时间转换后的实际坐标位置
         */
        fun onPositionChanged(timePosition: Float)
    }
}