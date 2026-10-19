package com.example.application.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.application.R
import com.example.application.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.exportToJson(uri) { success, msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Тёмная тема (пока заглушка)
        view.findViewById<Switch>(R.id.switchDarkTheme).isChecked = true

        // Экспорт
        view.findViewById<android.widget.LinearLayout>(R.id.rowExport).setOnClickListener {
            exportLauncher.launch("recipe_backup.json")
        }

        // Импорт (пока заглушка)
        view.findViewById<android.widget.LinearLayout>(R.id.rowImport).setOnClickListener {
            Toast.makeText(requireContext(), "Импорт скоро будет", Toast.LENGTH_SHORT).show()
        }

        // Очистка (пока заглушка)
        view.findViewById<android.widget.LinearLayout>(R.id.rowClear).setOnClickListener {
            Toast.makeText(requireContext(), "Очистка скоро будет", Toast.LENGTH_SHORT).show()
        }
    }
}