package com.zxl.web.external

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.{Admin, Connection}
import org.slf4j.LoggerFactory


object HBaseSnapshotAdmin {
  private val logger = LoggerFactory.getLogger(classOf[HBaseSnapshotAdmin])
}

class HBaseSnapshotAdmin(conn: Connection) {
  private val hbaseTableNamespace = System.getProperty("web.etl.hbase.namespace", "default")

  import HBaseSnapshotAdmin._

  def takeSnapshot(snapshotName: String, tableName: String): Unit = {
    val admin: Admin = conn.getAdmin
    val table = TableName.valueOf(hbaseTableNamespace, tableName)
    admin.disableTable(table)
    // check if snapshot with the name already exists
    val snapshots = admin.listSnapshots(snapshotName)
    if (!snapshots.isEmpty) {
      logger.warn(s"Snapshot ${snapshotName} already exists, deleting it and take the latest snapshot again.")
      admin.deleteSnapshot(snapshotName)
    }
    admin.snapshot(snapshotName, table)
    admin.enableTable(table)
    logger.info(s"Successfully took a snapshot ${snapshotName} for table ${tableName}.")

    admin.close();
  }
}
