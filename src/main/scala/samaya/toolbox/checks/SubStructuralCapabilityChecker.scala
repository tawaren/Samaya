package samaya.toolbox.checks

import samaya.compilation.ErrorManager.{Error, LocatedMessage, PlainMessage, feedback, unexpected}
import samaya.structure.types._
import samaya.toolbox.track.OwnershipTracker


trait SubStructuralCapabilityChecker extends OwnershipTracker with CapabilityChecker{

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val frame = stack.frameValues
    frame.drop(assigns.size).foreach { value =>
      stack.getStatus(value) match {
        case OwnershipTracker.Unknown | OwnershipTracker.Owned if !stack.getType(value).hasCap(context, Capability.Drop) =>
          feedback(LocatedMessage("Active not borrowed values without the Drop capability must be consumed or returned from a block ", origin, Error))
        case OwnershipTracker.Locked(_) => unexpected("Local parameters should have been unlocked before the block ends")
        case _ =>
      }
    }
    super.traverseBlockEnd(assigns, origin, stack)
  }

  override def discard(trg:Ref, origin:SourceId, stack: Stack): Stack = {
    val value = stack.resolve(trg)
    stack.getStatus(value) match {
      case OwnershipTracker.Unknown | OwnershipTracker.Owned => if(!stack.getType(value).hasCap(context, Capability.Drop)) {
        feedback(LocatedMessage("Active values without the Drop capability can not be discarded", origin, Error))
      }
      case OwnershipTracker.Locked(_) => feedback(LocatedMessage("Locked values can not be discarded", origin, Error))
      case OwnershipTracker.Borrowed => feedback(LocatedMessage("Borrowed values can not be discarded", origin, Error))
      case OwnershipTracker.Consumed => feedback(LocatedMessage("Consumed values can not be discarded", origin, Error))
    }
    super.discard(trg, origin, stack)
  }
}
