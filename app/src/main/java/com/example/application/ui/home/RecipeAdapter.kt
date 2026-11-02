package com.example.application.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.application.R
import com.example.application.data.local.entity.RecipeEntity
import com.example.application.viewmodels.RecipeWithMissing

class RecipeAdapter(
    private val onClick: (RecipeEntity) -> Unit,
    private val onLongClick: ((RecipeEntity) -> Unit)? = null
) : ListAdapter<RecipeWithMissing, RecipeAdapter.RecipeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item.recipe, item.missingCount)
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.recipeImage)
        private val title: TextView = itemView.findViewById(R.id.recipeTitle)
        private val time: TextView = itemView.findViewById(R.id.recipeTime)
        private val dot1: ImageView = itemView.findViewById(R.id.dot1)
        private val dot2: ImageView = itemView.findViewById(R.id.dot2)
        private val dot3: ImageView = itemView.findViewById(R.id.dot3)
        private val missing: TextView = itemView.findViewById(R.id.missingChip)

        fun bind(recipe: RecipeEntity, missingCount: Int) {
            title.text = recipe.title
            time.text = "${recipe.cookingTimeMinutes} мин"

            if (!recipe.imageUrl.isNullOrEmpty()) {
                image.load(recipe.imageUrl)
            } else {
                image.setImageDrawable(null)
            }

            val dots = listOf(dot1, dot2, dot3)
            dots.forEachIndexed { i, dot ->
                val colorRes = if (i < recipe.complexity) {
                    R.color.purple_primary
                } else {
                    R.color.surface_dark // Неактивные звезды будут темными (или можно взять text_secondary)
                }
                // Красим саму иконку звезды
                dot.setColorFilter(androidx.core.content.ContextCompat.getColor(itemView.context, colorRes))
            }

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

    // Класс, который говорит адаптеру, какие элементы изменились
    class DiffCallback : DiffUtil.ItemCallback<RecipeWithMissing>() {
        override fun areItemsTheSame(oldItem: RecipeWithMissing, newItem: RecipeWithMissing): Boolean {
            return oldItem.recipe.id == newItem.recipe.id
        }
        override fun areContentsTheSame(oldItem: RecipeWithMissing, newItem: RecipeWithMissing): Boolean {
            return oldItem == newItem
        }
    }
}