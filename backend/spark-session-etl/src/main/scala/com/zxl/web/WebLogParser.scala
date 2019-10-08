package com.zxl.web

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.zxl.metadata.loader.impl.MongoProfileConfigLoader
import com.zxl.parser.LogParser
import com.zxl.parser.configuration.loader.impl.DefaultProfileConfigLoader
import com.zxl.parser.dataobject.{BaseDataObject, InvalidLogObject, ParsedDataObject}
import com.zxl.parser.dataobjectbuilder._
import com.zxl.parser.dataobjectbuilder.helper.TargetPageAnalyzer
import com.zxl.preparser.PreParsedLog
import com.zxl.spark.web.CombinedId
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
  * 和weblog-parser交互的对象
  */
object WebLogParser {
  private val logger = LoggerFactory.getLogger(WebLogParser.getClass)
  private val localExceptionCounters = new ConcurrentHashMap[String, Int]
  private val LOGGING_THRESHOLD_PER_EXCEPTION = Integer.getInteger("web.logparser.logging.exception.threshold", 5000)

  //初始化weblog-parser的LogParser对象
  private val parser = {
    val cmds = new util.HashSet[String]()
    cmds.add("pv")
    cmds.add("mc")
    cmds.add("ev")
    cmds.add("hb")

    val builders = new util.ArrayList[AbstractDataObjectBuilder]()
    builders.add(new PvDataObjectBuilder(new TargetPageAnalyzer(new DefaultProfileConfigLoader(new MongoProfileConfigLoader))))
    builders.add(new McDataObjectBuilder)
    builders.add(new EvDataObjectBuilder)
    builders.add(new HbDataObjectBuilder)

    new LogParser(cmds, builders);
  }

  def parse(p: PreParsedLog): Seq[(CombinedId, BaseDataObject)] = {
    val parsedObjects: util.List[_ <: ParsedDataObject] = parser.parse(p)
    val buffer = new ArrayBuffer[(CombinedId, BaseDataObject)]()

    import scala.collection.JavaConversions._
    parsedObjects.foreach {
      case base: BaseDataObject => {
        val userId = base.getUserId
        if (!AbnormalUserDetector.hasReachUserLimit(userId)) { //如果一个Executor中某个用户访问日志数量达到指定数量，则过滤掉后面的访问行为数据，防止数据倾斜。
          val combinedId = CombinedId(base.getProfileId, userId)
          buffer.add((combinedId, base))
        }
      }
      case invalid: InvalidLogObject => {
        tryLogException("Invalid data object while parsing RequestInfo\n, details: ", new RuntimeException(invalid.getEvent))
      }
    }
    buffer
  }

  /**
    * 记录异常日志信息
    * 因为数据量比较大，所以异常信息可能会比较多，所以呢对于每一种异常只记录一定量的日志信息
    * 超过这个量的话则不记录了
    *
    * @param errorMsg 错误信息
    * @param ex       异常
    */
  private def tryLogException(errorMsg: String, ex: Exception): Unit = {
    val exceptionName = ex.getClass.getSimpleName
    val current = localExceptionCounters.getOrDefault(exceptionName, 0)
    localExceptionCounters.put(exceptionName, current + 1)
    if (current < LOGGING_THRESHOLD_PER_EXCEPTION)
      logger.error(errorMsg, ex)
  }
}