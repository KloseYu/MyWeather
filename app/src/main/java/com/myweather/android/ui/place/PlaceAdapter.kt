package com.myweather.android.ui.place

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myweather.android.R
import com.myweather.android.logic.model.PlaceResponse

/**
 * 文件名: PlaceAdapter
 * 作者: KloseYu
 * 日期: 2021/10/25
 * 描述: place适配器
 */
class PlaceAdapter(private val fragment: PlaceFragment,private val placeList: List<PlaceResponse.Place>) :
    RecyclerView.Adapter<PlaceAdapter.ViewHolder>(){

        inner class ViewHolder(view: View) :RecyclerView.ViewHolder(view){
            val placeName:TextView = view.findViewById(R.id.placeName)
            val placeAddress:TextView = view.findViewById(R.id.placeAddress)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.place_item,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val place = placeList[position]
        holder.placeName.text = place.name
        holder.placeAddress.text = place.address
    }

    override fun getItemCount(): Int {
        return placeList.size
    }

}