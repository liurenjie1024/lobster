package com.taobao.lobster.core.hprof.parser

import java.io.EOFException

import com.taobao.lobster.core.hprof.model.Tag

class HprofParser(private val recordReader: RecordReader, private val recordHandler: RecordHandler) {
  def parse(): Unit = {
    var done = false
    while(!done) {
      try {
        val tagId = recordReader.readTagId()
        Tag.getTag(tagId) match {
          case Some(tag) => recordHandler.handle(tag.parser(recordReader))
          case None => println(s"Ignoring tag: ${tagId}")
        }
      } catch {
        case e: EOFException =>
          done = true
        case t: Throwable => throw t
      }
    }
  }
}

object HprofParser {
  def main(args: Array[String]): Unit = {
    val filename = "/Users/bairui.lrj/Downloads/x.hprof"
    val parser = new HprofParser(new FileRecordReader(filename), new RecordPrinter)

    parser.parse()
  }
}
