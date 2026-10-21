package com.example.application.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import com.example.application.R
import com.example.application.viewmodels.SortType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet : BottomSheetDialogFragment() {

    var onApply: ((SortType, Boolean, Boolean) -> Unit)? = null
    var currentSort: SortType = SortType.NAME
    var currentMinMode: Boolean = false
    var currentAscending: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.sortRadioGroup)
        val switchMin = view.findViewById<Switch>(R.id.switchMinPurchases)
        val switchDesc = view.findViewById<Switch>(R.id.switchDescending)

        // Устанавливаем текущие значения
        switchMin.isChecked = currentMinMode
        switchDesc.isChecked = !currentAscending // Если по убыванию = true, значит по возрастанию = false

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
            val newMinMode = switchMin.isChecked
            val newAscending = !switchDesc.isChecked

            onApply?.invoke(newSort, newMinMode, newAscending)
            dismiss()
        }
    }
}