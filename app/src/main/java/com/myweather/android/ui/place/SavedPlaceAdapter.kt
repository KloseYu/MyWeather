package com.myweather.android.ui.place

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myweather.android.databinding.SavedPlaceItemBinding
import com.myweather.android.logic.model.Place

// 已保存城市列表适配器，支持点击切换/删除，当前城市加粗+●标识
class SavedPlaceAdapter(
    private val savedPlaceList: List<Place>,
    private val currentLng: String,
    private val currentLat: String,
    private val isInDrawer: Boolean,
    private val showDeleteButton: Boolean,
    private val onPlaceClick: (Place) -> Unit,
    private val onPlaceDelete: (Place) -> Unit
) : RecyclerView.Adapter<SavedPlaceAdapter.ViewHolder>() {

    private val nameColor = if (isInDrawer) Color.WHITE else Color.BLACK
    private val addressColor = if (isInDrawer) Color.parseColor("#BDBDBD") else Color.parseColor("#757575")

    inner class ViewHolder(val binding: SavedPlaceItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SavedPlaceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)
        holder.binding.root.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPlaceClick(savedPlaceList[position])
            }
        }
        holder.binding.deleteBtn.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onPlaceDelete(savedPlaceList[position])
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = savedPlaceList[position]
        val isCurrent = place.location.lng == currentLng && place.location.lat == currentLat
        holder.binding.placeName.text = if (isCurrent) "● ${place.name}" else place.name
        holder.binding.placeName.setTypeface(
            holder.binding.placeName.typeface,
            if (isCurrent) Typeface.BOLD else Typeface.NORMAL
        )
        holder.binding.placeName.setTextColor(nameColor)
        holder.binding.placeAddress.text = place.address
        holder.binding.placeAddress.setTextColor(addressColor)
        holder.binding.deleteBtn.visibility = if (showDeleteButton) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = savedPlaceList.size
}