package com.cxj.timeruler

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import com.cxj.timeruler.bean.TimeItem
import com.cxj.timeruler.indicator.CircleIndicator
import com.cxj.timeruler.indicator.RangeIndicator
import com.cxj.timeruler.indicator.TimeIndicator
import com.cxj.timeruler.util.AnimatorUtil
import com.cxj.timeruler.util.DisplayUtil
import com.cxj.timeruler.util.Utils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 *  author : chenxiaojin
 *  date : 2021/4/28 下午 07:13
 *  description : 时间刻度尺
 *  包含功能：
 *  1、增加、删除时间项
 *  2、时间指示器
 *  3、A-B段指示器
 *  4、播放指定时间段的时间项
 *  5、切换完整模式和普通模式
 */
class TimeRuler : View {
    private var tag = "TimeRuler"

    private var minHeight = 0

    // 白色
    private var colorWhite = 0

    // 时间轴最长时间(秒)
    private var maxTime: Int = 600

    // 每个分段的长度
    var segmentWidth = 100

    // 每个分段的时间间隔(秒)
    private var segmentTimeInterval = 0.2f

    // 刻度画笔
    private lateinit var segmentPaint: Paint

    // 秒刻度高度
    private var secondSegmentHeight = 0

    // 圆形秒刻度半径(除第一和最后一个刻度外，其他秒刻度是原型刻度)
    private var secondSegmentRadius = 0f

    // 圆形时间项与尺子的间距
    private var circleIndicatorGap = 0

    // 毫秒刻度高度
    private var millisecondSegmentHeight = secondSegmentHeight

    // 刻度尺起始高度 = 圆形时间项高度+原型时间项和尺子的间距+秒刻度的高度
    var rulerStartTop = 0f

    // 秒刻度开始高度 = 刻度尺起始高度 - 秒刻度高度
    var secondStartTop = rulerStartTop - secondSegmentHeight

    // 毫秒刻度开始高度 = 刻度尺起始高度 - 毫秒刻度高度
    private var millisecondStartTop = rulerStartTop - millisecondSegmentHeight

    // 普通模式:1秒有多少个刻度
    private var segmentCountOfSecond = 0

    // 普通模式:总共有多少个刻度
    private var totalSegmentCount = 0

    // 刻度文字大小
    private var segmentTextRect = Rect()

    // 刻度起始偏移量
    private var segmentStartOffset = 0f

    // 刻度结束偏移量
    private var segmentEndOffset = 0f

    // 刻度storeWidth
    private var segmentStrokeWidth = 0f

    // 滚动、触摸事件相关参数
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var isMoving = false
    private var scrollSlop = 0
    private var minVelocity = 0
    private var maxVelocity = 0

    /**
     * 刻度数值颜色、大小、与时刻度的距离和每个多少个间隔显示文本
     */
    private var segmentTextColor = 0
    private var segmentTextSize = 0
    private var segmentTextGap = 0

    // 普通模式:每隔多少个刻度显示文本
    private var segmentTextPerCount = 0
    private var segmentColor = 0

    // 当前的最左侧起始坐标
    var currentLeft = 0f
    private var currentLeftIndex = 0
    private var screenWidth = 0

    // 刻度总长度 = 刻度数 * 单个刻度宽度
    private var segmentMaxWidth = 0

    // 惯性滑动计算
    private var velocityTracker: VelocityTracker? = null

    // 滑动器
    private lateinit var scroller: Scroller
    private lateinit var timeIndicator: TimeIndicator
    private lateinit var rangeIndicator: RangeIndicator
    private lateinit var playIndicator: TimeIndicator

    // 最大滚动距离
    private var maxScrollDistance = 0

    // 时间项列表
    private var timeItemList = arrayListOf<CircleIndicator>()

    // 圆形刻度半径
    private var timeCircleIndicatorRadius = 0

    // 是否正在滚动
    private var isFling = false

    // 是否正在操作，如新增、删除
    private var isOperating = false

    // 普通模式下时间轴播放相关参数
    private lateinit var playAnimator: ValueAnimator
    private var startPlayRealPosition = 0
    private var startPlayTimePosition = 0

    // 播放时, 实时计算出当前的时间索引
    private var currentPlayTimeIndex = 0
    private var screenCenterPosition = 0

    // 开始播放时的索引
    private var startPlayTimeIndex = 0

    // 结束播放时的索引
    private var stopPlayTimeIndex = 0

    // 完整模式下时间轴播放相关参数
    private var startPlayTimePositionInFullMode = 0f
    private var startPlayTimePositionInFullModeIndex = 0
    private var endPlayTimePositionInFullMode = 0f
    private var endPlayTimePositionInFullModeIndex = 0

    // 是否正在播放
    private var isPlaying = false

    // 是否重复播放
    private var isRepeatPlay = false

    // 是否停止
    private var isCancel = false

    // 是否需要重置播放指示器, 在取消播放需要使用到
    private var isNeedResetPlayIndicator = false

    // 是否需要播放
    private var isNeedToPlay = false

    // 开始播放时间轴前, 将播放点移动到屏幕中间的动画
    private var movePlayIndicatorToCenterAnimator = ValueAnimator()

    // 当前是否是完整模式
    private var isFullMode = false

    // 完整模式下有多少个刻度
    private var totalSegmentCountInFullMode = 10

    // 完整模式下每个刻度的宽度
    private var segmentWidthInFullMode = 0f

    // 完整模式下刻度的总宽度
    private var segmentMaxWidthInFullMode = 0

    // 完整模式下一个刻度对应的时间(单位秒)
    private var timeOfPerSegment = 60

    // 完整模式下一个像素多少毫秒
    private var timeOfPerPixel = 0f

    // 完整模式下显示时间项的范围
    private var rangeRectF = RectF()
    private var rangeSegmentColor = 0

    // 用于移动时间位置到指令位置后, 是否还需要继续播放
    private var isNeedToContinuePlay = false

    // 是否已经调用了开始回调, 避免重复播放时重复调用
    private var isCalledStart = false

    // 上次的时间索引, 用于比较时间索引变化, 以便实现时间项二次点击才算点击的功能
    private var latestTimeIndex = 0

    // 当前系统是否开启动画, 如果么有开启动画, 播放时则不使用动画
    private var isAnimatorEnable = true

    // 播放监听器
    var playListener: PlayListener? = null

    // 时间监听器
    var timeListener: TimeListener? = null

    // 时间项监听器
    var timeItemListener: TimeItemStateListener? = null

    // 时间项单击监听
    var timeItemClickListener: TimeItemClickListener? = null

    // 时间段监听器
    var rangeIndicatorListener: RangeIndicatorListener? = null

