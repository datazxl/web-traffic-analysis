package com.zxl.web.external

import com.zxl.spark.web.CombinedId
import org.apache.hadoop.hbase.{Cell, CellUtil, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory

object HBaseUserVisitInfoComponent {
  private val logger = LoggerFactory.getLogger(classOf[HBaseUserVisitInfoComponent])
  private val hbaseTableNamespace = System.getProperty("web.etl.hbase.namespace", "default")
  private val targetUserTable = TableName.valueOf(hbaseTableNamespace, System.getProperty("web.etl.hbase.UserTableName", "web-user"))
  private val conn: Connection = HbaseConnectionFactory.getHbaseConn
  private val columnFamily = Bytes.toBytes("f")
  private val columnQualifier = Bytes.toBytes("v")
}

trait HBaseUserVisitInfoComponent extends UserVisitInfoComponent {

  import HBaseUserVisitInfoComponent._

  /**
    * 根据访客唯一标识查询访客的历史访问信息
    *
    * @param ids
    * @return
    */
  def retrieveUsersVisitInfo(ids: Seq[CombinedId]): Map[CombinedId, UserVisitInfo] = {
    //1.获取连接，得到操作的表
    val table: Table = conn.getTable(targetUserTable)
    //2.构建gets，得到多个用户的result
    val gets: Seq[Get] = ids map (id => new Get(Bytes.toBytes(id.encode)).addColumn(Bytes.toBytes("f"), Bytes.toBytes("v")))
    import scala.collection.JavaConversions._
    val results: Array[Result] = table.get(gets)
    //3.返回结果
    val resOpt: Seq[Option[(CombinedId, UserVisitInfo)]] = ids zip results map { case (id, result) => {
      if (result.isEmpty) {
        None
      } else {
        val cell: Cell = result.getColumnLatestCell(columnFamily, columnQualifier)
        val userVisitInfo = new UserVisitInfo(id, cell.getTimestamp, Bytes.toInt(CellUtil.cloneValue(cell)))
        Some(id -> userVisitInfo)
      }
    }}
    table.close()
    resOpt.flatten.toMap
  }

  /**
    * 更新访客的历史访问信息
    *
    * @param users
    */
  def updateUsersVisitInfo(users: Seq[UserVisitInfo]): Unit = {
    //1.获取连接，得到操作的表
    val table: Table = conn.getTable(targetUserTable)
    //2.构建puts
    val puts: Seq[Put] = users map (userVisitInfo => {
      val put = new Put(Bytes.toBytes(userVisitInfo.id.encode))
      put.addColumn(columnFamily, columnQualifier, userVisitInfo.lastVisitTime, Bytes.toBytes(userVisitInfo.lastVisitIndex))
    })
    //3.
    import scala.collection.JavaConversions._
    try {
      table.put(puts)
    } catch {
      case e: Exception => logger.error("Failed puts to hbase",e)
    }
    table.close()
  }
}