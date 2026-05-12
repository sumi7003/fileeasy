package com.xplay.player.update

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.xplay.player.utils.ApkInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object AppHttpUpdateController {
    private const val TAG = "AppHttpUpdateController"

    @Volatile
    private var isCheckedVersion = false

    fun checkVersion2(activity: ComponentActivity, callback: (() -> Unit)? = null) {
        if (isCheckedVersion) {
            callback?.invoke()
            return
        }

        activity.lifecycleScope.launch {
            val versionData = withContext(Dispatchers.IO) {
                runCatching { OtaHttpClient.checkVersion(activity.applicationContext) }
                    .onFailure { Log.w(TAG, "checkVersion2 failed", it) }
                    .getOrNull()
            }

            isCheckedVersion = true

            if (versionData == null || versionData.package_name.isNullOrBlank() || versionData.url.isNullOrBlank()) {
                callback?.invoke()
                return@launch
            }

            val packageInfo = getPackageInfo(activity, versionData.package_name)
            val shouldUpdate = packageInfo == null || currentVersionCode(packageInfo) < versionData.version
            if (!shouldUpdate) {
                callback?.invoke()
                return@launch
            }

            showUpdateDialog(activity, versionData, callback)
        }
    }

    private fun showUpdateDialog(
        activity: ComponentActivity,
        versionData: OtaVersionData,
        callback: (() -> Unit)?
    ) {
        val forceUpdate = versionData.enforce == 1
        val message = versionData.remark
            ?.takeIf { it.isNotBlank() }
            ?: if (forceUpdate) "发现新版本，请先更新后再使用" else "发现新版本，是否更新？"

        AlertDialog.Builder(activity)
            .setTitle("版本更新")
            .setMessage(message)
            .setCancelable(false)
            .apply {
                if (!forceUpdate) {
                    setNegativeButton("下次再说") { _, _ -> callback?.invoke() }
                }
                setPositiveButton("立即更新") { _, _ ->
                    downloadAndInstall(activity, versionData.url.orEmpty(), forceUpdate, callback)
                }
            }
            .show()
    }

    private fun downloadAndInstall(
        activity: ComponentActivity,
        url: String,
        forceUpdate: Boolean,
        callback: (() -> Unit)?
    ) {
        val apkFile = File(activity.cacheDir, "ota-${System.currentTimeMillis()}.apk")
        val progressText = TextView(activity).apply {
            text = "正在下载... 0%"
        }
        val progressBar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        val progressContent = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(24), activity.dp(8), activity.dp(24), activity.dp(4))
            addView(
                progressText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                progressBar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = activity.dp(12)
                }
            )
        }
        val progressDialog = AlertDialog.Builder(activity)
            .setTitle("版本更新")
            .setView(progressContent)
            .setCancelable(false)
            .create()
        progressDialog.show()

        activity.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    OtaDownloader.download(url, apkFile) { progress ->
                        launch(Dispatchers.Main) {
                            progressBar.progress = progress
                            progressText.text = "正在下载... $progress%"
                        }
                    }
                }.onFailure {
                    Log.w(TAG, "downloadAndInstall failed", it)
                }.getOrDefault(false)
            }

            progressDialog.dismiss()

            if (success) {
                Toast.makeText(activity, "下载完成，开始安装", Toast.LENGTH_SHORT).show()
                ApkInstaller.install(activity, apkFile)
                if (!forceUpdate) callback?.invoke()
            } else {
                AlertDialog.Builder(activity)
                    .setTitle("提示")
                    .setMessage("下载失败")
                    .setCancelable(false)
                    .setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                        if (forceUpdate) {
                            activity.finish()
                        } else {
                            callback?.invoke()
                        }
                    }
                    .show()
            }
        }
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun getPackageInfo(context: Context, packageName: String): PackageInfo? {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
    }

    private fun currentVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
