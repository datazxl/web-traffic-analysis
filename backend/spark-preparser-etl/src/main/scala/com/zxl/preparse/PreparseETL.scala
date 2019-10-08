package com.zxl.preparse

import com.zxl.preparser.{PreParsedLog, WebLogPreParser}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}

/**
export HADOOP_CONF_DIR=/home/hadoop-zxl/apps/hadoop-2.7.5/etc/hadoop

spark-submit --class com.zxl.preparse.PreparseETL \
--master yarn \
--driver-memory 512m \
--executor-memory 512m \
--executor-cores 1 \
--num-executors 2 \
--conf spark.traffic.analysis.rawdata.input=hdfs://master:9000/user/hadoop-zxl/traffic-analysis/rawlog/20180616 \
 /home/hadoop-zxl/course/web-traffic-analysis/jars/spark-preparser-etl-1.0-SNAPSHOT-jar-with-dependencies.jar prod
  * 功能：完成对原始访问数据的预解析，并入库到Hive中
  */
object PreparseETL {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    if (args.isEmpty) {
      conf.setMaster("local")
    }
    val spark: SparkSession = SparkSession.builder().appName("PreparseETL")
      .enableHiveSupport()
      .config(conf)
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
      .partitionBy("year", "month", "day")
      .saveAsTable("rawdata.web")
    spark.stop()
  }
}