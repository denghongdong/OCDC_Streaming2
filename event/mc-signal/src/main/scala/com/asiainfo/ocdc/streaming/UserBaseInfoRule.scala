package com.asiainfo.ocdc.streaming

import com.asiainfo.ocdc.streaming.cache.CacheCenter
import com.asiainfo.ocdc.streaming.constant.LabelConstant
import com.asiainfo.ocdc.streaming.eventrule.StreamingCache

import scala.collection.mutable.Map

/**
 * Created by leo on 4/29/15.
 */
class UserBaseInfoRule extends MCLabelRule {
  def attachMCLabel(mcSourceObjs: Seq[MCSourceObject], cache: StreamingCache): StreamingCache = {
    val imsi = mcSourceObjs.sortBy(_.time).last.imsi

    // get user base info by imsi
    //    val user_info_map = CacheFactory.getManager.getHashCacheMap("userinfo:" + imsi)
    val user_info_map = CacheCenter.getValue("userinfo:" + imsi, null, "hashall", System.currentTimeMillis()).asInstanceOf[Map[String, String]]
    val info_cols = conf.get("user_info_cols").split(",")

    val propMap = scala.collection.mutable.Map[String, String]()
    if (info_cols.length > 0) {
      info_cols.foreach(x => {
        var v: String = ""
        user_info_map.get(x) match {
          case Some(value) => v = value
          case None =>
        }
        propMap += (x -> v)
      })
    }

    mcSourceObjs.map(_.setLabel(LabelConstant.USER_BASE_INFO, propMap))
    cache
  }

}