    /**
     * 关闭动画的场景下, 时间轴播放处理(步进方式)
     */
    private val playHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            startPlayTimeIndex += 1
            if (startPlayTimeIndex > stopPlayTimeIndex) {
                invalidate()
                if (!isRepeatPlay) {
                    playIndicator.isVisible = false
                    timeIndicator.isVisible = true
                    // 延迟200毫秒, 否则会直接调到起始位置, 看不到在结束位置停留
                    postDelayed({ resetPlayTime() }, 200)
                } else {
                    postDelayed({ resetPlayTime() }, 200)
                }
                return
            }
            // 更新时间
            playIndicator.timePosition = startPlayTimeIndex * segmentWidth
            timeIndicator.timePosition = playIndicator.timePosition
            // 当前的时间位置超过屏幕中心点时, 则移动刻度尺位置, 保持播放指示器一直在中间
            if (playIndicator.timePosition - currentLeft > screenCenterPosition) {
                currentLeft += (playIndicator.timePosition - currentLeft - screenCenterPosition)
            }
            // 回调时间变动
            playListener?.onTimeChanged(
                (startPlayTimeIndex.toFloat() * 1000
                        / segmentCountOfSecond).toLong(), timeItemList[startPlayTimeIndex].timeItem
            )
            // 如果播放到最后, 则根据是否重复播放来确定是否停止播放
            if (startPlayTimeIndex == stopPlayTimeIndex) {
                invalidate()
                if (!isRepeatPlay) {
                    playIndicator.isVisible = false
                    timeIndicator.isVisible = true
                    postDelayed({ resetPlayTime() }, 200)
                    isPlaying = false
                    playListener?.onStop()
                } else {
                    postDelayed({ resetPlayTime() }, 200)
                }
                return
            }
            // 如果还未播放到最后, 继续播放
            sendEmptyMessageDelayed(0, 200)
            // 计算并刷新视图
            computeTime()
        }
    }

    /**
     * 完整模式下播放处理Handler
     */
    private val playInFullModeHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            startPlayTimePositionInFullModeIndex += 1
            // 当开始时间索引大于结束时间索引, 说明是在最后一个时间进行播放, 不需要进行时间变动回调
            if (startPlayTimePositionInFullModeIndex > endPlayTimePositionInFullModeIndex) {
                if (!isRepeatPlay) {
                    playIndicator.isVisible = false
                    timeIndicator.isVisible = true
                    resetPlayTimeInFullMode()
                    isPlaying = false
                    playListener?.onStop()
                } else {
                    resetPlayTimeInFullMode()
                    playInFullMode()
                }
                return
            }
            // 更新播放时间位置和回调时间变动
            startPlayTimePositionInFullMode =
                convertToFullModeTimePosition(timeItemList[startPlayTimePositionInFullModeIndex].timePosition)
            playListener?.onTimeChanged(
                (startPlayTimePositionInFullModeIndex.toFloat() * 1000 / segmentCountOfSecond).toLong(),
                timeItemList[startPlayTimePositionInFullModeIndex].timeItem
            )
            playIndicator.timePositionInFullMode = startPlayTimePositionInFullMode
            timeIndicator.timePositionInFullMode = startPlayTimePositionInFullMode
            // 如果播放到最后, 则根据是否重复播放来确定是否停止播放
            if (startPlayTimePositionInFullModeIndex == endPlayTimePositionInFullModeIndex) {
                if (!isRepeatPlay) {
                    playIndicator.isVisible = false
                    timeIndicator.isVisible = true
                    resetPlayTimeInFullMode()
                    isPlaying = false
                    playListener?.onStop()
                } else {
                    resetPlayTimeInFullMode()
                    playInFullMode()
                }
                return
            }
            // 如果还未播放到最后, 继续播放
            sendEmptyMessageDelayed(0, 200)
            invalidate()
        }
    }


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        // 判断动画是否可用, 不可用时, 播放时间轴不使用动画
        isAnimatorEnable = AnimatorUtil.checkAndSetAnimatorEnable()
        isClickable = true
        // 初始化属性值
        initAttributes(attrs)
        // 初始化刻度笔
        initSegmentPaint()

        // 最大时间显示长度的一半+系统设置的paddingLeft/Right作为偏移量, 避免刻度尺的开始和结束的刻度文本被遮挡
        val maxTimeTextHalfWidth = (calcTextWidth("${maxTime}s") / 2)
        segmentStartOffset =
            max(timeCircleIndicatorRadius, maxTimeTextHalfWidth).toFloat() + paddingLeft
        segmentEndOffset =
            max(timeCircleIndicatorRadius, maxTimeTextHalfWidth).toFloat() + paddingRight
        currentLeft = -segmentStartOffset
        segmentMaxWidth = totalSegmentCount * segmentWidth

        maxScrollDistance = totalSegmentCount * segmentWidth + segmentStartOffset.toInt()

        // 完整模式的初始化
        segmentMaxWidthInFullMode =
            (screenWidth - segmentStartOffset - segmentEndOffset).toInt()
        segmentWidthInFullMode = segmentMaxWidthInFullMode.toFloat() / totalSegmentCountInFullMode
        timeOfPerPixel = maxTime * 1000 / segmentMaxWidthInFullMode.toFloat()

        // 屏幕中间的位置
        val viewWidth = screenWidth - paddingLeft - paddingRight
        screenCenterPosition = viewWidth / 2

        // 初始化滚动器
        initScroller()
        initTimeIndicator()
        initRangeIndicator()
        initPlayIndicator()
        initPlayAnimator()
        initPlayIndicatorMoveAnimator()

        // 最小高度, 尺子的绘制位置加上指示器的高度
        minHeight = rulerStartTop.toInt() + timeIndicator.height + DisplayUtil.dip2px(context, 20f)
    }

    private fun initAttributes(attrs: AttributeSet?) {
        minHeight = DisplayUtil.dip2px(context, 80f)
        screenWidth = resources.displayMetrics.widthPixels
        circleIndicatorGap = DisplayUtil.dip2px(context, 4f)
        // 刻度属性
        segmentTextSize = DisplayUtil.dip2px(context, 10f)
        segmentTextColor = ContextCompat.getColor(context!!, R.color.segment_text_color)
        segmentTextGap = DisplayUtil.dip2px(context, 5f)
        segmentColor = ContextCompat.getColor(context, R.color.segment_color)
        segmentStrokeWidth = DisplayUtil.dip2px(context, 1f).toFloat()
        timeCircleIndicatorRadius = DisplayUtil.dip2px(context, 9f)
        secondSegmentHeight = DisplayUtil.dip2px(context, 3f)
        secondSegmentRadius = DisplayUtil.dip2px(context, 2f).toFloat()
        millisecondSegmentHeight = secondSegmentHeight
        colorWhite = ContextCompat.getColor(context, android.R.color.white)
        // 刻度尺横线起始TOP = paddingTop + 时间项目高度 + 时间项与刻度的间距 + 秒刻度的高度
        rulerStartTop =
            paddingTop + timeCircleIndicatorRadius * 2 + circleIndicatorGap + secondSegmentHeight.toFloat()
        secondStartTop = rulerStartTop - secondSegmentHeight

        millisecondStartTop = rulerStartTop - millisecondSegmentHeight

        segmentCountOfSecond = (1 / segmentTimeInterval).toInt()
        segmentTextPerCount = segmentCountOfSecond
        totalSegmentCount = maxTime * segmentCountOfSecond

        rangeSegmentColor = ContextCompat.getColor(context, R.color.choose_segment_color)
    }


    private fun initSegmentPaint() {
        // 初始化画笔
        segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        segmentPaint.style = Paint.Style.FILL_AND_STROKE
        segmentPaint.isAntiAlias = true
        segmentPaint.color = segmentColor
        segmentPaint.textSize = segmentTextSize.toFloat()
        segmentPaint.strokeWidth = DisplayUtil.dip2px(context, 1f).toFloat()
    }

    private fun initScroller() {
        scroller = Scroller(context)
        val viewConfiguration = ViewConfiguration.get(context)
        scrollSlop = viewConfiguration.scaledTouchSlop
        minVelocity = viewConfiguration.scaledMinimumFlingVelocity
        maxVelocity = viewConfiguration.scaledMaximumFlingVelocity
    }

    private fun refreshRangeRectF() {
        if (timeItemList.isNotEmpty()) {
            rangeRectF.top = millisecondStartTop
            rangeRectF.left = segmentStartOffset
            rangeRectF.bottom = rangeRectF.top + millisecondSegmentHeight.toFloat()
            rangeRectF.right =
                rangeRectF.left + (((timeItemList[timeItemList.size - 1].timePosition
                        / segmentWidth) * segmentTimeInterval) * 1000) / timeOfPerPixel
        }
    }

    /**
     * 初始化时间指示器
     */
    private fun initTimeIndicator() {
        timeIndicator = TimeIndicator(this)
        timeIndicator.timeListener = object : TimeIndicator.TimeListener {
            override fun onPositionChanged(timePosition: Float) {
                // 根据模式计算当前位置对应的时间(毫秒)
                val millisecond = if (isFullMode) {
                    (((timeIndicator.timePositionInFullMode * timeOfPerPixel).toInt()
                            / 200) * 200).toLong()
                } else {
                    (((timeIndicator.timePosition / segmentWidth) * segmentTimeInterval) * 1000).toLong()
                }
                // 计算时间对应的刻度索引
                val timeIndex = (millisecond / segmentTimeInterval / 1000).toInt()
                // 刻度索引有对应时间项, 则回调时间和时间项， 否则回调时间
                if (timeIndex <= timeItemList.size - 1) {
                    timeListener?.onTimeChanged(millisecond, timeItemList[timeIndex].timeItem)
                    timeItemListener?.onSelectedStateChanged(true, timeItemList[timeIndex].timeItem)
                } else {
                    timeListener?.onTimeChanged(millisecond, null)
                    timeItemListener?.onSelectedStateChanged(false, null)
                }
            }
        }
    }

    /**
     * 初始化AB段时间指示器
     */
    private fun initRangeIndicator() {
        rangeIndicator = RangeIndicator(this)
        rangeIndicator.timeListener = object : RangeIndicator.TimeListener {
            override fun onPositionChanged(aTimePosition: Float, bTimePosition: Float) {
                // 时间段变动后, 判断当前的时间指示器是否在时间段范围内, 不在的话自动调整到时间段范围内
                if (!isFullMode) {
                    if (rangeIndicator.isIndicatorAVisible() && timeIndicator.timePosition < aTimePosition) {
                        timeIndicator.updatePosition(aTimePosition - currentLeft)
                    } else if (rangeIndicator.isIndicatorBVisible() && timeIndicator.timePosition > bTimePosition) {
                        timeIndicator.updatePosition(bTimePosition - currentLeft)
                    }
                } else {
                    if (rangeIndicator.isIndicatorAVisible() && timeIndicator.timePositionInFullMode < aTimePosition) {
                        timeIndicator.updatePosition(aTimePosition + segmentStartOffset)
                    } else if (rangeIndicator.isIndicatorBVisible() && timeIndicator.timePositionInFullMode > bTimePosition) {
                        timeIndicator.updatePosition(bTimePosition + segmentStartOffset)
                    }
                }
            }
        }
    }

    /**
     * 初始化播放指示器的移动动画
     */
    private fun initPlayIndicatorMoveAnimator() {
        movePlayIndicatorToCenterAnimator.addUpdateListener {
            currentLeft = it.animatedValue as Float
            computeTime()
        }
        movePlayIndicatorToCenterAnimator.addListener(onEnd = {
            when {
                isNeedToPlay -> {// 移动完之后需要播放的, 直接进行播放
                    if (!isFullMode) {
                        doPlay(startPlayTimePosition)
                    } else {
                        checkIfNeedToContinuePlay()
                    }
                }
                isRepeatPlay -> {// 移动完之后需要重新开始播放的
                    post {
                        if (!isFullMode) {
                            preparePlay()
                        } else {
                            checkIfNeedToContinuePlay()
                        }
                    }
                }
                else -> { // 移动完不做其他动作时 ， 隐藏播放指示器，显示时间指示器
                    timeIndicator.isVisible = true
                    playIndicator.isVisible = false
                }
            }
        })
    }

    /**
     * 初始化播放指示器
     */
    private fun initPlayIndicator() {
        playIndicator = TimeIndicator(this)
        val width = DisplayUtil.dip2px(context, 12f)
        val height = DisplayUtil.dip2px(context, 61f)
        playIndicator.width = width
        playIndicator.height = height
        playIndicator.backgroundBitmap = Utils.drawableToBitmap(
            context,
            width,
            height,
            R.mipmap.light_timeline_slider2
        )
        playIndicator.offsetY =
            -(segmentTextGap + segmentTextSize + DisplayUtil.dip2px(context, 13f))
        playIndicator.isVisible = false
    }

    /**
     * 初始化播放动画
     */
    private fun initPlayAnimator() {
        playAnimator = ValueAnimator()
        playAnimator.interpolator = LinearInterpolator()
        playAnimator.addUpdateListener() {
            playIndicator.timePosition = it.animatedValue as Int + startPlayRealPosition
            // 当前的时间位置超过屏幕中心点时, 则移动刻度尺位置, 保持播放指示器一直在中间
            if (playIndicator.timePosition - currentLeft > screenCenterPosition) {
                currentLeft += (playIndicator.timePosition - currentLeft - screenCenterPosition)
            }
            timeIndicator.timePosition = it.animatedValue as Int + startPlayRealPosition
            currentPlayTimeIndex = playIndicator.timePosition / segmentWidth
            // 时间刻度变化时, 回调时间变化
            if (startPlayTimeIndex != currentPlayTimeIndex) {
                startPlayTimeIndex = currentPlayTimeIndex
                playListener?.onTimeChanged(
                    (currentPlayTimeIndex.toFloat() * 1000 / segmentCountOfSecond).toLong(),
                    timeItemList[currentPlayTimeIndex].timeItem
                )
            }
            // 计算并刷新视图
            computeTime()
        }

        playAnimator.addListener(onEnd = {
            // 如果不是手动停止, 则自动跳转到开始时间的位置
            if (!isCancel) {
                postDelayed({
                    // 重复播放则重置到开始时间再继续播放
                    if (isRepeatPlay) {
                        // 需要检查是否需要重复播放和在完整模式下是否需要继续播放
                        if (!isFullMode) {
                            resetPlayTime()
                        } else {
                            checkIfNeedToContinuePlay()
                        }
                    } else {
                        // 重置开始时间并停止播放
                        playIndicator.isVisible = false
                        timeIndicator.isVisible = true
                        isPlaying = false
                        resetPlayTime()
                        playListener?.onStop()
                    }
                }, 500) // 停500ms, 主要是提高用户体验
            } else if (isNeedResetPlayIndicator) {
                // 取消播放时, 当位置已经调整完之后, 隐藏播放指示器, 显示时间指示器
                timeIndicator.isVisible = true
                playIndicator.isVisible = false
                checkIfNeedToContinuePlay()
            }
        }, onCancel = {
            isCancel = true
            // 修正当前时间到正确的刻度上
            fixTimeIndicatorOffset()
        })
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val mWidth = MeasureSpec.getSize(widthMeasureSpec)
        var mHeight = MeasureSpec.getSize(heightMeasureSpec)

        // 只处理wrap_content的高度，设置为时间轴的最低高度
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            mHeight = minHeight
        }

        setMeasuredDimension(mWidth, mHeight)
    }

    /**
     * 普通模式下的绘制
     * @param canvas 画布
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isFullMode) {
            // 画刻度
            drawSegment(canvas)
            // 画指示器
            timeIndicator.onDraw(segmentPaint, canvas)
            // 画时间项
            timeItemList.forEach { it.onDraw(canvas) }
            // AB段指示器
            rangeIndicator.onDraw(canvas)
            // 画播放指示器
            playIndicator.onDraw(segmentPaint, canvas)
        } else {
            drawOnFullMode(canvas)
        }
    }

    /**
     * 完整模式下的绘制
     * @param canvas 画布
     */
    private fun drawOnFullMode(canvas: Canvas) {
        // 先画进度, 不会覆盖掉刻度
        drawRange(canvas)
        // 画刻度
        drawSegmentInFullMode(canvas)
        // 画时间指示器
        timeIndicator.onDrawInFullMode(segmentPaint, canvas)
        // 画时间段指示器
        rangeIndicator.onDrawInFullMode(canvas)
        playIndicator.onDrawInFullMode(segmentPaint, canvas)
    }

    /**
     * 完整模式下画范围
     * @param canvas 画布
     */
    private fun drawRange(canvas: Canvas) {
        segmentPaint.color = rangeSegmentColor
        segmentPaint.strokeWidth = 0f
        canvas.drawRect(rangeRectF, segmentPaint)
    }

    /**
     * 完整模式下画刻度
     * @param canvas 画布
     */
    private fun drawSegmentInFullMode(canvas: Canvas) {
        segmentPaint.color = segmentColor
        segmentPaint.strokeWidth = segmentStrokeWidth

        // 画线
        canvas.drawLine(
            segmentStartOffset,
            rulerStartTop,
            segmentStartOffset + segmentMaxWidthInFullMode,
            rulerStartTop,
            segmentPaint
        )

        // 画刻度
        for (index in 0 until totalSegmentCountInFullMode + 1) {
            val offset =
                segmentStartOffset + index * segmentWidthInFullMode
            // 第一个和最后一个刻度是线， 其余的为圆形
            segmentPaint.color = segmentColor
            segmentPaint.strokeWidth = segmentStrokeWidth
            if (index == 0 || index == totalSegmentCountInFullMode) {
                canvas.drawLine(
                    offset, secondStartTop, offset, rulerStartTop, segmentPaint
                )
            } else {
                if (index % 2 == 0) {
                    canvas.drawCircle(
                        offset, rulerStartTop, secondSegmentRadius, segmentPaint
                    )
                } else {
                    canvas.drawLine(
                        offset, secondStartTop, offset, rulerStartTop, segmentPaint
                    )
                }
            }

            // 画文本
            if (index % 2 == 0) {
                val timeText = formatPosToTimeText(index)
                segmentPaint.strokeWidth = 0f
                segmentPaint.color = segmentTextColor
                val timeTextHalfWidth = calcTextWidth(timeText) / 2
                val y = rulerStartTop + segmentTextGap + segmentTextSize
                canvas.drawText(
                    timeText, offset - timeTextHalfWidth, y, segmentPaint
                )
            }
        }

    }

    /**
     * 普通模式下画刻度
     * @param canvas 画布
     */
    private fun drawSegment(canvas: Canvas) {
        // 计算刻度结束坐标(相对坐标)
        val segmentStopPos =
            if (currentLeft + width > segmentMaxWidth) segmentMaxWidth - currentLeft else width
        segmentPaint.color = segmentColor

        // 计算开始绘制刻度的坐标(相对屏幕坐标)
        val segmentStartPos = if (currentLeft < 0) {
            -currentLeft
        } else {
            -currentLeft % segmentWidth
        }

        // 画刻度底部完整长线
        segmentPaint.strokeWidth = segmentStrokeWidth
        canvas.drawLine(
            segmentStartPos - segmentPaint.strokeWidth / 2,
            rulerStartTop,
            segmentStopPos.toFloat() + segmentPaint.strokeWidth / 2,
            rulerStartTop,
            segmentPaint
        )

        // 画刻度, 从当前时间索引开始画
        for (index in currentLeftIndex until totalSegmentCount + 1) {
            val offset = segmentStartPos + (index - currentLeftIndex) * segmentWidth.toFloat()
            // 超出屏幕宽度的不再绘制
            if (offset <= screenWidth + segmentWidth) {
                segmentPaint.color = segmentColor
                segmentPaint.strokeWidth = segmentStrokeWidth
                // 画秒刻度
                if (index % segmentCountOfSecond == 0) {
                    // 第一个和最后一个刻度是线， 其余的为圆形
                    if (index == 0 || index == totalSegmentCount) {
                        canvas.drawLine(
                            offset, secondStartTop, offset, rulerStartTop, segmentPaint
                        )
                    } else {
                        canvas.drawCircle(
                            offset, rulerStartTop, secondSegmentRadius, segmentPaint
                        )
                    }
                } else {
                    // 画毫秒刻度
                    canvas.drawLine(
                        offset, millisecondStartTop, offset, rulerStartTop, segmentPaint
                    )
                }
                // 画文本刻度
                if (index % segmentTextPerCount == 0) {
                    val timeText = formatPosToTimeText(index)
                    segmentPaint.strokeWidth = 0f
                    segmentPaint.color = segmentTextColor
                    val timeTextHalfWidth = calcTextWidth(timeText) / 2
                    val y = rulerStartTop + segmentTextGap + segmentTextSize
                    canvas.drawText(
                        timeText, offset - timeTextHalfWidth, y, segmentPaint
                    )
                }
            }
        }
    }

    /**
     * 在完整模式下，是否需要继续播放
     */
    private fun checkIfNeedToContinuePlay() {
        if (isNeedToContinuePlay && isFullMode) {
            isNeedToContinuePlay = false
            convertTimeToFullMode()
            playInFullMode()
        } else {
            playListener?.onStop()
        }
    }

    /**
     * 停止播放时, 修正时间索引, 要修正到刚好在时间刻度上
     */
    private fun fixTimeIndicatorOffset() {
        val currentValue = playAnimator.animatedValue as Int
        // 如果停止播放时, 时间索引没有刚刚匹配到刻度, 则移动到下一个刻度再停止
        if (playIndicator.timePosition % segmentWidth != 0) {
            // 计算距离下一个时间刻度的距离, 通过距离计算到下一个时间刻度所需要的时间并作为动画的时长
            val offset = segmentWidth - playIndicator.timePosition % segmentWidth
            playAnimator.duration =
                ((offset.toFloat() / segmentWidth / segmentCountOfSecond) * 1000).toLong()
            // 以当前动画取消时的值作为开始, 以取消值+到下一个时间刻度距离作为结束值
            playAnimator.setIntValues(
                playAnimator.animatedValue as Int,
                currentValue + offset
            )
            post { // 需要post, 否则无法启动动画
                isNeedResetPlayIndicator = true
                playAnimator.start()
            }
        } else {
            timeIndicator.isVisible = true
            playIndicator.isVisible = false
            checkIfNeedToContinuePlay()
        }
    }


    /**
     * 计算文本宽度
     * @param text 需要计算的文本
     */
    private fun calcTextWidth(text: String): Int {
        segmentPaint.getTextBounds(text, 0, text.length, segmentTextRect)
        return segmentTextRect.width()
    }

    /**
     * 移动时间指示器， 保持在屏幕中间
     */
    private fun moveTimeIndicatorToCenter() {
        val viewWidth = screenWidth - paddingLeft - paddingRight
        val position = viewWidth / 2
        val valueAnimator = ValueAnimator()
        // 指示器时间索引超过屏幕一半的宽度, 则开启自动滚动
        if (timeIndicator.timePosition >= position) {
            // 保存旧的时间刻度位置, 动画执行过程中进行累加
            val oldCurrentLeft = currentLeft
            // 计算要移动的距离: 时间指示器当前位置+1个刻度的位置 - 当前屏幕最左侧的时间索引-  屏幕中间坐标X
            valueAnimator.setFloatValues(
                0f,
                (timeIndicator.timePosition + segmentWidth - currentLeft - position)
            )
            post {
                valueAnimator.addUpdateListener {
                    // 时间指示器超出中线多少, 就移动时间刻度多少
                    currentLeft = oldCurrentLeft + it.animatedValue as Float
                    // 重新计算时间刻度， 已经到刻度尺尾段了, 重新刷新currentLeft
                    computeTime()
                }
                valueAnimator.start()
            }

            // 时间指示器如果已经指向最后一个刻度， 则不在继续向前移动, 否则向前移动一个刻度
            if (timeIndicator.timePosition / segmentWidth != totalSegmentCount) {
                val animator = ValueAnimator()
                val oldTimeIndicatorPosition = timeIndicator.timePosition
                // 向前移动一个刻度, 修改timePosition, 因为同时有移动时间刻度的动画, 不能使用updatePosition
                animator.setIntValues(0, segmentWidth)
                animator.addUpdateListener {
                    timeIndicator.timePosition =
                        oldTimeIndicatorPosition + it.animatedValue as Int
                }
                animator.addListener(onEnd = {
                    timeIndicator.updatePosition(timeIndicator.timePosition - currentLeft)
                })
                post {
                    animator.start()
                }
            }
        } else {
            // 未超过屏幕中间, 则指示器向前移动一个刻度
            timeIndicator.updatePosition(timeIndicator.currentPosition + segmentWidth.toFloat())
        }
    }


    /**
     * 准备播放时间轴
     */
    private fun preparePlay() {
        isNeedResetPlayIndicator = false
        isCancel = false
        // 开始播放的时间位置
        startPlayTimePosition = getTimePosition()
        stopPlayTimeIndex = timeItemList.size - 1
        // 如果当前指示器指向的不是时间项内的时间点, 则从头开始播放, 否则从指向的时间点开始播放
        if (startPlayTimePosition > timeItemList[timeItemList.size - 1].timePosition) {
            startPlayTimePosition = 0
        }
        // 如果未完整设置时间段, 则重置时间段
        if (!rangeIndicator.isIndicatorBVisible()) {
            rangeIndicator.reset()
        } else {
            stopPlayTimeIndex = getTimeIndex(rangeIndicator.getIndicatorBTimePosition())
        }
        playIndicator.timePosition = startPlayTimePosition
        startPlayTimeIndex = startPlayTimePosition / segmentWidth

        // 播放指示器在当前屏幕的实际坐标
        val realPosition = startPlayTimePosition - currentLeft
        when {
            // 指示器在屏幕中心之后, 移动到屏幕中心后才开始播放
            realPosition > screenCenterPosition -> {
                isNeedToPlay = true
                movePlayIndicatorToCenterAnimator.setFloatValues(
                    currentLeft,
                    startPlayTimePosition - screenCenterPosition.toFloat()
                )
                post { movePlayIndicatorToCenterAnimator.start() }
            }
            // 指示器在屏幕中心之前, 移动到屏幕中心后才开始播放
            realPosition < screenCenterPosition -> {
                isNeedToPlay = true
                movePlayIndicatorToCenterAnimator.setFloatValues(
                    currentLeft,
                    startPlayTimePosition - screenCenterPosition.toFloat()
                )
                post { movePlayIndicatorToCenterAnimator.start() }
            }
            else -> {
                // 指示器在屏幕中心, 直接播放
                doPlay(startPlayTimePosition)
            }
        }
    }

    /**
     * 完整模式下的播放逻辑
     */
    private fun playInFullMode() {
        isPlaying = true
        playIndicator.isVisible = true
        timeIndicator.isVisible = false
        // 从当前时间指示器指向的位置开始播放
        startPlayTimePositionInFullMode = timeIndicator.timePositionInFullMode
        val startPlayTimePosition = convertToTimePosition(startPlayTimePositionInFullMode)
        startPlayTimePositionInFullModeIndex = getTimeIndex(startPlayTimePosition)
        // 如果当前指示器指向的不是时间项内的时间点, 则从头开始播放, 否则从指向的时间点开始播放
        endPlayTimePositionInFullMode =
            convertToFullModeTimePosition(timeItemList[timeItemList.size - 1].timePosition)
        endPlayTimePositionInFullModeIndex = timeItemList.size - 1

        if (startPlayTimePositionInFullMode > endPlayTimePositionInFullMode) {
            startPlayTimePositionInFullMode = 0f
            startPlayTimePositionInFullModeIndex = 0
        }
        if (rangeIndicator.isIndicatorBVisible()) {
            endPlayTimePositionInFullMode = rangeIndicator.getIndicatorBTimePositionInFullMode()
            val timePosition =
                convertToTimePosition(endPlayTimePositionInFullMode)
            endPlayTimePositionInFullModeIndex = getTimeIndex(timePosition)
        } else {
            rangeIndicator.reset()
        }
        playIndicator.timePositionInFullMode = startPlayTimePositionInFullMode
        invalidate()
        callbackStart()
        playInFullModeHandler.sendEmptyMessageDelayed(0, 200)
    }


    /**
     * 回调开始播放事件, 避免循环播放时重复回调开始播放
     */
    private fun callbackStart() {
        if (!isCalledStart) {
            isCalledStart = true
            playListener?.onStart()
        }
    }

    /**
     * 获取当前时间位置对应的时间刻度索引
     */
    private fun getTimeIndex(timePosition: Int): Int {
        var timeIndex = -1
        timeItemList.forEachIndexed { index, circleIndicator ->
            if (timePosition == circleIndicator.timePosition) {
                timeIndex = index
                return@forEachIndexed
            }
        }
        return timeIndex
    }

    /**
     * 读取当前时间指示器对应的刻度索引
     */
    private fun getCurrentTimeIndex(): Int {
        var timePosition = getTimePosition()
        if (isFullMode) timePosition = convertToTimePosition(timePosition.toFloat())
        return getTimeIndex(timePosition)
    }

    /**
     * 播放时间轴
     * @param startPlayTimePosition 播放时间位置
     */
    private fun doPlay(startPlayTimePosition: Int) {
        // 当前没有AB段时， 直接播放动画
        // 计算一共要移动的距离, 如果有B端, 结束的时间点为B的时间点, 否则为最后一个时间项的时间点
        val totalMovePos = if (rangeIndicator.isIndicatorBVisible()) {
            rangeIndicator.getIndicatorBTimePosition() - startPlayTimePosition
        } else {
            (timeItemList.size - startPlayTimeIndex - 1) * segmentWidth
        }
        // 计算移动距离所需要的时间 : = (总移动距离/每个刻度距离/1秒多少个刻度)* 1秒
        val duration =
            ((totalMovePos / segmentWidth / segmentCountOfSecond.toFloat()) * 1000).toLong()
        // 计算当前播放指示器的真实坐标位置
        startPlayRealPosition = playIndicator.timePosition - currentLeft.toInt()
        playIndicator.isVisible = true
        timeIndicator.isVisible = false

        if (isAnimatorEnable) {
            // 调整时间尺的进度
            playAnimator.setIntValues(currentLeft.toInt(), totalMovePos + currentLeft.toInt())
            playAnimator.duration = duration
            callbackStart()
            playAnimator.start()
        } else {
            invalidate()
            callbackStart()
            playHandler.sendEmptyMessageDelayed(0, 200)
        }
    }

    /**
     * 完整模式下停止播放
     */
    private fun stopPlayInFullMode() {
        playInFullModeHandler.removeCallbacksAndMessages(null)
        playIndicator.isVisible = false
        timeIndicator.isVisible = true
        invalidate()
        playListener?.onStop()
    }

    /**
     * 普通模式下重置播放时间
     */
    private fun resetPlayTime() {
        isNeedToPlay = false
        // 有设置AB段的情况, 重置到A段位置
        if (rangeIndicator.isIndicatorBVisible()) {
            timeIndicator.timePosition = rangeIndicator.getIndicatorATimePosition()
            movePlayIndicatorToCenterAnimator.setFloatValues(
                currentLeft,
                timeIndicator.timePosition - screenCenterPosition.toFloat()
            )
        } else {
            // 没有设置AB段的情况, 重置到0位置
            timeIndicator.timePosition = 0
            movePlayIndicatorToCenterAnimator.setFloatValues(currentLeft, -segmentStartOffset)
        }
        playIndicator.timePosition = timeIndicator.timePosition
        val timeIndex = playIndicator.timePosition / segmentWidth
        playListener?.onTimeChanged(
            (timeIndex.toFloat() * 1000 / segmentCountOfSecond).toLong(),
            timeItemList[timeIndex].timeItem
        )
        movePlayIndicatorToCenterAnimator.start()
    }

    /**
     * 完整模式下的重置时间轴播放时间范围
     */
    private fun resetPlayTimeInFullMode() {
        // 有设置A-B段时, 开始时间在A， 结束时间在B
        if (rangeIndicator.isIndicatorAVisible() && rangeIndicator.isIndicatorBVisible()) {
            startPlayTimePositionInFullMode = rangeIndicator.getIndicatorATimePositionInFullMode()
            var indicatorTimeIndex =
                getTimeIndex(convertToTimePosition(startPlayTimePositionInFullMode))
            startPlayTimePositionInFullModeIndex = indicatorTimeIndex

            endPlayTimePositionInFullMode = rangeIndicator.getIndicatorBTimePositionInFullMode()
            indicatorTimeIndex =
                getTimeIndex(convertToTimePosition(endPlayTimePositionInFullMode))
            endPlayTimePositionInFullModeIndex = indicatorTimeIndex

        } else {
            // 没有设置A-B时间段时, 开始时间在设置的时间项的第1个，结束时间在设置的时间项的最后一个
            startPlayTimePositionInFullMode =
                convertToFullModeTimePosition(timeItemList[0].timePosition)
            startPlayTimePositionInFullModeIndex = 0
            endPlayTimePositionInFullMode =
                convertToFullModeTimePosition(timeItemList[timeItemList.size - 1].timePosition)
            endPlayTimePositionInFullModeIndex =
                getTimeIndex(timeItemList[timeItemList.size - 1].timePosition)
        }
        // 刷新播放和时间指示器
        playIndicator.timePositionInFullMode = startPlayTimePositionInFullMode
        timeIndicator.timePositionInFullMode = startPlayTimePositionInFullMode
        if (!isRepeatPlay) {
            playIndicator.isVisible = false
            timeIndicator.isVisible = true
        }
        playListener?.onTimeChanged(
            (startPlayTimePositionInFullModeIndex.toFloat() * 1000 / segmentCountOfSecond).toLong(),
            timeItemList[startPlayTimePositionInFullModeIndex].timeItem
        )
        invalidate()
    }

    /**
     * 转换当前时间到完整模式格式
     */
    private fun convertTimeToFullMode() {
        // 转换时间指示器位置
        timeIndicator.timePositionInFullMode =
            convertToFullModeTimePosition(timeIndicator.timePosition)
        timeIndicator.currentPositionInFullMode =
            timeIndicator.timePositionInFullMode + segmentStartOffset
        playIndicator.timePositionInFullMode = timeIndicator.timePositionInFullMode
        playIndicator.currentPositionInFullMode = timeIndicator.currentPositionInFullMode

        // 转换A-B时间指示器位置
        rangeIndicator.setIndicatorATimePositionInFullMode(
            convertToFullModeTimePosition(
                rangeIndicator.getIndicatorATimePosition()
            )
        )
        rangeIndicator.setIndicatorBTimePositionInFullMode(
            convertToFullModeTimePosition(
                rangeIndicator.getIndicatorBTimePosition()
            )
        )
        refreshRangeRectF()
        invalidate()
    }

    /**
     * 转换当前时间到普通模式格式
     */
    private fun convertTimeToNormalMode() {
        // 转换时间指示器位置
        timeIndicator.timePosition = convertToTimePosition(timeIndicator.timePositionInFullMode)
        playIndicator.timePosition = timeIndicator.timePosition
        currentLeft = timeIndicator.timePosition - screenCenterPosition.toFloat()
        rangeIndicator.setIndicatorATimePosition(convertToTimePosition(rangeIndicator.getIndicatorATimePositionInFullMode()))
        rangeIndicator.setIndicatorBTimePosition(convertToTimePosition(rangeIndicator.getIndicatorBTimePositionInFullMode()))
        computeTime()
    }

    /**
     * 转换非完整模式的时间位置为完整模式下对应的时间位置
     */
    private fun convertToFullModeTimePosition(timePosition: Int): Float {
        // 先计算多少个刻度,再乘以每个刻度对应的时间(秒), 再乘以1000得到毫秒时间
        // 最后除以完整模式下1个像素多少毫秒得出最终占用的像素大小
        return (((timePosition / segmentWidth) * segmentTimeInterval) * 1000) / timeOfPerPixel
    }

    /**
     * 转换完整模式的时间位置为非完整模式对应的时间位置
     */
    private fun convertToTimePosition(timePositionInFullMode: Float): Int {
        // 像素数*每个像素对应的毫秒时间，得到毫秒时间
        val timeMS = timePositionInFullMode * timeOfPerPixel
        // 毫秒时间除以1000得到秒时间， 乘以每秒占用多少刻度得到刻度总数，刻度总数*刻度宽度=时间对应的像素大小
        return (((timeMS) / 1000) * segmentCountOfSecond * segmentWidth).roundToInt()
    }

    /**
     * 处理时间轴滚动、指示器滚动
     * @param event 触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // 初始化惯性滑动计算器
        if (null == velocityTracker) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)
        val x = event.x
        val y = event.y
        // 非拖拽的时候， 检测触摸区域，避免触控区域过大
        if (!timeIndicator.isDragging && !rangeIndicator.isDragging()) {
            if (!checkIfInTouchArea(y)) {
                return super.onTouchEvent(event)
            }
        }
        if (isPlaying || isOperating) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> run {
                isMoving = false
                isFling = false
                startX = x
                // 按下时, 停止滑动
                if (!scroller.isFinished) {
                    scroller.forceFinished(true)
                }
                invalidate()

                if (!isOperating) {
                    // 判断当前是否按住了A-B指示器
                    if (rangeIndicator.isTouched(x.toInt(), y.toInt())) {
                        rangeIndicator.setDragging(true)
                    } else if (timeIndicator.isTouched(x.toInt(), y.toInt())) {
                        // 判断当前是否按住了指示器
                        timeIndicator.isDragging = true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> run {
                if (rangeIndicator.isDragging()) {
                    rangeIndicator.moveTo(x)
                    return@run
                }
                // 如果当前已经按住了指示器，则直接移动指示器
                if (timeIndicator.isDragging) {
                    moveTimeIndicator(x)
                    return@run
                }
                val moveX = x - lastX
                // 根据系统的配置确定当前移动距离是否可以算移动
                if (!isMoving) {
                    val dy: Int = (y - lastY).toInt()
                    lastX = x
                    lastY = y
                    if (abs(x - startX) <= scrollSlop || abs(moveX) <= abs(dy)) {
                        return@run
                    }
                    isMoving = true
                }
                currentLeft -= moveX
                computeTime()
            }
            MotionEvent.ACTION_UP -> run {
                if (rangeIndicator.isDragging()) {
                    rangeIndicator.setDragging(false)
                    rangeIndicator.updatePosition(x)
                    return@run
                } else if (timeIndicator.isDragging) {
                    timeIndicator.isDragging = false
                    updateTimeIndicatorPosition(x)
                    latestTimeIndex = getCurrentTimeIndex()
                    return@run
                }
                if (!isMoving) {
                    if (abs(x - startX) <= 6) {
                        updateTimeIndicatorPosition(x)
                        val timeIndex = getCurrentTimeIndex()
                        // 处理时间项点击事件
                        processTimeItemClick(timeIndex, x.toInt(), y.toInt())
                        latestTimeIndex = timeIndex
                    }
                    return@run
                }

                // 计算惯性滑动并进行自动滑动
                processScroller()
            }
        }
        lastX = x
        lastY = y
        return super.onTouchEvent(event)

    }

    /**
     * 处理惯性滚动
     */
    private fun processScroller() {
        // 计算惯性滑动并进行自动滑动
        velocityTracker?.computeCurrentVelocity(2000, maxVelocity.toFloat())
        val xVelocity = velocityTracker?.xVelocity!!.toInt()
        if (abs(xVelocity) >= minVelocity) {
            isFling = true
            // 惯性滑动

            scroller.fling(
                currentLeft.toInt(),
                0,
                -xVelocity,
                0,
                -2000,
                maxScrollDistance + 2000,
                0,
                0
            )
            invalidate()
        }
    }


    /**
     * 计算时间
     */
    private fun computeTime() {
        // 左侧超过最大的padding值， 则不让继续滚动
        if (currentLeft < -segmentStartOffset) {
            currentLeft = -segmentStartOffset
            isMoving = false
        }

        // 右侧超过刻度尺宽度, 则不让继续滚动
        if (currentLeft + screenWidth > totalSegmentCount * segmentWidth + segmentEndOffset) {
            currentLeft =
                (totalSegmentCount * segmentWidth + segmentEndOffset) - screenWidth.toFloat()
        }

        // 当前左侧的时间刻度
        currentLeftIndex = (currentLeft.toInt() / segmentWidth)

        invalidate()
    }

    /**
     * 处理滚动计算
     */
    override fun computeScroll() {
        isFling = !scroller.isFinished
        if (scroller.computeScrollOffset()) {
            val currX = scroller.currX.toFloat()
            currentLeft = currX
            val maxDistance = totalSegmentCount * segmentWidth + segmentStartOffset.toInt()
            // 滚动超出最左边后立刻停止, 避免继续重复绘制
            if (currX < -segmentStartOffset || currX > maxDistance) {
                scroller.forceFinished(true)
            }
            computeTime()
        }
    }

    /**
     * 当删除时间项时，对AB段范围进行检查和调整
     */
    private fun checkRangeIndicatorWithDelete(deleteCircleIndicator: CircleIndicator) {
        // 只有A段, 没有B段的时候, 刷新A段索引
        // 如果往前已经没有刻度时, 删除A段
        if (rangeIndicator.isIndicatorAVisible() && !rangeIndicator.isIndicatorBVisible()) {
            // 删除的时间项是第一个时间项, 则重置AB段
            if (deleteCircleIndicator.timePosition == 0) {
                rangeIndicator.reset()
            } else if (deleteCircleIndicator.timePosition == rangeIndicator.getIndicatorATimePosition()
                && deleteCircleIndicator.timePosition == timeItemList[timeItemList.size - 1].timePosition
            ) {
                // 删除的时间项是A指向的时间项且是最后一个时间项, 则A往前移动一个刻度
                rangeIndicator.updateIndicatorAPosition(getTimePosition() - segmentWidth - currentLeft)
            }
        } else if (rangeIndicator.isIndicatorBVisible()) {
            if ((deleteCircleIndicator.timePosition == rangeIndicator.getIndicatorBTimePosition()
                        && rangeIndicator.getIndicatorATimePosition() == rangeIndicator.getIndicatorBTimePosition())
                || deleteCircleIndicator.timePosition == 0
            ) {// AB在同一个时间项且当前删除的就是这个时间项 或者 当前删除的时间索引是0， 则删除AB时间段
                rangeIndicator.reset()
            } else if (deleteCircleIndicator.timePosition in (rangeIndicator.getIndicatorATimePosition()
                        until rangeIndicator.getIndicatorBTimePosition() + 1)
            ) {
                // 删除的时间项是A-B的范围之间的， 将B向前移动一个刻度
                rangeIndicator.updateIndicatorBPosition(
                    rangeIndicator.getIndicatorBTimePosition()
                            - segmentWidth - currentLeft
                )
            }
        }
    }

    /**
     * 新增时间项在AB段范围内的, 自动扩大AB范围
     * @param timePosition 新增的时间项位置
     */
    private fun checkRangeIndicatorWhenAdd(timePosition: Int) {
        // AB段可见的情况下, 如果在AB范围内新增, 则扩大AB范围
        if (rangeIndicator.isIndicatorBVisible() &&
            timePosition in (rangeIndicator.getIndicatorATimePosition()
                    until rangeIndicator.getIndicatorBTimePosition() + 1)
        ) {
            rangeIndicator.updateIndicatorBPosition(rangeIndicator.getIndicatorBTimePosition() + segmentWidth - currentLeft)
        }
    }

    /**
     * 处理时间项点击事件
     * 当时间指示器已经指向时间项时，再次点击才算是单击事件
     */
    private fun processTimeItemClick(timeIndex: Int, x: Int, y: Int) {
        // 处理时间项点击事件
        if (timeIndex != -1 && timeIndex < timeItemList.size) {
            val circleIndicator = timeItemList[timeIndex]
            // 时间项被触碰, 且时间指示器之前已经指向了当前时间项
            if (circleIndicator.isTouched(x, y)
                && latestTimeIndex == timeIndex
            ) {
                timeItemClickListener?.onClick(timeIndex, circleIndicator.timeItem)
            }
        }
    }

    /**
     * 移动时间指示器
     * @param targetPosition 目标坐标x
     */
    private fun moveTimeIndicator(targetPosition: Float) {
        // 如果时间段A已经设置, 则不能移动到A时间之前
        if (rangeIndicator.isIndicatorAVisible()) {
            val indicatorATimePosition = rangeIndicator.getIndicatorAPosition()
            if (targetPosition < indicatorATimePosition) {
                timeIndicator.moveTo(indicatorATimePosition)
                return
            }
        }
        // 如果时间段B已经设置, 则不能移动到B时间之后
        if (rangeIndicator.isIndicatorBVisible()) {
            val indicatorBTimePosition = rangeIndicator.getIndicatorBPosition()
            if (targetPosition > indicatorBTimePosition) {
                timeIndicator.moveTo(indicatorBTimePosition)
                return
            }
        }
        timeIndicator.moveTo(targetPosition)
    }

    /**
     * 更新时间指示器位置
     * @param targetPosition 目标坐标x
     */
    private fun updateTimeIndicatorPosition(targetPosition: Float) {
        // 如果时间段A已经设置, 则不能移动到A时间之前
        if (rangeIndicator.isIndicatorAVisible()) {
            val indicatorATimePosition = rangeIndicator.getIndicatorAPosition()
            if (targetPosition < indicatorATimePosition) {
                timeIndicator.updatePosition(indicatorATimePosition)
                return
            }
        }
        // 如果时间段B已经设置, 则不能移动到B时间之后
        if (rangeIndicator.isIndicatorBVisible()) {
            val indicatorBTimePosition = rangeIndicator.getIndicatorBPosition()
            if (targetPosition > indicatorBTimePosition) {
                timeIndicator.updatePosition(indicatorBTimePosition)
                return
            }
        }
        timeIndicator.updatePosition(targetPosition)
    }

    /**
     * 将位置格式化为具体时间
     * @param position 时间位置
     */
    private fun formatPosToTimeText(position: Int): String {
        return if (!isFullMode) {
            "${(position * segmentTimeInterval).toInt()}s"
        } else {
            "${position * timeOfPerSegment}s"
        }
    }

    /**
     * 增加时间项
     * @param flag 设备索引
     * @param color 颜色
     * @param data 携带设备信息
     */
    fun addTimeItem(flag: Int, color: Int, data: Any?) {
        if (isPlaying || isFullMode) {
            return
        }
        // 满了就不再增加
        if (isFull()) {
            Log.e(tag, "Can not add because is full.")
            return
        }
        //  已经在执行操作时, 不能继续操作
        if (isOperating) {
            Log.e(tag, "Current is operating, ignored to add.")
            return
        }
        isOperating = true
        val timePosition = getTimePosition()
        // 计算当前刻度索引
        val timeSegmentIndex = timePosition / segmentWidth
        // 新建时间项
        val circleIndicator = CircleIndicator(this, TimeItem(flag, color, data), timePosition)
        // 判断当前索引是否已经添加时间项， 已经添加了则新增再移动， 否则直接新增
        if (timeItemList.size - 1 < timeSegmentIndex) {
            // 用空时间项填充timeItemList最后一个时间项到timeSegmentIndex
            val startIndex = if (timeItemList.isEmpty()) 0 else timeItemList.size
            for (index in startIndex until timeSegmentIndex) {
                timeItemList.add(
                    CircleIndicator(
                        this,
                        TimeItem(-1, colorWhite, null),
                        index * segmentWidth
                    )
                )
            }

            // 直接新增。 如果当前正在滑动, 则不做动画, 否则显示动画
            timeItemList.add(circleIndicator)
            circleIndicator.show(object : CircleIndicator.OnShowCallback {
                override fun onFinishShow() {
                    isOperating = false
                    refreshRangeRectF()
                    timeItemListener?.onAdd()
                    timeItemListener?.onSizeChanged(timeItemList.size)
                }
            })
            moveTimeIndicatorToCenter()
        } else {
            // 新增并移动
            // 筛选出需要移动的时间项
            val moveCircleIndicator = ArrayList<CircleIndicator>()
            for (i in timeSegmentIndex until timeItemList.size) {
                val indicatorPos = timeItemList[i].timePosition - currentLeft
                // 只移动屏幕可见的时间项, 避免性能浪费
                if (indicatorPos > -segmentWidth && indicatorPos < screenWidth + segmentWidth) {
                    moveCircleIndicator.add(timeItemList[i])
                } else {
                    timeItemList[i].timePosition += segmentWidth
                }
            }
            // 移动时间项
            moveCircleIndicator.forEach {
                it.moveToNext(true)
            }

            // 新增时间项
            timeItemList.add(timeSegmentIndex, circleIndicator)
            circleIndicator.show(object : CircleIndicator.OnShowCallback {
                override fun onFinishShow() {
                    isOperating = false
                    refreshRangeRectF()
                    timeItemListener?.onAdd()
                    timeItemListener?.onSizeChanged(timeItemList.size)
                }
            })
            // 检查
            checkRangeIndicatorWhenAdd(timePosition)
        }
    }

    /**
     * 增加空白时间项
     */
    fun addEmptyTimeItem() {
        addTimeItem(-1, colorWhite, null)
    }

    /**
     * 是否有时间项
     */
    fun hasTimeItem(): Boolean {
        return timeItemList.isNotEmpty()
    }

    /**
     * 切换模式, 完整和非完整模式
     */
    fun setFullMode(isFullMode: Boolean) {
        if (isOperating) {
            return
        }
        // 正在播放时, 取消非完整模式的播放, 切换到完整模式下的播放
        if (isPlaying) {
            if (isFullMode) {
                isNeedToContinuePlay = true
                // 根据当前是否动画播放来处理继续播放逻辑
                if (isAnimatorEnable) {
                    playAnimator.cancel()
                } else {
                    playHandler.removeCallbacksAndMessages(null)
                    convertTimeToFullMode()
                    playInFullMode()
                }
            }
        }
        this.isFullMode = isFullMode
        if (isFullMode) {
            // 转换时间到完整模式下
            convertTimeToFullMode()
        } else {
            if (isPlaying) {
                playInFullModeHandler.removeCallbacksAndMessages(null)
                // 转换时间到正常模式下
                convertTimeToNormalMode()
                preparePlay()
            } else {
                // 转换时间到正常模式下
                convertTimeToNormalMode()
            }
        }
    }

    /**
     * 获取当前时间指示器指向的时间项
     */
    fun getCurrentTimeItem(): TimeItem? {
        val timeItemIndex = getCurrentTimeIndex()
        return if (timeItemIndex != -1) timeItemList[timeItemIndex].timeItem else null
    }

    /**
     * 删除当前选中的时间项
     */
    fun delete() {
        if (isPlaying || isFullMode) {
            return
        }
        val index = getTimePosition() / segmentWidth
        if (index > timeItemList.size - 1) {
            return
        }
        if (isOperating) {
            Log.e(tag, "Current is operating, ignored to delete.")
            return
        }
        isOperating = true
        val deleteCircleIndicator = timeItemList[index]

        checkRangeIndicatorWithDelete(deleteCircleIndicator)

        val moveCircleIndicator = ArrayList<CircleIndicator>()
        for (i in index + 1 until timeItemList.size) {
            val indicatorPos = timeItemList[i].timePosition - currentLeft
            // 只移动屏幕可见的时间项, 避免性能浪费
            if (indicatorPos > -segmentWidth && indicatorPos < screenWidth + segmentWidth) {
                moveCircleIndicator.add(timeItemList[i])
            } else {
                timeItemList[i].timePosition -= segmentWidth
            }
        }

        moveCircleIndicator.forEach {
            it.moveToPrevious(true)
        }

        deleteCircleIndicator.delete(object : CircleIndicator.OnDeleteCallback {
            override fun onDeleted() {
                timeItemList.remove(deleteCircleIndicator)
                isOperating = false
                refreshRangeRectF()
                timeItemListener?.onDelete()
                if (timeItemList.isNotEmpty() && index < timeItemList.size) {
                    timeItemListener?.onSelectedStateChanged(true, timeItemList[index].timeItem)
                } else {
                    timeItemListener?.onSelectedStateChanged(false, null)
                }
                timeItemListener?.onSizeChanged(timeItemList.size)
            }
        })
    }

    /**
     * 播放时间轴
     * @param isRepeat 是否重复播放
     */
    fun play(isRepeat: Boolean) {
        if (timeItemList.isEmpty()) {
            Log.e(tag, "There has no time item to play")
            return
        }
        if (isPlaying) {
            Log.e(tag, "Current is playing, cannot play again.")
            return
        }
        // 判断动画是否可用, 不可用时, 播放时间轴不使用动画
        isAnimatorEnable = AnimatorUtil.checkAndSetAnimatorEnable()
        isRepeatPlay = isRepeat
        isPlaying = true
        isCalledStart = false
        if (!isFullMode) {
            preparePlay()
        } else {
            playInFullMode()
        }
    }

    /**
     * 停止播放时间轴
     */
    fun stopPlay() {
        if (!isPlaying) {
            return
        }
        isPlaying = false
        isRepeatPlay = false
        isNeedToPlay = false
        isCalledStart = false
        if (!isFullMode) {
            if (isAnimatorEnable) {
                playAnimator.cancel()
            } else {
                playHandler.removeCallbacksAndMessages(null)
                timeIndicator.isVisible = true
                playIndicator.isVisible = false
                invalidate()
            }
        } else {
            stopPlayInFullMode()
        }
    }

    /**
     * 重置时间轴
     */
    fun reset() {
        if (isPlaying) {
            return
        }
        if (isFling) {
            scroller.forceFinished(true)
        }
        timeItemList.clear()
        currentLeft = -segmentStartOffset
        timeIndicator.updatePosition(0f)
        timeIndicator.timePosition = 0
        isOperating = false
        rangeRectF = RectF()
        if (rangeIndicator.isIndicatorAVisible()) {
            rangeIndicator.reset()
        }
        computeTime()

    }

    fun print() {
        timeItemList.forEach {
            Log.e(tag, "print: value:${it.value}, timePosition:${it.timePosition}")
        }
    }

    fun getTimePosition(): Int {
        return timeIndicator.timePosition
    }

    fun isFull(): Boolean {
        return timeItemList.size == totalSegmentCount + 1
    }

    fun isFullMode(): Boolean {
        return isFullMode
    }

    fun getSegmentStartOffset(): Float {
        return segmentStartOffset
    }

    fun getSegmentEndOffset(): Float {
        return segmentEndOffset
    }

    fun getTotalSegmentCount(): Int {
        return totalSegmentCount
    }

    fun getMillisecondStartTop(): Float {
        return millisecondStartTop
    }

    fun getSegmentCountOfSecond(): Int {
        return segmentCountOfSecond
    }

    fun getCircleIndicatorGap(): Int {
        return circleIndicatorGap
    }

    fun getScreenWidth(): Int {
        return screenWidth
    }

    fun setRangATime() {
        if (isPlaying) {
            return
        }
        if (!isFullMode) {
            val timeIndex = getTimeIndex()
            // 指示器当前时间没有设置时间项, 则不能添加, 必须在设置的时间项范围内添加
            if (timeItemList.size - 1 < timeIndex) {
                return
            }
            rangeIndicator.setATime(getTimePosition().toFloat())
            rangeIndicatorListener?.onSetA()
        } else {
            // 时间不能超过时间项的最大索引
            val timeItemsMaxTime =
                (((timeItemList[timeItemList.size - 1].timePosition / segmentWidth)
                        * segmentTimeInterval) * 1000) / timeOfPerPixel
            if (timeIndicator.timePositionInFullMode > timeItemsMaxTime) {
                return
            }
            rangeIndicator.setATime(timeIndicator.timePositionInFullMode)
            rangeIndicatorListener?.onSetA()
        }
    }

    fun setRangeBTime() {
        if (isPlaying) {
            return
        }
        if (!isFullMode) {
            val timeIndex = getTimeIndex()
            // 指示器当前时间没有设置时间项, 则不能添加, 必须在设置的时间项范围内添加
            if (timeItemList.size - 1 < timeIndex) {
                return
            }
            rangeIndicator.setBTime(getTimePosition().toFloat())
            rangeIndicatorListener?.onSetB()
        } else {
            // 时间不能超过时间项的最大索引
            val timeItemsMaxTime =
                (((timeItemList[timeItemList.size - 1].timePosition / segmentWidth)
                        * segmentTimeInterval) * 1000) / timeOfPerPixel
            if (timeIndicator.timePositionInFullMode > timeItemsMaxTime) {
                return
            }
            rangeIndicator.setBTime(timeIndicator.timePositionInFullMode)
            rangeIndicatorListener?.onSetB()
        }
    }

    /**
     * 重置时间段选择
     */
    fun resetTimeRange() {
        if (isPlaying) {
            return
        }
        rangeIndicator.reset()
        rangeIndicatorListener?.onReset()
    }

    private fun getTimeIndex(): Int {
        return getTimePosition() / segmentWidth
    }

    fun getTimeItemList(): ArrayList<CircleIndicator> {
        return timeItemList
    }

    fun getSegmentMaxWidth(): Int {
        return segmentMaxWidth
    }

    fun getSecondSegmentRadius(): Float {
        return secondSegmentRadius
    }

    fun isPlaying(): Boolean {
        return isPlaying
    }

    fun getTimeOfPerPixel(): Float {
        return timeOfPerPixel
    }

    fun getTimeItems(): ArrayList<TimeItem> {
        val timeItemList = ArrayList<TimeItem>()
        this.timeItemList.forEach {
            timeItemList.add(it.timeItem)
        }
        return timeItemList
    }

    fun getSegmentTimeInterval(): Float {
        return segmentTimeInterval
    }

    /**
     * 检查是否在触控范围内, 触控范围为最小高度, 避免触摸时间轴之外也可以响应触摸事件
     */
    private fun checkIfInTouchArea(y: Float): Boolean {
        return (y <= minHeight && y >= paddingTop)
    }

    /**
     * 时间项状态监听
     */
    interface TimeItemStateListener {
        /**
         * 新增时间项
         */
        fun onAdd()

        /**
         * 删除时间项
         */
        fun onDelete()

        /**
         * 时间项选中状态变化
         * @param isSelected 是否有选中时间项
         * @param selectedTimeItem 选中的时间项, isSelected=false时, 为空
         */
        fun onSelectedStateChanged(isSelected: Boolean, selectedTimeItem: TimeItem?)

        /**
         * 时间项数量变动
         */
        fun onSizeChanged(count: Int)
    }

    interface TimeItemClickListener {
        fun onClick(index: Int, timeItem: TimeItem)
    }

    /**
     * 播放监听
     */
    interface PlayListener {
        fun onStart()
        fun onStop()
        fun onTimeChanged(time: Long, timeItem: TimeItem)
    }

    /**
     * 时间变化监听
     */
    interface TimeListener {
        fun onTimeChanged(time: Long, timeItem: TimeItem?)
    }

    interface RangeIndicatorListener {
        fun onSetA()
        fun onReset()
        fun onSetB()
    }

}