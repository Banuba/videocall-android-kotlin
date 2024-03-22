package com.banuba.sdk.example.videocall.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.banuba.sdk.example.videocall.databinding.EffectItemBinding
import com.banuba.sdk.manager.EffectInfo

class EffectsListAdapter(
    private val screenWidth: Int,
    private val elementWidth: Int,
    private val itemSelectedCallback: (EffectInfo?, Int) -> Unit
) : ListAdapter<EffectInfo, EffectsListAdapter.ItemViewHolder>(DiffCallback())  {

    private var currentEffectIndex = -1

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
            selectedBackground.visibility(position == currentEffectIndex)
            image.setImageBitmap(item.previewImage())

            root.setOnClickListener {
                if (currentEffectIndex == position) {
                    currentEffectIndex = -1
                    itemSelectedCallback(null, position)
                } else {
                    val oldPosition = currentEffectIndex
                    currentEffectIndex = position
                    notifyItemChanged(oldPosition)
                    itemSelectedCallback(item, position)
                }
                notifyItemChanged(position)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<EffectInfo>() {
        override fun areItemsTheSame(old: EffectInfo, new: EffectInfo): Boolean = old.path == new.path

        override fun areContentsTheSame(old: EffectInfo, new: EffectInfo): Boolean = old.equals(new)
    }
}

fun View.visibility(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}
