package mandalac.plugin.impl.inter.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

object JsonModel {
  sealed trait TypeEncodings{
    val name:String
    def unapply(arg: String): Boolean = arg ==  name
  }
  object TypeEncodings {
    case object Local extends TypeEncodings {override val  name = "$local"}
    case object Native extends TypeEncodings {override val  name = "$native"}
    case object Generic extends TypeEncodings {override val  name = "$generic"}
    case object Image extends TypeEncodings {override val  name = "$image"}
  }

  case class Type(datatype:Ref, applies:Seq[Type])
  case class Field(name:String, typ:Type)
  case class Risk(name:String, offset:Int)
  case class Return(name:String, typ:Type, borrows:Set[String])
  case class Param(name:String, typ:Type, isConsumed:Boolean)
  case class Constructor(name:String, fields:Seq[Field])
  case class Generic(name:String, isProtected: Boolean = false, isPhantom:Boolean, capabilities:Set[String])
  case class Ref(module:String = TypeEncodings.Native.name, offset:Option[Int] = None, args:Option[Int] = None)
  case class Datatype(name:String, offset:Int, capabilities:Set[String] ,generics:Seq[Generic], constructors:Seq[Constructor])
  case class Function(name:String, offset:Int, risks:Set[Ref], generics:Seq[Generic], params:Seq[Param], returns:Seq[Return])
  case class Interface(name:String, hash: String, language:String, version:String, classifier:String, functions: Seq[Function], datatypes: Seq[Datatype], risks: Seq[Risk] )
  implicit val codec: JsonValueCodec[Interface] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))
}

/*

  def index:Int
  def name:String
  def attributes:List[FunctionAttribute]
  def risks:List[Risk]
  def generics:List[GenericInterface]
  def generic(index:Int):GenericInterface
  def params:List[ParamInterface]
  def param(pos:Int):ParamInterface
  def results:List[ResultInterface]
  def result(index:Int):ResultInterface

{
  "name": "MyModule",
  "hash":"0x0e...af", //is code hash
  "functions": [
    {"offset":0,"name":"MyFun", "risks":[risk], "generics":[{name, protected, caps}], "params":[{name,type,consume}], "returns":[{name,type,borrows}]}
  ],

  "datatypes": [
    {"offset":0, "name":"MyAdt", "generics":[{name, caps}], "category":[Cap], "constructors":[{tag,fields}]}
  ],

  "risks": [
    {"offset":0, "name":"MyRisk",}
  ]

}
* */