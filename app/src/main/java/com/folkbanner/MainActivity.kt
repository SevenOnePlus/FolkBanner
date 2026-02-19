package com.folkbanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager.getInstance(this)
        initImageLoader()
        initViews()
        observeData()
    }

    private fun initImageLoader() {
        imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
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
        binding.fabRefresh.setOnClickListener {
            viewModel.refreshWallpaper()
        }

        binding.fabDownload.setOnClickListener {
            viewModel.currentWallpaper.value?.let { item ->
                downloadWallpaper(item.url)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    viewModel.loadRandomWallpaper(position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnRetry.setOnClickListener {
            viewModel.loadApis()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun observeData() {
        viewModel.apis.observe(this) { apis ->
            binding.tabLayout.removeAllTabs()
            apis.forEach { api ->
                binding.tabLayout.addTab(binding.tabLayout.newTab().setText(api.name))
            }
        }

        viewModel.currentWallpaper.observe(this) { item ->
            item?.let {
                loadImage(it.url)
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
            if (error != null) {
                binding.errorLayout.visibility = View.VISIBLE
                binding.imageView.visibility = View.GONE
            } else {
                binding.errorLayout.visibility = View.GONE
                binding.imageView.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateTabLayoutVisibility()
    }

    private fun updateTabLayoutVisibility() {
        if (settingsManager.useUpstreamApi) {
            binding.tabLayout.visibility = View.VISIBLE
        } else {
            binding.tabLayout.visibility = View.GONE
        }
    }

    private fun loadImage(url: String) {
        lifecycleScope.launch {
            try {
                val request = ImageRequest.Builder(this@MainActivity)
                    .data(url)
                    .target(binding.imageView)
                    .crossfade(true)
                    .build()

                imageLoader.enqueue(request)
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.load_failed),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadWallpaper(url: String) {
        binding.fabDownload.isEnabled = false
        Snackbar.make(binding.root, getString(R.string.downloading), Snackbar.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val success = DownloadManager.downloadImage(this@MainActivity, url, imageLoader)

            if (success) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.download_success),
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.download_failed),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            binding.fabDownload.isEnabled = true
        }
    }

    private fun showAboutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_about, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.btnGithub).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)))
            startActivity(intent)
        }

        val imageView = dialogView.findViewById<android.widget.ImageView>(R.id.ivAvatar)
        lifecycleScope.launch {
            try {
                val request = ImageRequest.Builder(this@MainActivity)
                    .data("https://q.qlogo.cn/headimg_dl?dst_uin=3231515355&spec=640&img_type=jpg")
                    .target(imageView)
                    .build()
                imageLoader.enqueue(request)
            } catch (e: Exception) {
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

}