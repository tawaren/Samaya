package samaya.toolbox.transform

import samaya.compilation.ErrorManager.{Checking, Info, LocatedMessage, PlainMessage, feedback}
import samaya.structure.types._

import scala.collection.immutable.ListMap

//todo: make helper fun for ctrs that extend with unknown
abstract class TransformTraverser extends Transformer {

  def transform(): Seq[OpCode] = {
    val stack = initialState()
    val expect = results.map(r => Id(r.name, r.src))
    val provided = params.map(p => AttrId(Id(p.name, p.src), p.attributes))

    val (nCode,finalStack) = if(code.nonEmpty) {
      val (nCode, s) = transformBlockLocal(provided, code, expect, origin, stack, s => functionStart(params, origin, s))
      (nCode, traverseJoin(expect, origin, Seq(s)))
    } else{
      (code,abstractedBody(expect, origin, stack))
    }
    finalState(finalStack)
    nCode
  }

  private def transformBlockLocal(provided: Seq[AttrId], code: Seq[OpCode], expected: Seq[Id], src: SourceId, stack: State, header: State => State) = {
    val newState = traverseBlockStart(provided, expected, code, src, stack)
    val stateAfterHeader = header(newState)
    val nCodes = transformBlock(provided, expected, code, src, stateAfterHeader)
    val (resCodes,populatedState) = nCodes.foldLeft((Seq.empty[OpCode], stateAfterHeader)){
      case ((acc,beforeState),o) =>
        val (codes, afterState) = transformOpCodeLocal(o,beforeState)
        (acc ++ codes, afterState)
    }
    (resCodes , traverseBlockEnd(expected, src, populatedState))
  }

  private def  transformOpCodeLocal(code:OpCode, stack: State): (Seq[OpCode], State) = {
    def transform(stack: State, acc:Seq[OpCode], code:OpCode, trans: State => State)= (acc :+ code, trans(stack))
    def transformLocal(stack: State, acc:Seq[OpCode], trans: State => (OpCode,State))= {
      val (code, nStack) = trans(stack)
      (acc :+ code, nStack)
    }

    val nCodes = transformOpCode(code, stack)
    nCodes.foldLeft((Seq.empty[OpCode], stack)) {
      case ((acc, nStack), code@OpCode.Lit(res, value, id)) => transform(nStack,acc,code, lit(res, value, id, _))
      case ((acc, nStack), OpCode.Let(res, block, id)) => transformLocal(nStack,acc, transformLetLocal(res, block, id, _))
      case ((acc, nStack), code@OpCode.Fetch(res, src, mode, id)) => transform(nStack,acc,code, fetch(res, src, mode, id, _))
      case ((acc, nStack), code@OpCode.Return(res, src, id)) => transform(nStack,acc,code, _return(res, src, id, _))
      case ((acc, nStack), code@OpCode.Discard(trg, id)) => transform(nStack,acc,code, discard(trg, id, _))
      case ((acc, nStack), code@OpCode.DiscardMany(trgs, id)) => transform(nStack,acc,code, trgs.foldLeft(_) { (s, v) => discard(v, id, s) })
      case ((acc, nStack), code@OpCode.Unpack(res, src, mode, id)) => transform(nStack,acc,code,unpack(res, src, mode, id, _))
      case ((acc, nStack), code@OpCode.InspectUnpack(res, src, id)) => transform(nStack,acc,code,inspectUnpack(res, src, id, _))
      case ((acc, nStack), code@OpCode.Field(res, src, pos, mode, id)) => transform(nStack,acc,code,field(res, src, pos, mode, id, _))
      case ((acc, nStack), OpCode.Switch(res, src, branches, mode, id)) => transformLocal(nStack,acc, transformBranchLocal(res, src, branches, Some(mode), id, _))
      case ((acc, nStack), OpCode.InspectSwitch(res, src, branches, id)) => transformLocal(nStack,acc, transformBranchLocal(res, src, branches, None, id, _))
      case ((acc, nStack), code@OpCode.Pack(res, src, tag, mode, id)) => transform(nStack,acc,code,pack(res, src, tag, mode, id, _))
      case ((acc, nStack), code@OpCode.Invoke(res, func, param, id)) => transform(nStack,acc,code,invoke(res, func, param, id, _))
      case ((acc, nStack), OpCode.TryInvoke(res, src, branches, success, failure, id)) => transformLocal(nStack,acc, transformTryInvokeLocal(res, Left(src), branches, success, failure, id, _))
      case ((acc, nStack), code@OpCode.InvokeSig(res, func, param, id)) => transform(nStack,acc,code,invokeSig(res, func, param, id, _))
      case ((acc, nStack), OpCode.TryInvokeSig(res, src, branches, success, failure, id)) => transformLocal(nStack,acc, transformTryInvokeLocal(res, Right(src), branches, success, failure, id, _))
      case ((acc, nStack), code@OpCode.Project(res, src, id)) => transform(nStack,acc,code,project(res, src, id, _))
      case ((acc, nStack), code@OpCode.UnProject(res, src, id)) => transform(nStack,acc,code,unproject(res, src, id, _))
      case ((acc, nStack), code@OpCode.RollBack(res, src, types, id)) => transform(nStack,acc,code,rollback(res, types, src, id, _))
      case ((acc, nStack), code:OpCode.VirtualOpcode) => transform(nStack,acc,code, (state: State) => virtual(code, state))
    }
  }

