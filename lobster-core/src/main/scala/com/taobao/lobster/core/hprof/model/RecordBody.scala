package com.taobao.lobster.core.hprof.model

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.time.Instant

import com.taobao.lobster.core.hprof.parser.{Format, RecordInput}

case class Record(header: RecordHeader, body: RecordBody)

case class RecordHeader(tag: TagId, creationTime: Int, length: Int)
object RecordHeader extends Format[RecordHeader] {
  override def read(input: RecordInput): RecordHeader = ???
}

trait RecordBody

case class StringRecordBody(id: RecordId, utf8: ByteBuffer) extends RecordBody

case class LoadClassRecordBody(serialNumber: Int,
                               classId: RecordId,
                               stackTraceSerialNumber: Int,
                               classNameId: RecordId)
    extends RecordBody

object LoadClassRecordBody extends Format[LoadClassRecordBody] {
  override def read(input: RecordInput): LoadClassRecordBody = ???
}

case class UnloadClassRecordBody(serialNumber: Int) extends RecordBody

case class StackFrame(id: RecordId,
                      methodNameId: RecordId,
                      methodSignatureId: RecordId,
                      sourceFileName: RecordId,
                      classSerialNumber: Int,
                      lineNumber: Int)
    extends RecordBody

case class StackTrace(serialNumber: Int,
                      threadSerialNumber: Int,
                      numberOfFrames: Int,
                      frameIds: Array[Int])
    extends RecordBody

case class AllocSites(flag: Short,
                      cutoffRatio: Float,
                      totalLiveBytes: Int,
                      totalLiveInstances: Int,
                      totalBytesAllocated: Long,
                      totalInstancesAllocated: Long,
                      sitesCount: Int,
                      sites: Array[AllocSite])
    extends RecordBody

case class HeapSummary(totalLiveBytes: Int,
                       totalLiveInstances: Int,
                       totalBytesAllocated: Long,
                       totalInstancesAllocated: Long)
    extends RecordBody

case class StartThread(threadSerialNumber: Int,
                       threadObjectId: RecordId,
                       stackTraceSerialNumber: Int,
                       threadName: RecordId,
                       threadGroupName: RecordId,
                       threadParentGroupName: RecordId)
    extends RecordBody

case class EndThread(threadSerialNumber: Int) extends RecordBody
case class HeapDump(records: Array[HeapDumpRecord]) extends RecordBody
case class HeapDumpSegment(records: Array[HeapDumpRecord]) extends RecordBody
case class HeapDumpEnd() extends RecordBody
case class CpuSamples(sampleCount: Int, cpuTraces: SizedList4[StackTraceWithCount]) extends RecordBody
case class ControlSettings(flag: Int, stackTraceDepth: Short) extends RecordBody

case class AllocSite(arrayIndicator: Byte,
                     classSerialNumber: Int,
                     stackTraceSerialNumber: Int,
                     liveObjectsCount: Int,
                     liveInstancesCount: Int,
                     allocatedBytesCount: Int,
                     allocatedInstancesCount: Int)
case class Header(formatName: String, idSize: Int, creationTime: Instant)

trait HeapDumpRecord

case class RootUnknown(objectId: RecordId) extends HeapDumpRecord
case class RootJniGlobal(objectId: RecordId, jniGlobalRefId: RecordId) extends HeapDumpRecord
case class RootJniLocal(objectId: RecordId, threadSerialNumber: Int, frameNumber: Int)
    extends HeapDumpRecord
case class RootJavaFrame(objectId: RecordId, threadSerialNumber: Int, frameNumber: Int)
    extends HeapDumpRecord
case class RootNativeStack(objectId: RecordId, threadSerialNumber: Int) extends HeapDumpRecord
case class RootStickyClass(objectId: RecordId) extends HeapDumpRecord
case class RootThreadBlock(objectId: RecordId, threadSerialNumber: Int) extends HeapDumpRecord
case class RootMonitorUsed(objectId: RecordId) extends HeapDumpRecord
case class RootThreadObject(objectId: RecordId,
                            threadSerialNumber: Int,
                            stackTraceSerialNumber: Int)
    extends HeapDumpRecord
case class ClassDump(objectId: RecordId,
                     stackTraceSerialNumber: Int,
                     superClassObjectId: RecordId,
                     classLoaderObjectId: RecordId,
                     signerObjectId: RecordId,
                     protectionDomainObjectId: RecordId,
                     reserved1: RecordId,
                     reserved2: RecordId,
                     instanceSize: Int,
                     constants: SizedList2[Constant],
                     staticFields: SizedList2[StaticField],
                     instanceFields: SizedList2[InstanceField])
    extends HeapDumpRecord

case class InstanceDump(objectId: RecordId,
                        stackTraceSerialNumber: Int,
                        classObjectId: RecordId,
                        fieldsBytes: Int,
                        fields: Array[JValue])
    extends HeapDumpRecord

case class ObjectArrayDump(objectId: RecordId,
                           stackTraceSerialNumber: Int,
                           length: Int,
                           classObjectId: RecordId,
                           values: Array[RecordId])
    extends HeapDumpRecord
case class PrimitiveArrayDump(objectId: RecordId,
                              stackTraceSerialNumber: Int,
                              length: Int,
                              typeId: TagId,
                              values: Array[JValue])
    extends HeapDumpRecord

case class Constant(index: Short, value: TypedJValue)
case class StaticField(fieldName: RecordId, value: TypedJValue)
case class InstanceField(fieldName: RecordId, typeId: TagId)
case class StackTraceWithCount(sampleCount: Int, stackTraceSerialNumber: Int)

case class TypedJValue(typeId: TagId, value: JValue)
case class SizedList2[T](count: Short, records: Array[T])
case class SizedList4[T](count: Int, records: Array[T])
