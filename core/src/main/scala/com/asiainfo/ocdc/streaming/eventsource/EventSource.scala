package com.asiainfo.ocdc.streaming.eventsource

import com.asiainfo.ocdc.streaming._
import com.asiainfo.ocdc.streaming.eventrule.{EventRule, StreamingCache}
import com.asiainfo.ocdc.streaming.labelrule.LabelRule
import com.asiainfo.ocdc.streaming.subscribe.BusinessEvent
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import scala.collection.mutable.{ArrayBuffer, Map}
import scala.collection.{immutable, mutable}

abstract class EventSource() extends Serializable with org.apache.spark.Logging {
  var id: String = null
  var conf: EventSourceConf = null
  var shuffleNum: Int = 0

  protected val labelRules = new ArrayBuffer[LabelRule]
  protected val eventRules = new ArrayBuffer[EventRule]
  protected val bsEvents = new ArrayBuffer[BusinessEvent]

  def addEventRule(rule: EventRule): Unit = {
    eventRules += rule
  }

  def addLabelRule(rule: LabelRule): Unit = {
    labelRules += rule
  }

  def addBsEvent(bs: BusinessEvent): Unit = {
    bsEvents += bs
  }

  def init(conf: EventSourceConf): Unit = {
    this.conf = conf
    id = this.conf.get("id")
    shuffleNum = conf.getInt("shufflenum")
  }

  def readSource(ssc: StreamingContext): DStream[String] = {
    EventSourceReader.readSource(ssc, conf)
  }

  //  def transform(source: String): Option[SourceObject]
  def transform(source: String): Option[(String, SourceObject)]

  def transformDF(sqlContext: SQLContext, labeledRDD: RDD[SourceObject]): DataFrame

  /*final def process(ssc: StreamingContext) = {
    val sqlContext = new SQLContext(ssc.sparkContext)
    val inputStream = readSource(ssc)
    inputStream.foreachRDD { rdd =>
      if (rdd.partitions.length > 0) {
        var sourceRDD = rdd.map(transform).collect {
          case Some(source: SourceObject) => source
        }

        if (shuffleNum > 0) sourceRDD = sourceRDD.map(x => (x.generateId, x)).groupByKey(shuffleNum).flatMap(_._2)
        else sourceRDD = sourceRDD.map(x => (x.generateId, x)).groupByKey().flatMap(_._2)

        if (sourceRDD.partitions.length > 0) {
          val labeledRDD = execLabelRule(sourceRDD: RDD[SourceObject])

          val eventMap = makeEvents(sqlContext, labeledRDD)

          subscribeEvents(eventMap)

        }
      }
    }
  }*/

  final def process(ssc: StreamingContext) = {
    val sqlContext = new SQLContext(ssc.sparkContext)
    val inputStream = readSource(ssc)

    val updateFunc = (values: Seq[SourceObject], state: Option[(SourceObject, immutable.Map[String, StreamingCache])]) => {
      val labelRuleArray = labelRules.toArray

      var rule_caches = state match {
        case Some(v) => v._2
        case None => {
          val cachemap = mutable.Map[String, StreamingCache]()
          labelRuleArray.foreach(labelRule => {
            cachemap += (labelRule.conf.get("id") -> null)
          })
          cachemap.toMap
        }
      }

      if (values.isEmpty) {
        Some((null, rule_caches))
      } else {
        labelRuleArray.foreach(labelRule => {
          logDebug(" Exec label : " + labelRule.conf.getClassName())
          val cacheOpt = rule_caches.get(labelRule.conf.get("id"))
          var old_cache: StreamingCache = null
          if (cacheOpt != None) old_cache = cacheOpt.get

          val newcache = labelRule.attachLabel(values, old_cache)
          rule_caches = rule_caches.updated(labelRule.conf.get("id"), newcache)
        })

        Some((values.last, rule_caches))
      }
    }

    var updateStateStream: DStream[(String, (SourceObject, immutable.Map[String, StreamingCache]))] = null
    if (shuffleNum > 0) inputStream.map(transform).filter(_ != None).map(_.get).updateStateByKey(updateFunc, shuffleNum)
    else updateStateStream = inputStream.map(transform).filter(_ != None).map(_.get).updateStateByKey(updateFunc)

    updateStateStream.map(_._2._1).filter(_ != null).foreachRDD { rdd =>
      if (rdd.partitions.length > 0) {
        val eventMap = makeEvents(sqlContext, rdd)

        subscribeEvents(eventMap)

      }
    }
  }

  def subscribeEvents(eventMap: Map[String, DataFrame]) {
    println(" Begin subscribe events : " + System.currentTimeMillis())
    if (eventMap.size > 0) {

      eventMap.foreach(x => {
        x._2.persist()
      })

      val bsEventIter = bsEvents.iterator

      while (bsEventIter.hasNext) {
        val bsEvent = bsEventIter.next
        bsEvent.execEvent(eventMap)
      }

      eventMap.foreach(x => {
        x._2.unpersist()
      })
    }
  }

  def makeEvents(sqlContext: SQLContext, labeledRDD: RDD[SourceObject]) = {
    val eventMap: Map[String, DataFrame] = Map[String, DataFrame]()
    if (labeledRDD.partitions.length > 0) {
      println(" Begin exec evets : " + System.currentTimeMillis())
      val df = transformDF(sqlContext, labeledRDD)
      // cache data
      df.persist
      df.printSchema()

      val f4 = System.currentTimeMillis()
      val eventRuleIter = eventRules.iterator

      while (eventRuleIter.hasNext) {
        val eventRule = eventRuleIter.next
        // handle filter first
        val filteredData = df.filter(eventRule.filterExp)
        eventMap += (eventRule.conf.get("id") -> filteredData)
      }
      logDebug(" Exec eventrules cost time : " + (System.currentTimeMillis() - f4) + " millis ! ")

      df.unpersist()
    }
    eventMap
  }

}

