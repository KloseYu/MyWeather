package com.myweather.android.ui.place

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.myweather.android.R
import com.myweather.android.databinding.SavedPlaceItemBinding
import com.myweather.android.logic.model.Place

// 已保存城市列表适配器，支持点击切换/删除，当前城市加粗+●标识
class SavedPlaceAdapter(
    private val savedPlaceList: List<Place>,
    private val currentIndex: Int,
    private val isInDrawer: Boolean,
    private val showDeleteButton: Boolean,
    private val onPlaceClick: (Int) -> Unit,
    private val onPlaceDelete: (Int) -> Unit
) : RecyclerView.Adapter<SavedPlaceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: SavedPlaceItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SavedPlaceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        holder.binding.root.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPlaceClick(position)
            }
        }
        holder.binding.deleteBtn.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPlaceDelete(position)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = savedPlaceList[position]
        val context = holder.binding.root.context
        // 城市名用主题里的正文色（浅色模式深、深色模式浅），
        // 地址用主题里的次要文字色，两者都随昼夜自动切换，
        // 不再写死颜色，避免浅色模式下浅灰字压浅底看不清
        val nameColor = ContextCompat.getColor(context, R.color.colorForecastText)
        val addressColor = ContextCompat.getColor(context, R.color.colorSavedLabel)
        // 用列表索引判定当前城市，与 ViewPager 页面索引天然一一对应
        val isCurrent = position == currentIndex
        holder.binding.placeName.text = if (isCurrent) "● ${place.name}" else place.name
        holder.binding.placeName.setTypeface(
            holder.binding.placeName.typeface,
            if (isCurrent) Typeface.BOLD else Typeface.NORMAL
        )
        holder.binding.placeName.setTextColor(nameColor)
        // 地址与城市名相同时不重复显示：名字已是"省+市+区"的完整层级，再显示一遍只是噪音
        val address = place.address.trim()
        if (address.isEmpty() || address == place.name.trim()) {
            holder.binding.placeAddress.visibility = View.GONE
        } else {
            holder.binding.placeAddress.visibility = View.VISIBLE
            holder.binding.placeAddress.text = address
            holder.binding.placeAddress.setTextColor(addressColor)
        }
        holder.binding.deleteBtn.visibility = if (showDeleteButton) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = savedPlaceList.size
}