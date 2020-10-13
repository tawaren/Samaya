package samaya.toolbox.track

import samaya.structure.types.{Val, _}
import samaya.toolbox.stack.SlotFrameStack.SlotDomain
import samaya.toolbox.stack.SlotFrameStack
import samaya.compilation.ErrorManager.unexpected
import samaya.structure.Param

import scala.collection.mutable
import scala.language.implicitConversions

//Todo Next: needs major rework -- as the semantics have changed positively

trait OwnershipTracker extends TypeTracker {

  import OwnershipTracker._

  //provide an implicit view of the stack that knows how to fetch / set types
  final implicit class OwnedStack(stack:Stack) {
    //returns None if Id does not exist
    def getStatus(id:Ref):SlotStatus = stack.getStatusInternal(id).getOrElse(Unknown)
  }

  private val lockStack =  mutable.Stack[Option[Val]]()

  override def initialState(): Stack = {
    lockStack.clear()
    super.initialState()
  }

  override def abstractedBody(rets: Seq[Id], src: SourceId, stack: Stack): Stack = {
    val nStack = super.abstractedBody(rets, src, stack)
    rets.foldLeft(nStack){
      case (s, id)  => s.withOwned(id)
    }
  }

  override def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
    //we do not yet know if we have to lock something we overwrite this if necessary
    lockStack.push(None)
    super.traverseBlockStart(input, result, code, origin, stack)
  }

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val unlockedStack = lockStack.pop() match {
      case Some(lock) => stack.unlock(lock)
      case None => stack
    }
    super.traverseBlockEnd(assigns, origin, unlockedStack)
  }

  override def functionStart(params: Seq[Param], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.functionStart(params, origin, stack)
    //we mark everything owned even unconsumed
    //  because current semantic is check at end that it is unconsumed
    //  an alternative would be an empty borrow (borrow from nothing) -- could have nasty side effect with discards & setals
    params.foldLeft(nStack){
      case (s, p) if p.consumes => s.withOwned(getParamVal(p))
      case (s, p) if !p.consumes => s.withReadOnly(getParamVal(p))
    }
  }

  override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): Stack = {
    val srcVal = stack.resolve(src)
    val nStack = super.caseStart(fields, src, ctr, mode, origin, stack)
    mode match {
      case None =>
        //replace the lock entry on top of the stack
        lockStack.pop()
        lockStack.push(Some(srcVal))
        fields.foldLeft(nStack.lock(srcVal)){case (s,e) => s.withReadOnly(e)}
      case Some(FetchMode.Copy | FetchMode.Infer) => fields.foldLeft(nStack){case (s,e) => s.withOwned(e)}
      case Some(FetchMode.Move)  => fields.foldLeft(nStack.consume(srcVal)){case (s,e) => s.withOwned(e)}
    }
  }

  override def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.invokeSuccStart(fields, call, origin, stack)
    fields.foldLeft(nStack){case (s,e) => s.withOwned(e)}
  }

  override def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.invokeFailStart(fields, call, essential, origin, stack)
    fields.foldLeft(nStack){case (s,e) => s.withOwned(e)}
  }

  override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.lit(res, value, origin, stack)
    nStack.withOwned(res.id)
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val srcValOpt = stack.resolve(src)
    val nStack = super.fetch(res, src, mode, origin, stack)
    mode match {
      //we just assume the infer will end up as copy (is better as just not putting anything)
      case FetchMode.Copy | FetchMode.Infer => nStack.withOwned(res)
      case FetchMode.Move => nStack.consume(srcValOpt).withOwned(res)
    }
  }

  override def discard(trg:Ref, origin:SourceId, stack: Stack): Stack = {
    val nStack = stack.consume(stack.resolve(trg))
    super.discard(trg, origin, nStack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val srcValOpt = stack.resolve(src)
    val nStack = super.unpack(res, src, mode, origin, stack)
    mode match {
      //we just assume the infere will end up as copy (is better as just not putting anything)
      case FetchMode.Copy | FetchMode.Infer => res.foldLeft(nStack){case (s,e) => s.withOwned(e)}
      case FetchMode.Move => res.foldLeft(nStack.consume(srcValOpt)){case (s,e) => s.withOwned(e)}
    }
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val srcValOpt = stack.resolve(src)
    val nStack = super.field(res, src, fieldName, mode, origin, stack)

    mode match {
      //we just assume the infer will end up as copy(is better as just not putting anything)
      case FetchMode.Copy | FetchMode.Infer => nStack.withOwned(res)
      case FetchMode.Move => nStack.consume(srcValOpt).withOwned(res)
    }
  }

  override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: Stack): Stack = {
    val srcVals= stack.resolveAll(srcs)
    val nStack = super.pack(res, srcs, ctr, mode, origin, stack)
    mode match {
      //we just assume the infer will end up as copy (is better as just not putting anything)
      case FetchMode.Copy | FetchMode.Infer  => nStack.withOwned(res.id)
      case FetchMode.Move => srcVals.foldLeft(nStack)(_.consume(_)).withOwned(res.id)
    }
  }


  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val srcVals = stack.resolveAll(params)
    val nStack = super.rollback(res, resTypes, params, origin, stack)
    val consumedStack = srcVals.foldLeft(nStack)(_.consume(_))
    res.foldLeft(consumedStack){case (s,e) => s.withOwned(e.id)}
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val srcVals = stack.resolveAll(src)
    val nStack = super._return(res, src, origin, stack)
    val consumedStack = srcVals.foldLeft(nStack)(_.consume(_))
    res.foldLeft(consumedStack){case (s,e) => s.withOwned(e)}
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val paramValOpts = params.map(stack.resolve)
    val nStack = super.invoke(res, func, params, origin, stack)
    val paramInfo = func.paramInfo(context)
    val paramConsumeInfo = paramInfo.map(_._2).padTo(paramValOpts.size, false)
    val consStack = paramConsumeInfo.zip(paramValOpts).filter(_._1).map(_._2).foldLeft(nStack) {
      case (s,v) => s.consume(v)
    }

    res.foldLeft(consStack)(_.withOwned(_))
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    val paramValOpts = params.map(_._2).map(stack.resolve)
    val nStack =  super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
    val paramInfo = func.paramInfo(context)
    val paramConsumeInfo = paramInfo.map(_._2).padTo(paramValOpts.size, false)
    //Note the ownership of the return values is done by the join of te branches
    paramConsumeInfo.zip(paramValOpts).filter(_._1).map(_._2).foldLeft(nStack) {
      case (s,v) => s.consume(v)
    }
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val paramValOpts = params.map(stack.resolve)
    val srcVal = stack.resolve(src)
    val srcType = stack.getType(src)
    val nStack = super.invokeSig(res, src, params, origin, stack)
    val consStack = nStack.consume(srcVal)
    val pConsStack = srcType match {
      case typ:SigType =>
        val paramInfo = typ.paramInfo(context)
        val paramConsumeInfo = paramInfo.map(_._2).padTo(paramValOpts.size, false)
        paramConsumeInfo.zip(paramValOpts).filter(_._1).map(_._2).foldLeft(consStack) {
          case (s,v) => s.consume(v)
        }
      case _ => consStack
    }
    res.foldLeft(pConsStack)(_.withOwned(_))
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack =  {
    val paramValOpts = params.map(_._2).map(stack.resolve)
    val srcVal = stack.resolve(src)
    val srcType = stack.getType(src)
    val nStack = super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
    val consStack =  nStack.consume(srcVal)
    //Note the ownership of the return values is done by the join of te branches
    srcType match {
      case typ:SigType =>
        val paramInfo = typ.paramInfo(context)
        val paramConsumeInfo = paramInfo.map(_._2).padTo(paramValOpts.size, false)
        paramConsumeInfo.zip(paramValOpts).filter(_._1).map(_._2).foldLeft(consStack) {
          case (s,v) => s.consume(v)
        }
      case _ => consStack
    }
  }

  override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val nStack =  super.project(res, src, origin, stack)
    nStack.withOwned(res)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.unproject(res, src, origin, stack)
    nStack.withOwned(res)
  }
}


