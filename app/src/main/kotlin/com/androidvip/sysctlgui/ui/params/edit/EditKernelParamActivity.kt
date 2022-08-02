package com.androidvip.sysctlgui.ui.params.edit

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.transition.Explode
import android.transition.Slide
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.androidvip.sysctlgui.R
import com.androidvip.sysctlgui.data.models.KernelParam
import com.androidvip.sysctlgui.databinding.ActivityEditKernelParamBinding
import com.androidvip.sysctlgui.domain.repository.AppPrefs
import com.androidvip.sysctlgui.domain.usecase.ApplyParamsUseCase
import com.androidvip.sysctlgui.domain.usecase.UpdateUserParamUseCase
import com.androidvip.sysctlgui.goAway
import com.androidvip.sysctlgui.readLines
import com.androidvip.sysctlgui.show
import com.androidvip.sysctlgui.showAsLight
import com.androidvip.sysctlgui.toast
import com.androidvip.sysctlgui.ui.params.user.RemovableParamAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.InputStream

// TODO: Improve by delegating any non-presentation logic to a view model
class EditKernelParamActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditKernelParamBinding
    private val prefs: AppPrefs by inject()
    private val applyParamsUseCase: ApplyParamsUseCase by inject()
    private val updateUserParamUseCase: UpdateUserParamUseCase by inject()

    private lateinit var kernelParameter: KernelParam

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        window.enterTransition = Explode()
        window.exitTransition = Slide()

        binding = ActivityEditKernelParamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val extraParam = RemovableParamAdapter.EXTRA_PARAM
        (intent.getParcelableExtra(extraParam) as? KernelParam)?.also {
            kernelParameter = it

            if (!kernelParameter.hasValidPath() || !kernelParameter.hasValidName()) {
                showInvalidParamError()
            } else {
                defineInputTypeForValue(kernelParameter.value)
                binding.editParamInput.setText(kernelParameter.value)
                updateTextUi(kernelParameter)
                binding.editParamApply.setOnClickListener {
                    lifecycleScope.launch {
                        applyParam()
                    }
                }
            }
        } ?: run {
            showInvalidParamError()
        }
    }

    override fun onBackPressed() {
        supportFinishAfterTransition()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (this::kernelParameter.isInitialized.not()) {
            return false
        }
        menuInflater.inflate(R.menu.menu_edit_params, menu)
        menu.findItem(R.id.action_favorite)?.let {
            if (kernelParameter.favorite) {
                it.setIcon(R.drawable.ic_favorite_selected)
            } else {
                it.setIcon(R.drawable.ic_favorite_unselected)
            }
        }

        menu.findItem(R.id.action_tasker)?.let {
            if (isTaskerInstalled()) {
                it.isVisible = true
                if (kernelParameter.taskerParam) {
                    it.setIcon(R.drawable.ic_action_tasker_remove)
                } else {
                    it.setIcon(R.drawable.ic_action_tasker_add)
                }
            } else {
                it.isVisible = false
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_favorite -> {
                if (kernelParameter.favorite) {
                    kernelParameter.favorite = false
                    item.setIcon(R.drawable.ic_favorite_unselected)
                } else {
                    kernelParameter.favorite = true
                    item.setIcon(R.drawable.ic_favorite_selected)
                }

                lifecycleScope.launch {
                    updateUserParamUseCase(kernelParameter)
                }
                return true
            }

            R.id.action_tasker -> {
                selectTaskerListAsDialog { taskerList ->
                    if (kernelParameter.taskerParam) {
                        kernelParameter.taskerParam = false
                        item.setIcon(R.drawable.ic_action_tasker_add)
                        toast(getString(R.string.removed_from_tasker_list, taskerList))
                    } else {
                        kernelParameter.favorite = true
                        item.setIcon(R.drawable.ic_action_tasker_remove)
                        toast(getString(R.string.added_to_tasker_list, taskerList))
                    }

                    kernelParameter.taskerList = taskerList
                    lifecycleScope.launch {
                        updateUserParamUseCase(kernelParameter)
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateTextUi(param: KernelParam) {
        ViewCompat.setTransitionName(binding.editParamName, NAME_TRANSITION_NAME)
        ViewCompat.setTransitionName(binding.editParamInput, VALUE_TRANSITION_NAME)

        val paramName = param.name.split(".").last()
        binding.editParamName.text = paramName
        binding.editParamSub.text = param.name.removeSuffix(paramName).removeSuffix(".")
        binding.editParamInfo.text = findInfoForParam(param)
        binding.editParamApply.show()

        supportStartPostponedEnterTransition()
    }

    private fun showInvalidParamError() {
        binding.editParamErrorText.show()
        binding.editParamScroll.goAway()
        binding.editParamApply.hide()
    }

    private fun selectTaskerListAsDialog(block: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.select_tasker_list)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setSingleChoiceItems(R.array.tasker_lists, -1) { dialog, which ->
                block(which)
                dialog.dismiss()
            }.also {
                if (!isFinishing) {
                    it.show()
                }
            }
    }

    private fun defineInputTypeForValue(paramValue: String) {
        if (!prefs.guessInputType) return

        if (paramValue.length > 12) {
            binding.editParamInput.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            binding.editParamInput.setLines(3)
            return
        }

        try {
            paramValue.toInt()
            binding.editParamInput.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        } catch (e: Exception) {
            try {
                paramValue.toDouble()
                binding.editParamInput.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } catch (e: Exception) {
                binding.editParamInput.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            }
        }
    }

    private fun findInfoForParam(kernelParam: KernelParam): String {
        val paramName = kernelParam.name.split(".").last()
        val resId = resources.getIdentifier(
            paramName.replace("-", "_"),
            "string",
            packageName
        )
        val stringRes: String? = if (resId != 0) {
            runCatching {
                getString(resId)
            }.getOrNull()
        } else null

        // Prefer the documented string resource
        if (stringRes != null) return stringRes

        if (!kernelParam.path.startsWith("/")) {
            return stringRes ?: getString(R.string.no_info_available)
        }

        val subdirs = kernelParam.path.split("/")
        if (subdirs.isEmpty() || subdirs.size < 4) {
            return stringRes ?: getString(R.string.no_info_available)
        }

        val rawInputStream: InputStream? = when (subdirs[3]) {
            "abi" -> resources.openRawResource(R.raw.abi)
            "fs" -> resources.openRawResource(R.raw.fs)
            "kernel" -> resources.openRawResource(R.raw.kernel)
            "net" -> resources.openRawResource(R.raw.net)
            "vm" -> resources.openRawResource(R.raw.vm)
            else -> null
        }

        val documentationBuilder = StringBuilder()
        rawInputStream.readLines {
            documentationBuilder.append(it).append("\n")
        }

        val documentation = documentationBuilder.toString()
        if (documentation.isEmpty()) {
            return stringRes ?: getString(R.string.no_info_available)
        }

        val info: String? = runCatching {
            documentation.split("=+".toRegex()).last {
                it.contains("$paramName\n")
            }.split("$paramName\n").last()
        }.getOrNull()

        return if (info.isNullOrEmpty()) getString(R.string.no_info_available) else info
    }

    private suspend fun applyParam() {
        val isEditingSavedParam = intent.getBooleanExtra(
            RemovableParamAdapter.EXTRA_EDIT_SAVED_PARAM,
            false
        )

        val newValue = binding.editParamInput.text.toString()
        kernelParameter.value = newValue

        val result = runCatching { applyParamsUseCase(kernelParameter) }
        val feedback = if (result.isSuccess) {
            setResult(Activity.RESULT_OK)
            updateUserParamUseCase(kernelParameter)
            getString(R.string.done)
        } else {
            setResult(Activity.RESULT_CANCELED)
            getString(R.string.apply_failure_format, result.exceptionOrNull()?.message.orEmpty())
        }

        if (isEditingSavedParam) {
            toast(feedback)
            finish()
        } else {
            Snackbar.make(
                binding.editParamApply, feedback, Snackbar.LENGTH_LONG
            ).setAction(R.string.undo) {
                lifecycleScope.launchWhenResumed {
                    updateUserParamUseCase(kernelParameter)
                    binding.editParamInput.setText(kernelParameter.value)
                }
            }.showAsLight()
        }
    }

    private fun isTaskerInstalled(): Boolean {
        return runCatching {
            packageManager.getPackageInfo("net.dinglisch.android.taskerm", 0)
            true
        }.getOrDefault(false)
    }

    companion object {
        const val NAME_TRANSITION_NAME = "transition_title"
        const val VALUE_TRANSITION_NAME = "transition_value"
    }
}
