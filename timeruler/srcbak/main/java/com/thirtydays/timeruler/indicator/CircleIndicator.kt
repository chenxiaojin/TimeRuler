package com.cxj.timeruler.indicator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import com.cxj.timeruler.TimeRuler
import com.cxj.timeruler.bean.TimeItem
import com.cxj.timeruler.util.DisplayUtil

/**
 *  author : chenxiaojin
 *  date : 2021/5/6 下午 12:05
 *  description : 圆形时间指示器
 *  时间刻度上的圆形指示器
 *  包括选中样式、背景颜色、索引文字显示
 */
class CircleIndicator {
    private var tag = "CircleIndicator"

    // 指示器圆形与圆形边框的间隔
    private var borderPadding = 0

    // 指示器位置
    private var rect: Rect = Rect()

    // 时间轴
    private var timeRuler: TimeRuler? = null

    // 指示器浮现动画
    private var showAnimator = ValueAnimator()

    // 指示器隐藏动画
    private var hideAnimator = ValueAnimator()

    // 指示器移动动画
    private var moveAnimator = ValueAnimator()

    // 文字显示相关参数
    private var textRect = Rect()
    private var textColor: Int = 0
    private var textSize: Int = 0
    private var strokeWidth = 0f

    // 删除和显示回调, 动画执行完后回调
    private var deleteCallback: OnDeleteCallback? = null
    private var showCallback: OnShowCallback? = null

    // 画笔
    private lateinit var paint: Paint

    // 做动画显示效果判断, 动画在结束后, 动画的isRunning还是true , 下次才是false, 会导致无法正确显示
    // 所以用额外的变量来判断
    private var isDeleting = false
    private var isShowing = false
    private var isMoving = false

    // 背景颜色
    var backgroundColor: Int = 0

    // 文本
    var value: Int = 0

    var timeItem: TimeItem

    // 边框颜色
    var borderColor: Int = 0

    // 指示器半径大小
    var radius: Int = 0

    // 指示器当前指向的时间刻度
    var timePosition = 0

    // 指示器在当前屏幕的坐标X
    var currentPosition = 0

    // 指示器起始高度
    var top: Float = 0f

    // 当前是否选中
    var isSelected = true

    /**
     * timeItem的data为null时, 表示是空白时间项
     */
    constructor(timeRuler: TimeRuler, timeItem: TimeItem, timePosition: Int) {
        this.timeRuler = timeRuler
        this.value = timeItem.flag
        this.timeItem = timeItem
        this.backgroundColor = timeItem.backgroundColor
        // 默认背景颜色为白色
        if (backgroundColor == 0) {
            backgroundColor = ContextCompat.getColor(timeRuler.context, android.R.color.white)
        }
        this.timePosition = timePosition
        isSelected = timePosition == timeRuler.getTimePosition()
        init(timeRuler.context)
    }


    private fun init(context: Context) {
        radius = DisplayUtil.dip2px(context, 9.5f)
        top = timeRuler?.secondStartTop!! - timeRuler!!.getCircleIndicatorGap() - radius * 2
        borderPadding = DisplayUtil.dip2px(context, 1.5f)

        rect.left = currentPosition
        rect.top = top.toInt()
        rect.right = rect.left + radius * 2
        rect.bottom = rect.top + radius * 2
        textColor = ContextCompat.getColor(context, android.R.color.black)
        // 当前字体大小需要和圆圈大小匹配单复数, 否则可能出现字体对不齐问题
        textSize = DisplayUtil.dip2px(context, 8f)
        strokeWidth = DisplayUtil.dip2px(context, 0.5f).toFloat()

        initPaint()
        initAnimator()
    }

    private fun initPaint() {
        paint = Paint()
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.isAntiAlias = true
        paint.color = backgroundColor
        paint.textSize = textSize.toFloat()
    }

    private fun initAnimator() {
        // 动画时间设置一样，同时执行显示和移动动画时才能保证执行时间一样，状态才能同步
        hideAnimator.duration = 300
        moveAnimator.duration = 300
        showAnimator.duration = 300
        showAnimator.setFloatValues(
            0f,
            255f
        )
        hideAnimator.setFloatValues(
            255f,
            0f
        )
    }

