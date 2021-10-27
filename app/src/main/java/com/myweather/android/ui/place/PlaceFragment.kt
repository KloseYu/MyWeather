package com.myweather.android.ui.place

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myweather.android.MainActivity
import com.myweather.android.R
import com.myweather.android.ui.weather.WeatherActivity


/**
 * 文件名: PlaceFragment
 * 作者: KloseYu
 * 日期: 2021/10/25
 * 描述: PlaceFragment
 */
class PlaceFragment : Fragment() {


    val viewModel by lazy { ViewModelProvider(this).get(PlaceViewModel::class.java) }

    private lateinit var adapter: PlaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.place_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // 必须是从主界面跳转过去
        if(activity is MainActivity && viewModel.isPlaceSaved()){
            val place = viewModel.getSavedPlace()
            val intent = Intent(context, WeatherActivity::class.java).apply {
                putExtra("location_lng",place.location.lng)
                putExtra("location_lat",place.location.lat)
                putExtra("place_name",place.name)
            }
            startActivity(intent)
            activity?.finish()
            return
        }

        val layoutManager = LinearLayoutManager(activity)
        val recyclerView:RecyclerView = view!!.findViewById(R.id.recyclerView)
        val bgImgView:ImageView = view!!.findViewById(R.id.bgImgView)
        val searchPlaceEdit:EditText = view!!.findViewById(R.id.searchPlaceEdit)
        recyclerView.layoutManager =layoutManager
        adapter =PlaceAdapter(this,viewModel.placeList)
        recyclerView.adapter = adapter
        searchPlaceEdit.addTextChangedListener {
            val content =it.toString()
            if (content.isNotEmpty()){
                viewModel.searchPlaces(content)
            }else{
                recyclerView.visibility =View.GONE
                bgImgView.visibility = View.VISIBLE
                viewModel.placeList.clear()
                adapter.notifyDataSetChanged()
            }
        }

        viewModel.placeLiveData.observe(viewLifecycleOwner, Observer {
            val places =it.getOrNull()
            Log.e("PlaceFragment", "onActivityCreated: $places")
            if(places!=null){
                recyclerView.visibility =View.VISIBLE
                bgImgView.visibility =View.GONE
                viewModel.placeList.clear()
                viewModel.placeList.addAll(places)
                adapter.notifyDataSetChanged()
            }else{
                Toast.makeText(activity,"未能查询到任何地点",Toast.LENGTH_SHORT).show()
                it.exceptionOrNull()?.printStackTrace()
            }
        })
    }

}