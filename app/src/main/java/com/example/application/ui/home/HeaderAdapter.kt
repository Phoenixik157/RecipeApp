package com.example.application.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HeaderAdapter(private val headerView: View) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        // ВАЖНО: отсоединяем view от предыдущего родителя, чтобы RecyclerView не упал
        (headerView.parent as? ViewGroup)?.removeView(headerView)
        return HeaderViewHolder(headerView)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {}
    override fun getItemCount(): Int = 1
}