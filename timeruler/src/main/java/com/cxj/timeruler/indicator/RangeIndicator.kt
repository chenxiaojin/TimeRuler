package com.cxj.timeruler.indicator

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.core.content.ContextCompat
import com.cxj.timeruler.R
import com.cxj.timeruler.TimeRuler
import com.cxj.timeruler.util.DisplayUtil
import com.cxj.timeruler.util.Utils

/**
 *  author : chenxiaojin
 *  date : 2021/5/12 下午 06:20
 *  description : A-B段指示器
 */
class RangeIndicator {
    private var tag = "RangeIndicator"
    private var segmentColor = 0

    // 画笔
    private lateinit var paint: Paint
    private var timeRuler: TimeRuler

    // A、B指示器
    private lateinit var timeIndicatorA: TimeIndicator
    private lateinit var timeIndicatorB: TimeIndicator

    // 指示器宽高
    private var indicatorWidth = 0
    private var indicatorHeight = 0

    // 当前是否被触碰到
    private var isTouchedA = false
    private var isTouchedB = false

    // 是否正在拖拽
    private var isDragging = false

    // A、B指示器是否可见
    private var isIndicatorAVisible = false
    private var isIndicatorBVisible = false

    // 时间段变更
    var timeListener: TimeListener? = null

    constructor(timeRuler: TimeRuler) {
        this.timeRuler = timeRuler
        segmentColor = ContextCompat.getColor(timeRuler.context, R.color.choose_segment_color)
        initPaint()
        initIndicator()
    }

    private fun initIndicator() {
        indicatorWidth = DisplayUtil.dip2px(timeRuler.context, 18f)
        indicatorHeight = DisplayUtil.dip2px(timeRuler.context, 25f)
        timeIndicatorA = TimeIndicator(timeRuler)
        timeIndicatorB = TimeIndicator(timeRuler)

        timeIndicatorA.backgroundBitmap = Utils.drawableToBitmap(
            timeRuler.context,
            indicatorWidth,
            indicatorHeight,
            R.mipmap.dgsb_zt_bga
        )
        timeIndicatorB.backgroundBitmap = Utils.drawableToBitmap(
            timeRuler.context,
            indicatorWidth,
            indicatorHeight,
            R.mipmap.dgsb_zt_bgb
        )

        timeIndicatorA.width = indicatorWidth
        timeIndicatorA.height = indicatorHeight
        timeIndicatorA.offsetX = -DisplayUtil.dip2px(timeRuler.context, 4f)
        timeIndicatorA.offsetY = -DisplayUtil.dip2px(timeRuler.context, 3f)

        timeIndicatorB.width = indicatorWidth
        timeIndicatorB.height = indicatorHeight
        timeIndicatorB.offsetX = DisplayUtil.dip2px(timeRuler.context, 4f)
        timeIndicatorB.offsetY = -DisplayUtil.dip2px(timeRuler.context, 3f)

        var touchWidth = DisplayUtil.dip2px(timeRuler.context, 16f)
        val touchHeight = DisplayUtil.dip2px(timeRuler.context, 16f)

        // 自定义触摸区域
        timeIndicatorA.setTouchArea(touchWidth, touchHeight)
        touchWidth = DisplayUtil.dip2px(timeRuler.context, 18f)
        timeIndicatorB.setTouchArea(touchWidth, touchHeight)
        initIndicatorListener()
    }


    private fun initIndicatorListener() {
        timeIndicatorA.timeListener = object : TimeIndicator.TimeListener {
            override fun onPositionChanged(timePosition: Float) {
                timeListener?.onPositionChanged(
                    timeIndicatorA.getRealPosition(),
                    timeIndicatorB.getRealPosition()
                )
            }
        }
        timeIndicatorB.timeListener = object : TimeIndicator.TimeListener {
            override fun onPositionChanged(timePosition: Float) {
                timeListener?.onPositionChanged(
                    timeIndicatorA.getRealPosition(),
                    timeIndicatorB.getRealPosition()
                )
            }
        }
    }

    private fun initPaint() {
        paint = Paint()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.isAntiAlias = true
        paint.strokeWidth = DisplayUtil.dip2px(timeRuler.context, 1f).toFloat()
        paint.color = segmentColor
    }

