package com.trackasia.sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trackasia.sample.R
import com.trackasia.sample.RouterAddressModel
import com.trackasia.sample.utils.MapRouteUtils

class DirectionRouterAdapter(private val data: MutableList<RouterAddressModel>) :
    RecyclerView.Adapter<DirectionRouterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconDrawable: ImageView = view.findViewById(R.id.iconDirection)
        val textDirection: TextView = view.findViewById(R.id.textDirection)
        val textName: TextView = view.findViewById(R.id.textName)
        val textDistance: TextView = view.findViewById(R.id.textDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.street_router_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val mapRouteUtils = MapRouteUtils()
        holder.iconDrawable.setImageResource(mapRouteUtils.getRouterDirection(item.router))
        holder.textDirection.text = mapRouteUtils.getNameDirection(item.router)
        holder.textName.text = item.name
        holder.textDistance.text = (item.distance?.toDouble()?.div(1000)).toString()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(newData: List<RouterAddressModel>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    fun addData(newData: RouterAddressModel) {
        data.add(newData)
        notifyDataSetChanged()
    }

    fun clearData() {
        data.clear()
    }

}