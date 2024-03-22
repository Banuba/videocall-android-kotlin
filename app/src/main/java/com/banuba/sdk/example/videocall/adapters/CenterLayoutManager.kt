package com.banuba.sdk.example.videocall.adapters

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class CenterLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val centerSmoothScroller = CenterSmoothScroller(recyclerView.context)
        centerSmoothScroller.targetPosition = position
        startSmoothScroll(centerSmoothScroller)
    }

    private class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
        override fun calculateDtToFit( viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int
                = (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float
                = MILLISECONDS_PER_INCH / displayMetrics.densityDpi
    }

    companion object {
        // This number controls the speed of smooth scroll
        private const val MILLISECONDS_PER_INCH = 150f
    }
}
