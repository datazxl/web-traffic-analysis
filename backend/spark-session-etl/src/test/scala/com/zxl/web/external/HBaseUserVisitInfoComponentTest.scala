package com.zxl.web.external

import com.zxl.spark.web.CombinedId
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.scalatest.FunSuite

class HBaseUserVisitInfoComponentTest extends FunSuite with HBaseUserVisitInfoComponent {


  private val columnFamily = "f".getBytes("UTF-8")
  private val lastVisit = "v".getBytes("UTF-8")

  System.setProperty("web.etl.hbase.zk.quorums", "master")
  System.setProperty("web.etl.hbase.UserTableName", "web-user-test")
  val hbaseConn = HbaseConnectionFactory.getHbaseConn
  val admin = hbaseConn.getAdmin
  val tableName = TableName.valueOf("web-user-test")
  if (!admin.tableExists(tableName)) {
    val hTableDescriptor = new HTableDescriptor(tableName)
    hTableDescriptor.addFamily(new HColumnDescriptor("f"))
    admin.createTable(hTableDescriptor)
  }
  admin.disableTable(tableName)
  admin.truncateTable(tableName, true)
  val userTable = hbaseConn.getTable(tableName)

  val userVisitInfo1 = new UserVisitInfo(CombinedId(1, "user1"), 123, 2)
  val put1 = new Put(userVisitInfo1.id.encode.getBytes("utf-8"))
  put1.addColumn(columnFamily,lastVisit,
    userVisitInfo1.lastVisitTime, Bytes.toBytes(userVisitInfo1.lastVisitIndex))
  val userVisitInfo2 = new UserVisitInfo(CombinedId(2, "user2"), 1234, 4)
  val put2 = new Put(userVisitInfo2.id.encode.getBytes("utf-8"))
  put2.addColumn(columnFamily, lastVisit,
    userVisitInfo2.lastVisitTime, Bytes.toBytes(userVisitInfo2.lastVisitIndex))
  userTable.put(put1)
  userTable.put(put2)
  userTable.close()

  test("testRetrieveUsersVisitInfo") {
    val idToInfo: Map[CombinedId, UserVisitInfo] = retrieveUsersVisitInfo(Seq(CombinedId(1, "user1"),CombinedId(2, "user2")))
    assert(idToInfo.size == 2)
    val userVisitInfo1 = idToInfo.get(CombinedId(1, "user1")).get
    assert(userVisitInfo1.lastVisitTime == 123)
    assert(userVisitInfo1.lastVisitIndex == 2)
    val userVisitInfo2 = idToInfo.get(CombinedId(2, "user2")).get
    assert(userVisitInfo2.lastVisitTime == 1234)
    assert(userVisitInfo2.lastVisitIndex == 4)
  }

  test("testUpdateUsersVisitInfo") {
    val visitInfoes = Seq(new UserVisitInfo(CombinedId(3,"user3"),666,5),new UserVisitInfo(CombinedId(4,"user4"),999,6))
    updateUsersVisitInfo(visitInfoes)
    val idToInfo: Map[CombinedId, UserVisitInfo] = retrieveUsersVisitInfo(Seq(CombinedId(3,"user3"),CombinedId(4,"user4")))
    assert(idToInfo.size == 2)
    val userVisitInfo1 = idToInfo.get(CombinedId(3, "user3")).get
    assert(userVisitInfo1.lastVisitTime == 666)
    assert(userVisitInfo1.lastVisitIndex == 5)
    val userVisitInfo2 = idToInfo.get(CombinedId(4, "user4")).get
    assert(userVisitInfo2.lastVisitTime == 999)
    assert(userVisitInfo2.lastVisitIndex == 6)
  }

}
