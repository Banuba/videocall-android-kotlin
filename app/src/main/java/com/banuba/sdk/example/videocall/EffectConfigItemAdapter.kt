package com.banuba.sdk.example.videocall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.banuba.sdk.example.videocall.databinding.EffectItemBinding
import com.banuba.sdk.manager.EffectInfo

class EffectConfigItemAdapter(
    private val mScreenWidth: Int,
    private val mElementWidth: Int,
    private val mItemSelectedCallback: (EffectInfo?, Int) -> Unit
) : ListAdapter<EffectInfo, EffectConfigItemAdapter.ItemViewHolder>(DiffCallback())  {

    private var mCurrentEffectIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder
            = ItemViewHolder(EffectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val central = (mScreenWidth - mElementWidth) / 2
        val marginLeft = if (position == 0) central else 12
        val marginRight = if (position == itemCount - 1) central else 12

        (holder.itemView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(marginLeft, 0, marginRight, 0)

        holder.bind(getItem(position), position)
    }

    inner class ItemViewHolder(private val binding: EffectItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EffectInfo, position: Int) = with(binding) {
            if (position == mCurrentEffectIndex) {
                selectedBackground.visible()
            } else {
                selectedBackground.invisible()
            }

            image.setImageBitmap(item.previewImage())

            root.setOnClickListener {
                if (mCurrentEffectIndex == position) {
                    mCurrentEffectIndex = -1
                    mItemSelectedCallback(null, position)
                } else {
                    val oldPosition = mCurrentEffectIndex
                    mCurrentEffectIndex = position
                    notifyItemChanged(oldPosition)
                    mItemSelectedCallback(item, position)
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

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}
