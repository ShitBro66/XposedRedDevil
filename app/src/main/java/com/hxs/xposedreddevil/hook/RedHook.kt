package com.hxs.xposedreddevil.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import cn.xiaowine.dsp.DSP
import cn.xiaowine.dsp.data.MODE
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.hxs.xposedreddevil.BuildConfig
import com.hxs.xposedreddevil.config.SafeConfig
import com.hxs.xposedreddevil.model.DBean
import com.hxs.xposedreddevil.model.FilterSaveBean
import com.hxs.xposedreddevil.model.MsgsBean
import com.hxs.xposedreddevil.utils.AcxiliaryServiceStaticValues
import com.hxs.xposedreddevil.utils.Hanzi2PinyinHelper
import com.hxs.xposedreddevil.utils.PlaySoundUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fr.arnaudguyon.xmltojsonlib.XmlToJson
import java.lang.reflect.Field

class RedHook  // 加载dexkit
{
    private var classLoader: XC_LoadPackage.LoadPackageParam? = null
    var gson: Gson = Gson()
    var bean: MsgsBean = MsgsBean()
    var dBean: DBean = DBean()
    var nativeUrlString = ""
    var cropname = ""
    var stringMap: MutableMap<String, Any> = HashMap()
    var parser: JsonParser = JsonParser()
    var filterSaveBean: FilterSaveBean? = null
    lateinit var context: Context
    private var die_count = 0
    var jsonObject: JsonObject = JsonObject()
    private val activity = "" //当前页面
    private var content = "" //返回的内容
    lateinit var config: SafeConfig
    fun init(classLoader: XC_LoadPackage.LoadPackageParam) {
        if (this.classLoader == null) {
            this.classLoader = classLoader
            hook(classLoader)
        }
    }

    public object RedHookHolder {
        @SuppressLint("StaticFieldLeak")
        var instance = RedHook()
    }