object OwnershipTracker{

  sealed trait SlotStatus{
    def isActive:Boolean = true
    def isBorrowed:Boolean = false
    def isUnknown:Boolean = false
  }

  case object Consumed extends SlotStatus{override val isActive = false}
  case object Borrowed extends SlotStatus{override val isBorrowed = true}
  case object Owned extends SlotStatus
  case class Locked(original:SlotStatus) extends SlotStatus {
    override val isActive = false
    override val isBorrowed:Boolean = original.isBorrowed
  }
  //helper to represent failed tracking
  case object Unknown extends SlotStatus {
    override def isUnknown: Boolean = true
  }

  object SlotStatusDomain extends SlotDomain[SlotStatus] {
    override def merge(vs: Seq[SlotStatus]): Option[SlotStatus] = {
      if(vs.forall(_ == vs.head)) {
        Some(vs.head)
      } else {
        Some(Unknown)
      }
    }
  }

  //Helper to manage the substructural properties of the stack
  //Especially repeated functionality
  private implicit class SubStructuralStack(stack: SlotFrameStack) {

    def lock(v:Val): SlotFrameStack = {
      stack.readSlot(SlotStatusDomain,v) match {
        case Some(x) =>  stack.updateSlot(SlotStatusDomain,v)(_ => Some(Locked(x)))
        case x => stack
      }
    }

    def unlock(v:Val): SlotFrameStack = {
      stack.readSlot(SlotStatusDomain,v) match {
        case Some(Locked(x)) =>  stack.updateSlot(SlotStatusDomain,v)(_ => Some(x))
        case x => stack
      }
    }

    def consume(v:Val): SlotFrameStack = {
      stack.readSlot(SlotStatusDomain,v) match {
        case Some(Owned) =>  stack.updateSlot(SlotStatusDomain,v)(_ => Some(Consumed))
        case _ => stack
      }
    }

    def getStatusInternal(v:Val): Option[SlotStatus] = stack.readSlot(SlotStatusDomain, v)
    def getStatusInternal(id:Ref): Option[SlotStatus] = stack.readSlot(SlotStatusDomain, id)
    def withReadOnly(v:Val): SlotFrameStack = stack.updateSlot(SlotStatusDomain, v)(_ => Some(Borrowed))
    def withReadOnly(src:Ref): SlotFrameStack = stack.updateSlot(SlotStatusDomain, src)(_ => Some(Borrowed))
    def withOwned(v:Val): SlotFrameStack = stack.updateSlot(SlotStatusDomain, v)(_ => Some(Owned))
    def withOwned(src:Ref): SlotFrameStack = stack.updateSlot(SlotStatusDomain, src)(_ => Some(Owned))

  }
}

