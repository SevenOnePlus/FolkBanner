package com.folkbanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.folkbanner.databinding.ActivitySettingsBinding
import com.folkbanner.utils.SettingsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager.getInstance(this)
        
        initViews()
        loadSettings()
    }

    private fun initViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.optionUpstream.setOnClickListener {
            settingsManager.useUpstreamApi = true
            updateRadioButtons()
        }

        binding.optionDownstream.setOnClickListener {
            settingsManager.useUpstreamApi = false
            updateRadioButtons()
        }
    }

    private fun loadSettings() {
        updateRadioButtons()
    }

    private fun updateRadioButtons() {
        val useUpstream = settingsManager.useUpstreamApi
        binding.radioUpstream.isChecked = useUpstream
        binding.radioDownstream.isChecked = !useUpstream
    }

}
