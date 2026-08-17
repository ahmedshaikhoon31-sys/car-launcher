package com.zarwa.launcher.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zarwa.launcher.databinding.ItemHourBinding

class HourAdapter(private val items: List<HourWeather>) :
    RecyclerView.Adapter<HourAdapter.VH>() {

    inner class VH(val b: ItemHourBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHourBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = items[position]
        holder.b.hourLabel.text = WeatherRepo.hourLabel(holder.itemView.context, h)
        holder.b.hourTemp.text = "${h.tempC}°"
        holder.b.hourIcon.setImageResource(h.iconRes)
    }

    override fun getItemCount() = items.size
}
