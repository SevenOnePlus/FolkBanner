package com.folkbanner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.folkbanner.databinding.ActivitySettingsBinding
import com.folkbanner.utils.AppLogger
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
        
        binding.switchDebug.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.debugMode = isChecked
            AppLogger.debugMode = isChecked
            if (!isChecked) {
                AppLogger.clear()
            }
        }
    }

    private fun loadSettings() {
        updateRadioButtons()
        binding.switchR18.isChecked = settingsManager.r18Enabled
        binding.switchDebug.isChecked = settingsManager.debugMode
    }

    private fun updateRadioButtons() {
        val useUpstream = settingsManager.useUpstreamApi
        binding.radioUpstream.isChecked = useUpstream
        binding.radioDownstream.isChecked = !useUpstream
        
        if (useUpstream) {
            hideCardWithAnimation(binding.cardR18)
        } else {
            showCardWithAnimation(binding.cardR18)
        }
    }
    
    private fun showCardWithAnimation(view: View) {
        if (view.isVisible) return
        
        view.animate().cancel()
        view.alpha = 0f
        view.scaleY = 0.8f
        view.visibility = View.VISIBLE
        
        view.animate()
            .alpha(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
            .setListener(null)
            .start()
    }
    
    private fun hideCardWithAnimation(view: View) {
        if (!view.isVisible) return
        
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .scaleY(0.8f)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    view.scaleY = 1f
                }
            })
            .start()
    }

}
