package com.cxj.timeruler.util

import android.animation.ValueAnimator
import android.os.Build
import java.lang.Exception

/**
 *  author : chenxiaojin
 *  date : 2021/5/21 上午 11:16
 *  description :
 *  动画工具
 */
object AnimatorUtil {
    /**
     * 9.0以下系统通过反射重置动画, 重置成功返回true, 失败返回false
     * 重置成1倍速, 否则时间轴计算会有问题
     */
    private fun resetAnimatorDurationScale(): Boolean {
        return try {
            val field = ValueAnimator::class.java.getDeclaredField("sDurationScale")
            field.isAccessible = true
            field.setFloat(null, 1f)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 动画是否开启
     * 由于时间轴与动画倍数息息相关, 动画倍数不是1倍的话, 都默认成不支持动画
     * 后续由反射进行重置
     */
    private fun isAnimatorEnable(): Boolean {
        return try {
            val field = ValueAnimator::class.java.getDeclaredField("sDurationScale")
            field.isAccessible = true
            val value = field.getFloat(null)
            if (value < 1.0f || value > 1.0f) {
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查动画是否开启
     * 没有开启且系统是8.0以下的, 通过反射开启动画并重置倍数为1倍
     */
    fun checkAndSetAnimatorEnable(): Boolean {
        var isAnimatorEnable: Boolean
        // Android 8 以上, 调用原生API判断是否关闭动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            isAnimatorEnable = ValueAnimator.areAnimatorsEnabled()
        } else {
            // 使用反射判断动画是否关闭
            isAnimatorEnable = isAnimatorEnable()
            // 如果动画关闭, 使用反射开启
            if (!isAnimatorEnable) {
                // 反射开启并返回开启结果
                isAnimatorEnable = resetAnimatorDurationScale()
            }
        }
        return isAnimatorEnable
    }
}