package samaya.toolbox.checks

import samaya.compilation.ErrorManager.{Checking, Error, LocatedMessage, PlainMessage, feedback}
import samaya.structure.types._
import samaya.toolbox.process.TypeInference
import samaya.toolbox.track.ValueTracker

import scala.collection.immutable.ListMap

trait IdAvailabilityChecker extends ValueTracker{
  private final val Priority = 150;
  
  private def useInfo(src:Set[SourceId]):String = {
    src.map(_.origin.start.localRefString).mkString(",")
  }

  implicit class IdChecker(stack:Stack) {
    def checkId(id:Ref):Unit = {

      if(!stack.exists(stack.resolve(id))){
        feedback(LocatedMessage(s"No value is bound to the name ${id.name} in the current scope", id.src, Error, Checking(Priority)))
      }
    }

    def checkIds(ids:Seq[Ref]):Unit = ids.foreach(checkId)
  }

  private def checkDuplicates(src:SourceId, res:Seq[AttrId]): Unit ={
    val ids = res.map(_.id)
    if(ids.distinct.size != res.size) {
      ids.groupBy(v => v).filter(_._2.size >= 2).map(_._2.map(_.src).toSet).foreach{ vs =>
        feedback(LocatedMessage(s"Name is used twice (${useInfo(vs)}) as an OpCode return binding", src, Error, Checking(Priority)))
      }
    }
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.checkId(src)
    super.fetch(res, src, mode, origin, stack)
  }

  override def unpack(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkId(src)
    super.unpack(res, innerCtrTyp, src, mode, origin, stack)
  }

  override def inspectUnpack(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkId(src)
    super.inspectUnpack(res, innerCtrTyp, src, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.checkId(src)
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.checkIds(srcs)
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkIds(params)
    super.invoke(res, func, params, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkId(src)
    stack.checkIds(params)
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkIds(params.map(_._2))
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkId(src)
    stack.checkIds(params.map(_._2))
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkIds(params)
    super.rollback(res, resTypes, params, origin, stack)
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkIds(src)
    super._return(res, src, origin, stack)
  }

  override def discard(trg: Ref, origin: SourceId, stack: Stack): Stack = {
    stack.checkId(trg)
    super.discard(trg, origin, stack)
  }

  override def switchBefore(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkId(src)
    super.switchBefore(res, innerCtrTyp, src, branches, mode, origin, stack)
  }

  override def inspectSwitchBefore(res: Seq[AttrId], innerCtrTyp: Option[AdtType], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    checkDuplicates(origin,res)
    stack.checkId(src)
    super.inspectSwitchBefore(res, innerCtrTyp, src, branches, origin, stack)
  }

  override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    stack.checkId(src)
    super.project(res, src, origin, stack)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    stack.checkId(src)
    super.unproject(res, src, origin, stack)
  }


  override def virtual(code: OpCode.VirtualOpcode, stack: Stack): Stack = {
    code match {
      //Todo: I do not like this here we need more general if we keep
      case TypeInference.TypeHint(src, typ, id) =>
        stack.checkId(src)
        super.virtual(code, stack)
      case _ =>  super.virtual(code, stack)
    }
  }
}
