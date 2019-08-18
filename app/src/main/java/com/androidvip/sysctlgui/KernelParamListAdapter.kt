package com.androidvip.sysctlgui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KernelParamListAdapter(private val context: Context, private val dataSet: MutableList<KernelParameter>) : RecyclerView.Adapter<KernelParamListAdapter.ViewHolder>() {

    companion object {
        const val EXTRA_PARAM = "kernel_param"
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var name: TextView = v.findViewById(R.id.listKernelParamName)
        var value: TextView = v.findViewById(R.id.listKernelParamValue)
        var itemLayout: LinearLayout = v.findViewById(R.id.listKernelParamLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.list_item_kernel_param_list, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kernelParam = dataSet[position]

        holder.name.text = kernelParam.param
        holder.itemLayout.setOnClickListener(null)

        GlobalScope.launch {
            val paramValue = getParamValue(kernelParam.path)
            withContext(Dispatchers.Main) {
                holder.value.text = paramValue
                kernelParam.value = paramValue
            }

            holder.itemLayout.setOnClickListener {
                Intent(context, EditKernelParamActivity::class.java).apply {
                    putExtra(EXTRA_PARAM, kernelParam)
                    context.startActivity(this)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun updateData(newData: List<KernelParameter>) {
        dataSet.clear()
        dataSet.addAll(newData)
        notifyDataSetChanged()
    }

    private suspend fun getParamValue(path: String) = withContext(Dispatchers.Default) {
        RootUtils.executeWithOutput("cat $path", "")
    }
}