    /**
     * 时间项绘制
     * @param canvas 画布
     */
    fun onDraw(canvas: Canvas) {
        isSelected = timePosition == timeRuler!!.getTimePosition()
        // 滚动时刷新指示器在当前屏幕中的相对坐标

        // 动画显示操作, 用额外的变量来
        when {
            isShowing -> {// 渐变显示
                doShowIndicator(canvas)
            }
            isMoving -> { // 平移
                doMoveIndicator(canvas)
            }
            isDeleting -> {// 渐变隐藏
                doDeleteIndicator(canvas)
            }
            timePosition - timeRuler?.currentLeft!! < timeRuler?.width!! + radius * 2 -> {
                // 在屏幕显示范围内才进行绘制
                refreshPosAndDraw(canvas, timePosition)
            }
        }
    }

    /**
     * 更新坐标后进行绘制
     * @param canvas 画布
     * @param timePosition 时间转换后的位置
     */
    private fun refreshPosAndDraw(canvas: Canvas, timePosition: Int) {
        // 滚动时刷新指示器在当前屏幕中的相对坐标
        currentPosition =
            (timePosition - timeRuler?.currentLeft!!).toInt()
        // 当前时间位置和时间轴时间指示器位置相同时为选中状态
        isSelected = timePosition == timeRuler!!.getTimePosition()
        refreshRect()
        doOnDraw(canvas)
    }

    /**
     * 动画显示时间项
     * @param canvas 画布
     */
    private fun doShowIndicator(canvas: Canvas) {
        currentPosition =
            (timePosition - timeRuler?.currentLeft!!).toInt()
        refreshRect()
        isSelected = timePosition == timeRuler!!.getTimePosition()
        if (showAnimator.isRunning) {
            doShowAnimation(canvas)
        } else {
            isShowing = false
            doOnDraw(canvas)
            timeRuler!!.post {
                showCallback?.onFinishShow()
            }
        }
    }

    /**
     * 动画删除时间项
     * @param canvas 画布
     */
    private fun doDeleteIndicator(canvas: Canvas) {
        if (hideAnimator.isRunning) {
            doDeleteAnimation(canvas)
        } else {
            isDeleting = false
            timeRuler!!.post {
                deleteCallback?.onDeleted()
            }
        }
    }

    /**
     * 动画移动时间项
     * @param canvas 画布
     */
    private fun doMoveIndicator(canvas: Canvas) {
        val position: Int
        if (moveAnimator.isRunning) {
            position = moveAnimator.animatedValue as Int
        } else {
            isMoving = false
            position = timePosition
        }
        refreshPosAndDraw(canvas, position)
    }

    /**
     * 新增时间项后进行动画显示
     * @param showCallback 显示后回调
     */
    fun show(showCallback: OnShowCallback) {
        isShowing = true
        this.showCallback = showCallback
        // 滚动时刷新指示器在当前屏幕中的相对坐标
        currentPosition = (timePosition - timeRuler?.currentLeft!!).toInt()
        refreshRect()

        showAnimator.start()
        timeRuler?.invalidate()
    }

    /**
     * 删除时间项
     * @param deleteCallback 删除回调
     */
    fun delete(deleteCallback: OnDeleteCallback) {
        isDeleting = true
        this.deleteCallback = deleteCallback

        hideAnimator.start()
        timeRuler?.invalidate()
    }

    fun isTouched(x: Int, y: Int): Boolean {
        return ((x >= rect.left && x <= rect.right) && (y > rect.top && y <= rect.bottom))
    }

    private fun doShowAnimation(canvas: Canvas) {
        paint.color = Color.argb(
            (showAnimator.animatedValue as Float).toInt(),
            Color.red(backgroundColor),
            Color.green(backgroundColor),
            Color.blue(backgroundColor)
        )

        drawCircleStroke(canvas)
        drawCircle(canvas)
        if (!timeItem.isEmpty()) {
            drawText(canvas, Color.argb((showAnimator.animatedValue as Float).toInt(), 0, 0, 0))
        }
        timeRuler!!.invalidate()
    }

    private fun doDeleteAnimation(canvas: Canvas) {
        // 渐变透明度
        paint.color = Color.argb(
            (hideAnimator.animatedValue as Float).toInt(), Color.red(backgroundColor),
            Color.green(backgroundColor),
            Color.blue(backgroundColor)
        )
        drawCircleStroke(canvas)
        drawCircle(canvas)
        if (!timeItem.isEmpty()) {
            drawText(canvas, Color.argb((hideAnimator.animatedValue as Float).toInt(), 0, 0, 0))
        }
        timeRuler!!.invalidate()
    }


