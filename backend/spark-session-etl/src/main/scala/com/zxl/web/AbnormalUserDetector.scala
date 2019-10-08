package com.zxl.web

import java.util.concurrent.ConcurrentHashMap

import org.slf4j.{Logger, LoggerFactory}

class AbnormalUserDetector {}

object AbnormalUserDetector {
  private val logger: Logger = LoggerFactory.getLogger(classOf[AbnormalUserDetector])
  //每个Task允许某个用户的数据量最多为5000
  private val MAX_USERDATAOBJECT_PER_EXECUTOR = Integer.getInteger("wd.etl.MaxUserDataObjectPerExecutor", 5000)
  private val userCounter = new ConcurrentHashMap[String, Int]()

  def hasReachUserLimit(userId: String): Boolean = {
    val count: Int = userCounter.getOrDefault(userId, 0)
    if (count < MAX_USERDATAOBJECT_PER_EXECUTOR) {
      false
    } else {
      logger.warn(s"the user ${userId} total number of dataobjects exceded the limit ${MAX_USERDATAOBJECT_PER_EXECUTOR} in a executor")
      true
    }
  }
}