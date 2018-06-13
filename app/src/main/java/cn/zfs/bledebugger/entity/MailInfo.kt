package cn.zfs.bledebugger.entity

import java.util.*


/**
 * 描述:
 * 时间: 2018/6/13 23:03
 * 作者: zengfansheng
 */
class MailInfo {
    var mailServerHost = ""// 发送邮件的服务器的IP
    var mailServerPort = ""// 发送邮件的服务器的端口
    var fromAddress = ""// 邮件发送者的地址
    var toAddress = ""// 邮件接收者的地址
    var userName = ""// 登陆邮件发送服务器的用户名
    var password = ""// 登陆邮件发送服务器的密码
    var needValidate = true// 是否需要身份验证
    var subject = ""// 邮件主题
    var content = ""// 邮件的文本内容

    /**
     * 获得邮件会话属性
     */
    fun getProperties(): Properties {
        val p = Properties()
        p["mail.host"] = mailServerHost
        p["mail.transport.protocol"] = "smtp"
        if (!mailServerPort.isEmpty()) {
            p["mail.smtp.port"] = mailServerPort
        }
        p["mail.smtp.auth"] = if (needValidate) "true" else "false"
        return p
    }
}