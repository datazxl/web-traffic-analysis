package com.zxl.web

import com.zxl.parser.dataobject.BaseDataObject
import com.zxl.preparser.PreParsedLog
import com.zxl.spark.web.CombinedId
import com.zxl.web.external.{HBaseSnapshotAdmin, HbaseConnectionFactory}
import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.{Encoders, Row, SparkSession}
import org.apache.spark.{HashPartitioner, SparkConf}

/**
spark-submit --class com.zxl.web.WebETL \
  --master spark://master:7077 \
  --driver-memory 512m \
  --executor-memory 1g \
  --executor-cores 1 \
  --total-executor-cores 1 \
  --conf spark.web.etl.inputBaseDir=hdfs://master:9000/user/hive/warehouse/rawdata.db/web \
  --conf spark.web.etl.outputBaseDir=hdfs://master:9000/user/hadoop-zxl/traffic-analysis/web \
  --conf spark.web.etl.startDate=20180616 \
  --conf spark.driver.extraJavaOptions="-Dweb.metadata.mongodbAddr=192.168.43.169 -Dweb.etl.hbase.zk.quorums=master" \
  --conf spark.executor.extraJavaOptions="-Dweb.metadata.mongodbAddr=192.168.43.169 -Dweb.etl.hbase.zk.quorums=master
-Dcom.sun.management.jmxremote.port=1119 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
  /home/hadoop-zxl/course/web-traffic-analysis/jars/spark-session-etl-1.0-SNAPSHOT-jar-with-dependencies.jar prod
  * 网站流量离线分析Spark ETL入口
  */
object WebETL {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf
    if (args.isEmpty) {
      conf.setMaster("local")
    }
    //使用kryo序列化，并注册使用Kryo的类
    conf.set("spark.serializer", classOf[KryoSerializer].getName)
    conf.set("spark.kryo.registrator", classOf[WebRegistrator].getName)

    //预处理输出的基本路径
    val inputBaseDir: String = conf.get("spark.web.etl.inputBaseDir",
      "hdfs://master:9000/user/hive/warehouse/rawdata.db/web")
    //ETL的输出路径
    val outputBaseDir: String = conf.get("spark.web.etl.outputBaseDir",
      "hdfs://master:9000/user/hadoop-zxl/traffic-analysis/web")
    //处理的日志的时间
    val dateStr: String = conf.get("spark.web.etl.startDate", "20180615")
    //分区数
    val numberPartitions: Int = conf.getInt("spark.web.etl.numberPartitions", 5)

    conf.setAppName(s"WebETL-${dateStr}");
    val spark: SparkSession = SparkSession.builder().config(conf).getOrCreate()

    //预处理输出的具体路径
    val preParsedLogPath = s"${inputBaseDir}/year=${dateStr.substring(0, 4)}/month=${dateStr.substring(0, 6)}/day=${dateStr}"

    val parsedLogRDD: RDD[(CombinedId, BaseDataObject)] = spark.read.parquet(preParsedLogPath)
      //将DataFrame转成Dataset[PreParsedLog]
      .map(transform(_))(Encoders.bean(classOf[PreParsedLog]))
      //将Dataset[PreParsedLog]转成RDD[(CombinedId, BaseDataObject)]
      .flatMap(WebLogParser.parse(_))(Encoders.bean(classOf[(CombinedId, BaseDataObject)])).rdd

    /**
      * WebLogParser.parse转换后数据为：
      * (CombinedId(profileId1,user1), BaseDataObject(profileId1,user1,pv,client_ip.....))
      * (CombinedId(profileId1,user1), BaseDataObject(profileId1,user1,mc,client_ip.....))
      * (CombinedId(profileId2,user2), BaseDataObject(profileId2,user2,pv,client_ip.....))
      * ............
      * (CombinedId(profileId3,user3), BaseDataObject(profileId3,user3,ev,client_ip.....))
      * (CombinedId(profileIdn,usern), BaseDataObject(profileIdn,usern,pv,client_ip.....))
      */
    //将parsedLogRDD按照key进行分组，将相同访客的dataObject聚合在一起
    //没有解决数据倾斜（一个访客一天内的访问行为数据不会超级多。），如果一个用户数据量过多，那么前面解析时过滤掉该用户数据
    parsedLogRDD.groupByKey(new HashPartitioner(numberPartitions)).mapPartitionsWithIndex((index, iter) => {
      //groupByKey后的每一个分区的数据为：
      /**
        * 转换后数据为：
        * (CombinedId(profileId1,user1), List(BaseDataObject(profileId1,user1,pv,client_ip.....),
        * BaseDataObject(profileId1,user1,mc,client_ip.....)))
        * (CombinedId(profileId2,user2), List(BaseDataObject(profileId2,user2,pv,client_ip.....),
        * BaseDataObject(profileId2,user2,mc,client_ip.....),
        * BaseDataObject(profileId2,user2,ev,client_ip.....)
        * BaseDataObject(profileId2,user2,hb,client_ip.....)))
        * ............
        * (CombinedId(profileId3,user3), List(BaseDataObject(profileId3,user3,ev,client_ip.....)))
        * (CombinedId(profileIdn,usern), List(BaseDataObject(profileIdn,usern,pv,client_ip.....),
        * BaseDataObject(profileIdn,usern,mc,client_ip.....),
        * BaseDataObject(profileIdn,usern,pv,client_ip.....)))
        */
      //处理每一个分区的数据
      val partitionProcessor = new PartitionProcessor(index, iter, outputBaseDir, dateStr)
      partitionProcessor.run()
      Iterator[Unit]()
    }).foreach((_:Unit) => {})

    //给HBase的web-user表创建snapshot，以便数据的重跑
    val snapshotAdmin = new HBaseSnapshotAdmin(HbaseConnectionFactory.getHbaseConn)
    val tableName: String = System.getProperty("web.etl.hbase.UserTableName", "web-user")
    snapshotAdmin.takeSnapshot(s"${tableName}-${dateStr}",tableName);

    spark.stop()
  }

  private def transform(row: Row): PreParsedLog = {
    val p = new PreParsedLog
    p.setClientIp(row.getAs[String]("clientIp"))
    p.setCommand(row.getAs[String]("command"))
    p.setMethod(row.getAs[String]("method"))
    p.setProfileId(row.getAs[Int]("profileId"))
    p.setQueryString(row.getAs[String]("queryString"))
    p.setServerIp(row.getAs[String]("serverIp"))
    p.setServerPort(row.getAs[Int]("serverPort"))
    p.setServerTime(row.getAs[String]("serverTime"))
    p.setUriStem(row.getAs[String]("uriStem"))
    p.setUserAgent(row.getAs[String]("userAgent"))
    p
  }
}