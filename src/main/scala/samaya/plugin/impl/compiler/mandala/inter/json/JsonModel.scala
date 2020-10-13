package samaya.plugin.impl.compiler.mandala.inter.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import samaya.plugin.impl.inter.json.JsonModel._
import samaya.structure

object JsonModel {
  case class EntryRef(module:String, componentIndex:Int)
  case class Alias(name:String, target:EntryRef)
  case class InstanceEntry(clazz:String, instances:Seq[String])
  case class InterfaceInstance(name:String, hadError:Boolean,  language:String, version:String, classifier:Set[String], classTarget:String, applies:Seq[Type], funAliases: Seq[Alias], implAliases: Seq[Alias])
  case class InterfaceSigClass(name:String, hadError:Boolean,  link: Option[String], mode:structure.Module.Mode, language:String, version:String, classifier:Set[String], generics:Seq[Generic], signatures: Seq[FunctionSignature], datatypes: Seq[DataSignature], classTarget:String)
  case class InterfaceFunClass(name:String, hadError:Boolean,  language:String, version:String, classifier:Set[String], generics:Seq[Generic], functions: Seq[FunctionSignature])
  case class InterfaceMandalaModule(name:String, link: Option[String], mode:structure.Module.Mode, hadError:Boolean, language:String, version:String, classifier:Set[String], functions: Seq[FunctionSignature], implements: Seq[FunctionSignature], datatypes: Seq[DataSignature], instances:Seq[InstanceEntry])


  implicit val modCodec: JsonValueCodec[InterfaceMandalaModule] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))
  implicit val instanceCodec: JsonValueCodec[InterfaceInstance] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))
  implicit val funClassCodec: JsonValueCodec[InterfaceFunClass] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))
  implicit val sigClassCodec: JsonValueCodec[InterfaceSigClass] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))

}