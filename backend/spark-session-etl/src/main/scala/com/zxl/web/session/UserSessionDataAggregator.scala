package com.zxl.web.session

import com.zxl.parser.dataobject._
import com.zxl.parser.dataobject.dim.{SiteResourceInfo, TargetPageInfo}
import com.zxl.spark.web.CombinedId
import com.zxl.web._
import com.zxl.web.external.UserVisitInfo

import scala.collection.immutable.VectorBuilder

/**
  * 对一个用户的所有sessionData 转化为最终实体DataRecords(用户级别)
  *
  * @param profileUser          user唯一标识
  * @param serverSessionIdStart 起始会话id
  */
case class UserSessionDataAggregator(profileUser: CombinedId,
                                     serverSessionIdStart: Long,
                                     lastPersistedUserVisitInfo: Option[UserVisitInfo])
  extends AvroRecordBuilder with ConversionBuilder {

  import com.zxl.web.RichCollection._

  /**
    * 对一个用户的所有sessionData 转化为最终实体DataRecords(用户级别)
    * 1、生成最终的Session对象
    * 2、生产最终的Conversion对象
    * 3、生成最终的PageView对象
    * 4、生成最终的Heartbeat对象
    * 5、生成最终的MouseClick对象
    *
    * @param sessionDatas 一个user产生的所有的会话
    * @return 这个user产生的所有的聚合后DataRecords
    */
  def aggregate(sessionDatas: Seq[SessionData]): (UserVisitInfo, Seq[DataRecords]) = {
    //1.每一个会话中的所有dataObject进行归类（相同类型dataObjects放在一起，为什么？方便最终实体字段的计算）(会话级别)
    val classfiedSessionDatas: Seq[ClassifiedSessionData] = sessionDatas.zipWithIndex.map { case (sessionData, index) =>
      new ClassifiedSessionData(index, sessionData)
    }
    //用户访问行为信息
    val userVisitInfo = lastPersistedUserVisitInfo getOrElse UserVisitInfo(profileUser,UserVisitInfo.INIT_LAST_VISIT_TIME,UserVisitInfo.INIT_LAST_VISIT_INDEX)

    //2.对每一个会话进行聚合计算，将最终的实体计算出来(会话级别)
    val (resultUserVisitInfo, resultBuilder) = classfiedSessionDatas.foldLeft(userVisitInfo, Vector.newBuilder[DataRecords]) {
      case ((currentUserVisitInfo, builder), data) => {
        //计算会话id
        val sessionId = serverSessionIdStart + data.sessionIndex
        //计算这个会话的Session的信息
        val session: Session = produceSession(sessionId, data, currentUserVisitInfo)
        //计算这个会话中的所有的PageView实体
        val pageViews: Seq[PageView] = producePageViews(data, session)
        //计算这个会话中的所有的Heartbeat实体
        val heartbeats: Seq[Heartbeat] = produceHeartBeats(data.hbDataMap, session)
        //计算这个会话中的所有的MouseClick实体
        val mouseClicks: Seq[MouseClick] = produceMouseClicks(data.mcData, session)
        //计算这个会话中的转化
        val conversions: Seq[Conversion] = produceConversions(sessionId, data.allActiveTargetInfo, data.eventData)
        //将会话的信息派生到转化实体中
        val conversionsWithSession: Seq[Conversion] = conversions.map(buildConversion(_, session))

        builder += DataRecords(session, pageViews, mouseClicks, conversionsWithSession, heartbeats)
        //更新用户访问行为信息
        val visitInfo: UserVisitInfo = userVisitInfo.copy(profileUser, data.sessionStartTime, currentUserVisitInfo.lastVisitIndex + 1)
        (visitInfo, builder)
      }
    }
    (resultUserVisitInfo,resultBuilder.result())
  }

  private def produceConversions(sessionId: Long,
                                 targetInfoData: Seq[(TargetPageDataObject, TargetPageInfo)],
                                 eventData: Seq[EvDataObject]): Seq[Conversion] = {
    buildConversions(sessionId, targetInfoData, eventData)
  }


  private def produceHeartBeats(hbDataObjects: Map[String, HbDataObject], session: Session): Seq[Heartbeat] = {
    hbDataObjects.values.map(buildHeartbeat(_, session))(scala.collection.breakOut)
  }

  private def produceMouseClicks(mcData: Seq[McDataObject],
                                 session: Session): Seq[MouseClick] = {
    mcData.map(buildMouseClick(_, session))(scala.collection.breakOut)
  }
  /**
    *  计算PageView的字段
    * @param data 当前会话中的所有的dataObject
    * @param session  当前会话
    * @return PageView
    */
  private def producePageViews(data: ClassifiedSessionData, session: Session): Seq[PageView] = {

    val pvData = data.pvData
    val (_, pageViewsBuilder, _) = pvData.zipWithIndex.foldLeft(1, new VectorBuilder[PageView], None: Option[(PvDataObject, Int)]) {
      //pageViewDepth表示页面访问深度，pageViewBuilder表示计算后的PageView的列表
      //previous表示前一个pv及其在所有pv中的index
      //currentPv表示当前pv， currentIndex当前pv在所有pv中的index
      case ((pageViewDepth, pageViewBuilder, previous), (currentPv, currentIndex)) =>
        val recordBuilder = PageView.newBuilder()

        //获取当前pv对应的hb,计算页面加载时长
        val currentPvHbOpt = data.hbDataMap.get(currentPv.getPvId)
        val loading = currentPvHbOpt match {
          case Some(hb) => hb.getLoadingDuration
          case None => 0
        }
        recordBuilder.setLoadingDuration(loading)
        recordBuilder.setAccessOrder(currentIndex + 1) //页面的访问顺序
        recordBuilder.setPageDuration(currentPv.getDuration) //页面的停留时长，已经在计算会话停留时长的时候计算过了
        recordBuilder.setIsExitPage(currentIndex >= pvData.length - 1) //判断是否是退出页

        //计算页面访问深度
        val nextDepth = previous map { case (previousPv, _) =>
          //和前一个页面对比
          if (currentPv.getSiteResourceInfo.getUrl.equals(previousPv.getSiteResourceInfo.getUrl)) {
            recordBuilder.setIsRefresh(true)
            pageViewDepth
          } else {
            pageViewDepth + 1
          }
        } getOrElse (pageViewDepth)
        recordBuilder.setPageViewDepth(nextDepth)
        //计算PageView的其他的字段
        val filledPageView = buildPageView(currentPv, recordBuilder, session)
        pageViewBuilder += filledPageView
        (nextDepth, pageViewBuilder, Some(currentPv, currentIndex))
    }
    pageViewsBuilder.result()
  }
  /**
    * 计算Session
    *
    * @param sessionId            会话id
    * @param data 会话中所有的DataObject
    * @return Session
    */
  private def produceSession(sessionId: Long, data: ClassifiedSessionData, userVisitInfo: UserVisitInfo): Session = {
    val sessionBuilder: Session.Builder = Session.newBuilder()
    sessionBuilder.setServerSessionId(sessionId)
    //计算会话停留时长
    sessionBuilder.setSessionDuration(data.fetchSessionDuration)

    //计算是否是新的访客
    val isNewVisitor = if (userVisitInfo.lastVisitTime == UserVisitInfo.INIT_LAST_VISIT_TIME) true else false
    //计算这个访客自从上次访问到这次访问中间隔了多少天
    val daysSinceLastVisit =
      if (isNewVisitor) -1
      else {
        ((data.sessionStartTime - userVisitInfo.lastVisitTime)/1000 / 60 / 60 / 24).toInt
      }
    sessionBuilder.setIsNewVisitor(isNewVisitor)
    sessionBuilder.setDaysSinceLastVisit(daysSinceLastVisit)
    sessionBuilder.setUserVisitNumber(userVisitInfo.lastVisitIndex + 1) //访客访问的次数
    //计算会话特定页面维度
    val (landingPageViewInfo, secondPageViewInfo, exitPagePageViewInfo) =
      getPageViewInfos(data.pvData, data.selectedPVOpt)
    sessionBuilder.setLandingPageUrl(landingPageViewInfo.url)
    sessionBuilder.setLandingPageOriginalUrl(landingPageViewInfo.originalUrl)
    sessionBuilder.setLandingPageHostname(landingPageViewInfo.hostName)
    sessionBuilder.setLandingPageTitle(landingPageViewInfo.title)

    sessionBuilder.setSecondPageUrl(secondPageViewInfo.url)
    sessionBuilder.setSecondPageOriginalUrl(secondPageViewInfo.originalUrl)
    sessionBuilder.setSecondPageHostname(secondPageViewInfo.hostName)
    sessionBuilder.setSecondPageTitle(secondPageViewInfo.title)

    sessionBuilder.setExitPageUrl(exitPagePageViewInfo.url)
    sessionBuilder.setExitPageOriginalUrl(exitPagePageViewInfo.originalUrl)
    sessionBuilder.setExitPageHostname(exitPagePageViewInfo.hostName)
    sessionBuilder.setExitPageTitle(exitPagePageViewInfo.title)

    //会话实体统计维度
    sessionBuilder.setPvCount(data.pvData.length)
    sessionBuilder.setPvDistinctCount(data.pvData.distinctBy(_.getSiteResourceInfo.getUrl).length)
    sessionBuilder.setIsBounced(data.pvData.size == 1)
    sessionBuilder.setMouseClickCount(data.mcData.length)
    sessionBuilder.setTargetCount(data.allActiveTargetInfo.length)
    sessionBuilder.setEventCount(data.eventData.length)
    sessionBuilder.setConversionCount(data.eventData.length + data.allActiveTargetInfo.length)
    sessionBuilder.setTargetDistinctCount(data.allActiveTargetInfo.distinctBy { case (_, info) => info.getKey }.length)
    sessionBuilder.setEventDistinctCount(data.eventData.distinctBy(e => (e.getEventCategory, e.getEventLabel, e.getEventAction)).length)

    //其他
    buildSession(data.pvData,data.selectedPVOpt,data.selectedFirstDataObject,sessionBuilder)
  }

  private def getPageViewInfos(pvArray: Seq[PvDataObject], mandatoryPvOpt: Option[PvDataObject]
                              ): (PageViewInfo, PageViewInfo, PageViewInfo) = pvArray match {
    case Seq(onlyOnePage) => //如果只有一个pv的话，则这个pv是着陆页也是退出页
      val info: SiteResourceInfo = onlyOnePage.getSiteResourceInfo
      val pageViewInfo = PageViewInfo(info.getUrl, info.getOriginalUrl, info.getDomain, info.getPageTitle)
      (pageViewInfo, PageViewInfo.default, pageViewInfo)
    case Seq(firstPv, secondPv, _*) => { //含有2个pv或者以上的情况
      //着陆页pv先取是重要入口的pv，如果没有重要入口pv的话就取首个pv
      val firstPvSiteResourceInfo = mandatoryPvOpt.getOrElse(firstPv).getSiteResourceInfo
      val secondPvSiteResourceInfo = if (mandatoryPvOpt.nonEmpty && firstPv.isDifferentFrom(mandatoryPvOpt.get)) firstPv.getSiteResourceInfo else secondPv.getSiteResourceInfo
      val lastPv = pvArray.last
      val lastPvSiteResourceInfo = if (mandatoryPvOpt.nonEmpty && lastPv.isDifferentFrom(mandatoryPvOpt.get)) lastPv.getSiteResourceInfo else pvArray(pvArray.length - 2).getSiteResourceInfo
      (PageViewInfo(firstPvSiteResourceInfo.getUrl, firstPvSiteResourceInfo.getOriginalUrl, firstPvSiteResourceInfo.getDomain, firstPvSiteResourceInfo.getPageTitle),
        PageViewInfo(secondPvSiteResourceInfo.getUrl, secondPvSiteResourceInfo.getOriginalUrl, secondPvSiteResourceInfo.getDomain, secondPvSiteResourceInfo.getPageTitle),
        PageViewInfo(lastPvSiteResourceInfo.getUrl, lastPvSiteResourceInfo.getOriginalUrl, lastPvSiteResourceInfo.getDomain, lastPvSiteResourceInfo.getPageTitle))
    }
    case _ => (PageViewInfo.default, PageViewInfo.default, PageViewInfo.default)
  }
}

//会话特定的页面维度
private case class PageViewInfo(url: String, originalUrl: String, hostName: String, title: String)

private object PageViewInfo {
  val default = PageViewInfo("-", "-", "-", "-")
}