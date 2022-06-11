package com.whatsapp.spark.kafka

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.types.{StringType, StructType}
import org.apache.spark.sql.functions.{col, from_json, window}
case class Employee_Cloudera(name : String)

object Spark_Kafka_WhatsApp {

  private val whatsapp: SendWhatsApp = new SendWhatsApp()

  private def sendtoWhatsapp(batchDF : Dataset[Employee_Cloudera], batchId : Long): Unit = {
    batchDF.show()
    batchDF.collect().foreach(employee => {
      whatsapp.sendmessage( "Hello  " + employee.name)
    })
  }

  def main(args: Array[String]): Unit = {

  /*  if(args.length > 2 ) {
      System.err.println("Usage : SparkKafkaIntegrationApp <KAFKA_BOOTSTRAP_SERVERS> <KAFKA_TOPIC_NAME>");
      System.exit(0);
    } */

    val appName = "Spark Kafka Integration"

    whatsapp.init()

    // Creating the SparkSession object
    val spark: SparkSession = SparkSession.builder().master("local").appName(appName).getOrCreate()
    import spark.implicits._
    // logger.info("SparkSession created successfully")

    val kafkaBootstrapServers = "c4648-node2.coelab.cloudera.com:9092,c4648-node3.coelab.cloudera.com:9092,c4648-node4.coelab.cloudera.com:9092" //args(0)
    val inputTopicNames = "test_topic" //args(1)

    val schema = new StructType()
      .add("message", StringType, true)
      .add("@timestamp", StringType, true)
      .add("@version", StringType, true)


    val inputDf = spark.
      readStream.
      format("kafka").
      option("kafka.bootstrap.servers", kafkaBootstrapServers).
      option("subscribe", inputTopicNames).
      option("startingOffsets", "latest").
      option("kafka.security.protocol","PLAINTEXT").
      load().selectExpr("CAST(value AS STRING)").as[String]

    inputDf.printSchema()

    val dfJSON = inputDf.withColumn("jsonData",from_json(col("value"),schema)).select("jsonData.*")

    val messageData : DataFrame = dfJSON.select("message").filter(col("message") =!= "" && col("message") =!= "[\"empty_data\"]")

    messageData.printSchema()

    val nameDF = messageData.map(value => {
      val nameData : String = value.toString().split(":")(1).replace("}]]", "").replaceAll("^\"|\"$", "")
      Employee_Cloudera(nameData)
    })

    nameDF.printSchema()

    val outputDF = nameDF.writeStream.foreachBatch((batchDF: Dataset[Employee_Cloudera], batchId : Long) => sendtoWhatsapp(batchDF, batchId))
      .outputMode("append")

    outputDF.start().awaitTermination()

  }
}
