package com.example.application.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.application.R
import com.example.application.data.local.entity.CategoryEntity

class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var items: List<CategoryEntity> = emptyList()

    fun submitList(newItems: List<CategoryEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emojiText: TextView = itemView.findViewById(R.id.emojiText)
        private val nameText: TextView = itemView.findViewById(R.id.nameText)

        fun bind(category: CategoryEntity) {
            emojiText.text = category.emoji
            nameText.text = category.name
        }
    }
}