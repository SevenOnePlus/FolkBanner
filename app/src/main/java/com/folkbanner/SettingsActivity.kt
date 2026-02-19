package com.folkbanner

import android.os.Bundle
import android.view.View
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

        binding.switchR18.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.r18Enabled = isChecked
        }
    }

    private fun loadSettings() {
        updateRadioButtons()
        binding.switchR18.isChecked = settingsManager.r18Enabled
    }

    private fun updateRadioButtons() {
        val useUpstream = settingsManager.useUpstreamApi
        binding.radioUpstream.isChecked = useUpstream
        binding.radioDownstream.isChecked = !useUpstream
        
        if (useUpstream) {
            binding.cardR18.visibility = View.GONE
        } else {
            binding.cardR18.visibility = View.VISIBLE
        }
    }

}
