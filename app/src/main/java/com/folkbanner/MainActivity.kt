package com.folkbanner

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.folkbanner.databinding.ActivityMainBinding
import com.folkbanner.ui.viewmodel.MainViewModel
import com.folkbanner.utils.AppLogger
import com.folkbanner.utils.DownloadManager
import com.folkbanner.utils.SettingsManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var imageLoader: ImageLoader
    private lateinit var settingsManager: SettingsManager
    private var lastApiMode = true

    private lateinit var logContainer: ScrollView
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager.getInstance(this)
        lastApiMode = settingsManager.useUpstreamApi
        AppLogger.debugMode = settingsManager.debugMode
        
        logContainer = findViewById(R.id.logContainer)
        tvLog = findViewById(R.id.tvLog)
        
        initImageLoader()
        initViews()
        observeData()
    }

    private fun initImageLoader() {
        imageLoader = ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    private fun initViews() {
        binding.fabRefresh.setOnClickListener { runWithNetwork { viewModel.refreshWallpaper() } }
        binding.fabDownload.setOnClickListener { runWithNetwork { downloadCurrentWallpaper() } }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnRetry.setOnClickListener { viewModel.loadApis() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { viewModel.loadRandomWallpaper(it) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun runWithNetwork(action: () -> Unit) {
        if (isNetworkAvailable()) action() else showNetworkError()
    }

    private fun showNetworkError() {
        Snackbar.make(binding.root, getString(R.string.no_network), Snackbar.LENGTH_SHORT).show()
    }

    private fun observeData() {
        viewModel.apis.observe(this) { apis ->
            binding.tabLayout.removeAllTabs()
            apis.forEach { binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it.name)) }
        }

        viewModel.currentWallpaper.observe(this) { item ->
            item?.let {
                if (it.bitmap != null) {
                    binding.imageView.setImageBitmap(it.bitmap)
                } else {
                    loadImage(it.url)
                }
                binding.fabDownload.show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.errorLayout.visibility = View.GONE
            binding.fabRefresh.isEnabled = !isLoading
            binding.fabDownload.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { error ->
            val hasError = error != null
            binding.errorLayout.visibility = if (hasError) View.VISIBLE else View.GONE
            binding.imageView.visibility = if (hasError) View.GONE else View.VISIBLE
        }

        AppLogger.logs.observe(this) { logs ->
            tvLog.text = logs
            logContainer.post { logContainer.fullScroll(View.FOCUS_DOWN) }
            updateLogVisibility(logs.isNotEmpty())
        }

        AppLogger.toast.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                AppLogger.clearToast()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentApiMode = settingsManager.useUpstreamApi
        AppLogger.debugMode = settingsManager.debugMode
        updateLogVisibility()
        
        if (currentApiMode != lastApiMode) {
            lastApiMode = currentApiMode
            viewModel.loadApis()
        }
        binding.tabLayout.visibility = if (currentApiMode) View.VISIBLE else View.GONE
    }

    private fun updateLogVisibility(hasLogs: Boolean = true) {
        val showLog = settingsManager.debugMode && !settingsManager.useUpstreamApi && hasLogs
        logContainer.visibility = if (showLog) View.VISIBLE else View.GONE
    }

    private fun loadImage(url: String) {
        imageLoader.enqueue(
            ImageRequest.Builder(this)
                .data(url)
                .target(binding.imageView)
                .crossfade(true)
                .build()
        )
    }

    private fun downloadCurrentWallpaper() {
        viewModel.currentWallpaper.value?.let { downloadWallpaper(it.url) }
    }

    private fun downloadWallpaper(url: String) {
        binding.fabDownload.isEnabled = false
        Snackbar.make(binding.root, getString(R.string.downloading), Snackbar.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val bitmap = viewModel.currentWallpaper.value?.bitmap
            val success = if (bitmap != null) {
                DownloadManager.saveBitmap(this@MainActivity, bitmap)
            } else {
                DownloadManager.downloadImage(this@MainActivity, url, imageLoader)
            }

            val messageRes = if (success) R.string.download_success else R.string.download_failed
            Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
            binding.fabDownload.isEnabled = true
        }
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        dialogView.findViewById<View>(R.id.btnGithub).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url))))
        }

        dialogView.findViewById<ImageView>(R.id.ivAvatar)?.let { avatarView ->
            imageLoader.enqueue(
                ImageRequest.Builder(this)
                    .data("https://q.qlogo.cn/headimg_dl?dst_uin=3231515355&spec=640&img_type=jpg")
                    .target(avatarView)
                    .build()
            )
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
