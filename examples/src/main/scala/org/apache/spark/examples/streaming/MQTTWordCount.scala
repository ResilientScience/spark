/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.examples.streaming

import org.eclipse.paho.client.mqttv3.{MqttClient, MqttClientPersistence, MqttException, MqttMessage, MqttTopic}
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.mqtt._
import org.apache.spark.SparkConf

/**
 * A simple Mqtt publisher for demonstration purposes, repeatedly publishes
 * Space separated String Message "hello mqtt demo for spark streaming"
 */
object MQTTPublisher {

  var client: MqttClient = _

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: MQTTPublisher <MqttBrokerUrl> <topic>")
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()

    val Seq(brokerUrl, topic) = args.toSeq

    try {
      var peristance:MqttClientPersistence =new MqttDefaultFilePersistence("/tmp")
      client = new MqttClient(brokerUrl, MqttClient.generateClientId(), peristance)
    } catch {
      case e: MqttException => println("Exception Caught: " + e)
    }

    client.connect()

    val msgtopic: MqttTopic = client.getTopic(topic)
    val msg: String = "hello mqtt demo for spark streaming"

    while (true) {
      val message: MqttMessage = new MqttMessage(String.valueOf(msg).getBytes("utf-8"))
      msgtopic.publish(message)
      println("Published data. topic: " + msgtopic.getName() + " Message: " + message)
    }
   client.disconnect()
  }
}

/**
 * A sample wordcount with MqttStream stream
 *
 * To work with Mqtt, Mqtt Message broker/server required.
 * Mosquitto (http://mosquitto.org/) is an open source Mqtt Broker
 * In ubuntu mosquitto can be installed using the command  `$ sudo apt-get install mosquitto`
 * Eclipse paho project provides Java library for Mqtt Client http://www.eclipse.org/paho/
 * Example Java code for Mqtt Publisher and Subscriber can be found here
 * https://bitbucket.org/mkjinesh/mqttclient
 * Usage: MQTTWordCount <MqttbrokerUrl> <topic>
 *   <MqttbrokerUrl> and <topic> describe where Mqtt publisher is running.
 *
 * To run this example locally, you may run publisher as
 *    `$ bin/run-example \
 *      org.apache.spark.examples.streaming.MQTTPublisher tcp://localhost:1883 foo`
 * and run the example as
 *    `$ bin/run-example \
 *      org.apache.spark.examples.streaming.MQTTWordCount tcp://localhost:1883 foo`
 */
object MQTTWordCount {

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(
        "Usage: MQTTWordCount <MqttbrokerUrl> <topic>")
      System.exit(1)
    }

    val Seq(brokerUrl, topic) = args.toSeq
    val sparkConf = new SparkConf().setAppName("MQTTWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    val lines = MQTTUtils.createStream(ssc, brokerUrl, topic, StorageLevel.MEMORY_ONLY_SER_2)

    val words = lines.flatMap(x => x.toString.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordCounts.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
