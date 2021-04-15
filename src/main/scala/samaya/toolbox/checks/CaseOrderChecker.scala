package samaya.toolbox.checks

import samaya.compilation.ErrorManager.{Checking, Error, LocatedMessage, feedback}
import samaya.structure.types.{AdtType, AttrId, FetchMode, Id, OpCode, Ref, SourceId, UnknownSourceId}

import scala.collection.immutable.ListMap

trait CaseOrderChecker extends TypeChecker {

  private final val Priority = 50;

  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract{
      case adt:AdtType => if(adt.ctrs(context).keys != branches.keys.map(_.name)) {
        feedback(LocatedMessage("Cases in a switch must have the same order as the constructors in the type declaration", origin, Error, Checking(Priority)))
      }
      case _ =>
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectSwitchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract {
      case adt:AdtType => if(adt.ctrs(context).keys != branches.keys.map(_.name)) {
        feedback(LocatedMessage("Cases in a inspect must have the same order the constructors in the type declaration", origin, Error, Checking(Priority)))
      }
      case _ =>
    }
    super.inspectSwitchBefore(res, src, branches, origin, stack)
  }
}
