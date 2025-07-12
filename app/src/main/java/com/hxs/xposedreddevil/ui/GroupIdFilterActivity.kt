package com.hxs.xposedreddevil.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.hxs.xposedreddevil.adapter.GroupIdFilterAdapter
import com.hxs.xposedreddevil.adapter.GroupIdFilterAdapter.OnItemClickListener
import com.hxs.xposedreddevil.base.BaseActivity
import com.hxs.xposedreddevil.databinding.ActivityGroupIdFilterBinding
import com.hxs.xposedreddevil.model.GroupIdFilterBean

/**
 * 群ID过滤Activity
 * 用于通过群ID进行过滤，不依赖数据库获取群聊数据
 * 支持手动添加群ID和群聊名称
 */
class GroupIdFilterActivity : BaseActivity(), OnItemClickListener {
    private var binding: ActivityGroupIdFilterBinding? = null
    var filterBean: GroupIdFilterBean? = null
    var beanList: MutableList<GroupIdFilterBean> = ArrayList()
    var adapter: GroupIdFilterAdapter? = null
    var gson = Gson()
    var parser = JsonParser()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupIdFilterBinding.inflate(LayoutInflater.from(this))
        setContentView(binding!!.root)
        DataInit()
    }

    @SuppressLint("RestrictedApi")
    private fun DataInit() {
        binding!!.tvClassName.text = "群ID过滤设置"
        binding!!.rlSelect.layoutManager = LinearLayoutManager(this)
        
        // 滚动监听
        binding!!.rlSelect.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    binding!!.fabAdd.visibility = View.VISIBLE
                } else if (dy == 0) {
                    binding!!.fabAdd.visibility = View.VISIBLE
                } else {
                    binding!!.fabAdd.visibility = View.GONE
                }
            }
        })
        
        adapter = GroupIdFilterAdapter(beanList, this)
        binding!!.rlSelect.adapter = adapter
        adapter!!.setOnItemClickListener(this)
        
        // 加载已保存的群ID过滤列表
        loadSavedGroupIdFilters()
        
        // 返回按钮
        binding!!.ivClassBack.setOnClickListener { finish() }
        
        // 保存按钮
        binding!!.tvClassAdd.setOnClickListener {
            saveGroupIdFilters()
            Toast.makeText(this, "保存完成", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        // 添加群ID按钮
        binding!!.fabAdd.setOnClickListener {
            showAddGroupIdDialog()
        }
        
        // 批量添加按钮
        binding!!.fabBatchAdd.setOnClickListener {
            showBatchAddDialog()
        }
    }
    
    /**
     * 加载已保存的群ID过滤列表
     */
    private fun loadSavedGroupIdFilters() {
        val savedData = config.groupIdFilter
        if (savedData.isNotEmpty()) {
            try {
                val jsonArray: JsonArray = parser.parse(savedData).asJsonArray
                beanList.clear()
                for (element in jsonArray) {
                    val filterItem = gson.fromJson(element, GroupIdFilterBean::class.java)
                    if (filterItem != null) {
                        beanList.add(filterItem)
                    }
                }
                adapter!!.notifyDataSetChanged()
                Log.d("GroupIdFilterActivity", "加载了 ${beanList.size} 个群ID过滤项")
            } catch (e: Exception) {
                Log.e("GroupIdFilterActivity", "加载群ID过滤列表失败: $e")
                Toast.makeText(this, "加载数据失败，将重新开始", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 保存群ID过滤列表
     */
    private fun saveGroupIdFilters() {
        val enabledFilters = beanList.filter { it.isEnabled }
        config.groupIdFilter = gson.toJson(beanList)
        config.groupIdFilterEnabled = gson.toJson(enabledFilters)
        Log.d("GroupIdFilterActivity", "保存了 ${beanList.size} 个群ID过滤项，其中 ${enabledFilters.size} 个已启用")
    }
    
    /**
     * 显示添加群ID对话框
     */
    private fun showAddGroupIdDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("添加群ID过滤")
        builder.setMessage("请输入群ID和群聊名称：\n\n" +
                "• 群ID：微信群的唯一标识符（如：123456789@chatroom）\n" +
                "• 群聊名称：用于显示的群聊名称（可选）\n" +
                "• 如果只输入群ID，系统会自动补全@chatroom后缀")
        
        val layout = layoutInflater.inflate(com.hxs.xposedreddevil.R.layout.dialog_add_group_id, null)
        val etGroupId = layout.findViewById<EditText>(com.hxs.xposedreddevil.R.id.et_group_id)
        val etGroupName = layout.findViewById<EditText>(com.hxs.xposedreddevil.R.id.et_group_name)
        
        builder.setView(layout)
        
        builder.setPositiveButton("添加") { dialog, _ ->
            val groupId = etGroupId.text.toString().trim()
            val groupName = etGroupName.text.toString().trim()
            
            if (groupId.isNotEmpty()) {
                addGroupIdFilter(groupId, groupName)
            } else {
                Toast.makeText(this, "请输入群ID", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    /**
     * 显示批量添加对话框
     */
    private fun showBatchAddDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("批量添加群ID")
        builder.setMessage("请输入多个群ID，每行一个：\n\n" +
                "格式示例：\n" +
                "123456789@chatroom\n" +
                "987654321@chatroom\n" +
                "或者：\n" +
                "123456789@chatroom,工作群\n" +
                "987654321@chatroom,家人群")
        
        val input = EditText(this)
        input.hint = "每行一个群ID，可用逗号分隔群ID和群名"
        input.minLines = 5
        builder.setView(input)
        
        builder.setPositiveButton("批量添加") { dialog, _ ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                batchAddGroupIds(text)
            } else {
                Toast.makeText(this, "请输入群ID", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }
    
    /**
     * 添加单个群ID过滤
     */
    private fun addGroupIdFilter(groupId: String, groupName: String) {
        var finalGroupId = groupId
        
        // 自动补全@chatroom后缀
        if (!finalGroupId.contains("@chatroom") && !finalGroupId.contains("@")) {
            finalGroupId = "${finalGroupId}@chatroom"
        }
        
        // 检查是否已存在
        val existing = beanList.find { it.groupId == finalGroupId }
        if (existing != null) {
            Toast.makeText(this, "该群ID已存在", Toast.LENGTH_SHORT).show()
            return
        }
        
        val filterBean = GroupIdFilterBean()
        filterBean.groupId = finalGroupId
        filterBean.groupName = if (groupName.isNotEmpty()) groupName else "群聊_${finalGroupId.substring(0, 8)}"
        filterBean.isEnabled = true
        filterBean.addTime = System.currentTimeMillis()
        
        beanList.add(filterBean)
        adapter!!.notifyDataSetChanged()
        
        Toast.makeText(this, "已添加群ID：${filterBean.groupName}", Toast.LENGTH_SHORT).show()
        Log.d("GroupIdFilterActivity", "添加群ID过滤: ${filterBean.groupId} - ${filterBean.groupName}")
    }
    
    /**
     * 批量添加群ID
     */
    private fun batchAddGroupIds(text: String) {
        val lines = text.split("\n")
        var addedCount = 0
        var skippedCount = 0
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            val parts = trimmedLine.split(",")
            val groupId = parts[0].trim()
            val groupName = if (parts.size > 1) parts[1].trim() else ""
            
            if (groupId.isNotEmpty()) {
                var finalGroupId = groupId
                
                // 自动补全@chatroom后缀
                if (!finalGroupId.contains("@chatroom") && !finalGroupId.contains("@")) {
                    finalGroupId = "${finalGroupId}@chatroom"
                }
                
                // 检查是否已存在
                val existing = beanList.find { it.groupId == finalGroupId }
                if (existing != null) {
                    skippedCount++
                    continue
                }
                
                val filterBean = GroupIdFilterBean()
                filterBean.groupId = finalGroupId
                filterBean.groupName = if (groupName.isNotEmpty()) groupName else "群聊_${finalGroupId.substring(0, 8)}"
                filterBean.isEnabled = true
                filterBean.addTime = System.currentTimeMillis()
                
                beanList.add(filterBean)
                addedCount++
            }
        }
        
        adapter!!.notifyDataSetChanged()
        
        val message = "批量添加完成：新增 $addedCount 个，跳过 $skippedCount 个重复项"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d("GroupIdFilterActivity", message)
    }
    
    override fun itemClickListener(v: View, position: Int) {
        if (position < beanList.size) {
            beanList[position].isEnabled = !beanList[position].isEnabled
            adapter!!.notifyItemChanged(position)
        }
    }
    
    /**
     * 删除群ID过滤项
     */
    fun deleteItem(position: Int) {
        if (position < beanList.size) {
            val item = beanList[position]
            beanList.removeAt(position)
            adapter!!.notifyItemRemoved(position)
            Toast.makeText(this, "已删除：${item.groupName}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}