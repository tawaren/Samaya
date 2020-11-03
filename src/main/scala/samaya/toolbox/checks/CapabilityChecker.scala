package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.DataDef
import samaya.structure.types.Type.Projected
import samaya.structure.types._
import samaya.toolbox.track.TypeTracker

//Note: does not check the following as these need linearity information:
//  1: That Owned values dropped on return have the drop capability
//  2: That a Owned value can not be discarded over the discard opcode
//todo: check taht all is checked
trait CapabilityChecker extends TypeTracker{

  private def hasCap(typ:Option[Type], cap:Capability):Boolean = typ match {
    case Some(value) => value.hasCap(context, cap)
    case None => false
  }

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val frame = stack.frameValues
    frame.take(assigns.size).map(v => (v,stack.getType(v))).foreach {
      case (v, typ) => if(!typ.hasCap(context, Capability.Unbound)) {
        feedback(LocatedMessage("A value returned from a block must be of a type with the unbound capability", v.src, Error))
      }
    }
    super.traverseBlockEnd(assigns, origin, stack)
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val typ = stack.getType(src)
    mode match {
      case FetchMode.Copy => if(!typ.hasCap(context,Capability.Copy)) {
        feedback(LocatedMessage("A value copied with the fetch opcode must be of a type with the copy capability", origin, Error))
      }
      case _ =>
    }
    super.fetch(res, src, mode, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val typ = stack.getType(src)
    mode match {
      case FetchMode.Copy => if(!typ.hasCap(context,Capability.Copy)) {
        feedback(LocatedMessage("A value copied with the unpack opcode must be of a type with the copy capability", origin, Error))
      }
      case _ =>
    }
    super.unpack(res, src, mode, origin, stack)
  }


  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val fieldId = fieldName.src
    mode match {
      case FetchMode.Copy => stack.getType(src) match {
        case adt:AdtType =>
          val ctrs = adt.ctrs(context)
          val fields = ctrs.head._2
          if (!hasCap(fields.get(fieldName.name), Capability.Copy)) {
            feedback(LocatedMessage("A value copied with the field opcode must be of a type with the copy capability", fieldId, Error))
          }
        case _ =>
      }
      case FetchMode.Move | FetchMode.Infer => stack.getType(src) match {
        case adt:AdtType =>
          val ctrs = adt.ctrs(context)
          val fields = ctrs.head._2
          if (!fields.filter(_._1 != fieldName.name).forall(_._2.hasCap(context, Capability.Drop))) {
            feedback(LocatedMessage("Extracting a field requires that the other fields must be of a type with the drop capability", fieldId, Error))
          }
        case _ =>
      }
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    //Ensure it is not freed
    stack.getType(src) match {
      case proj:Projected => if(!proj.inner.hasCap(context, Capability.Primitive)) {
        feedback(LocatedMessage("Only projections of primitive values can be reversed", origin, Error))
      }
      case _ =>
    }
    super.unproject(res, src, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin:SourceId, stack: Stack): Stack = {
    mode match {
      case FetchMode.Copy =>
        srcs.map(id => (id, stack.getType(id))).foreach {
          case (id,typ) => if(!typ.hasCap(context, Capability.Copy)) {
            feedback(LocatedMessage("The Argument to a copy pack must be of a type with the copy capability", id.src, Error))
          }
        }
      case _ =>
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }


  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    func.paramInfo(context).zip(params).foreach{
      case ((typ, _), (true, ref)) => if(!typ.hasCap(context, Capability.Value)){
        feedback(LocatedMessage("Essential try invoke arguments must have the value capability", ref.src, Error))

      }
      case ((typ, true), (false, ref)) => if(!typ.hasCap(context, Capability.Drop)){
        feedback(LocatedMessage("Consumed non essential try invoke arguments must have the drop capability", ref.src, Error))
      }
      case _ =>
    }

    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, state: Stack): Stack = super.tryInvokeSigBefore(res, src, params, succ, fail, origin, state)
}