  private def transformLetLocal(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: State) = {
    val newState = letBefore(res, block, origin, stack)
    val (code, stackAfter) = transformBlockLocal(Seq.empty, block, res.map(_.id), origin, newState, s => letStart(origin, s))
    val fState = traverseJoin(res.map(_.id), origin, Seq(stackAfter))
    (OpCode.Let(res, code, origin), letAfter(res, code, origin, fState))
  }

  private def transformBranchLocal(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: Option[FetchMode], origin: SourceId, stack: State): (OpCode with OpCode.SingleSourceOpcodes, State) = {
    val (isInspect, newState) = if(mode.isDefined) {
      (false, switchBefore(res, src, branches, mode.get, origin, stack))
    } else {
      (true, inspectSwitchBefore(res, src, branches, origin, stack))
    }

    val branchStates = branches.zipWithIndex.map { case ((ctrName, (intro, code)), idx) =>
      val caseId = origin.deriveSourceId(idx,ctrName.src.origin)
      transformBlockLocal(intro, code, res.map(_.id), caseId, newState, state => caseStart(intro, src, ctrName, mode, caseId, state))
    }.toSeq

    val nBranches = branches.zip(branchStates.map(_._1)).map{
      case ((ctr,(param, _)),block) => (ctr,(param, block))
    }

    //traverseJoin is only defined for non empty branches
    val fState = if(branchStates.nonEmpty) {
      traverseJoin(res.map(_.id), origin, branchStates.map(_._2))
    } else {
      feedback(LocatedMessage("Empty branching detected", origin, Info, Checking(10)))
      newState
    }

    if(!isInspect) {
      (OpCode.Switch(res, src, nBranches, mode.get, origin), switchAfter(res, src, nBranches, mode.get, origin, fState))
    } else {
      (OpCode.InspectSwitch(res, src, nBranches, origin), inspectSwitchAfter(res, src, nBranches, origin, fState))
    }
  }

  private def transformTryInvokeLocal(res: Seq[AttrId], fun: Either[Func, Ref], params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: State) = {
    val newState = fun match {
      case Left(func) => tryInvokeBefore(res, func, params, succ, fail, origin, stack)
      case Right(ref) => tryInvokeSigBefore(res, ref, params, succ, fail, origin, stack)
    }

    val succId = origin.deriveSourceId(0) //todo: add region
    val (nSucc, succState) = transformBlockLocal(succ._1, succ._2, res.map(_.id), succId, newState, state => invokeSuccStart(succ._1, fun, succId, state))
    val failId = origin.deriveSourceId(1) //todo: add region
    val (nFail, failState) = transformBlockLocal(fail._1, fail._2, res.map(_.id), failId, newState, state => invokeFailStart(fail._1, fun, params.map(_._1), failId, state))

    val fState = traverseJoin(res.map(_.id),origin,Seq(succState, failState))
    fun match {
      case Left(func) => (OpCode.TryInvoke(res, func, params, (succ._1, nSucc), (fail._1, nFail), origin),  tryInvokeAfter(res, func, params, (succ._1, nSucc), (fail._1, nFail), origin, fState))
      case Right(ref) => (OpCode.TryInvokeSig(res, ref, params, (succ._1, nSucc), (fail._1, nFail), origin),  tryInvokeSigAfter(res, ref, params, (succ._1, nSucc), (fail._1, nFail), origin, fState))
    }
  }
}