    /**
     * 普通模式下的绘制
     */
    fun onDraw(canvas: Canvas) {
        if (isIndicatorAVisible) {
            timeIndicatorA.onDraw(paint, canvas)
        }
        if (isIndicatorBVisible) {
            timeIndicatorB.onDraw(paint, canvas)
        }

        // 如果A和B不是全部可见, 则代表还未选中时间段, 不进行时间段绘制
        if (!(isIndicatorAVisible && isIndicatorBVisible)) {
            return
        }

        // 如果是完整模式, 不需要绘制AB段连接的刻度
        if (timeRuler.isFullMode()) {
            return
        }
        paint.color = segmentColor

        // 测试代码, 放开后可查看触控区域
//        val testRectF = RectF()
//        paint.color = ContextCompat.getColor(timeRuler.context, R.color.purple_200)
//        testRectF.top = timeIndicatorB.displayRectF.top - timeIndicatorB.offsetY
//        testRectF.left = timeIndicatorB.displayRectF.left - timeIndicatorB.offsetX
//        testRectF.right = testRectF.left + timeIndicatorB.customTouchWidth
//        testRectF.bottom = testRectF.top - timeIndicatorB.offsetY + timeIndicatorB.customTouchHeight
//        canvas.drawRect(testRectF, paint)
        val realLeftPosition = timeRuler.currentLeft - timeRuler.getSegmentBeginPosition()
        // 计算当前屏幕从左到右的时间索引
        val currentLeftIndex = timeRuler.currentLeftIndex

        // +1是为了B指示器超出屏幕时, 可以正常绘制
        val currentRightIndex =
            timeRuler.calcTimeIndex((timeRuler.currentLeft + timeRuler.width)) + 1
        Log.e(
            tag,
            "currentLeftIndex:$currentLeftIndex, realLeftPosition:$realLeftPosition, rightIndex:$currentRightIndex"
        )
        // 计算选中时间段的索引区间
        val indicatorATimeIndex =
            ((timeIndicatorA.getRealPosition() - timeRuler.getSegmentBeginPosition()) / timeRuler.segmentWidth).toInt()
        val indicatorBTimeIndex =
            ((timeIndicatorB.getRealPosition() - timeRuler.getSegmentBeginPosition()) / timeRuler.segmentWidth).toInt()
        // 计算开始绘制刻度的坐标(相对屏幕坐标)
        val segmentStartPos = if (timeRuler.currentLeft < timeRuler.getSegmentBeginPosition()) {
            timeRuler.getSegmentBeginPosition() - timeRuler.currentLeft
        } else {
            // 取余可以算超出当前刻度多少位置
            -(timeRuler.currentLeft - timeRuler.getSegmentBeginPosition()) % timeRuler.segmentWidth
        }
        // 循环屏幕的时间索引，判断索引是否在A-B之中， 是的话画刻度，否则不处理
        for (index in currentLeftIndex until currentRightIndex) {
            val offset =
                segmentStartPos + (index - currentLeftIndex) * timeRuler.segmentWidth.toFloat()
            // 判断索引是否在A-B之中, 在A-B之中进行刻度绘制和横线绘制
            if (index in (indicatorATimeIndex until indicatorBTimeIndex + 1)) {
                // 画秒刻度
                if (index % timeRuler.getSegmentCountOfSecond() == 0) {
                    if (index == 0 || index == timeRuler.getTotalSegmentCount()) {
                        canvas.drawLine(
                            offset,
                            timeRuler.secondStartTop,
                            offset,
                            timeRuler.rulerStartTop,
                            paint
                        )
                    } else {
                        canvas.drawCircle(
                            offset,
                            timeRuler.rulerStartTop,
                            timeRuler.getSecondSegmentRadius(),
                            paint
                        )
                    }
                } else {
                    // 画毫秒刻度
                    canvas.drawLine(
                        offset,
                        timeRuler.getMillisecondStartTop(),
                        offset,
                        timeRuler.rulerStartTop,
                        paint
                    )
                }
                // 如果下个点也在索引范围内, 则画当前点到下一个点之间的横线
                if ((index + 1) in (indicatorATimeIndex until indicatorBTimeIndex + 1)) {
                    // 画刻度底部完整长线
                    canvas.drawLine(
                        offset - paint.strokeWidth / 2,
                        timeRuler.rulerStartTop,
                        offset + timeRuler.segmentWidth + paint.strokeWidth / 2,
                        timeRuler.rulerStartTop,
                        paint
                    )
                }
            }
        }
    }

