package com.taobao.lobster.core.hprof.parser

import java.io.{BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import com.taobao.lobster.core.hprof.model.{RecordId, TagId}

class MemoryRecordInput(data: Array[Byte]) extends RecordInput {
  val input = new DataInputStream(new ByteArrayInputStream(data))
  override def idSize: Int = 8
  override def close(): Unit = input.close()
}

class MemoryRecordInputBuilder {
  private val buffer = new ByteArrayOutputStream()
  private val output = new DataOutputStream(buffer)

  def writeRecordId(recordId: RecordId): this.type = {
    output.writeLong(recordId)
    this
  }

  def writeTagId(tagId: TagId): this.type = {
    output.writeByte(tagId)
    this
  }

  def writeInt(value: Int): this.type = {
    output.writeInt(value)
    this
  }

  def toRecordInput: RecordInput = {
    val ret = new MemoryRecordInput(buffer.toByteArray)
    buffer.close()
    ret
  }
}