    @SuppressLint("PrivateApi")
    private fun hook(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            if (lpparam.packageName == "com.tencent.mm") {
                AcxiliaryServiceStaticValues.SetValues()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                    val disableHooksFiled = ClassLoader.getSystemClassLoader()
                        .loadClass("de.robv.android.xposed.XposedBridge")
                        .getDeclaredField("disableHooks")
                    disableHooksFiled.isAccessible = true
                    val enable = disableHooksFiled[null] // 当前状态
                    println("状态---------->$enable")
                    disableHooksFiled[null] = false // 设置为开启
                    //            disableHooksFiled.set(null, true);            // 设置为开启
                    // 过防止调用loadClass加载 de.robv.android.xposed.
                    XposedHelpers.findAndHookMethod(
                        ClassLoader::class.java,
                        "loadClass",
                        String::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            protected override fun beforeHookedMethod(param: MethodHookParam) {
                                if (param.args != null && param.args.get(0) != null && param.args[0].toString()
                                        .startsWith("de.robv.android.xposed.")
                                ) {
                                    // 改成一个不存在的类
                                    param.args[0] = "com.tencent.cndy"
                                }
                                super.beforeHookedMethod(param)
                            }
                        })
                }
                // System.out.println("监听微信");
                // hook微信插入数据的方法，监听红包消息
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    "attach",
                    Context::class.java,
                    object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        protected override fun afterHookedMethod(param: MethodHookParam) {
                            println("监听微信")
                            println(
                                "可否操作：" + DSP.init(
                                    param.thisObject as Application,
                                    BuildConfig.APPLICATION_ID,
                                    MODE.HOOK,
                                    true
                                )
                            )
                            config = SafeConfig()
                            context = param.args[0] as Context
                            println("微信版本:" + config.wechatversion)
                            try {
                                val ContextClass: Class<*> = XposedHelpers.findClass(
                                    "android.content.ContextWrapper",
                                    lpparam.classLoader
                                )
                                XposedHelpers.findAndHookMethod(
                                    ContextClass,
                                    "getApplicationContext",
                                    object : XC_MethodHook() {
                                        @Throws(Throwable::class)
                                        protected override fun afterHookedMethod(param: MethodHookParam) {
                                            super.afterHookedMethod(param)
//                                            if (share!!.getString("openwechat", "2") != "2") {
//                                                if (!AppMD5Util.isRunning(
//                                                        Bugly.applicationContext,
//                                                        "com.tencent.mm"
//                                                    )
//                                                ) {
//                                                    if (die_count == 10) {
//                                                        val uri = Uri.parse("weixin://")
//                                                        val intent = Intent(Intent.ACTION_VIEW, uri)
//                                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                                                        Bugly.applicationContext.startActivity(
//                                                            intent
//                                                        )
//                                                        die_count = 0
//                                                    }
//                                                    die_count += 1
//                                                }
//                                            }
                                        }
                                    })
                            } catch (t: Throwable) {
//            log("微信状态错误：" + t);
                            }
                            //                        System.out.println("启动状态：" + AppUtils.isAppRunning("io.ubug.popup.motor"));
//                        if (!AppUtils.isAppRunning("io.ubug.popup.motor")) {
//                            AppUtils.launchApp("io.ubug.popup.motor");
//                        }
                            val cl = context!!.classLoader // 获取ClassLoader
                            var hookClass: Class<*>? = null
                            var hookLauncherUIClass: Class<*>? = null
                            hookClass = cl.loadClass("com.tencent.wcdb.database.SQLiteDatabase")
                            hookLauncherUIClass = cl.loadClass("com.tencent.mm.ui.LauncherUI")
                            XposedHelpers.findAndHookMethod(hookClass,
                                "insertWithOnConflict",
                                String::class.java,
                                String::class.java,
                                ContentValues::class.java,
                                Int::class.javaPrimitiveType,
                                object : XC_MethodHook() {
                                    @RequiresApi(api = Build.VERSION_CODES.O)
                                    @Throws(
                                        Throwable::class
                                    )
                                    protected override fun afterHookedMethod(param: MethodHookParam) {
                                        // 打印插入数据信息
//                                        System.out.println("------------------------insert start---------------------" + "\n\n");
                                        val contentValues = param.args.get(2) as ContentValues
                                        var title = ""
                                        var realTalker = ""
                                        
                                        // 清空之前的stringMap
                                        stringMap.clear()
                                        
                                        for ((key, value) in contentValues.valueSet()) {
                                            if (value != null) {
                                                if (key.contains("content")) {
                                                    content = value.toString()
                                                }
                                                if (key == "talker") {
                                                    title = value.toString()
                                                    realTalker = value.toString()
                                                }
                                                //记录talker信息
                                                if (key == "talker") {
                                                    stringMap["talker"] = value.toString()
                                                }
                                                if (key.contains("isSend")) {
                                                    stringMap[key] = value.toString()
                                                } else {
                                                    stringMap[key] = "null"
                                                }
                                            } else {
                                                stringMap[key] = "null"
                                            }
                                        }
                                        
                                        println("=== ContentValues调试信息 ===")
                                        println("realTalker from ContentValues: '$realTalker'")
                                        println("title: '$title'")
                                        println("stringMap['talker']: '${stringMap["talker"]}'")
                                        println("stringMap size: ${stringMap.size}")
                                        
                                        //                                        System.out.println("------------------------insert end---------------------" + "\n\n");
                                        // 判断插入的数据是否是发送过来的消息
                                        val tableName = param.args.get(0) as String
                                        // System.out.println("tableName:" + tableName);
                                        // System.out.println.d(TAG, "tableName: " + tableName);
                                        if (TextUtils.isEmpty(tableName) || tableName != "message") {
                                            return
                                        }
                                        // 判断是否是红包消息类型
                                        val type = contentValues.getAsInteger("type") ?: return
                                        //                                        System.out.println("当前type结果：" + type);
                                        if (type == 436207665 || type == 469762097) {
                                            println("动态数据：" + config.redMain)
                                            //                                            if (!activity.contains("com.tencent.mm.ui.LauncherUI")) {
//                                                Uri uri = Uri.parse("weixin://");
//                                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                                context.startActivity(intent);
                                            if (handleBefore(title, realTalker)) {
                                                // 处理红包消息
                                                handleLuckyMoney(contentValues, cl)
                                            }
                                            //                                            }
                                        }
                                    }
                                })
                            // hook 微信主界面的onCreate方法，获得主界面对象
                            XposedHelpers.findAndHookMethod(
                                hookLauncherUIClass,
                                "onCreate",
                                Bundle::class.java,
                                object : XC_MethodHook() {
                                    @Throws(Throwable::class)
                                    protected override fun afterHookedMethod(param: MethodHookParam) {
                                        launcherUiActivity = param.thisObject as Activity?
                                    }
                                })