    /**
     * 是否触摸了A或B的指示器
     */
    fun isTouched(x: Int, y: Int): Boolean {
        // 只有A时, 只判断是否触摸到A
        if (isIndicatorAVisible && !isIndicatorBVisible) {
            return isTouchedA(x, y)
        }
        // 两个指示器在同一个位置时, 判断优先检测那个被按下
        if (isIndicatorBVisible && timeIndicatorA.getRealPosition() == timeIndicatorB.getRealPosition()) {
            // 如果不是在时间项的最后, 则优先检查B是否被按下， 提升用户体验
            if (timeRuler.calcTimeIndex(timeIndicatorA.getRealPosition())
                != timeRuler.getTimeItemList().size - 1
            ) {
                return if (isTouchedB(x, y)) true else isTouchedA(x, y)
            }
            // 其他情况都先判断A是否按下
            return if (isTouchedA(x, y)) true else isTouchedB(x, y)
        }
        return if (isTouchedA(x, y)) true else isTouchedB(x, y)
    }

    /**
     * A指示器是否被按下
     */
    private fun isTouchedA(x: Int, y: Int): Boolean {
        if (timeIndicatorA.isTouched(x, y)) {
            isTouchedA = true
            return true
        }
        return false
    }

    /**
     * B指示器是否被按下
     */
    private fun isTouchedB(x: Int, y: Int): Boolean {
        if (timeIndicatorB.isTouched(x, y)) {
            isTouchedB = true
            return true
        }
        return false
    }

    /**
     * 获取时间项最大的真实坐标
     */
    private fun getRulerTimeItemsMaxPosition(): Float {
        // 计算时间项的最大位置(相对屏幕的位置)
        val timeItemList = timeRuler.getTimeItemList()
        return timeItemList[timeItemList.size - 1].time /
                timeRuler.getTimeOfPerPixel() + timeRuler.getSegmentBeginPosition() - timeRuler.currentLeft
    }

    /**
     * 计算最终的索引
     */
    private fun calcPosition(position: Float, maxPosition: Float): Float {
        if (position < timeRuler.getSegmentBeginPosition()) {
            return timeRuler.getSegmentBeginPosition()
        }
        return if (maxPosition > position) position else maxPosition
    }

    /**
     * 更新指示器位置, 会自动计算合适位置
     * @param position 屏幕坐标x
     */
    fun updatePosition(position: Float) {
        if (isTouchedA) {
            updateIndicatorAPosition(position)
        } else if (isTouchedB) {
            updateIndicatorBPosition(position)
        }
        isTouchedA = false
        isTouchedB = false
    }

    /**
     * 更新A指示器位置
     * @param position 屏幕坐标x
     */
    fun updateIndicatorAPosition(position: Float) {
        if (isIndicatorBVisible) {
            val indicatorBPosition = getIndicatorBPosition()
            timeIndicatorA.updatePosition(calcPosition(position, indicatorBPosition))
        } else {
            val maxPosition = getRulerTimeItemsMaxPosition()
            if (maxPosition > position) {
                timeIndicatorA.updatePosition(position)
            } else {
                timeIndicatorA.updatePosition(maxPosition)
            }
        }
    }

    /**
     * 更新B段索引
     * @param position 屏幕坐标x
     */
    fun updateIndicatorBPosition(position: Float) {
        // 时间不能小于A指示器
        var indicatorAPosition = getIndicatorAPosition()
        if (position > indicatorAPosition) {
            indicatorAPosition = getRulerTimeItemsMaxPosition()
            if (indicatorAPosition > position) {
                timeIndicatorB.updatePosition(position)
            } else {
                timeIndicatorB.updatePosition(indicatorAPosition)
            }
        } else {
            timeIndicatorB.updatePosition(indicatorAPosition)
        }
    }

    /**
     * 获取A指示器在当前屏幕的位置
     */
    fun getIndicatorAPosition(): Float {
        return this.timeIndicatorA.getRealPosition() - timeRuler.currentLeft
    }

