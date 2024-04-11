package samaya.toolbox.transform

import samaya.structure.types.OpCode.VirtualOpcode
import samaya.structure.types.{AdtType, AttrId, Const, FetchMode, Func, Id, OpCode, Ref, SourceId, Type, TypedId}
import samaya.toolbox.traverse.Traverser

import scala.collection.immutable.ListMap

trait Transformer extends Traverser {

  def transformOpCode(code:OpCode, stack: State):Seq[OpCode] = (code match {
    case OpCode.Lit(res, value, id) => transformLit(res, value, id, stack)
    case OpCode.Let(res, block, id) => transformLet(res, block, id, stack)
    case OpCode.Fetch(res, src, mode, id) => transformFetch(res, src, mode, id, stack)
    case OpCode.Return(res, param, id) => transformReturn(res, param, id, stack)
    case OpCode.Discard(trg, id) => transformDiscard(trg, id, stack)
    case OpCode.DiscardMany(trg, id) => transformDiscardMany(trg, id, stack)
    case OpCode.Unpack(res, typ, src, mode, id) => transformUnpack(res, typ, src, mode, id, stack)
    case OpCode.InspectUnpack(res, typ, src, id) => transformInspectUnpack(res, typ, src, id, stack)
    case OpCode.Field(res, src, pos, mode, id) => transformField(res, src, pos, mode, id, stack)
    case OpCode.Switch(res, typ, src, branches, mode, id) => transformSwitch(res, typ, src, branches, mode, id, stack)
    case OpCode.InspectSwitch(res, typ, src, branches, id) => transformInspectSwitch(res, typ, src, branches, id, stack)
    case OpCode.Pack(res, src, ctr, mode, id) => transformPack(res, src, ctr, mode, id, stack)
    case OpCode.Invoke(res, func, param, id) => transformInvoke(res, func, param, id, stack)
    case OpCode.TryInvoke(res, func, param, success, failure,  id) => transformTryInvoke(res, func, param, success, failure, id, stack)
    case OpCode.InvokeSig(res, func, param, id) => transformInvokeSig(res, func, param, id, stack)
    case OpCode.TryInvokeSig(res, func, param, success, failure, id) => transformTryInvokeSig(res, func, param, id, stack, success, failure)
    case OpCode.Project(res, src, id) => transformProject(res, src, id, stack)
    case OpCode.UnProject(res, src, id) => transformUnProject(res, src, id, stack)
    case OpCode.RollBack(res, src, types, id) => transformRollback(res, src, types, id, stack)
    case code:OpCode.VirtualOpcode => transformVirtual(code, stack)

  }).getOrElse(Seq(code))

  def transformBlock(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, state: State):Seq[OpCode] = code
  def transformReturn(rets: Seq[AttrId], params: Seq[Ref], origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformLit(res:TypedId, value:Const, origin:SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformLet(res: Seq[AttrId], body: Seq[OpCode], origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformFetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformDiscard(trg:Ref, origin:SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformDiscardMany(trg:Seq[Ref], origin:SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformUnpack(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, mode: FetchMode, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformInspectUnpack(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformField(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformSwitch(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, bodies: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformInspectSwitch(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, bodies: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformPack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack:State):Option[Seq[OpCode]] = None
  def transformInvoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformTryInvoke(res: Seq[AttrId], func: Func, param: Seq[(Boolean, Ref)], success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode]), id: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformInvokeSig(res: Seq[AttrId], func: Ref, param: Seq[Ref], id: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformTryInvokeSig(res: Seq[AttrId], func: Ref, param: Seq[(Boolean, Ref)], id: SourceId, stack: State, success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode])):Option[Seq[OpCode]] = None
  def transformProject(res: AttrId, src: Ref, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformUnProject(res: AttrId, src: Ref, origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformRollback(rets: Seq[AttrId], params: Seq[Ref], types: Seq[Type], origin: SourceId, stack: State):Option[Seq[OpCode]] = None
  def transformVirtual(code:VirtualOpcode, stack: State):Option[Seq[OpCode]] = None

}
