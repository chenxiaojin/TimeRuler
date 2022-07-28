package com.cxj.custom.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cxj.timeruler.R
import com.cxj.timeruler.TimeRuler
import com.cxj.timeruler.bean.TimeItem
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var num = 1
    private var isFullMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnAdd.setOnClickListener {
            timeRuler.addTimeItem(
                num,
                ContextCompat.getColor(
                    applicationContext,
                    if (num % 2 == 0) R.color.choose_segment_color else R.color.purple_200
                ),
                "$num"
            )
//            timeRuler.addEmptyTimeItem()
            num++
        }
        btnDelete.setOnClickListener {
            timeRuler.delete()
        }
        btnReset.setOnClickListener {
            timeRuler.reset()
        }
        btnPrint.setOnClickListener {
            timeRuler.print()
        }

        btnAddA.setOnClickListener {
            timeRuler.setRangATime()
        }

        btnAddB.setOnClickListener {
            timeRuler.setRangeBTime()
        }

        btnResetTime.setOnClickListener {
            timeRuler.resetTimeRange()
        }
        btnPlay.setOnClickListener {
            timeRuler.play(true)
        }
        btnStopPlay.setOnClickListener {
            timeRuler.stopPlay()
        }

        btnToggleFullMode.setOnClickListener {
            isFullMode = !isFullMode
            timeRuler.setFullMode(isFullMode)
        }

        timeRuler.playListener = object : TimeRuler.PlayListener {
            override fun onStart() {
                Log.e("TAG", "Main onStart")
            }

            override fun onStop() {
                Log.e("TAG", "Main onStop")
            }

            override fun onTimeChanged(time: Long, timeItem: TimeItem) {
                Log.e("TAG", "Main time changed:${time}, timeItem:${timeItem.data}")
            }
        }

        timeRuler.timeItemListener = object : TimeRuler.TimeItemStateListener {
            override fun onAdd() {
                Log.e("TAG", "Main onAdd")
            }

            override fun onDelete() {
                Log.e("TAG", "Main onDelete")
            }

            override fun onSelectedStateChanged(isSelected: Boolean, selectedTimeItem: TimeItem?) {
                Log.e("TAG", "Main onSelectedStateChanged, isSelected:$isSelected")
                if (isSelected) {
                    Log.e("TAG", "Main onSelectedStateChanged, timeItem:${selectedTimeItem?.flag}")
                }
            }

            override fun onSizeChanged(count: Int) {
                Log.e("TAG", "Main onSizeChanged, count:$count")
            }
        }

        timeRuler.timeListener = object : TimeRuler.TimeListener {
            override fun onTimeChanged(time: Long, timeItem: TimeItem?) {
                Log.e("TAG", "Main onTimeChanged, time:$time")
                if (null != timeItem) {
                    Log.e("TAG", "Main onTimeChanged, timeItem:${timeItem.flag}")
                }
            }
        }

        timeRuler.rangeIndicatorListener = object : TimeRuler.RangeIndicatorListener {
            override fun onSetA() {
                Log.e("TAG", "Main onSetA")
            }

            override fun onReset() {
                Log.e("TAG", "Main onReset")
            }

            override fun onSetB() {
                Log.e("TAG", "Main onSetB")
            }
        }

        timeRuler.timeItemClickListener = object : TimeRuler.TimeItemClickListener {
            override fun onClick(index: Int, timeItem: TimeItem) {
                Log.e("TAG", "Main onClick, index:$index, timeItem:${timeItem.flag}")
            }
        }
    }
}