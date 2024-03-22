package com.banuba.sdk.example.videocall

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.banuba.sdk.example.videocall.databinding.EffectItemBinding
import com.banuba.sdk.manager.EffectInfo

class CustomEffectsListAdapter(
    private val screenWidth: Int,
    private val elementWidth: Int,
    private val itemSelectedCallback: (String, Int) -> Unit
) : ListAdapter<EffectInfo, CustomEffectsListAdapter.ItemViewHolder>(DiffCallback())  {

    private var current = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder
            = ItemViewHolder(EffectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val central = (screenWidth - elementWidth) / 2
        val marginLeft = if (position == 0) central else 12
        val marginRight = if (position == itemCount - 1) central else 12

        (holder.itemView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(marginLeft, 0, marginRight, 0)

        holder.bind(getItem(position), position)
    }

    inner class ItemViewHolder(private val binding: EffectItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EffectInfo, position: Int) = with(binding) {
            selectedBackground.visibility(position == current)
            image.setImageBitmap(item.previewImage())

            root.setOnClickListener {
                val oldPosition = current
                val effectPath = if (current == position) "" else item.path
                current = if (current == position) -1 else position
                notifyItemChanged(position)
                notifyItemChanged(oldPosition)
                itemSelectedCallback(effectPath, position)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<EffectInfo>() {
        override fun areItemsTheSame(old: EffectInfo, new: EffectInfo): Boolean = old.path == new.path

        override fun areContentsTheSame(old: EffectInfo, new: EffectInfo): Boolean = old.equals(new)
    }

    class CenterLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

        override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
            val centerSmoothScroller = CenterSmoothScroller(recyclerView.context)
            centerSmoothScroller.targetPosition = position
            startSmoothScroll(centerSmoothScroller)
        }

        private inner class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
            override fun calculateDtToFit( viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int
                    = (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float
                    = 150f / displayMetrics.densityDpi
        }
    }
}

fun View.visibility(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}
