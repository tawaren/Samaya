package samaya.toolbox.checks

import samaya.compilation.ErrorManager.{Error, LocatedMessage, PlainMessage, feedback, unexpected}
import samaya.structure.types._
import samaya.toolbox.track.OwnershipTracker
import samaya.toolbox.track.OwnershipTracker.{Borrowed, Unknown, Consumed, Locked, Owned}



trait SubStructuralCapabilityChecker extends OwnershipTracker with CapabilityChecker{

  private def useInfo(src:Set[SourceId]):String = {
    src.map(_.origin.start.localRefString).mkString(",")
  }

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val frame = stack.frameValues
    frame.drop(assigns.size).foreach { value =>
      stack.getStatus(value) match {
        case Owned =>
          val typ = stack.getType(value)
          if(!typ.isUnknown && !typ.hasCap(context, Capability.Drop)) {
            feedback(LocatedMessage("Owned values without the Drop capability must be consumed or returned", origin, Error))
          }
        case Locked(_,_) => unexpected("Local parameters should have been unlocked before the block ends")
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
          feedback(LocatedMessage("Owned values without the Drop capability can not be discarded", origin, Error))
        }
      case Locked(_,lockPos) => feedback(LocatedMessage(s"Locked values can not be discarded (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Consumed values can not be discarded (value was locked at: ${useInfo(consumePos)})", origin, Error))
      case Borrowed => feedback(LocatedMessage("Borrowed values can not be discarded", origin, Error))
      case _ =>
    }
    super.discard(trg, origin, stack)
  }
}
