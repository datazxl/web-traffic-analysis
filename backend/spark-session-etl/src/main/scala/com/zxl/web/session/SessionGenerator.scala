package com.zxl.web.session

import com.zxl.parser.dataobject.{BaseDataObject, PvDataObject}
import scala.collection.mutable.ArrayBuffer

case class SessionData(dataObjects: Seq[BaseDataObject])

/**
  * 会话切割逻辑(user级别)
  */
trait SessionGenerator {
  private val THIRTY_MINS_IN_MS = 30 * 60 * 1000;

  /**
    * 对一个user下的所有的访问记录(dataObjects)进行会话的切割，逻辑为：
    * 1、如果两个相邻的访问记录时间相隔超过30分钟，则切割为一个会话
    * 2、如果一个pv是重要的入口，则从这个pv开始重新算作一个新的会话
    *
    * @param sortedDataObjects 一个user的排序好的所有的dataObjects
    * @return 返回这个user产生的所有的会话，一个user可能产生若干个会话
    */
  def getSessions(sortedDataObjects: Seq[BaseDataObject]): Seq[SessionData] = {
    //1. 对sortedDataObjects的相邻的两个dataObjects进行时间比较，超过30分钟则切成一个新的会话
    val (sessions, session, _) = sortedDataObjects.foldLeft(ArrayBuffer.empty[ArrayBuffer[BaseDataObject]], ArrayBuffer.empty[BaseDataObject], null: BaseDataObject) {
      //sessions表示会话的列表，session表示一个会话中的BaseDataObject列表
      //previous表示前一个BaseDataObject，current表示当前的BaseDataObject
      case ((sessions, session, previous), current) => {
        //如果不是第一个BaseDataObject，则用当前的BaseDataObject和前一个BaseDataObject的时间进行比较
        //超过30分钟则切成一个新的会话
        if (previous != null && current.getServerTime.getTime - previous.getServerTime.getTime > THIRTY_MINS_IN_MS) {
          sessions += session.clone()
          session.clear()
        }
        //没有超过30分钟，则算在当前的会话中
        session += current
        (sessions, session, current)
      }
    }
    //如果最后一个会话中含有BaseDataObject的值，则将最后的一个会话加入到会话列表sessions中
    if (session.nonEmpty) {
      sessions += session.clone()
    }
    //2.根据重要入口再次进行会话的切割
    val resSessions: ArrayBuffer[ArrayBuffer[BaseDataObject]] = sessions.flatMap(rawSession => {
      val (resSessions, session, _) = rawSession.foldLeft(ArrayBuffer.empty[ArrayBuffer[BaseDataObject]], ArrayBuffer.empty[BaseDataObject], null: PvDataObject) {
        case ((resSessions, session, previousPv), currentDataObject) => {
          currentDataObject match {
            //如果是pv，且这个pv是一个重要入口，并且这个pv和上一个pv不是同一时间产生的，并且这个pv和前一个pv不一样(不是刷新来的)
            //而且这个pv不是这个会话的第一个dataObject,那么这个pv就是重要入口，就需要从这个pv重新切割会话
            case currentPv: PvDataObject if previousPv != null && currentPv.isMandatoryEntrance && currentPv.isDifferentFrom(previousPv) && !sentAtSameTime(currentPv, previousPv) => {
              resSessions += session.clone()
              session.clear()
              session += currentPv
              (resSessions, session, currentPv)
            }
            case _ => {
              session += currentDataObject
              (resSessions, session, previousPv)
            }
          }
        }
      }
      //如果最后一个会话中含有BaseDataObject的值，则将最后的一个会话加入到会话列表sessions中
      if (session.nonEmpty) {
        resSessions += session.clone()
      }
      resSessions
    })

    return resSessions.map(SessionData(_))
  }

  /**
    * 得到当前会话所有重要入口在当前会话中的Indexs
    *
    * @param session 需要根据重要入口切割会话的会话
    * @return
    */
  private def getEntranceIndexs(session: ArrayBuffer[BaseDataObject]): Seq[Int] = {

    //循环遍历这个会话中的所有的DataObject，找到重要入口的pv的index
    val (indexes, _) = session.zipWithIndex.foldLeft(Seq(0), null: PvDataObject) {
      case ((indexes, previousPv), (currentDataObject, index)) => {
        currentDataObject match {
          //如果是pv，且这个pv是一个重要入口，并且这个pv和上一个pv不是同一时间产生的，并且这个pv和前一个pv不一样(不是刷新来的)
          //而且这个pv不是这个会话的第一个dataObject,那么这个pv就是重要入口，就需要从这个pv重新切割会话
          case currentPv: PvDataObject if currentPv.isMandatoryEntrance && currentPv.isDifferentFrom(previousPv) && !sentAtSameTime(currentPv, previousPv) => {
            if (index != 0) (indexes :+ index, currentPv) else (indexes, currentPv)
          }
          case _ => (indexes, previousPv)
        }
      }
    }

    indexes :+ session.size
  }

  private def sentAtSameTime(pv: PvDataObject, previousPv: PvDataObject): Boolean = {
    if (pv != null && previousPv != null && pv.getServerTime.getTime == previousPv.getServerTime.getTime) {
      true
    }
    false
  }
}