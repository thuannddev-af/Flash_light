package com.kt.flash.light.led.banner.component.permission

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.kt.flash.light.led.banner.data.model.PermissionModel
import com.kt.flash.light.led.banner.databinding.ItemPermissionBinding
import com.kt.flash.light.led.banner.utils.isPermissionsGranted


class PermissionAdapter : ListAdapter<PermissionModel, ViewHolder>(ItemPermissionModelDiffCallBack) {
    var onAllowClick: ((Int, PermissionModel) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ItemPermissionBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
            .run {
                PermissionViewHolder(this, onAllowClick)
            }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        (holder as PermissionViewHolder).bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            (holder as PermissionViewHolder).bind(getItem(position))
        }
    }

    class PermissionViewHolder(
        private val binding: ItemPermissionBinding,
        private val onAllowClick: ((Int, PermissionModel) -> Unit)? = null,
    ) : ViewHolder(binding.root) {
        private val context get() = binding.root.context

        fun bind(item: PermissionModel) =
            with(binding) {
                imgPermission.setImageResource(item.iconRes)
                tvTitle.text = context.resources.getString(item.title)
                tvContent.text = context.resources.getString(item.content)
                val isPermissionGranted = context.isPermissionsGranted(item.permissions)
                swAllow.isChecked = isPermissionGranted
                swAllow.isEnabled = isPermissionGranted.not()

                initAction(item)
            }

        private fun initAction(item: PermissionModel) =
            with(binding) {
                swAllow.setOnCheckedChangeListener(
                    object : CompoundButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                            if (context.isPermissionsGranted(item.permissions)) return
                            onAllowClick?.invoke(
                                bindingAdapterPosition,
                                item.copy(
                                    isAllowed = isChecked,
                                ),
                            )
                            swAllow.isChecked = false
                        }
                    },
                )
            }
    }

    companion object {
        private val ItemPermissionModelDiffCallBack =
            object : ItemCallback<PermissionModel>() {
                override fun areItemsTheSame(
                    oldItem: PermissionModel,
                    newItem: PermissionModel,
                ): Boolean = oldItem.title == newItem.title && oldItem.content == newItem.content

                override fun areContentsTheSame(
                    oldItem: PermissionModel,
                    newItem: PermissionModel,
                ): Boolean = oldItem == newItem

                override fun getChangePayload(
                    oldItem: PermissionModel,
                    newItem: PermissionModel,
                ): Any? {
                    return if (oldItem.iconRes != newItem.iconRes) {
                        return 1
                    } else {
                        null
                    }
                }
            }
    }
}
