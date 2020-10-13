package samaya.structure.types

import scala.collection.immutable.ListMap
import samaya.compilation.ErrorManager.unexpected


sealed trait OpCode {
  def id:SourceId
  def origin:Region = id.src
  def params:Seq[Ref]
  def rets:Seq[AttrId]
  def retVal(index:Int):Val = Val(rets(index).id,id,index)
  val isVirtual:Boolean = false
}

object OpCode {

  trait SingleResOpcodes{
    def ret:AttrId
    def rets: Seq[AttrId] = Seq(ret)
  }

  trait SingleTypedResOpcodes{
    def ret:TypedId
    def rets: Seq[AttrId] = Seq(AttrId(ret.id, ret.attributes))
  }

  trait SingleSourceOpcodes{
    def src:Ref
    def params: Seq[Ref] = Seq(src)
  }

  trait EssentialSourceOpcodes{
    def essentialParams:Seq[(Boolean,Ref)]
    def params: Seq[Ref] = essentialParams.map(_._2)
  }

  trait ZeroResOpcodes{
    def rets: Seq[AttrId] = Seq()
  }

  trait ZeroSrcOpcodes{
    def params: Seq[Ref] = Seq()
  }

  case class Lit(override val ret:TypedId, value:Const, id:SourceId) extends OpCode with SingleTypedResOpcodes with ZeroSrcOpcodes
  case class Let(rets:Seq[AttrId], block:Seq[OpCode], id:SourceId) extends OpCode with ZeroSrcOpcodes
  case class Fetch(override val ret:AttrId, override val src:Ref, mode:FetchMode, id:SourceId) extends OpCode with SingleResOpcodes with SingleSourceOpcodes
  case class Return(rets:Seq[AttrId], params:Seq[Ref], id:SourceId) extends OpCode
  case class Discard(src:Ref, id:SourceId) extends OpCode with ZeroResOpcodes with ZeroSrcOpcodes
  case class DiscardMany(params:Seq[Ref], id:SourceId) extends OpCode with ZeroResOpcodes
  case class Unpack(rets:Seq[AttrId], override val src:Ref, mode:FetchMode, id:SourceId) extends OpCode with SingleSourceOpcodes
  case class Field(override val ret:AttrId, override val src:Ref, pos:Id, mode:FetchMode, id:SourceId) extends OpCode with SingleResOpcodes with SingleSourceOpcodes
  case class Switch(rets:Seq[AttrId], override val src:Ref, branches:ListMap[Id,(Seq[AttrId],Seq[OpCode])], mode:FetchMode, id:SourceId) extends OpCode with SingleSourceOpcodes
  case class Inspect(rets:Seq[AttrId], override val src:Ref, branches:ListMap[Id,(Seq[AttrId],Seq[OpCode])], id:SourceId) extends OpCode with SingleSourceOpcodes
  case class Pack(override val ret:TypedId, params:Seq[Ref], tag:Id, mode:FetchMode, id:SourceId) extends OpCode with SingleTypedResOpcodes
  case class Invoke(rets:Seq[AttrId], func:Func, params:Seq[Ref], id:SourceId) extends OpCode
  case class TryInvoke(rets:Seq[AttrId], func:Func, override val essentialParams:Seq[(Boolean,Ref)], success:(Seq[AttrId],Seq[OpCode]), failure:(Seq[AttrId],Seq[OpCode]), id:SourceId) extends OpCode with EssentialSourceOpcodes
  case class InvokeSig(rets:Seq[AttrId], func:Ref, params:Seq[Ref], id:SourceId) extends OpCode
  case class TryInvokeSig(rets:Seq[AttrId], func:Ref, override val essentialParams:Seq[(Boolean,Ref)], success:(Seq[AttrId],Seq[OpCode]), failure:(Seq[AttrId],Seq[OpCode]), id:SourceId) extends OpCode with EssentialSourceOpcodes
  case class Project(override val ret:AttrId, override val src:Ref, id:SourceId) extends OpCode with SingleResOpcodes with SingleSourceOpcodes
  case class UnProject(override val ret:AttrId, override val src:Ref, id:SourceId) extends OpCode with SingleResOpcodes with SingleSourceOpcodes
  case class RollBack(rets:Seq[AttrId], params:Seq[Ref], retTypes:Seq[Type], id:SourceId) extends OpCode
  trait VirtualOpcode extends OpCode {
    override def rets: Seq[AttrId] = unexpected("virtual opcodes do not have returns")
    override val isVirtual:Boolean = true
  }
}