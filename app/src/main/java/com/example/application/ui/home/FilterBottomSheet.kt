package com.example.application.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import com.example.application.R
import com.example.application.viewmodels.HomeViewModel
import com.example.application.viewmodels.SortType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet : BottomSheetDialogFragment() {

    var onApply: ((SortType, Boolean) -> Unit)? = null

    // Передаем текущие настройки
    var currentSort: SortType = SortType.NAME
    var currentMinMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_filters, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<android.widget.RadioGroup>(R.id.sortRadioGroup)
        val switch = view.findViewById<Switch>(R.id.switchMinPurchases)

        // Устанавливаем текущие значения
        switch.isChecked = currentMinMode
        when (currentSort) {
            SortType.NAME -> radioGroup.check(R.id.radioName)
            SortType.TIME -> radioGroup.check(R.id.radioTime)
            SortType.COMPLEXITY -> radioGroup.check(R.id.radioComplexity)
        }

        view.findViewById<TextView>(R.id.applyBtn).setOnClickListener {
            val newSort = when (radioGroup.checkedRadioButtonId) {
                R.id.radioTime -> SortType.TIME
                R.id.radioComplexity -> SortType.COMPLEXITY
                else -> SortType.NAME
            }
            onApply?.invoke(newSort, switch.isChecked)
            dismiss()
        }
    }
}