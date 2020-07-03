package com.taobao.lobster.core.hprof.parser

import com.taobao.lobster.core.hprof.model.Record

trait RecordHandler {
  def handle(record: Record): Unit
}

class RecordPrinter extends RecordHandler {
  override def handle(record: Record): Unit = println(s"$record")
}
