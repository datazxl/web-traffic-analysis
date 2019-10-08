package com.zxl.web

import com.zxl.parser.dataobject.BaseDataObject
import com.zxl.spark.web.{AvroOutputComponent, CombinedId}
import com.zxl.web.external.{HBaseUserVisitInfoComponent, UserVisitInfo}
import com.zxl.web.session.{SessionGenerator, UserSessionDataAggregator}

/**
  * 每一个分区数据的处理者(分区级别)
  *
  * @param index              分区的index
  * @param usersIter 分区中的数据
  * @param outputBasePath     分区输出的路径
  * @param dateStr            处理的数据日期
  */
class PartitionProcessor(
                          index: Int,
                          usersIter: Iterator[(CombinedId, Iterable[BaseDataObject])],
                          outputBasePath: String,
                          dateStr: String)
  extends SessionGenerator with AvroOutputComponent with HBaseUserVisitInfoComponent{

  //一个user的开始的serverSessionId
  private var serverSessionIdStart = index.toLong + System.currentTimeMillis() * 1000

  def run() = {
    //循环处理该分区每一个user的DataObjects(分区级别)
    usersIter grouped(512) foreach (batch => {

      //分区中每512个用户是一个批次
      //查询该批次的所有用户历史访问信息
      val idToUserVisitInfo: Map[CombinedId, UserVisitInfo] = retrieveUsersVisitInfo(batch.map(_._1))

      val userVisitInfosBuilder = Vector.newBuilder[UserVisitInfo]
      batch foreach { case (profileUser, dataObjects) =>
        //用户级别
        //1、对一个user中的所有的DataObject按照时间进行升序排序
        val sortedObjectSeq = dataObjects.toSeq.sortBy(obj => obj.getServerTime.getTime)
        //2、会话切割
        val sessionDatas = getSessions(sortedObjectSeq)
        //3、对当前user产生的会话进行聚合计算(会话级别)
        val aggregator = new UserSessionDataAggregator(profileUser, serverSessionIdStart, idToUserVisitInfo.get(profileUser))
        val (currentUserVisitInfo, records) = aggregator.aggregate(sessionDatas)
        userVisitInfosBuilder += currentUserVisitInfo
        serverSessionIdStart += sessionDatas.size
        //4、将产生的记录写到HDFS中
        writeDataRecords(records, outputBasePath, dateStr, index)
      }

      //写出该批次的用户历史访问信息到HBase
      updateUsersVisitInfo(userVisitInfosBuilder.result())
    })
  }

}
