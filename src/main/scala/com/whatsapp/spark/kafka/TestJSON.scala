package com.whatsapp.spark.kafka

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.catalyst.dsl.expressions.{DslExpression, StringToAttributeConversionHelper}
import org.apache.spark.sql.functions.col


case class Employee(name : String)

object TestJSON {

  def main(args: Array[String]): Unit = {

    val spark: SparkSession = SparkSession.builder().master("local").appName("Test JSON").getOrCreate()
    import spark.implicits._

    val data = spark.read.json("file:///Users//dpanda//IdeaProjects//Spark_Kafka//src/main//resources//data2.json")

    val messageData : DataFrame = data.select("message").filter(col("message") =!= "" && col("message") =!= "[\"empty_data\"]")

    messageData.printSchema()

   val nameDF = messageData.map(value => {
      val nameData : String = value.toString().split(":")(1).replace("}]]", "").replaceAll("^\"|\"$", "")
      Employee(nameData)
    })

   nameDF.show(false)

  }
}
