package com.zarwa.launcher

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zarwa.launcher.databinding.ItemAppBinding

class AppsAdapter(
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppsAdapter.VH>() {

    private val items = mutableListOf<AppInfo>()

    fun submit(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.b.appIcon.setImageDrawable(app.icon)
        holder.b.appLabel.text = app.label
        holder.b.root.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = items.size
}