                            // hook领取红包页面的onCreate方法，打印Intent中的参数（只起到调试作用）
                            XposedHelpers.findAndHookMethod(
                                "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI",
                                lpparam.classLoader,
                                "onCreate",
                                Bundle::class.java,
                                object : XC_MethodHook() {
                                    @Throws(Throwable::class)
                                    protected override fun afterHookedMethod(param: MethodHookParam) {
                                    }
                                })
                            XposedHelpers.findAndHookMethod(
                                "com.tencent.mm.plugin.wallet.balance.ui.WalletBalanceManagerUI",
                                lpparam.classLoader,
                                "onCreate",
                                Bundle::class.java,
                                object : XC_MethodHook() {
                                    @Throws(Throwable::class)
                                    protected override fun afterHookedMethod(param: MethodHookParam) {
                                        if (!config.money) {
                                            return
                                        }
                                        val activity = param.thisObject as Activity
                                        val tv = XposedHelpers.getObjectField(
                                            activity,
                                            "ddl"
                                        ) as TextView
                                        tv.text = "¥9999999999.99"
                                        tv.addTextChangedListener(object : TextWatcher {
                                            override fun beforeTextChanged(
                                                s: CharSequence,
                                                start: Int,
                                                count: Int,
                                                after: Int
                                            ) {
                                            }

                                            override fun onTextChanged(
                                                s: CharSequence,
                                                start: Int,
                                                before: Int,
                                                count: Int
                                            ) {
                                            }

                                            override fun afterTextChanged(s: Editable) {
                                                tv.removeTextChangedListener(this)
                                                tv.text = "¥9999999999"
                                                tv.addTextChangedListener(this)
                                            }
                                        })
                                    }
                                })
                            XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
                                AcxiliaryServiceStaticValues.LuckyMoneyNotHookReceiveUI,
                                cl
                            ),
                                AcxiliaryServiceStaticValues.LuckyMoneyNotHookReceiveUIMethod,
                                Int::class.javaPrimitiveType,
                                Int::class.javaPrimitiveType,
                                String::class.java,
                                XposedHelpers.findClass(
                                    AcxiliaryServiceStaticValues.LuckyMoneyNotHookReceiveUIMethodParameter,
                                    cl
                                ),
                                object : XC_MethodHook() {
                                    //进行hook操作
                                    @Throws(Throwable::class)
                                    protected override fun afterHookedMethod(param: MethodHookParam) {
//                                        System.out.println("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI: Method d called" + "\n");
                                        val buttonField: Field = XposedHelpers.findField(
                                            param.thisObject.javaClass,
                                            AcxiliaryServiceStaticValues.LuckyMoneyNotHookReceiveUIButton
                                        )
                                        val kaiButton = buttonField[param.thisObject] as Button
                                        kaiButton.performClick()
                                    }
                                })
                        }
                    })
            }
            // System.out.println("监听微信2");
        } catch (e: Exception) {
            e.printStackTrace()
            println("报错信息：$e")
        }
    }

    private fun handleBefore(title: String, realTalker: String): Boolean {
        println("=== 红包处理开始 ===")
        println("主开关状态：" + config.redMain)
        println("私聊过滤开关：" + config.privates)
        println("自己发的不抢开关：" + config.red)
        
        var title = title
        if (!config.redMain) {
            println("主开关关闭，不处理红包")
            return false
        }
        
        // 检查是否自己发的红包
        if (config.red) {
            if ((stringMap["isSend"] as? String) == "1") {
                println("自己发的红包，不抢")
                return false
            }
        }
        
        // 使用传入的realTalker，如果为空则尝试从stringMap获取
        val talker = if (realTalker.isNotEmpty()) {
            realTalker
        } else {
            (stringMap["talker"] as? String) ?: ""
        }
        
        println("传入的realTalker: '$realTalker'")
        println("stringMap中的talker: '${stringMap["talker"]}'")
        println("最终使用的talker: '$talker'")
        println("talker长度: ${talker.length}")
        
        // 如果talker仍然为空，记录错误并返回false
        if (talker.isEmpty() || talker == "null") {
            println("错误：talker为空或null，无法判断群聊/私聊类型")
            println("stringMap内容: $stringMap")
            return false
        }
        
        // 判断是群聊还是私聊
        val isGroupChat = talker.contains("@chatroom")
        println("群聊判断详情: '$talker'.contains('@chatroom') = $isGroupChat")
        
        if (isGroupChat) {
            println("这是群聊红包")
            // 这是群聊红包，检查群聊过滤
            println("检查群聊是否在过滤列表中...")
            println("群聊过滤列表内容: '${config.selectfilter}'")
            if (isGroupInFilterList(talker, title)) {
                println("群聊红包被过滤，跳过：$talker")
                return false
            } else {
                println("群聊不在过滤列表中，处理群聊红包")
            }
        } else {
            println("这是私聊红包")
            // 这是私聊红包
            if (config.privates) {
                println("111私聊红包过滤开启，跳过私聊红包：$talker")
                return false
            } else {
                println("私聊红包过滤关闭，处理私聊红包")
            }
        }
        
        if (config.sound) {
            PlaySoundUtils.Play()
        }
        if (config.push) {
//                                        EventBus.getDefault().post(new MessageEvent("天降红包"));
        }
        println("接收标题---------->$title")
        if (title.contains("CDATA")) {
            title =
                title.split("CDATA\\[".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        }
        try {
            if (Hanzi2PinyinHelper.Hanzi2Pinyin(title).contains("gua") ||
                title.contains("圭") ||
                title.contains("G") ||
                title.contains("GUA") ||
                title.contains("gua") ||
                title.contains("g")
            ) {
                println("检测到口令红包，跳过")
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(e)
        }
        println("=== 通过所有检查，准备抢红包 ===")
        return true
    }

    // 处理红包消息
    @Throws(Exception::class)
    private fun handleLuckyMoney(contentValues: ContentValues, lpparam: ClassLoader) {
        // 获得发送人
        val talker = contentValues.getAsString("talker")
        var content = contentValues.getAsString("content")
        if (!content.startsWith("<msg")) {
            content = content.substring(content.indexOf("<msg"))
        }
        val wcpayinfo: XmlToJson = XmlToJson.Builder(content).build()
        println("红包---------->$content")
        try {
            bean = gson.fromJson<MsgsBean>(wcpayinfo.toFormattedString(""), MsgsBean::class.java)
            nativeUrlString = bean.msg.appmsg.wcpayinfo.nativeurl
            cropname = bean.msg.appmsg.wcpayinfo.corpname
        } catch (e: JsonSyntaxException) {
            dBean = gson.fromJson<DBean>(wcpayinfo.toFormattedString(""), DBean::class.java)
            nativeUrlString = dBean.msg.appmsg.wcpayinfo.nativeurl
            cropname = ""
        }
        println("nativeurl: $nativeUrlString\n")
        println("cropname: $cropname\n")
        if (config.sleep) {
            Handler().postDelayed({
                // 启动红包页面
                if (launcherUiActivity != null) {
                    println("call method com.tencent.mm.br.d, start LuckyMoneyReceiveUI" + "\n")
                    val paramau = Intent()
                    paramau.putExtra("key_way", 1)
                    paramau.putExtra("key_native_url", nativeUrlString)
                    paramau.putExtra("key_username", talker)
                    paramau.putExtra("key_cropname", cropname) //7.0新增
                    println("界面1：" + AcxiliaryServiceStaticValues.handleLuckyMoney)
                    XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass(
                            AcxiliaryServiceStaticValues.handleLuckyMoney,
                            lpparam
                        ),
                        AcxiliaryServiceStaticValues.handleLuckyMoneyMethod,
                        launcherUiActivity,
                        "luckymoney",
                        AcxiliaryServiceStaticValues.handleLuckyMoneyClass,
                        paramau
                    )
                } else {
                    println("launcherUiActivity == null" + "\n")
                }
            }, config.sleeptime.toLong())
        } else {
            // 启动红包页面
            if (launcherUiActivity != null) {
                println("call method com.tencent.mm.br.d, start LuckyMoneyReceiveUI" + "\n")
                val paramau = Intent()
                paramau.putExtra("key_way", 1)
                paramau.putExtra("key_native_url", nativeUrlString)
                paramau.putExtra("key_username", talker)
                paramau.putExtra("key_cropname", cropname) //7.0新增
                println("界面2：" + AcxiliaryServiceStaticValues.handleLuckyMoney)
                XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass(AcxiliaryServiceStaticValues.handleLuckyMoney, lpparam),
                    AcxiliaryServiceStaticValues.handleLuckyMoneyMethod,
                    launcherUiActivity,
                    "luckymoney",
                    AcxiliaryServiceStaticValues.handleLuckyMoneyClass,
                    paramau
                )
            } else {
                println("launcherUiActivity == null" + "\n")
            }
        }
    }

    companion object {
        private var launcherUiActivity: Activity? = null
        private const val TAG = "RedHook"

        @JvmStatic
        val instance: RedHook
            get() = RedHookHolder.instance
    }
    
    /**
     * 检查群聊是否在过滤列表中
     */
    private fun isGroupInFilterList(talker: String, content: String): Boolean {
        println("=== 检查群聊过滤开始 ===")
        println("talker: $talker")
        
        // 首先检查群ID过滤列表
        if (isGroupInIdFilterList(talker)) {
            println("群聊在群ID过滤列表中: $talker")
            return true
        }
        
        // 然后检查原有的群聊过滤列表
        println("config.selectfilter: ${config.selectfilter}")
        
        if (config.selectfilter.isEmpty() || config.selectfilter.isBlank()) {
            println("群聊过滤列表为空或空白，不过滤任何群聊")
            return false
        }
        
        try {
            val jsonArray: JsonArray = parser.parse(config.selectfilter).asJsonArray
            
            if (jsonArray.size() == 0) {
                println("群聊过滤列表解析后为空，不过滤任何群聊")
                return false
            }
            
            println("解析到 ${jsonArray.size()} 个过滤项")
            
            for (i in 0 until jsonArray.size()) {
                try {
                    val element = jsonArray.get(i)
                    if (element == null || element.isJsonNull) {
                        println("跳过空的过滤项 $i")
                        continue
                    }
                    
                    val filterItem = gson.fromJson(element, FilterSaveBean::class.java)
                    if (filterItem == null || filterItem.name == null) {
                        println("跳过无效的过滤项 $i")
                        continue
                    }
                    
                    println("检查过滤项 $i: name='${filterItem.name}', displayname='${filterItem.displayname}'")
                    
                    // 简化逻辑：直接使用群ID匹配
                    val filterGroupId = filterItem.name.trim()
                    
                    // 精确匹配群ID
                    if (filterGroupId == talker) {
                        println("群聊在过滤列表中(群ID精确匹配): $talker")
                        return true
                    }
                    
                    // 如果过滤项不包含@chatroom，自动添加后再匹配
                    if (!filterGroupId.contains("@chatroom")) {
                        val fullGroupId = "$filterGroupId@chatroom"
                        if (fullGroupId == talker) {
                            println("群聊在过滤列表中(补全@chatroom后匹配): $talker")
                            return true
                        }
                    }
                    
                } catch (e: Exception) {
                    println("处理过滤项 $i 时出错: $e")
                    e.printStackTrace()
                    // 继续处理下一个项目
                }
            }
            
            println("群聊不在过滤列表中: $talker")
            return false
            
        } catch (e: Exception) {
            println("检查群聊过滤列表时出错: $e")
            e.printStackTrace()
            // 出错时不过滤，允许红包被抢
            println("出错时不过滤，允许抢红包")
            return false
        }
    }
    
    /**
     * 检查群聊是否在群ID过滤列表中
     */
    private fun isGroupInIdFilterList(talker: String): Boolean {
        println("=== 检查群ID过滤开始 ===")
        println("talker: $talker")
        println("config.groupIdFilterEnabled: ${config.groupIdFilterEnabled}")
        
        if (config.groupIdFilterEnabled.isEmpty() || config.groupIdFilterEnabled.isBlank()) {
            println("群ID过滤列表为空或空白，不过滤任何群聊")
            return false
        }
        
        try {
            val jsonArray: JsonArray = parser.parse(config.groupIdFilterEnabled).asJsonArray
            
            if (jsonArray.size() == 0) {
                println("群ID过滤列表解析后为空，不过滤任何群聊")
                return false
            }
            
            println("解析到 ${jsonArray.size()} 个群ID过滤项")
            
            for (i in 0 until jsonArray.size()) {
                try {
                    val element = jsonArray.get(i)
                    if (element == null || element.isJsonNull) {
                        println("跳过空的群ID过滤项 $i")
                        continue
                    }
                    
                    val filterItem = gson.fromJson(element, com.hxs.xposedreddevil.model.GroupIdFilterBean::class.java)
                    if (filterItem == null || filterItem.groupId.isEmpty()) {
                        println("跳过无效的群ID过滤项 $i")
                        continue
                    }
                    
                    println("检查群ID过滤项 $i: groupId='${filterItem.groupId}', groupName='${filterItem.groupName}', enabled=${filterItem.isEnabled}")
                    
                    // 只检查启用的过滤项
                    if (!filterItem.isEnabled) {
                        println("跳过未启用的群ID过滤项 $i")
                        continue
                    }
                    
                    // 使用GroupIdFilterBean的matches方法进行匹配
                    if (filterItem.matches(talker)) {
                        println("群聊在群ID过滤列表中: $talker 匹配 ${filterItem.groupId}")
                        return true
                    }
                    
                } catch (e: Exception) {
                    println("处理群ID过滤项 $i 时出错: $e")
                    e.printStackTrace()
                    // 继续处理下一个项目
                }
            }
            
            println("群聊不在群ID过滤列表中: $talker")
            return false
            
        } catch (e: Exception) {
            println("检查群ID过滤列表时出错: $e")
            e.printStackTrace()
            // 出错时不过滤，允许红包被抢
            println("出错时不过滤，允许抢红包")
            return false
        }
    }
}
