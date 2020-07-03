package com.taobao.lobster.core.hprof

/**
 * Format reference: http://hg.openjdk.java.net/jdk6/jdk6/jdk/raw-file/tip/src/share/demo/jvmti/hprof/manual.html#Basic_Type
 */
package object model {
  type RecordId = Long
  type TagId = Byte

  val FormatNameV1: String = "JAVA PROFILE 1.0.1"
  val FormatNameV2: String = "JAVA PROFILE 1.0.2"
  val FormatNameSize: Int = 18

  val IdSizes: Set[Int] = Set(4, 8)

  def checkFormat(condition: => Boolean, message: => String): Unit = {
    if (!condition) {
      throw new IllegalStateException(message)
    }
  }
}

