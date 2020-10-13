package samaya.toolbox.track

import samaya.compilation.ErrorManager.unexpected
import samaya.structure.Param
import samaya.structure.types.{Val, _}
import samaya.toolbox.stack.SlotFrameStack.SlotDomain

/**
  * The Position Tracker associates a position on the stack with each value produced by the Value Tracker
  *  others can read the position over Stack.getPos(...)
  */
trait PositionTracker extends ValueTracker {

  //provide an implicit view of the stack that knows how to fetch / set types
  final implicit class IndexedStack(s:Stack) {
    //returns None if Id does not exist
    def getPos(id:Ref):Option[Int] = s.readSlot(PositionTracker,id)
    def getPos(v:Val):Int = s.readSlot(PositionTracker,v).getOrElse(
      unexpected("Position Tracker is intended to map a Type to every Val either the caller made a mistake by using a non-existing value or their is a bug in PositionTracker")
    )

    def getRef(id:Ref):Option[Int] =  getPos(id).map(p => s.stackSize() - (p +1))
    def getRef(v:Val):Int = s.stackSize() - (getPos(v) +1)

    private[PositionTracker] def withPos(id:Id, p:Int):Stack = s.updateSlot(PositionTracker,id)(_ => Some(p))
    private[PositionTracker] def withPos(v:Val, p:Int):Stack = s.updateSlot(PositionTracker,v)(_ => Some(p))
  }

  //Helper to push a many ids
  private def placeMany(res:Seq[AttrId], start:Int, stack: Stack): Stack = {
    res.map(aid => stack.resolve(aid.id)).zipWithIndex.foldLeft(stack){
      case (s, (id,idx)) => s.withPos(id,idx + start)
    }
  }

  override def functionStart(params:Seq[Param], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.functionStart(params, origin, stack)
    params.zipWithIndex.foldLeft(nStack){
      case (s, (p,idx)) => s.withPos(getParamVal(p),idx)
    }
  }

  override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): State = {
    val nStack = super.caseStart(fields, src, ctr, mode, origin, stack)
    placeMany(fields, stack.stackSize(), nStack)
  }

  override def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, stack: Stack): State = {
    val nStack = super.invokeSuccStart(fields, call, origin, stack)
    placeMany(fields, stack.stackSize(), nStack)
  }

  override def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, stack: Stack): State = {
    val nStack = super.invokeFailStart(fields, call, essential, origin, stack)
    placeMany(fields, stack.stackSize(), nStack)
  }

  override def lit(res:TypedId, value:Const, origin:SourceId, stack: Stack): Stack = {
    val nStack = super.lit(res,value,origin,stack)
    nStack.withPos(res.id,stack.stackSize())
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.fetch(res, src, mode, origin, stack)
    nStack.withPos(res,stack.stackSize())
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.unpack(res, src, mode, origin, stack)
    placeMany(res, stack.stackSize(), nStack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.field(res, src, fieldName, mode, origin, stack)
    nStack.withPos(res,stack.stackSize())
  }

  override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: Stack): Stack = {
    val nStack = super.pack(res,srcs,ctr,mode,origin,stack)
    nStack.withPos(res.id,stack.stackSize())
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.invoke(res, func, params, origin, stack)
    placeMany(res, stack.stackSize(), nStack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.invokeSig(res, src, params, origin, stack)
    placeMany(res, stack.stackSize(), nStack)
  }

  override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.project(res, src, origin, stack)
    nStack.withPos(res,stack.stackSize())
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val nStack = super.unproject(res, src, origin, stack)
    nStack.withPos(res,stack.stackSize())
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val nStack = super._return(res, src, origin, stack)
    placeMany(res, stack.stackSize(), nStack)
  }


  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.rollback(res, resTypes, params, origin, stack)
    placeMany(res, stack.stackSize(), nStack)

  }

  //Overwrite Join for custom position tracking as the Join can not handle it
  override def traverseJoin(rets: Seq[Id], origin: SourceId, stacks: Seq[Stack]): Stack = {
    val nStack = super.traverseJoin(rets, origin, stacks)
    placeMany(rets.map(AttrId(_,Seq.empty)), nStack.stackSize() - rets.length, nStack)
  }
}

private object PositionTracker extends SlotDomain[Int] {
  override def merge(vs: Seq[Int]): Option[Int] = {
    if(vs.forall(_ == vs.head)) {
      //it can be that this returns a position from merged base frames even if the position is invalid
      // But this is no problem as we overwrite the position explicitly in: traverseJoin
      Some(vs.head)
    } else {
      //Positions are not mergeable
      // But this is no problem as we overwrite the position explicitly in: traverseJoin
      None
    }
  }
}


