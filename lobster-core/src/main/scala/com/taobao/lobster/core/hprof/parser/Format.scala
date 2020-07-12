package com.taobao.lobster.core.hprof.parser

import com.taobao.lobster.core.hprof.model.{Record, RecordBody, RecordHeader, RecordId, TagId}

trait Format[T] {
  def read(input: RecordInput): T
}

case class RecordInputContext(header: RecordHeader, input: RecordInput)

trait RecordBodyFormat[B <: RecordBody] {
  def tagId: TagId
  def read(context: RecordInputContext): B
}


object Format {
  import scala.reflect.runtime.universe._

  private lazy val universeMirror = runtimeMirror(getClass.getClassLoader)

  def read[T: TypeTag](input: RecordInput): T = {
    val tpe = typeOf[T]
    val classSymbol = typeOf[T].typeSymbol.asClass
    require(classSymbol.isCaseClass, s"${classSymbol} is not case class!")

    val cstorSymbol = tpe.member(termNames.CONSTRUCTOR).asMethod

    val cstorValues =  cstorSymbol.paramLists.flatten.map {
      case s if s.info =:= typeOf[Int] => input.input.readInt()
      case s if s.info =:= typeOf[TagId] => input.readTagId()
      case s if s.info =:= typeOf[RecordId] => input.readId()
      case s if isFormattable(s.info) => {
        universeMirror.reflectModule(s.info.typeSymbol.asClass.companion.asModule)
          .instance.asInstanceOf[Format[_]].read(input)
      }
      case s =>
        throw new IllegalArgumentException(s"Unable to parse field ${s.name} of ${classSymbol}")
    }

    universeMirror.reflectClass(classSymbol).reflectConstructor(cstorSymbol)(cstorValues:_*).asInstanceOf[T]
  }

  private def isFormattable(s: Type): Boolean = {
    val companionModule = s.typeSymbol.asClass.companion
    if (companionModule.isModule) {
      universeMirror.reflectModule(companionModule.asModule).instance.isInstanceOf[Format[_]]
    } else {
      false
    }
  }
}
