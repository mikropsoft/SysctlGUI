package com.androidvip.sysctlgui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

fun View.goAway() { this.visibility = View.GONE }
fun View.hide() { this.visibility = View.INVISIBLE }
fun View.show() { this.visibility = View.VISIBLE }

fun Snackbar.showAsLight() {
    view.setBackgroundColor(Color.parseColor("#cfd8dc"))
    setTextColor(Color.parseColor("#DE000000"))
    show()
}

fun Context?.toast(messageRes: Int, length: Int = Toast.LENGTH_SHORT) {
    if (this == null) return
    toast(getString(messageRes), length)
}

fun Context?.toast(message: String?, length: Int = Toast.LENGTH_SHORT) {
    if (message == null || this == null) return
    val ctx = this

    // Just in case :P
    GlobalScope.launch(Dispatchers.Main) {
        Toast.makeText(ctx, message, length).show()
    }
}

fun Uri.readLines(context: Context?, forEachLine: (String) -> Unit) {
    context?.contentResolver?.openInputStream(this).readLines(forEachLine)
}

fun InputStream?.readLines(forEachLine: (String) -> Unit) {
    this?.use { inputStream ->
        inputStream.bufferedReader().use {
            it.readLines().forEach { line ->
                forEachLine(line)
            }
        }
    }
}

suspend inline fun Activity?.runSafeOnUiThread(crossinline uiBlock: () -> Unit) {
    this?.let {
        if (!it.isFinishing && !it.isDestroyed) {
            withContext(Dispatchers.Main) {
                runCatching(uiBlock)
            }
        }
    }
}
