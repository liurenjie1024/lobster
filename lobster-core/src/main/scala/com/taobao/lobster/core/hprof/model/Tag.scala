package com.taobao.lobster.core.hprof.model

sealed trait Tag {
  type R <: RecordBody
  def identifier: TagId
}

object Tag {
  implicit object TagOrdering extends Ordering[Tag] {
    override def compare(x: Tag, y: Tag): Int = {
      x.identifier - y.identifier
    }
  }

  case object UTF8 extends Tag {
    type R = StringRecordBody
    override def identifier: TagId = 0x01.toByte
  }

  private val tags: Set[Tag] = Set()
  private val idToTag: Map[TagId, Tag] = tags.map(t => (t.identifier, t)).toMap

  def tagOf(tagId: TagId): Tag = idToTag(tagId)
  def getTag(tagId: TagId): Option[Tag] = idToTag.get(tagId)
}
