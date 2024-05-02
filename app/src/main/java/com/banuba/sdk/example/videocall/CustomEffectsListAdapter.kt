package com.banuba.sdk.example.videocall

import android.annotation.SuppressLint
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
import com.banuba.sdk.example.videocall.databinding.ItemEffectBinding
import com.banuba.sdk.manager.EffectInfo

// The adapter is used for demonstration purposes to interact with AR effects.
class CustomEffectsListAdapter(
    private val screenWidth: Int,
    private val itemSelectedCallback: (String, Int) -> Unit
) : ListAdapter<EffectInfo, CustomEffectsListAdapter.ItemViewHolder>(DiffCallback()) {

    companion object {
        private const val OFFSET = 12
    }

    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(
            ItemEffectBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val central = (screenWidth - holder.itemView.width) / 2
        val marginLeft = if (position == 0) {
            central
        } else {
            OFFSET
        }
        val marginRight = if (position == itemCount - 1) {
            central
        } else {
            OFFSET
        }

        (holder.itemView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
            marginLeft,
            0,
            marginRight,
            0
        )

        holder.bind(getItem(position), position)
    }

    inner class ItemViewHolder(private val binding: ItemEffectBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(effect: EffectInfo, position: Int) = with(binding) {
            ring.visibility(position == selectedIndex)

            val previewResource = effectPreviews[effect.name]
            if (previewResource == null) {
                image.setImageResource(R.drawable.ic_regular_background)
            } else {
                image.setImageResource(previewResource)
            }

            root.setOnClickListener {
                val oldPosition = selectedIndex
                val effectPath = if (selectedIndex == position) {
                    ""
                } else {
                    effect.path
                }
                selectedIndex = if (selectedIndex == position) {
                    -1
                } else {
                    position
                }
                notifyItemChanged(position)
                notifyItemChanged(oldPosition)
                itemSelectedCallback(effectPath, position)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<EffectInfo>() {
        override fun areItemsTheSame(old: EffectInfo, new: EffectInfo): Boolean =
            old.path == new.path

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(old: EffectInfo, new: EffectInfo): Boolean = old == new
    }

    class CenterLayoutManager(context: Context) : LinearLayoutManager(context, HORIZONTAL, false) {

        override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State,
            position: Int
        ) {
            val centerSmoothScroller = CenterSmoothScroller(recyclerView.context)
            centerSmoothScroller.targetPosition = position
            startSmoothScroll(centerSmoothScroller)
        }

        private inner class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int = (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float =
                150f / displayMetrics.densityDpi
        }
    }

    private fun View.visibility(visible: Boolean) {
        this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }
}
