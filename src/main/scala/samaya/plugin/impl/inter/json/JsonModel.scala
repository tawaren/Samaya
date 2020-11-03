package samaya.plugin.impl.inter.json

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import samaya.structure
import samaya.structure.Attribute

object JsonModel {
  sealed trait TypeEncodings{
    val name:String
    def unapply(arg: String): Boolean = arg ==  name
  }

  object TypeEncodings {
    case object Local extends TypeEncodings {override val  name = "$local"}
    case object Generic extends TypeEncodings {override val  name = "$generic"}
    case object Projection extends TypeEncodings {override val  name = "$projection"}
    case object Unknown extends TypeEncodings {override val  name = "$unknown"}
  }

  sealed trait TypeKinds{
    val name:String
    def unapply(arg: String): Boolean = arg ==  name
  }

  object TypeKinds {
    case object Adt extends TypeKinds {override val name = "adt"}
    case object Lit extends TypeKinds {override val name = "lit"}
    case object Sig extends TypeKinds {override val name = "sig"}
    case object Param extends TypeKinds {override val name = "param"}
  }

  case class Type(module:String = TypeEncodings.Generic.name, componentIndex:Option[(String, Int)], applies:Seq[Type], attributes:Seq[Attribute])
  case class Field(name:String, attributes:Seq[Attribute], typ:Type)
  case class Return(name:String, attributes:Seq[Attribute], typ:Type)
  case class Param(name:String, attributes:Seq[Attribute], typ:Type, isConsumed:Boolean)
  case class Constructor(name:String, attributes:Seq[Attribute], fields:Seq[Field])
  case class Generic(name:String, attributes:Seq[Attribute], isPhantom:Boolean, capabilities:Set[String])
  //todo: has strange printout: We better include perm and use seq instead of map
  case class Accessibility(name:String, guards:Set[String] = Set.empty)
  case class Ref(module:String = TypeEncodings.Generic.name, offset:Option[Int] = None, args:Option[Int] = None)
  //todo: if it is external or not is completely irrelevant if we just look at the interface
  case class DataSignature(name:String, offset:Int, position:Int, attributes:Seq[Attribute], accessibility:Map[String, Accessibility], capabilities:Set[String], generics:Seq[Generic], constructors:Seq[Constructor], external:Option[Short], top:Boolean)
  case class FunctionSignature(name:String, offset:Int, position:Int, attributes:Seq[Attribute], capabilities:Set[String], accessibility:Map[String, Accessibility], transactional:Boolean, generics:Seq[Generic], params:Seq[Param], returns:Seq[Return])
  case class InterfaceModule(name:String, link: Option[String], mode:structure.Module.Mode, hadError:Boolean, language:String, version:String, classifier:Set[String], attributes:Seq[Attribute], functions: Seq[FunctionSignature], implements: Seq[FunctionSignature], datatypes: Seq[DataSignature], sigtypes: Seq[FunctionSignature])
  case class InterfaceTransaction(name:String, link: Option[String], hadError:Boolean, language:String, version:String, classifier:Set[String], attributes:Seq[Attribute], transactional:Boolean, params:Seq[Param], returns:Seq[Return])

  implicit val moduleCodec: JsonValueCodec[InterfaceModule] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))
  implicit val transactionCodec: JsonValueCodec[InterfaceTransaction] = JsonCodecMaker.make(CodecMakerConfig(allowRecursiveTypes = true))

}