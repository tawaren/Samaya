package samaya.toolbox.traverse

import samaya.structure.types.OpCode.VirtualOpcode
import samaya.structure.{Attribute, FunctionDef, ImplementDef, Param, Result}
import samaya.structure.types._
import samaya.types.Context

import scala.collection.immutable.ListMap

//todo: Extend to include function Header
trait Traverser{
  type State
  def context:Context
  def entry:Either[FunctionDef,ImplementDef]

  //todo: seperate trait extending traverser?
  //Universal view on the different components
  // is internal meaning interpreted from the body and not from a caller
  lazy val name:String = entry match {
    case Left(func) => func.name
    case Right(impl) => impl.name
  }

  lazy val params:Seq[Param] = entry match {
    case Left(func) => func.params
    case Right(impl) => impl.params ++ impl.implements.paramInfo(context).zip(impl.sigParamBindings).map( p => new Param {
      override val name: String = p._2.name
      override val index: Int = p._2.index + impl.params.size
      override val typ: Type = p._1._1
      override val consumes: Boolean = p._1._2
      override val attributes: Seq[Attribute] = p._2.attributes
      override val src: SourceId = p._2.src
    })
  }

  lazy val results:Seq[Result] = entry match {
    case Left(func) => func.results
    case Right(impl) => impl.implements.returnInfo(context).zip(impl.sigResultBindings).map( p => new Result {
      override val name: String = p._2.name
      override val index: Int = p._2.index
      override val typ: Type = p._1
      override val attributes: Seq[Attribute] = p._2.attributes
      override val src: SourceId = p._2.src
    })
  }

  lazy val code:Seq[OpCode] = entry match {
    case Left(func) => func.code
    case Right(impl) => impl.code
  }

  lazy val origin:SourceId = entry match {
    case Left(func) => func.src
    case Right(impl) => impl.src
  }

  def initialState():State
  def finalState(state: State): Unit = {}
  def abstractedBody(rets: Seq[Id], src: SourceId, state: State): State = state


  def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, state: State): State = state
  //Markers for special blocks
  //Body of a function
  def functionStart(params:Seq[Param], origin: SourceId, state: State): State  = state
  //Body of a let
  def letStart( origin: SourceId, state: State): State = state
  //Branches in switch/inspect
  def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, state: State): State = state
  //Branches in TryInvoke
  def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, state: State): State = state
  def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, state: State): State = state

  def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, state: State): State = state
  def traverseJoin(rets:Seq[Id], origin:SourceId, states: Seq[State]):State

  def lit(res:TypedId, value:Const, origin:SourceId, state: State): State = state
  def letBefore(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, state: State): State = state
  def letAfter(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, state: State): State = state
  def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, state: State):State = state
  def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, state: State):State = state
  def discard(trg:Ref, origin:SourceId, state: State):State = state
  def unpack(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, mode: FetchMode, origin: SourceId, state: State):State = state
  def inspectUnpack(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, origin: SourceId, state: State):State = state
  def switchBefore(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, state: State): State = state
  def switchAfter(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, state: State): State = state
  def inspectSwitchBefore(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, state: State): State = state
  def inspectSwitchAfter(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, state: State): State = state
  def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, state: State):State = state

  def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, state:State):State = state
  def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, state: State): State = state
  def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, state: State): State = state
  def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, state: State): State = state
  def tryInvokeAfter(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, state: State): State = state
  def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, state: State): State = state
  def tryInvokeSigAfter(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, state: State): State = state
  def project(res: AttrId, src: Ref, origin: SourceId, state: State): State = state
  def unproject(res: AttrId, src: Ref, origin: SourceId, state: State): State = state
  def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, state: State):State = state
  def virtual(code:VirtualOpcode, state: State):State = state

}
