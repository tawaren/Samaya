package samaya.toolbox.traverse

import samaya.structure.types._
import samaya.toolbox.stack.SlotFrameStack

import scala.collection.immutable.ListMap

// This tracker tracks the active ids and assosiated values
// It does only check the bare minimum of constraints needed to provide its functionality
// All the checking is done by the frameState (see their for details)
//todo: make helper fun for ctrs that extend with unknown
abstract class ViewTraverser extends Traverser {

  def traverse(): Unit = {
    val stack = initialState()
    val expect = results.map(r => Id(r.name))
    val provided = params.map(p => AttrId(Id(p.name), p.attributes))

    val finalStack = if(code.nonEmpty) {
      val s = traverseBlockLocal(provided, code, expect, origin, stack, s => functionStart(params, origin, s))
      traverseJoin(expect, origin, Seq(s))
    } else {
      abstractedBody(expect, origin, stack)
    }

    finalState(finalStack)
  }

  private def traverseBlockLocal(provided: Seq[AttrId], code: Seq[OpCode], expected: Seq[Id], src: SourceId, stack: State, header: State => State) = {
    val newState = traverseBlockStart(provided, expected, code, src, stack)
    val stateAfterHeader = header(newState)
    traverseBlockReturn(code,expected,src,stateAfterHeader)
  }

  private def traverseBlockReturn(code:Seq[OpCode], expected:Seq[Id], src:SourceId, stack: State): State = {
    val resultingState = code.foldLeft(stack)((s, o) =>{
      traverseOpCode(o, s)
    })
    traverseBlockEnd(expected, src, resultingState)
  }


  private[toolbox] def traverseOpCode(code:OpCode, stack: State): State = {
    code match {
      case OpCode.Lit(res, value, id) => lit(res,value,id,stack)
      case OpCode.Let(res, block, id) => letLocal(res, block, id, stack)
      case OpCode.Fetch(res, src, mode, id) => fetch(res, src, mode, id, stack)
      case OpCode.Discard(trg, id) => discard(trg, id,stack)
      case OpCode.DiscardMany(trgs, id) => trgs.foldLeft(stack){ (s, v) => discard(v,id,s)}
      case OpCode.Unpack(res, src, mode, id) => unpack(res, src, mode, id, stack)
      case OpCode.Field(res, src, fieldName, mode, id) => field(res, src, fieldName, mode, id, stack)
      case OpCode.Switch(res, src, branches, mode, id) => branchLocal(res, src, branches, Some(mode), id, stack)
      case OpCode.Inspect(res, src, branches, id) => branchLocal(res, src, branches, None, id, stack)
      case OpCode.Pack(res, src, ctr, mode, id) => pack(res,src,ctr,mode,id,stack)
      case OpCode.Invoke(res, func, param, id) => invoke(res, func, param, id, stack)
      case OpCode.InvokeSig(res, src, param, id) => invokeSig(res, src, param, id, stack)
      case OpCode.TryInvoke(res, func, param, succ, fail, id) => tryInvokeLocal(res, Left(func), param, succ, fail, id, stack)
      case OpCode.TryInvokeSig(res, src, param, succ, fail, id) => tryInvokeLocal(res, Right(src), param, succ, fail, id, stack)
      case OpCode.Project(res, src, id) => project(res, src, id, stack)
      case OpCode.UnProject(res, src, id) => unproject(res, src, id, stack)
      case OpCode.Return(res, refs, id) => _return(res, refs, id, stack)
      case OpCode.RollBack(res, refs, retypes, id) => rollback(res, retypes, refs, id, stack)
      case code:OpCode.VirtualOpcode => virtual(code, stack)
    }
  }

  private def letLocal(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: State) = {
    val newState = letBefore(res, block, origin, stack)
    val stackAfter = traverseBlockLocal(Seq.empty, block, res.map(_.id), origin, newState, s => letStart(origin, s))
    val fState = traverseJoin(res.map(_.id), origin, Seq(stackAfter))
    letAfter(res, block, origin, fState)
  }

  private def branchLocal(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: Option[FetchMode], origin: SourceId, stack: State) = {
    val newState = if(mode.isDefined) {
      switchBefore(res, src, branches, mode.get, origin, stack)
    } else {
      inspectBefore(res, src, branches, origin, stack)
    }

    val branchStates = branches.zipWithIndex.map { case ((ctrName, (intro, code)), idx) =>
      val caseId = origin.deriveSourceId(idx,ctrName.src.getOrElse(origin.src))
      traverseBlockLocal(intro, code, res.map(_.id), caseId, newState, state => caseStart(intro, src, ctrName, mode, caseId, state))
    }.toSeq
    val fState = traverseJoin(res.map(_.id),origin,branchStates)
    if(mode.isDefined) {
      switchAfter(res, src, branches, mode.get, origin, fState)
    } else {
      inspectAfter(res, src, branches, origin, fState)
    }
  }

  private def tryInvokeLocal(res: Seq[AttrId], fun: Either[Func, Ref], params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: State) = {
    val newState = fun match {
      case Left(func) => tryInvokeBefore(res, func, params, succ, fail, origin, stack)
      case Right(ref) => tryInvokeSigBefore(res, ref, params, succ, fail, origin, stack)
    }
    val succId = origin.deriveSourceId(0) //todo: add region
    val succState = traverseBlockLocal(succ._1, succ._2, res.map(_.id), succId, newState, state => invokeSuccStart(succ._1, fun, succId, state))
    val failId = origin.deriveSourceId(1) //todo: add region
    val failState = traverseBlockLocal(fail._1, fail._2, res.map(_.id), failId, newState, state => invokeFailStart(fail._1, fun, params.map(_._1), failId, state))
    val fState = traverseJoin(res.map(_.id),origin,Seq(succState, failState))
    fun match {
      case Left(func) => tryInvokeAfter(res, func, params, succ, fail, origin, fState)
      case Right(ref) => tryInvokeSigAfter(res, ref, params, succ, fail, origin, fState)
    }
  }

}





