package com.hxs.xposedreddevil.model

/**
 * 群ID过滤数据模型
 * 用于存储群ID过滤相关信息
 */
data class GroupIdFilterBean(
    /**
     * 群ID，微信群的唯一标识符
     * 例如：123456789@chatroom
     */
    var groupId: String = "",
    
    /**
     * 群聊名称，用于显示
     * 可以是用户自定义的名称
     */
    var groupName: String = "",
    
    /**
     * 是否启用此过滤项
     * true: 启用过滤，不抢此群的红包
     * false: 禁用过滤，正常抢红包
     */
    var isEnabled: Boolean = true,
    
    /**
     * 添加时间戳
     * 用于记录何时添加的此过滤项
     */
    var addTime: Long = 0L,
    
    /**
     * 备注信息
     * 可选的备注说明
     */
    var remark: String = ""
) {
    /**
     * 获取显示名称
     * 如果群名称为空，则显示群ID的前8位
     */
    fun getDisplayName(): String {
        return if (groupName.isNotEmpty()) {
            groupName
        } else {
            "群聊_${groupId.take(8)}"
        }
    }
    
    /**
     * 获取格式化的群ID
     * 确保群ID包含@chatroom后缀
     */
    fun getFormattedGroupId(): String {
        return if (groupId.contains("@chatroom")) {
            groupId
        } else {
            "${groupId}@chatroom"
        }
    }
    
    /**
     * 检查是否匹配指定的群ID
     * 支持精确匹配和自动补全@chatroom后缀匹配
     */
    fun matches(targetGroupId: String): Boolean {
        val formattedGroupId = getFormattedGroupId()
        
        // 精确匹配
        if (formattedGroupId == targetGroupId) {
            return true
        }
        
        // 如果目标群ID没有@chatroom后缀，尝试补全后匹配
        if (!targetGroupId.contains("@chatroom")) {
            val targetWithSuffix = "${targetGroupId}@chatroom"
            if (formattedGroupId == targetWithSuffix) {
                return true
            }
        }
        
        return false
    }
    
    override fun toString(): String {
        return "GroupIdFilterBean(groupId='$groupId', groupName='$groupName', isEnabled=$isEnabled, addTime=$addTime, remark='$remark')"
    }
}