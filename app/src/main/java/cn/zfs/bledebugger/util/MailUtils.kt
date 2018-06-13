package cn.zfs.bledebugger.util

import cn.zfs.bledebugger.entity.MailInfo
import java.io.File
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Address
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.*


/**
 * 发送文本邮件
 */
fun sendTextMail(mailInfo: MailInfo): Boolean {
    // 根据邮件会话属性和密码验证器构造一个发送邮件的session
    val session = Session.getInstance(mailInfo.getProperties())
    return try {
        // 根据session创建一个邮件消息
        val message = MimeMessage(session)
        // 设置邮件消息的发送者
        message.setFrom(InternetAddress(mailInfo.fromAddress))
        // 设置邮件消息的主题
        message.subject = mailInfo.subject
        // 设置邮件消息发送的时间
        message.sentDate = Date()
        // 设置邮件消息的主要内容
        message.setText(mailInfo.content)
        val transport = session.transport
        // 连接邮件服务器  
        transport.connect(mailInfo.userName, mailInfo.password)
        // 发送邮件  
        transport.sendMessage(message, arrayOf<Address>(InternetAddress(mailInfo.fromAddress)))
        // 关闭连接  
        transport.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * 发送带附件的邮件
 */
fun sendFileMail(info: MailInfo, files: List<File>): Boolean {
    val session = Session.getInstance(info.getProperties())
    return try {
        val message = createAttachmentMail(info, session, files)
        val transport = session.transport
        // 连接邮件服务器  
        transport.connect(info.userName, info.password)
        // 发送邮件  
        transport.sendMessage(message, arrayOf<Address>(InternetAddress(info.fromAddress)))
        // 关闭连接  
        transport.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * 创建带有附件的邮件
 */
private fun createAttachmentMail(info: MailInfo, session: Session, files: List<File>): Message? {
    try {        
        val message = MimeMessage(session)
        //创建邮件发送者地址
        val from = InternetAddress(info.fromAddress)
        //设置邮件消息的发送者
        message.setFrom(from)
        //邮件标题
        message.subject = info.subject
        // 创建邮件正文，为了避免邮件正文中文乱码问题，需要使用CharSet=UTF-8指明字符编码
        val text = MimeBodyPart()
        text.setContent(info.content, "text/html;charset=UTF-8")
        // 创建容器描述数据关系
        val mp = MimeMultipart()
        mp.addBodyPart(text)
        files.forEach {
            // 创建邮件附件
            val attach = MimeBodyPart()
            val ds = FileDataSource(it)
            val dh = DataHandler(ds)
            attach.dataHandler = dh
            attach.fileName = MimeUtility.encodeText(dh.name)
            mp.addBodyPart(attach)
        }
        mp.setSubType("mixed")
        message.setContent(mp)
        message.saveChanges()
        return message
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}