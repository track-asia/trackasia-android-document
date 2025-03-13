package com.trackasia.sample.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager(context: Context, attrs: AttributeSet) : ViewPager(context, attrs) {
    private var scrollEnabled = false

    override fun onTouchEvent(e: MotionEvent): Boolean {
        return if (scrollEnabled) {
            super.onTouchEvent(e)
        } else {
            false
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        return if (scrollEnabled) {
            super.onInterceptTouchEvent(e)
        } else {
            false
        }
    }

    fun setPagingEnabled(enabled: Boolean) {
        scrollEnabled = enabled
    }
}
