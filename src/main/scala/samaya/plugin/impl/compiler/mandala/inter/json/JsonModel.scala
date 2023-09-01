package samaya.plugin.impl.compiler.mandala.inter.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import samaya.plugin.impl.inter.json.JsonModel._
import samaya.structure

object JsonModel {
  case class Applied(module:Option[String], entryIndex:Int, applies:Seq[Type])
  case class Implement(name:String, generics:Seq[Generic], fun:Applied, impl:Applied)
  case class TypeAlias(name:String, generics:Seq[Generic], typ:Type)
  case class InstanceEntry(clazz:String, instances:Seq[String])
  case class InterfaceInstance(name:String, hadError:Boolean, language:String, version:String, classifier:Set[String], priority:Int, generics:Seq[Generic], classTarget:String, applies:Seq[Type], implements: Seq[Implement])
  case class InterfaceSigClass(name:String, hadError:Boolean,  link: Option[String], mode:structure.Module.Mode, language:String, version:String, classifier:Set[String], generics:Seq[Generic], signatures: Seq[FunctionSignature], datatypes: Seq[DataSignature], classTarget:String)
  case class InterfaceFunClass(name:String, hadError:Boolean,  language:String, version:String, classifier:Set[String], generics:Seq[Generic], functions: Seq[FunctionSignature])
  case class InterfaceMandalaModule(name:String, link: Option[String], mode:structure.Module.Mode, hadError:Boolean, language:String, version:String, classifier:Set[String], functions: Seq[FunctionSignature], implements: Seq[FunctionSignature], datatypes: Seq[DataSignature], instances:Seq[InstanceEntry], typeAlias:Seq[TypeAlias])

  implicit val modCodec: JsonValueCodec[InterfaceMandalaModule] = JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
  implicit val instanceCodec: JsonValueCodec[InterfaceInstance] = JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
  implicit val funClassCodec: JsonValueCodec[InterfaceFunClass] = JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))
  implicit val sigClassCodec: JsonValueCodec[InterfaceSigClass] = JsonCodecMaker.make(CodecMakerConfig.withAllowRecursiveTypes(true))

}