    private fun doOnDraw(canvas: Canvas) {
        paint.color = backgroundColor
        drawCircleStroke(canvas)
        drawCircle(canvas)
        if (!timeItem.isEmpty()) {
            drawText(canvas, textColor)
        }
    }

    /**
     * 画选中圆圈
     * @param canvas 画布
     */
    private fun drawCircleStroke(canvas: Canvas) {
        if (isSelected && !timeRuler!!.isPlaying()) {
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(
                rect.left.toFloat() + radius,
                rect.top.toFloat() + radius, radius.toFloat(), paint
            )
        }
    }

    /**
     * 画圆圈
     * @param canvas 画布
     */
    private fun drawCircle(canvas: Canvas) {
        paint.style = if (timeItem.isEmpty()) Paint.Style.STROKE else Paint.Style.FILL_AND_STROKE
        canvas.drawCircle(
            rect.left.toFloat() + radius,
            rect.top.toFloat() + radius,
            radius.toFloat() - borderPadding,
            paint
        )
    }

    /**
     * 画文字显示
     * @param canvas 画布
     */
    private fun drawText(canvas: Canvas, textColor: Int) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.color = textColor
        paint.textSize = textSize.toFloat()
        // 设置居中显示
        paint.textAlign = Paint.Align.CENTER

//        val timeTextHalfWidth = calcTextWidth(paint, "$value") / 2
//        val circleWidth = (radius.toFloat() - borderPadding) * 2
//        val x =
//            currentPosition.toFloat() - (circleWidth / 2) + (circleWidth / 2 - timeTextHalfWidth)
//        Log.e(
//            tag,
//            "x:${x}, timeTextHalfWidth:$timeTextHalfWidth , rect.width():${
//                rect.width()
//            }, currentPosition:${currentPosition}, segmentOffsetLeft:${
//                timeRuler!!.getSegmentStartOffset()
//            }, circleWidth:${circleWidth}, circleLeft:${rect.left.toFloat() + radius}, rect.left.toFloat() + radius "
//        )
//        val y =
//            timeRuler?.secondStartTop!! - timeRuler!!.getCircleIndicatorGap() - radius + textSize / 2 - borderPadding
//        val x =
//            currentPosition.toFloat() - (circleWidth / 2) + (circleWidth / 2 - timeTextHalfWidth)
        val fontMetrics = paint.fontMetrics
        val top = fontMetrics.top;//为基线到字体上边框的距离,即上图中的top
        val bottom = fontMetrics.bottom;//为基线到字体下边框的距离,即上图中的bottom
        val y = (rect.centerY() - top / 2 - bottom / 2)
        canvas.drawText(
            "$value", currentPosition.toFloat(), y, paint
        )
    }

    /**
     * 刷新指示器位置
     */
    private fun refreshRect() {
        rect.left = currentPosition - radius
        rect.top = top.toInt()
        rect.right = rect.left + radius * 2
        rect.bottom = rect.top + radius * 2
    }

    /**
     * 移动下一个位置
     * @param isAnimator 是否动画执行
     */
    fun moveToNext(isAnimator: Boolean) {
        moveTo(timePosition + timeRuler!!.segmentWidth, isAnimator)
        isSelected = false
    }

    /**
     * 移动到上一个位置
     * @param isAnimator 是否动画执行
     */
    fun moveToPrevious(isAnimator: Boolean) {
        moveTo(timePosition - timeRuler!!.segmentWidth, isAnimator)
    }

    /**
     * 移动指示器位置
     * @param position 目标位置
     * @param isAnimator 是否动画执行
     */
    private fun moveTo(position: Int, isAnimator: Boolean) {
        isMoving = true
        // 执行移动
        if (isAnimator) {
            moveAnimator.setIntValues(
                timePosition,
                position
            )
            moveAnimator.start()
        }
        // 更新实际时间
        timePosition = position
    }

    /**
     * 删除指示器回调
     */
    interface OnDeleteCallback {
        /**
         * 删除后回调
         */
        fun onDeleted()
    }

    /**
     * 显示指示器回调
     */
    interface OnShowCallback {
        /**
         * 显示结束后回调
         */
        fun onFinishShow()
    }
}