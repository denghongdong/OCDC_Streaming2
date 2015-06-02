package com.asiainfo.ocdc.streaming

import com.asiainfo.ocdc.streaming.eventrule.StreamingCache
import com.asiainfo.ocdc.streaming.labelrule.LabelRule

abstract class MCLabelRule extends LabelRule {

  override def attachLabel(sources: Seq[SourceObject], cache: StreamingCache) = sources match {
    case so: Seq[MCSourceObject] => attachMCLabel(so, cache)
    case _ => throw new Exception("")
  }

  def attachMCLabel(mc: Seq[MCSourceObject], cache: StreamingCache): StreamingCache
}
