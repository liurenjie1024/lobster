package com.taobao.lobster.core.hprof.model

import java.nio.charset.StandardCharsets
import java.time.Instant

trait Record

case class StringRecord(id: RecordId, utf8: Array[Byte]) extends Record {
  override def toString: String = s"$id: ${new String(utf8, StandardCharsets.UTF_8)}"
}
//case class LoadClassRecord(container: Snapshot, serialNumber: Int, classObjectId: RecordId, stackTraceSerialNumber: Int, classNameObjectId: RecordId) extends Record(container)
//case class UnloadClassRecord(container: Snapshot, serialNumber: Int) extends Record(container)
//case class StackFrame(container: Snapshot, id: RecordId, methodNameString: RecordId, methodSignatureString: RecordId, sourceFileNameString: RecordId)



case class Header(formatName: String, idSize: Int, creationTime: Instant)


