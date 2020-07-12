package com.taobao.lobster.core.hprof.parser

import com.taobao.lobster.core.hprof.model.RecordBody

trait RecordHandler {
  def handle(record: RecordBody): Unit
}

class RecordPrinter extends RecordHandler {
  override def handle(record: RecordBody): Unit = println(s"$record")
}
