package com.zxl.web

import com.esotericsoftware.kryo.Kryo
import com.zxl.iplocation.IpLocation
import com.zxl.parser.dataobject._
import com.zxl.parser.dataobject.dim._
import org.apache.spark.serializer.KryoRegistrator

/**
  *  使用Kryo序列化机制
  */
class WebRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo): Unit = {
    kryo.register(classOf[BaseDataObject])
    kryo.register(classOf[PvDataObject])
    kryo.register(classOf[HbDataObject])
    kryo.register(classOf[EvDataObject])
    kryo.register(classOf[McDataObject])
    kryo.register(classOf[TargetPageDataObject])

    kryo.register(classOf[AdInfo])
    kryo.register(classOf[BrowserInfo])
    kryo.register(classOf[ReferrerInfo])
    kryo.register(classOf[SiteResourceInfo])
    kryo.register(classOf[TargetPageInfo])

    kryo.register(classOf[IpLocation])
  }
}
