package com.taobao.lobster.core.hprof.model

import com.taobao.lobster.core.hprof.parser.RecordReader
import com.taobao.lobster.util.macros.EnumerationMacros.sealedInstancesOf

sealed trait Tag {
  type R <: Record
  def identifier: TagId
  def parser: RecordReader => R
}

object Tag {
  implicit object TagOrdering extends Ordering[Tag] {
    override def compare(x: Tag, y: Tag): Int = {
      x.identifier - y.identifier
    }
  }

  case object UTF8 extends Tag {
    type R = StringRecord
    override def identifier: TagId = 0x01.toByte
    override def parser: RecordReader => StringRecord = _.readString()
  }

  private val tags: Set[Tag] = sealedInstancesOf[Tag]
  private val idToTag: Map[TagId, Tag] = tags.map(t => (t.identifier, t)).toMap

  def tagOf(tagId: TagId): Tag = idToTag(tagId)
  def getTag(tagId: TagId): Option[Tag] = idToTag.get(tagId)
}

