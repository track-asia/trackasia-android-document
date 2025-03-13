package com.trackasia.sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.trackasia.sample.R
import com.trackasia.sample.RouterAddressModel

class AddressRouterAdapter(
    private val data: MutableList<RouterAddressModel>,
) :
    RecyclerView.Adapter<AddressRouterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textPosition: TextView = view.findViewById(R.id.textPosition)
        val textName: TextView = view.findViewById(R.id.textName)
        val textDistance: TextView = view.findViewById(R.id.textDistance)
//        val rvDirections: RecyclerView = view.findViewById(R.id.rvDirections)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.waypoint_router_address, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.textPosition.text = "P($position):"
        holder.textName.text = item.name
        holder.textDistance.text = item.distance
//        if(item.directionData != null) {
//            val directionsAdapter = DirectionRouterAdapter(item.directionData)
//            holder.rvDirections.layoutManager = LinearLayoutManager(holder.itemView.context)
//            holder.rvDirections.adapter = directionsAdapter
//        }
//        holder.itemView.setOnClickListener {
//            holder.rvDirections.visibility = View.VISIBLE
//        }
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