package com.androidvip.sysctlgui

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils

object RootUtils {

    fun isBusyboxAvailable(): Boolean {
        val results: List<String> = Shell.sh("which busybox").exec().out
        return if (ShellUtils.isValidOutput(results)) {
            results.last().isNotEmpty()
        } else false
    }

    fun executeWithOutput(command: String?, defaultOutput: String = "", forEachLine: ((String?) -> Unit)? = null): String {
        val cmd = command ?: "pwd"
        val sb = StringBuilder()

        try {
            val outputs = Shell.su(cmd).exec().out
            if (ShellUtils.isValidOutput(outputs)) {
                outputs.forEach {
                    if (forEachLine != null) {
                        forEachLine(it)
                        sb.append(it).append("\n")
                    } else {
                        sb.append(it).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            return defaultOutput
        }

        return sb.toString().trim().removeSuffix("\n")
    }

    fun finishProcess() {
        runCatching {
            Shell.getCachedShell()?.close()
        }
    }
}