package com.example.application.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.application.R
import com.example.application.data.local.entity.RecipeEntity

class RecipeAdapter(
    private val onClick: (RecipeEntity) -> Unit,
    private val onLongClick: ((RecipeEntity) -> Unit)? = null
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private var items: List<com.example.application.viewmodels.RecipeWithMissing> = emptyList()

    fun submitList(newItems: List<com.example.application.viewmodels.RecipeWithMissing>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item.recipe, item.missingCount)
    }

    override fun getItemCount(): Int = items.size

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.recipeImage)
        private val title: TextView = itemView.findViewById(R.id.recipeTitle)
        private val time: TextView = itemView.findViewById(R.id.recipeTime)
        private val dot1: View = itemView.findViewById(R.id.dot1)
        private val dot2: View = itemView.findViewById(R.id.dot2)
        private val dot3: View = itemView.findViewById(R.id.dot3)
        private val missing: TextView = itemView.findViewById(R.id.missingChip)

        fun bind(recipe: RecipeEntity, missingCount: Int) {
            title.text = recipe.title
            time.text = "⏱ ${recipe.cookingTimeMinutes} мин"

            if (!recipe.imageUrl.isNullOrEmpty()) {
                image.load(recipe.imageUrl)
            } else {
                image.setImageDrawable(null)
            }

            val dots = listOf(dot1, dot2, dot3)
            dots.forEachIndexed { i, dot ->
                dot.setBackgroundColor(
                    if (i < recipe.complexity) 0xFFFF7043.toInt() else 0xFF2C2C2C.toInt()
                )
            }

            // Чипс «Не хватает: N ингр.»
            if (missingCount > 0) {
                missing.visibility = View.VISIBLE
                missing.text = "Не хватает: $missingCount ингр."
            } else {
                missing.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(recipe) }
            onLongClick?.let { callback ->
                itemView.setOnLongClickListener {
                    callback(recipe)
                    true
                }
            }
        }
    }
}