    /**
     * 获取B指示器在当前屏幕的位置
     */
    fun getIndicatorBPosition(): Float {
        return this.timeIndicatorB.getRealPosition() - timeRuler.currentLeft
    }

    /**
     * 移动指示器
     * @param position 屏幕坐标x
     */
    fun moveTo(position: Float) {
        if (isTouchedA) {
            if (isIndicatorBVisible) {
                // 移动不能超过B指示器
                val indicatorBPosition = getIndicatorBPosition()
                timeIndicatorA.moveTo(calcPosition(position, indicatorBPosition))
            } else {
                // 判断是否超过时间项, 只能移动到最后一个时间项
                val maxPosition = getRulerTimeItemsMaxPosition()
                timeIndicatorA.moveTo(calcPosition(position, maxPosition))
            }
        } else if (isTouchedB) {
            // 移动不能小于A指示器
            var indicatorAPosition = getIndicatorAPosition()
            if (position > indicatorAPosition) {
                // 判断是否超过时间项, 只能移动到最后一个时间项
                indicatorAPosition = getRulerTimeItemsMaxPosition()
                Log.e(
                    tag,
                    "Max time position:$indicatorAPosition, curpos:$position, currleft:${timeRuler.currentLeft}"
                )
                if (indicatorAPosition > position) {
                    timeIndicatorB.moveTo(position)
                } else {
                    timeIndicatorB.moveTo(indicatorAPosition)
                }
            } else {
                timeIndicatorB.moveTo(indicatorAPosition)
            }
        }
    }

    /**
     * 设置AB指示器拖动状态
     * @param isDragging 是否正在拖动
     */
    fun setDragging(isDragging: Boolean) {
        this.isDragging = isDragging
        if (!isDragging) {
            timeIndicatorA.isDragging = false
            timeIndicatorB.isDragging = false
        } else if (isTouchedA) {
            timeIndicatorA.isDragging = true
        } else if (isTouchedB) {
            timeIndicatorB.isDragging = true
        }
    }

    fun isDragging(): Boolean {
        return isDragging
    }

    /**
     * 设置A指示器时间
     * @param timePosition 时间转换后对应的坐标位置
     */
    fun setATime(timePosition: Float) {
        isIndicatorAVisible = true
        timeIndicatorA.setRealPosition(timePosition)
        timeRuler.invalidate()
    }

    /**
     * 设置B指示器时间
     * @param timePosition 时间转换后对应的坐标位置
     */
    fun setBTime(timePosition: Float) {
        // 必须设置了A才能设置B
        if (!isIndicatorAVisible) {
            return
        }
        isIndicatorBVisible = true
        timeIndicatorB.setRealPosition(timePosition)
        timeRuler.invalidate()
    }

    fun getATime(): Long {
        return timeIndicatorA.getTime()
    }

    fun getBTime(): Long {
        return timeIndicatorB.getTime()
    }

    /**
     * 重置AB指示器
     */
    fun reset() {
        timeIndicatorA.setRealPosition(timeRuler.getSegmentBeginPosition())
        timeIndicatorB.setRealPosition(timeRuler.getSegmentBeginPosition())
        isIndicatorAVisible = false
        isIndicatorBVisible = false
        timeRuler.invalidate()
    }

    fun refreshPosition() {
        timeIndicatorA.refreshPosition()
        timeIndicatorB.refreshPosition()
    }

    /**
     * A 指示器是否可见, 相当于是否已经设置的意思
     */
    fun isIndicatorAVisible(): Boolean {
        return isIndicatorAVisible
    }

    /**
     * B 指示器是否可见, 相当于是否已经设置的意思
     */
    fun isIndicatorBVisible(): Boolean {
        return isIndicatorBVisible
    }

    /**
     * 普通模式下获取A指示器当前时间位置
     */
    fun getIndicatorATimePosition(): Float {
        return timeIndicatorA.getRealPosition()
    }

    /**
     * 普通模式下获取B指示器当前时间位置
     */
    fun getIndicatorBTimePosition(): Float {
        return timeIndicatorB.getRealPosition()
    }


    /**
     * 时间位置变更监听
     */
    interface TimeListener {
        fun onPositionChanged(aTimePosition: Float, bTimePosition: Float)
    }
}