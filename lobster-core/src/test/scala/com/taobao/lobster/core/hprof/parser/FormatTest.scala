package com.taobao.lobster.core.hprof.parser

import com.taobao.lobster.core.hprof.model.{RecordId, TagId}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

case class TestBean(field1: Int, field2: TagId, fielde3: RecordId)
object TestBean extends Format[TestBean] {
  override def read(input: RecordInput): TestBean = Format.read[TestBean](input)
}

case class TestBean2(field1: Int, field2: TestBean)
object TestBean2 extends Format[TestBean2] {
  override def read(input: RecordInput): TestBean2 = Format.read[TestBean2](input)
}


@RunWith(classOf[JUnitRunner])
class FormatSuit extends AnyFunSuite {
  test("Parse simple format") {
    val recordInput = new MemoryRecordInputBuilder()
      .writeInt(1)
      .writeInt(10)
      .writeTagId(2.toByte)
      .writeRecordId(3)
      .toRecordInput

    val result = Format.read[TestBean2](recordInput)

    assert(TestBean2(1, TestBean(10, 2.toByte, 3)) == result)
  }
}
