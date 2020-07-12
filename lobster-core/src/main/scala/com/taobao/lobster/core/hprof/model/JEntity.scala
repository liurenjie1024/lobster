package com.taobao.lobster.core.hprof.model

sealed trait JEntity


sealed trait JValue extends JEntity

case class JObjectRef(id: RecordId) extends JValue



