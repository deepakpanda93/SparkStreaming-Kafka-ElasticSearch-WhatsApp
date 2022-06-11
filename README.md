
# Spark_Elastic_WhatsApp Example

Extract User input data from WEB API and send them to WhatsApp with Spark-Kafka Streaming



## Usage/Examples

1) Create a Kafka topic.

```bash
kafka-topics --bootstrap-server [HOST1:PORT1] --create --topic [TOPIC] --partitions <no_of_partitions> --replication-factor <replication_factor>
```

2) Create a Logstash config file to hit the WEB API every second and retrieve the user input data.

```javascript
## cat api_kafka_logstash.conf
input {
  http_poller {
    urls => {
      urlname => "https://test.ekhool.com/checking/user_fetch"
    }
    request_timeout => 60
    schedule => { every => "1s" }
    codec => "line"
  }
}

output {
elasticsearch{
 hosts => ["<elasticSearch_server>:9200"]
 index => "<index_name>"
}
kafka {
    bootstrap_servers => "<bootstrap_servers>:9092"
    codec => json
    topic_id => "<topic_name>"
}
  stdout {
    codec => rubydebug
  }
}
```

3) Create a Spark Streaming Code file.

```javascript
## cat Spark_Kafka_WhatsApp.scala

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
      System.err.println("Usage : SparkElasticWhatsappIntegrationApp <KAFKA_BOOTSTRAP_SERVERS> <KAFKA_TOPIC_NAME>");
      System.exit(0);
    } */

    val appName = "Spark Elastic Whatsapp Integration"

    whatsapp.init()

    // Creating the SparkSession object
    val spark: SparkSession = SparkSession.builder().master("local").appName(appName).getOrCreate()
    import spark.implicits._

    val kafkaBootstrapServers = "bootstrap_server" //args(0)
    val inputTopicNames = "topic_name" //args(1)

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

```

4) Create a Java code file to send to whatsapp using Twilio API.

```javascript
## cat SendWhatsApp.java

package com.whatsapp.spark.kafka;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SendWhatsApp {
   public void sendmessage(String yourmessage) {
        Message message = Message.creator(
                new com.twilio.type.PhoneNumber("whatsapp:+YOUR_WHATSAPP_PHONE_NUMBER"),
                new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                yourmessage)
                .create();

        System.out.println(message.getSid());
    }

    public void init() {
        String ACCOUNT_SID = "TWILIO_ACCOUNT_SID";
        String AUTH_TOKEN =  "TWILIO_AUTH_TOKEN";

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }
}

```

5) Prepare a pom.xml file for your project

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>Spark_Kafka</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <scala.version>2.11.8</scala.version>
        <spark.version>2.4.7</spark.version>
        <jackson.version>2.11.0</jackson.version>
        <spark.scope>provided</spark.scope>
    </properties>

    <!-- Developers -->
    <developers>
        <developer>
            <id>deepakpanda93</id>
            <name>Deepak Panda</name>
            <email>deepakpanda93@gmail.com</email>
            <url>https://github.com/deepakpanda93</url>
        </developer>
    </developers>

    <dependencies>
        <dependency>
            <groupId>org.scala-lang</groupId>
            <artifactId>scala-library</artifactId>
            <version>${scala.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.11</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql_2.11</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql-kafka-0-10_2.11</artifactId>
            <version>${spark.version}</version>
        </dependency>
        <dependency>
            <groupId>com.twilio.sdk</groupId>
            <artifactId>twilio</artifactId>
            <version>8.9.0</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
            <version>2.12.1</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>2.12.1</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.12.1</version>
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.12.1</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.module</groupId>
            <artifactId>jackson-module-scala_2.11</artifactId>
            <version>2.12.1</version>
        </dependency>
    </dependencies>
</project>
```



## Run Locally

Start the logstash

```bash
  /usr/share/logstash/bin/logstash -f api_kafka_logstash.conf
```

Start the spark-kafka streaming

```bash
  I used Intelij to run the project
```

Send some data from the URL : https://test.ekhool.com/checking/user_input

Check your whatsapp AND BOOM !!!!




## Demo

To be uploaded


## Screenshots

![App Screenshot](https://via.placeholder.com/468x300?text=App+Screenshot+Here)


## Tech Stack

** Logstash, Kafka, Spark Structured Streaming **


## 🚀 About Me

## Hi, I'm Deepak! 👋

I'm a Big Data Engineer...




## 🔗 Links
[![portfolio](https://img.shields.io/badge/my_portfolio-000?style=for-the-badge&logo=ko-fi&logoColor=white)](https://github.com/deepakpanda93)
[![linkedin](https://img.shields.io/badge/linkedin-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/deepakpanda93)

