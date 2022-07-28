package com.cxj.timeruler.bean

/**
 *  author : chenxiaojin
 *  date : 2021/5/15 下午 06:38
 *  description : 时间轴的时间项
 */
data class TimeItem(
    // 标识
    val flag: Int = -1,
    // 背景颜色
    val backgroundColor: Int,
    // 数据对象
    val data: Any?
) {
    // 数据对象为空表示为空时间项
    fun isEmpty(): Boolean {
        return data == null
    }
}

