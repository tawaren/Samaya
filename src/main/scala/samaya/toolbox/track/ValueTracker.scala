package samaya.toolbox.track

import samaya.structure.Param
import samaya.structure.types._
import samaya.toolbox.stack.SlotFrameStack
import samaya.toolbox.traverse.Traverser

/**
  *  This is the Basis of all Stack based Trackers
  *   It use a Stack as the State (currently a SlotFrameStack)
  *   It generates Val's for each stack slot
  *   It manages branch merges (by delegating them to the stack)
  *  The non final methods are intended to be wrapped (overwritten and then containing a super call)
  *
  */
//TODO: Make an Interface to the SlotFrameStack (so that it later can be replaced if necessary)
trait ValueTracker extends Traverser {
  final override type State = SlotFrameStack
  final type Stack = State

  //todo: do we need this if not we can do the pushMany in Blockcstart ans skip traverseBodyStart, caseStart, invokeSuccStart, invokeFailStart
  // traverseBodyStart still needs to prduce an empty stack
  private var paramMapping:Seq[Val] = _
  final def getParamVal(p:Param):Val = paramMapping(p.index)

  //Helper to push a many ids
  private def pushMany(res:Seq[AttrId], origin:SourceId, stack: Stack): Stack = {
    //distinct ensures that only one val per id exist
    res.distinct.zipWithIndex.foldLeft(stack)((stack, entry) => {
      val tid = entry._1
      stack.push(tid, origin, entry._2)
    })
  }


  override def initialState(): Stack = SlotFrameStack()
  override def abstractedBody(rets: Seq[Id], src: SourceId, stack: Stack): Stack =  pushMany(rets.map(AttrId(_,Seq.empty)), src, stack)

  //Frame managing methods
  override def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, stack: Stack): Stack = stack.openFrame()
  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: State): State = stack.closeFrame(assigns.size,origin)
  override def traverseJoin(rets:Seq[Id], origin:SourceId, stacks: Seq[Stack]): Stack = SlotFrameStack.joinFrames(rets,origin,stacks)

  //Block field introductions
  override def functionStart(params:Seq[Param], origin: SourceId, stack: Stack): Stack = {
    val values = params.map(p => AttrId(Id(p.name),p.attributes))
    val nStack = pushMany(values, origin, stack)
    paramMapping = nStack.frameValues.reverse //the stackmis last first but param index is first first, that is why reverse is needed
    nStack
  }

  override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): Stack = pushMany(fields,origin,stack)
  override def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, stack: Stack): Stack = pushMany(fields,origin,stack)
  override def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, stack: Stack): Stack = pushMany(fields,origin,stack)

  //Single value pushing methods
  override def lit(res:TypedId, value:Const, origin:SourceId, stack: Stack): Stack = stack.push(res.id, origin, 0)
  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = stack.push(res,  origin, 0)
  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = stack.push(res, origin, 0)
  override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: Stack): Stack = stack.push(res.id, origin, 0)
  override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = stack.push(res, origin, 0)
  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = stack.push(res, origin,0)

  //multi value pushing methods
  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = pushMany(res, origin, stack)
  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = pushMany(res, origin, stack)
  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = pushMany(res, origin, stack)
  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = pushMany(res, origin, stack)
  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = pushMany(res, origin, stack)
}





