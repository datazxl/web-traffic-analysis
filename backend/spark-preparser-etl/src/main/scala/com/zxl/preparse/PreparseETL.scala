package com.zxl.preparse

import com.zxl.preparser.{PreParsedLog, WebLogPreParser}
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}

/**
  * 功能：完成对原始访问数据的预解析，并入库到Hive中
  */
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