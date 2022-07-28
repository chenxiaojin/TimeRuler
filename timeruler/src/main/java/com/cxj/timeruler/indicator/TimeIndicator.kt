package com.cxj.timeruler.indicator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
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

    // 用于判断在完整模式下时间是否变更
    private var oldMillisecond = 0L

    // 指示器移动动画
    private var animator = ValueAnimator()

    // 指示器背景
    var backgroundBitmap: Bitmap? = null

    // 指示器当前所在真实位置(大小可能超过屏幕宽度)
    private var realPosition = 0f

    // 指示器在当前屏幕的坐标X
    var currentPosition = 0f

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

    // 指示器背景
    var indicatorBackgroundColor = 0

    private var time = 0L


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
        realPosition = timeRuler!!.getSegmentBeginPosition()

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
            else if (realPosition - timeRuler?.currentLeft!! < timeRuler?.width!! + width) {
                // 滚动时刷新指示器在当前屏幕中的相对坐标
                currentPosition = (realPosition - timeRuler?.currentLeft!!)
                refreshRect(realPosition - timeRuler!!.currentLeft)

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
     * 更新指示器位置
     * 自动计算位置, 保证指示器指向对应的刻度
     * @param position 屏幕坐标x
     */
    fun updatePosition(position: Float) {
        Log.e(tag, "update position:$position")
        // 上一次的位置(在当前屏幕的相对位置)
        val latestIndicatorPos = currentPosition
        // 计算当前坐标, 不能超过时间轴起始和结束坐标, 超过则强制纠正
        val fixedPosition = checkAndFixToRealPosition(position)
        Log.e(tag, "Move position: update fixPosition:$fixedPosition")
        // 实际位置=当前位置-时间轴起始位置
        var timePosition = fixedPosition - timeRuler!!.getSegmentBeginPosition()
        var currentTime = (timePosition * timeRuler!!.getTimeOfPerPixel()).roundToInt()
        // 不是刚好被200毫秒整除的情况, 说明当前指示器没有指在刻度上, 则向前或向后调整到刻度上
        val remainTime = currentTime % 200
        Log.e(tag, "remainTime:$remainTime, currentTime:$currentTime, timePosition:$timePosition")
        if (remainTime > 0) {
            // 超过100ms, 则移向前面的刻度, 否则移向后面的刻度
            val indexOffset = if (remainTime > 100) 1 else 0
            timePosition =
                (((currentTime / 200) + indexOffset) * 200 / timeRuler!!.getTimeOfPerPixel())
            currentTime = (timePosition * timeRuler!!.getTimeOfPerPixel()).roundToInt()
        }
        realPosition =
            currentTime / timeRuler!!.getTimeOfPerPixel() + timeRuler!!.getSegmentBeginPosition()
        time = currentTime.toLong()
        currentPosition = realPosition - timeRuler!!.currentLeft
        Log.e(
            tag,
            "calc remainTime:$remainTime, currentTime:$currentTime, timePosition:$timePosition"
        )
        // refreshRect(realPosition - timeRuler!!.currentLeft)
        // 时间变动后回调
        if (oldMillisecond != currentTime.toLong()) {
            oldMillisecond = currentTime.toLong()
            timeListener?.onPositionChanged(realPosition)
        }
        Log.e(
            tag,
            "doIndicatorAnimate, latestIndicatorPos:${latestIndicatorPos + offsetX}, realPosition:${realPosition + offsetX}"
        )

        // 非完整模式下使用动画移动指示器
        if (!timeRuler!!.isFullMode()) {
            Log.e(
                tag,
                "moveTo, last:${latestIndicatorPos + offsetX}, end:${realPosition - timeRuler!!.currentLeft + offsetX}"
            )
            // 进行位置偏差移动
            doIndicatorAnimate(
                latestIndicatorPos + offsetX,
                realPosition - timeRuler!!.currentLeft + offsetX // 移动到目标位置(相对当前屏幕)
            )
        } else {
            timeRuler!!.invalidate()
        }
    }

    /**
     * 检查和修正指示器位置
     * 计算当前坐标, 不能超过时间轴起始和结束坐标, 超过则强制纠正
     */
    private fun checkAndFixToRealPosition(position: Float): Float {
        // 先换算成屏幕实际位置
        var realPosition = timeRuler!!.currentLeft + position
        realPosition = when {
            // 坐标不能小于刻度起始位置
            realPosition < timeRuler!!.getSegmentBeginPosition() -> {
                timeRuler!!.getSegmentBeginPosition()
            }
            // 坐标不能大于刻度结束位置
            realPosition > timeRuler!!.getViewWidth() - timeRuler!!.getSegmentEndOffset() -> {
                timeRuler!!.getViewWidth() - timeRuler!!.getSegmentEndOffset()
            }
            else -> {
                realPosition
            }
        }
        return realPosition
    }

    /**
     *指示器移动到指定位置
     * @param position 屏幕坐标位置x
     */
    fun moveTo(position: Float) {
        val fixPosition = checkAndFixToRealPosition(position)
        Log.e(tag, "Move position: moveTo fixPosition:$fixPosition")
        if (timeRuler!!.isFullMode()) {
            // 计算当前坐标, 不能超过时间轴起始和结束坐标, 超过则强制纠正
            realPosition = fixPosition
            // 计算时间(毫秒, 按200ms整除)
            val millisecond = (((position * timeRuler!!.getTimeOfPerPixel()).toInt()
                    / 200) * 200).toLong()
            // 时间变动时回调变动函数
            if (oldMillisecond != millisecond) {
                oldMillisecond = millisecond
                timeListener?.onPositionChanged(realPosition)
            }
            time = millisecond
        }
        currentPosition = fixPosition - timeRuler!!.currentLeft
        refreshRect(currentPosition)
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

    fun reset() {
        updatePosition(0f)
        realPosition = timeRuler!!.getSegmentBeginPosition()
        time = 0
    }

    /**
     * 获取当前时间(毫秒)
     */
    fun getTime(): Long {
        return getTime(true)
    }

    /**
     * 获取当前时间, 是否需要四舍五入
     */
    fun getTime(isRound: Boolean): Long {
        return if (isRound) ((((realPosition - timeRuler!!.getSegmentBeginPosition()) * timeRuler!!.getTimeOfPerPixel()).roundToInt()
                / 200) * 200).toLong()
        else ((((realPosition - timeRuler!!.getSegmentBeginPosition()) * timeRuler!!.getTimeOfPerPixel()).toInt()
                / 200) * 200).toLong()
    }


    fun refreshPosition() {
        realPosition =
            time / timeRuler!!.getTimeOfPerPixel() + timeRuler!!.getSegmentBeginPosition()
    }

    fun getRealPosition(): Float {
        return realPosition
    }

    fun setRealPosition(realPosition: Float) {
        this.realPosition = realPosition
        time = getTime()
        //Log.e("tag", "refresh time, pos:$realPosition, time:$time")
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