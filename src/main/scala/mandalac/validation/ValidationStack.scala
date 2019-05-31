package mandalac.validation

import mandalac.structure.types.{Id, Type, TypedId, Val}
import mandalac.compilation.ErrorHandler._

import scala.collection.immutable.HashMap


//TODO TODO TODO TODO
//TODO Move some more elaborated stuff like return into the companion TODO
//Note: this is designed for a working compiler
//       meaning it does not try to give good error messages if the compiler produces incorrect code
//       in case that still happens it just aborts
object ValidationStack {
  sealed trait SlotStatus {
    def borrows:Set[Val] = Set()
    def borrowedBy:Set[Val] = Set()
    def isActive:Boolean = true
  }
  case object Freed extends SlotStatus{override val isActive = false}
  case class Borrowing(override val borrows:Set[Val], override val borrowedBy:Set[Val]) extends SlotStatus
  case class Owned(override val borrowedBy:Set[Val]) extends SlotStatus

  case class SlotInfo(typ:Type, status:SlotStatus)

  private def getValueOrThrow(v:Option[Val],id:Id):Val = {
    v match {
      case None => unexpected(s"Value with name $id not found")
      case Some(value) => value
    }
  }

}

import ValidationStack._

//TODO: CAN WE GET RID OF STACK ?? USE bindings instead (but order???)
//TODO: can we share???? Merge with other stack in serialization???
case class ValidationStack(
                       private val slots:HashMap[Val,ValidationStack.SlotInfo] = HashMap(),
                       private val bindings:HashMap[Id,Val] = HashMap(),
                       private val stack:List[Val] = List(),
                       private val parent:ValidationStack = null
                     ) {

  def resolve(id:Id):Val = {
    bindings.get(id) match {
      case None if parent == null => unexpected(s"Value with name $id not found")
      case None => parent.resolve(id)
      case Some(res) => res
    }
  }

  def readSlot(key:Val):SlotInfo = {
    slots.get(key) match {
      case None if parent == null => unexpected(s"Value with name ${key.id} not found")
      case None => parent.readSlot(key)
      case Some(res) => res
    }
  }

  def updateSlot(key:Val)( sl:SlotInfo => SlotInfo ): ValidationStack = {
    slots.get(key) match {
      case None if parent == null => unexpected(s"Value with name ${key.id} not found")
      case None => copy(parent = parent.updateSlot(key)(sl))
      case Some(res) => copy(slots = slots.updated(key,sl(res)))
    }
  }

  def markReleased(v:Val): ValidationStack = {
    readSlot(v) match {
      case SlotInfo(_, Freed) => unexpected(s"Value with name ${v.id} is already freed")
      case SlotInfo(_, status @ (Owned(_) | Borrowing(_, _))) if status.borrowedBy.nonEmpty =>
        unexpected(s"Value with name ${v.id} can not be released as it is borrowed by ${status.borrowedBy}")
      case SlotInfo(typ, Owned(_)) =>  updateSlot(v)(_ => SlotInfo(typ, Freed))
      case SlotInfo(typ, Borrowing(borrows,_)) => unlock(Set(v),borrows).updateSlot(v)(_ => SlotInfo(typ, Freed))
    }
  }


  def getStatus(v:Val): SlotStatus = readSlot(v).status
  def getType(v:Val): Type = readSlot(v).typ


  def getActive(v:Val): SlotInfo = {
    readSlot(v) match {
      case SlotInfo(_, Freed)=> unexpected(s"Value with name ${v.id} is already freed")
      case SlotInfo(_, status @ (Owned(_) | Borrowing(_, _))) if status.borrowedBy.nonEmpty => unexpected(s"Value with name ${v.id} is not active (is borrowedBy: ${status.borrowedBy})")
      case res => res
    }
  }

  def frame:Seq[Val] = stack

  def unlock(srcs:Set[Val], trgs:Set[Val]): ValidationStack = {
    val afterTrgs = trgs.foldLeft(this)((state, trg) => state.updateSlot(trg) {
      case SlotInfo(_, Freed) => unexpected(s"Value with name ${trg.id} is already freed")
      case SlotInfo(_, status @ (Owned(_) | Borrowing(_, _))) if status.borrowedBy.isEmpty => unexpected(s"Value ${trg.id} is already unlocked")
      case SlotInfo(typ, Owned(borrowedBy)) if srcs.subsetOf(borrowedBy) => SlotInfo(typ, Owned(borrowedBy -- srcs))
      case SlotInfo(typ, Borrowing(borrows, borrowedBy)) if srcs.subsetOf(borrowedBy) => SlotInfo(typ, Borrowing(borrows, borrowedBy -- srcs))
      case _ => unexpected(s"Value ${trg.id} is not locked by all of $srcs")
    })

    srcs.foldLeft(afterTrgs)((state, src) => state.updateSlot(src) {
      case SlotInfo(_, Freed) => unexpected(s"Value with name ${src.id} is already freed")
      case SlotInfo(_, Owned(_)) => unexpected(s"Value ${src.id} does not borrow anything")
      case SlotInfo(_, Borrowing(borrows, _)) if borrows.isEmpty => unexpected(s"Value ${src.id} does not borrow anything")
      case SlotInfo(typ, Borrowing(borrows, borrowedBy)) if trgs.subsetOf(borrows) => SlotInfo(typ, Borrowing(borrows--trgs, borrowedBy))
      case _ => unexpected(s"Value ${src.id} is does not borrow all of $trgs")
    })
  }

  private def lock(srcs:Set[Val], trgs:Set[Val], exclusive:Boolean): ValidationStack = {
    val afterTrgs = trgs.foldLeft(this)((state, trg) => state.updateSlot(trg) {
      case SlotInfo(_, Freed) => unexpected(s"Value with name ${trg.id} is already freed")
      case SlotInfo(_, status @ (Owned(_) | Borrowing(_, _))) if status.borrowedBy.nonEmpty && exclusive => unexpected(s"Value ${trg.id} is already locked")
      case SlotInfo(typ,Owned(borrowedBy)) => SlotInfo(typ, Owned(borrowedBy ++ srcs))
      case SlotInfo(typ,Borrowing(borrows, borrowedBy)) => SlotInfo(typ, Borrowing(borrows, borrowedBy ++ srcs))
    })

    srcs.foldLeft(afterTrgs)((state, src) => state.updateSlot(src) {
      case SlotInfo(_, Freed) => unexpected(s"Value with name ${src.id} is already freed")
      case SlotInfo(_, Owned(_)) => unexpected(s"Value with name ${src.id} is owned and can not borrow")
      case SlotInfo(typ, Borrowing(borrows, borrowedBy)) => SlotInfo(typ,  Borrowing(borrows ++ trgs, borrowedBy))
    })
  }

  def stealDiscard(trg:Val): ValidationStack = {
    readSlot(trg) match {
      case SlotInfo(_, Freed) => unexpected(s"Value with name ${trg.id} is already freed")
      case SlotInfo(_, Owned(_))=> unexpected(s"Value with name ${trg.id} is owned")
      case SlotInfo(_, Borrowing(borrows, borrowedBy)) =>
        if(borrows.isEmpty || borrowedBy.isEmpty) unexpected(s"Cannot steal value ${trg.id}")
        unlock(Set(trg),borrowedBy)
          .unlock(borrows,Set(trg))
          .lock(borrows, borrowedBy, exclusive = false)
          .markReleased(trg)
    }
  }

  private def push(src:TypedId, status:SlotStatus):(Val, ValidationStack) = {
    val newIndex = resolve(src.id).index+1
    val v = Val(src.id,newIndex)
    val entry =  SlotInfo(src.typ, status)
    (v, copy(
      slots = slots.updated(v, entry),
      stack = v+:stack ,
      bindings = bindings.updated(v.id, v)
    ))
  }

  def provide(src:TypedId):(Val, ValidationStack) = push(src, Owned(Set.empty))

  def borrow(src:TypedId, trgs:Set[Val], exclusive:Boolean = true): (Val, ValidationStack) = {
    val (v, state) = push(src, Borrowing(Set.empty,Set.empty))
    (v, state.lock(Set(v),trgs,exclusive))
  }


  def borrowMany(srcs:Set[TypedId], trg:Val, exclusive:Boolean = true): (Set[Val], ValidationStack) = {
    val (state, values) = srcs.foldLeft[(ValidationStack, Set[Val])]((this, Set.empty))((state,src) =>{
      val (v,newState) = state._1.push(src, Borrowing(Set.empty,Set.empty))
      (newState, state._2 + v)
    })
    (values, state.lock(values,Set(trg),exclusive))
  }

  def consume(v:Val): ValidationStack = {
    if(!getStatus(v).isInstanceOf[Owned]) unexpected(s"Cannot consume value ${v.id} as it is not owned")
    markReleased(v)
  }

  def free(v:Val): ValidationStack = {
    if(!getStatus(v).isInstanceOf[Borrowing]) unexpected(s"Cannot free value ${v.id} as it is not borrowing")
    markReleased(v)
  }

  //first val what is returned, second val what name it has afterwards
  def returnFrame(rets:Seq[(Val,Id)]): ValidationStack ={
    val retMap = Map[Val,Id](rets:_*)
    if(retMap.size != rets.size) unexpected("A value can only be returned once from a frame")

    if(!retMap.forall(vid => slots.contains(vid._1))) unexpected("Returned values must be inside the active frame")

    val afterRelease = stack.filter(v => !retMap.contains(v)).foldLeft(this){ (state,v) =>
      if(getStatus(v) != Freed){
        unexpected(s"Can not close frame as value ${v.id} is neither freed nor returned")
      }else{
        state
      }
    }

    //Does capture state and check that it is active can be returned -- this is necessary as we start changing the state (we could stilll access old state but this gets confusing) -- its cleaner this way
    val capturedRets = rets.map(vid => (vid._1, vid._2, afterRelease.getActive(vid._1)))

    //go from last to first to ensure borrow order is enforced
    val afterUnlock = capturedRets.reverse.foldLeft(afterRelease)((state, entry) => {
      state.getStatus(entry._1) match {
        case Freed =>
          unexpected(s"Can not return the freed value ${entry._1.id} from frame")
        case Owned(borrowedBy) =>
          //As all borrowers should be processed by now borrowedBy should be empty
          if(borrowedBy.nonEmpty) unexpected(s"Can not return value ${entry._1.id} from frame as it is still borrowed or borrowed by an earlier returned element")
          state
        case Borrowing(borrows, borrowedBy) =>
          //As all borrowers should be processed by now borrowedBy should be empty
          if(borrowedBy.nonEmpty) unexpected(s"Can not return value ${entry._1.id} from frame as it is still borrowed or borrowed by an earlier returned element")
          //Unlocks all targets inside the frame: will be relocked from new position later on
          state.unlock(Set(entry._1), borrows.filter(v => state.slots.contains(v)))
      }
    })

    //push and reborrow values | The Map is for tracking the new values of each old value
    capturedRets.foldLeft((afterUnlock.parent, Map[Val,Val]()))((state, entry) => {
      //push the current value
      val (newValue, pushed) = state._1.push(TypedId(entry._2,entry._3.typ), state._1.getStatus(entry._1))
      //relock if borrowing (extracted from the original borrows)
      (entry._3.status match {
        case Freed | Owned(_) => pushed
        case Borrowing(borrows, _) =>
          //calculate values to relock
          val relockValues = borrows
            //all inside discard frame
            .filter(v => afterUnlock.slots.contains(v))
            //change old discard values to return values
            .map(v => getValueOrThrow(state._2.get(v), v.id)) //todo: custom error: error means that we borrow earlier
          //relocking values
          pushed.lock(Set(newValue), relockValues,exclusive = false)
      }, state._2.updated(entry._1,newValue))
    })._1
  }


  //todo: recheck if it works correctly -- see try & switch for usage
  //Note: Assumes that return was called before
  def mergeReturnedBranches(other:ValidationStack):ValidationStack = {
    //shortcut as parents should be referenz equal always
    if(!this.eq(other)) return this

    if (stack.size != other.stack.size) unexpected("Branches must result in same stack size")

    val newStack = stack.zip(other.stack).map {e =>
      if (e._1 == e._2) {
        e._1
      } else {
        unexpected("Branches must result in the same stack values")
      }
    }

    val newEntries = slots.merged(other.slots) { (e1, e2) =>
      if (e1 != e2) {
        unexpected("Branches must result in the same state for each value")
      } else {
        e1
      }
    }

    val newBindings = bindings.merged(other.bindings) { (e1, e2) =>
      if (e1 != e2) {
        unexpected("Branches must result in the same state for each id")
      } else {
        e1
      }
    }

    assert(parent.eq(other.parent))
    val newParent = parent.mergeReturnedBranches(other.parent)

    ValidationStack(
      slots = newEntries,
      bindings= newBindings,
      stack = newStack,
      parent = newParent
    )
  }

}
