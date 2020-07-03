package com.taobao.lobster.core.hprof.parser

import java.io.{DataInput, DataInputStream, FileInputStream}
import java.nio.charset.StandardCharsets
import java.time.Instant

import com.taobao.lobster.core.hprof.model.{FormatNameSize, Header, StringRecord, TagId, _}

trait RecordReader extends AutoCloseable {
  def idSize: Int
  def input: DataInput

  def readTagId(): TagId = {
    input.readByte()
  }

  def readId(): Long = {
    idSize match {
      case 4 => input.readInt()
      case 8 => input.readLong()
      case s => throw new IllegalStateException(s"Unrecognized id size: $s")
    }
  }

  def readString(): StringRecord = {
    // Skip timestamp
    input.readInt()

    val len = input.readInt()
    val bytes = new Array[Byte](len)
    val id = readId()
    input.readFully(bytes)

    StringRecord(id, bytes)
  }

  def readHeader(): Header = {
    val formatNameBuffer = new Array[Byte](FormatNameSize)
    input.readFully(formatNameBuffer)

    val formatName = new String(formatNameBuffer, StandardCharsets.UTF_8)
    checkFormat(formatName == FormatNameV1 || formatName == FormatNameV2, s"Unrecognized format name: $formatName")

    val nullTerminator = input.readByte()
    checkFormat(nullTerminator == 0.toByte, "A null terminator should follow format name!")

    val idSize = input.readInt()
    checkFormat(IdSizes.contains(idSize), s"Illegal id size: $idSize")

    val timestampHigher = input.readInt().toLong
    val timestampLower = input.readInt().toLong
    val creationTime = Instant.ofEpochMilli((timestampHigher << 32) | timestampLower)

    Header(formatName, idSize, creationTime)
  }
}

class FileRecordReader(filename: String) extends RecordReader {
  override val input = new DataInputStream(new FileInputStream(filename))
  private val header = readHeader()
  override val idSize: Int = header.idSize

  override def close(): Unit = input.close()
}

