
package javax.bis

//import org.apache.activemq.ActiveMQConnectionFactory
//import javax.jms.DeliveryMode._
import JMS.AllImplicits._
import javax.jms._
import com.typesafe.config.ConfigFactory
import java.io.File
import java.util.UUID

import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.io.StringReader
import java.io.StringWriter

//import scala.io.Source
import progress.message.jclient.ConnectionFactory
import scala.xml._

object testJmsObj {
  def main(args: Array[String]): Unit = {
    println("testJmsObj main start")

    val confFileName = System.getProperty("confFile") match {
      case null =>
        println(s"-DconfFile param is empty. Used default file 'testJmsObj.conf'")
        "testJmsObj.conf"
      case everythingElse =>
        println(s"Config file (-DconfFile param): $everythingElse")
        everythingElse
    }

    val config = ConfigFactory.parseFile(new File(confFileName))

    val brokerSettings = config.getConfig("broker")
    val brokerTcp = brokerSettings.getString("tcp")
    val brokerLogin = brokerSettings.getString("login")
    val brokerPass = brokerSettings.getString("password")

    val queueSettings = config.getConfig("queue")
    val queueRequest = queueSettings.getString("request")
    val queueResponse = queueSettings.getString("response")

    val messageSettings = config.getConfig("message")
    val messageType = messageSettings.getString("type")
    val messageTimeToLive = messageSettings.getInt("timetolive")
    val messagePriority = messageSettings.getInt("priority")
    val messageDeliveryMode = messageSettings.getInt("deliverymode") /*NON_PERSISTENT = 1, PERSISTENT = 2*/
    val messagePurge = messageSettings.getBoolean("purge")
    val messageFile = messageSettings.getString("file")
    val messageText = messageSettings.getString("text")
    val messageTimeOut = messageSettings.getInt("timeout")
    val messageRespFile = messageSettings.getString("respfile")

    println(s"Host: $brokerTcp")
    println(s"Login: $brokerLogin")
    println(s"Password: $brokerPass")

    val connectionFactory = new ConnectionFactory

    connectionFactory.setBrokerURL(brokerTcp)
    connectionFactory.setDefaultUser(brokerLogin)
    connectionFactory.setDefaultPassword(brokerPass)

    val connection = connectionFactory.createConnection()
    connection.start()

    val session = {
      connection.session(transacted = false, Session.AUTO_ACKNOWLEDGE)
    }

    println(s"Request Queue: $queueRequest")
    val q=session.queue(queueRequest)

    println(s"Response Queue: $queueResponse")
    val respQueue = session.queue(queueResponse)

    val messageConsumer = q.consumer

    // clean up queue, consuming all messages
    println(s"Purge: $messagePurge")
    if (messagePurge)
      messageConsumer.purge()

    // send a couple of text messages
    println(s"TimeToLive: $messageTimeToLive")
    println(s"Priority: $messagePriority")
    println(s"DeliveryMode: $messageDeliveryMode")
    val prod = q.producer.deliveryMode(messageDeliveryMode)
    prod.setTimeToLive(messageTimeToLive)
    prod.setPriority(messagePriority)

    var msgContents = ""
    println(s"MessageType: $messageType")
    messageType match {
      case "text" =>
        println(s"Text: $messageText")
        msgContents = messageText
      case "file" =>
        println(s"File: $messageFile")
        val fileContents = XML.loadFile(messageFile)
        msgContents = Utility.trim(fileContents).toString
    }

    val msg = prod.create(msgContents)
    msg.setJMSReplyTo(respQueue)

    val msgCorrUID = UUID.randomUUID().toString
    println(s"Correlation ID: $msgCorrUID")
    msg.setJMSCorrelationID(msgCorrUID)

    // Send message
    prod.send(msg)
    println(s"Message ID: ${msg.getJMSMessageID}")

    prod.close()

    val respConsumer=respQueue.consumer(s"JMSCorrelationID='$msgCorrUID'")
    println(s"Message Timeout: $messageTimeOut")
    val respContents = respConsumer.receiveText(messageTimeOut)
    respConsumer.closeMe()

    messageType match {
      case "text" =>
        println(s"Response: $respContents")
      case "file" =>
        println(s"Response File: $messageRespFile")
        val fileContents = prettyFormat(respContents)
        Files.write(Paths.get(messageRespFile), fileContents.getBytes(StandardCharsets.UTF_8))
    }

    // send a map message with a couple of properties
    /*prod.sendMapWith(Map("one" -> 1, "two" -> 2))
    {_.propertiesMap(Map("someprop" -> "hello", "anotherprop" -> "Goodbye"))}*/

    // show the 3 current messages on the queue
    /*val messages=respQueue.browser().messages
    println("messages" + messages.mkString("\n"))
    println(messages.map{
      case mess:TextMessage=>mess.asText.substring(0,80)
      case mess:MapMessage=>mess.asMap
    }.map("BROWSE "+_).mkString("\n"))*/
    // consume and print a the first 2 messages
    /*println(messageConsumer.receiveText)
    println(messageConsumer.receiveText)*/

    // we don't get map directly, but get message
    // so that we can get both map and propertiesMap
    /*val message = messageConsumer.receive()

    println(message.asMap)
    println(message.propertiesMap)

    // create own internal queue, send text message
    val tq=session.temporaryQueue()
    tq.producer.send("A temp message").closeMe()
    // get the text back
    val tqConsumer=tq.consumer
    println("TemporaryQueue: "+tqConsumer.receiveText)
    tqConsumer.closeMe()*/

    connection.close()
    println("testJmsObj main finish")
  }


  def prettyFormat(input: String, indent: Int = 2): String = try {
    val xmlInput = new StreamSource(new StringReader(input))
    val stringWriter = new StringWriter
    val xmlOutput = new StreamResult(stringWriter)
    val transformerFactory = TransformerFactory.newInstance
    transformerFactory.setAttribute("indent-number", indent)
    val transformer = transformerFactory.newTransformer
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.transform(xmlInput, xmlOutput)
    xmlOutput.getWriter.toString
  } catch {
    case e: Exception =>
      throw new RuntimeException(e) // simple exception handling, please review it

  }
}
