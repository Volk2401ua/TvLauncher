package com.example.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val large: Boolean,
    private val accent: Int,
    var animations: Boolean,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private val items = mutableListOf<AppInfo>()
    private val focusScale = 1.04f
    private val focusDuration = 120L

    fun submit(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (large) R.layout.item_app_large else R.layout.item_app
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        v.background = UiHelpers.tile(v.context, accent)
        v.setOnFocusChangeListener { view, hasFocus ->
            if (animations) {
                view.animate()
                    .scaleX(if (hasFocus) focusScale else 1f)
                    .scaleY(if (hasFocus) focusScale else 1f)
                    .setDuration(focusDuration)
                    .start()
            } else {
                view.scaleX = 1f
                view.scaleY = 1f
            }
        }
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.itemView.scaleX = if (animations && holder.itemView.isFocused) focusScale else 1f
        holder.itemView.scaleY = if (animations && holder.itemView.isFocused) focusScale else 1f
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener { onLongClick(app); true }
    }

    override fun getItemCount(): Int = items.size
}