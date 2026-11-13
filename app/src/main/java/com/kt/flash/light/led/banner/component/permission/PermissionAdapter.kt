package com.kt.flash.light.led.banner.component.permission

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kt.flash.light.led.banner.R
import com.kt.flash.light.led.banner.data.model.PermissionModel
import com.kt.flash.light.led.banner.databinding.ItemPermissionBinding
import com.kt.flash.light.led.banner.utils.isPermissionsGranted
import com.kt.flash.light.led.banner.utils.setOnSingleClickListener

class PermissionAdapter : ListAdapter<PermissionModel, PermissionAdapter.PermissionViewHolder>(DiffCallback) {
    var onAllowClick: ((Int, PermissionModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PermissionViewHolder(
        ItemPermissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) = holder.bind(getItem(position))

    inner class PermissionViewHolder(private val binding: ItemPermissionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PermissionModel) = with(binding) {
            imgPermission.setImageResource(item.iconRes)
            tvTitle.setText(item.title)
            tvContent.setText(item.content)
            val granted = root.context.isPermissionsGranted(item.permissions)
            swAllow.setImageResource(if (granted) R.drawable.ic_on else R.drawable.ic_off)
            swAllow.contentDescription = root.context.getString(if (granted) R.string.on else R.string.off)
            swAllow.isEnabled = !granted
            swAllow.setOnSingleClickListener { requestPermission() }
            root.setOnSingleClickListener { requestPermission() }
        }

        private fun requestPermission() {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            val item = getItem(position)
            if (!binding.root.context.isPermissionsGranted(item.permissions)) onAllowClick?.invoke(position, item)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PermissionModel>() {
        override fun areItemsTheSame(oldItem: PermissionModel, newItem: PermissionModel) = oldItem.permissions == newItem.permissions
        override fun areContentsTheSame(oldItem: PermissionModel, newItem: PermissionModel) = oldItem == newItem
    }
}
