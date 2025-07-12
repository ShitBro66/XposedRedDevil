package com.hxs.xposedreddevil.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hxs.xposedreddevil.R
import com.hxs.xposedreddevil.model.GroupIdFilterBean
import com.hxs.xposedreddevil.ui.GroupIdFilterActivity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 群ID过滤列表适配器
 */
class GroupIdFilterAdapter(
    private val dataList: MutableList<GroupIdFilterBean>,
    private val activity: GroupIdFilterActivity
) : RecyclerView.Adapter<GroupIdFilterAdapter.ViewHolder>() {
    
    private var onItemClickListener: OnItemClickListener? = null
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    interface OnItemClickListener {
        fun itemClickListener(v: View, position: Int)
    }
    
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_id_filter, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        
        // 设置群聊名称
        holder.tvGroupName.text = item.getDisplayName()
        
        // 设置群ID
        holder.tvGroupId.text = item.groupId
        
        // 设置添加时间
        if (item.addTime > 0) {
            holder.tvAddTime.text = "添加时间: ${dateFormat.format(Date(item.addTime))}"
            holder.tvAddTime.visibility = View.VISIBLE
        } else {
            holder.tvAddTime.visibility = View.GONE
        }
        
        // 设置备注
        if (item.remark.isNotEmpty()) {
            holder.tvRemark.text = "备注: ${item.remark}"
            holder.tvRemark.visibility = View.VISIBLE
        } else {
            holder.tvRemark.visibility = View.GONE
        }
        
        // 设置选中状态
        holder.cbEnabled.isChecked = item.isEnabled
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClickListener?.itemClickListener(it, position)
        }
        
        holder.cbEnabled.setOnClickListener {
            onItemClickListener?.itemClickListener(it, position)
        }
        
        // 设置删除按钮点击事件
        holder.ivDelete.setOnClickListener {
            activity.deleteItem(position)
        }
        
        // 根据启用状态设置样式
        if (item.isEnabled) {
            holder.itemView.alpha = 1.0f
            holder.tvGroupName.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        } else {
            holder.itemView.alpha = 0.6f
            holder.tvGroupName.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }
    }
    
    override fun getItemCount(): Int {
        return dataList.size
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroupName: TextView = itemView.findViewById(R.id.tv_group_name)
        val tvGroupId: TextView = itemView.findViewById(R.id.tv_group_id)
        val tvAddTime: TextView = itemView.findViewById(R.id.tv_add_time)
        val tvRemark: TextView = itemView.findViewById(R.id.tv_remark)
        val cbEnabled: CheckBox = itemView.findViewById(R.id.cb_enabled)
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
    }
}