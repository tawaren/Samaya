package samaya.toolbox.checks

import samaya.compilation.ErrorManager.{Checking, Error, LocatedMessage, PlainMessage, feedback, unexpected}
import samaya.structure.types._
import samaya.toolbox.track.OwnershipTracker
import samaya.toolbox.track.OwnershipTracker.{Borrowed, Consumed, Locked, Owned, Unknown}



trait SubStructuralCapabilityChecker extends OwnershipTracker with CapabilityChecker{
  private final val Priority = 100;

  private def useInfo(src:Set[SourceId]):String = {
    src.map(_.origin.start.localRefString).mkString(",")
  }

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val frame = stack.frameValues
    frame.drop(assigns.size).foreach { value =>
      stack.getStatus(value) match {
        //In case of locked  traverseBlockEnd will unlock
        // but we can not call super first because it will cleanUp the frame and we loose all informations
        case Owned | Locked(Owned,_)=>
          val typ = stack.getType(value)
          if(!typ.isUnknown && !typ.hasCap(context, Capability.Drop)) {
            feedback(LocatedMessage("Owned values without the Drop capability must be consumed or returned", origin, Error, Checking(Priority)))
          }
        case _ =>
      }
    }
    super.traverseBlockEnd(assigns, origin, stack)
  }

  override def discard(trg:Ref, origin:SourceId, stack: Stack): Stack = {
    val value = stack.resolve(trg)
    stack.getStatus(value) match {
      case Owned =>
        val typ = stack.getType(value)
        if(!typ.isUnknown && !typ.hasCap(context, Capability.Drop)) {
          feedback(LocatedMessage("Owned values without the Drop capability can not be discarded", origin, Error, Checking(Priority)))
        }
      case Locked(_,lockPos) => feedback(LocatedMessage(s"Locked values can not be discarded (value was locked at: ${useInfo(lockPos)})", origin, Error, Checking(Priority)))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Consumed values can not be discarded (value was locked at: ${useInfo(consumePos)})", origin, Error, Checking(Priority)))
      case Borrowed => feedback(LocatedMessage("Borrowed values can not be discarded", origin, Error, Checking(Priority)))
      case _ =>
    }
    super.discard(trg, origin, stack)
  }
}
