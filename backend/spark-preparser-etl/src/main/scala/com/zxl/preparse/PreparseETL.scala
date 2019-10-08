package com.zxl.preparse

import com.zxl.preparser.{PreParsedLog, WebLogPreParser}
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}


object PreparseETL {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession.builder().appName("PreparseETL")
      .enableHiveSupport()
      .getOrCreate()

    val rawdataInputPath: String = spark.conf.get("spark.traffic.analysis.rawdata.input",
      "hdfs://master:9000/user/hadoop-zxl/traffic-analysis/rawlog/20180615")

    val numberPartitions: Int = spark.conf.get("spark.traffic.analysis.rawdata.numberPartitions",
      "2").toInt

    spark.read.textFile(rawdataInputPath)
      .flatMap(line => Option(WebLogPreParser.parse(line)))(Encoders.bean(classOf[PreParsedLog]))
      .coalesce(numberPartitions)
      .write
      .mode(SaveMode.Append)
      .partitionBy("year","month","day")
      .saveAsTable("rawdata.web")

    spark.stop()
  }
}
