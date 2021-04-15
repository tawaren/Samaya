package samaya.codegen

import java.io.DataOutputStream

import samaya.codegen.ModuleSerializer.ImportCollector
import samaya.compilation.ErrorManager.{CodeGen, unexpected}
import samaya.structure.{FunctionDef, ImplementDef}
import samaya.structure.types.{AdtType, AttrId, Const, FetchMode, Func, Id, OpCode, Permission, Ref, SourceId, Type, TypedId}
import samaya.toolbox.track.{PositionTracker, TypeTracker}
import samaya.toolbox.traverse.ViewTraverser
import samaya.types.Context

import scala.collection.immutable.ListMap


object CodeSerializer {

  def collectFunctionBodyDependencies(imports: ImportCollector, comp:Either[FunctionDef,ImplementDef], context: Context): Unit = {
    new DependencyCollector(imports, comp, context).traverse()
  }

  private class DependencyCollector(imports: ImportCollector, override val entry:Either[FunctionDef,ImplementDef], override val context: Context) extends ViewTraverser with TypeTracker {
    override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
      imports.addType(res.typ, Some(Permission.Create))
      super.lit(res, value, origin, stack)
    }

    override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      val perm = mode match {
        case FetchMode.Copy | FetchMode.Infer => Permission.Inspect
        case FetchMode.Move => Permission.Consume
      }
      imports.addType(stack.getType(src), Some(perm))
      super.unpack(res, src, mode, origin, stack)
    }

    override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      val perm = mode match {
        case FetchMode.Copy | FetchMode.Infer => Permission.Inspect
        case FetchMode.Move => Permission.Consume
      }
      imports.addType(stack.getType(src), Some(perm))
      super.field(res, src, fieldName, mode, origin, stack)
    }

    override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      imports.addType(stack.getType(src), Some(Permission.Consume))
      super.switchBefore(res, src, branches, mode, origin, stack)
    }

    override def inspectSwitchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
      imports.addType(stack.getType(src), Some(Permission.Inspect))
      super.inspectSwitchBefore(res, src, branches, origin, stack)
    }

    override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      imports.addType(res.typ, Some(Permission.Create))
      super.pack(res, srcs, ctr, mode, origin, stack)
    }

    override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      imports.addFunction(func, Some(Permission.Call))
      super.invoke(res, func, params, origin, stack)
    }

    override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      imports.addType(stack.getType(src), Some(Permission.Call))
      super.invokeSig(res, src, params, origin, stack)
    }

    override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      imports.addFunction(func, Some(Permission.Call))
      super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
    }

    override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      imports.addType(stack.getType(src), Some(Permission.Call))
      super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
    }

    override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      resTypes.foreach(imports.addType(_))
      super.rollback(res, resTypes, params, origin, stack)
    }

    override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
      imports.addType(stack.getType(src).projected(origin))
      super.project(res, src, origin, stack)
    }

    override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
      stack.getType(src) match {
        case p:Type.Projected =>  imports.addType(p.inner)
        case _ => unexpected("Type checker missed a unproject on a not projected type", CodeGen())
      }
      super.unproject(res, src, origin, stack)
    }
  }

  def serializeCode(out: DataOutputStream, imports: ImportCollector, comp: Either[FunctionDef, ImplementDef], context: Context): Unit = {
    new CodeSerializer(out, imports, comp, context).traverse()
  }

  //todo: position tracking
  private class CodeSerializer(out:DataOutputStream, imports: ImportCollector, override val entry: Either[FunctionDef, ImplementDef], override val context: Context) extends ViewTraverser with PositionTracker with TypeTracker {

    //#[derive(Debug, Parsable, Serializable)]
    //pub struct Exp(
    override def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
      //pub LargeVec<OpCode> -- OpCodes will be taken care of by traverser
      out.writeShort(code.length)
      super.traverseBlockStart(input, result, code, origin, stack)
    }
    //)

    private def fetchOffset(start:Byte, mode:FetchMode): Byte = {
      (start + (mode match {
        case FetchMode.Copy => 0
        case FetchMode.Move => 1
        case FetchMode.Infer => throw new Exception(); //todo: better error handling
      })).toByte
    }

    //[0]Lit(
    override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(0)
      //todo: assert size
      //LargeVec<u8>
      out.writeShort(value.bytes.length)
      out.write(value.bytes)
      //PermRef
      out.writeByte(imports.permIndex(res.typ))
      super.lit(res, value, origin, stack)
    }
    //)

    //[1]Let(
    override def letBefore(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
      out.writeByte(1)
      //Exp -- (traverser takes care of writing Exp)
      super.letBefore(res, block, origin, stack)
    }
    //)

    //[2]Copy(
    //[3]Move(
    override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(fetchOffset(2, mode))
      //todo: assert size & better error handling
      //ValueRef
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      super.fetch(res, src, mode, origin, stack)
    }
    //)

    //[4]Return(
    override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      out.writeByte(4)
      //Vec<ValueRef>
      out.writeByte(src.length)
      src.foreach(r => out.writeShort(stack.getRef(r).get.asInstanceOf[Short]))
      super._return(res, src, origin, stack)
    }
    //)

    //[5]Discard(
    override def discard(trg: Ref, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(5)
      //todo: assert size & better error handling
      //ValueRef
      out.writeShort(stack.getRef(trg).get.asInstanceOf[Short])
      super.discard(trg, origin, stack)
    }
    //)



    // --[6]DiscardMany(Vec<ValueRef>)-- <| Not used

    //[7]CopyUnpack(
    //[8]Unpack(
    override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(fetchOffset(7, mode))
      //ValueRef
      //todo: assert size & better error handlin
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      //PermRef
      //todo: better absent error handling
      out.writeByte(imports.permIndex(stack.getType(src)))
      super.unpack(res, src, mode, origin, stack)
    }
    //)

    //[9] InspectUnpack( //OPEN

    //[10]CopyField(
    //[11]Field(
    override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(fetchOffset(10, mode))
      //ValueRef
      //todo: assert size & better error handling
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      //PermRef
      out.writeByte(imports.permIndex(stack.getType(src)))
      //todo: better absent error handling
      val offset = stack.getType(src).projectionExtract{
        case adt:AdtType => adt.ctrs(context).head._2.keys.toSeq.indexOf(fieldName.name)
        case _ => unexpected("Type checker missed a field access on a not adt type", CodeGen())
      }
      out.writeByte(offset)
      super.field(res, src, fieldName, mode, origin, stack)
    }
    //)

    //[12]CopySwitch(
    //[13]Switch(
    override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(fetchOffset(12, mode))
      //ValueRef
      //todo: assert size & better error handling
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      //PermRef
      //todo: better absent error handling
      out.writeByte(imports.permIndex(stack.getType(src)))
      //Vec<Exp> -- we only write size, the expressions are written by traverser
      out.writeByte(branches.size)
      super.switchBefore(res, src, ListMap.empty, mode, origin, stack)
    }
    //)

    //[14]InspectSwitch(
    override def inspectSwitchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
      out.writeByte(14)
      //ValueRef
      //todo: assert size & better error handling
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      //PermRef
      //todo: better absent error handling
      out.writeByte(imports.permIndex(stack.getType(src)))
      //Vec<Exp> -- we only write size, the expressions are written by traverser
      out.writeByte(branches.size)
      super.inspectSwitchBefore(res, src, branches, origin, stack)
    }
    //)

    //[15]CopyPack(
    //[16]Pack(
    override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(fetchOffset(15, mode))
      //PermRef
      out.writeByte(imports.permIndex(res.typ))
      //Tag
      val offset = res.typ.projectionExtract {
        case adt:AdtType => adt.ctrs(context).keys.toSeq.indexOf(ctr.name)
        case _ => unexpected("Type checker missed a pack on a not adt type", CodeGen())
      }
      out.writeByte(offset)
      //Vec<ValueRef>
      out.writeByte(srcs.length)
      //todo: assert size & better error handling
      srcs.foreach(src => out.writeShort(stack.getRef(src).get.asInstanceOf[Short]))
      super.pack(res, srcs, ctr, mode, origin, stack)
    }
    //)

    //[17]Invoke(
    override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      out.writeByte(17)
      //PermRef
      out.writeByte(imports.permIndex(func))
      //Vec<ValueRef>
      out.writeByte(params.length)
      //todo: assert size & better error handling
      params.foreach(p => out.writeShort(stack.getRef(p).get.asInstanceOf[Short]))
      super.invoke(res, func, params, origin, stack)
    }
    //)

    //[18]TryInvoke()
    override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      out.writeByte(18)
      //PermRef
      out.writeByte(imports.permIndex(func))
      // Vec<(
      out.writeByte(params.length)
      //todo: assert size & better error handling
      params.foreach({
        case (essential, ref) =>
          //bool
          out.writeBoolean(essential)
          //ValueRef
          out.writeShort(stack.getRef(ref).get.asInstanceOf[Short])
      })
      //)>
      // Exp, Exp -- will be written by traverser
      super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
    }
    //)

    //[19]InvokeSig(
    override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      out.writeByte(19)
      //ValueRef
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      //PermRef
      out.writeByte(imports.permIndex(stack.getType(src)))
      //Vec<ValueRef>
      out.writeByte(params.length)
      //todo: assert size & better error handling
      params.foreach(p => out.writeShort(stack.getRef(p).get.asInstanceOf[Short]))
      super.invokeSig(res, src, params, origin, stack)
    }
    //)

    //[20]TryInvokeSig(
    override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      out.writeByte(20)
      //ValueRef
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      //PermRef
      out.writeByte(imports.permIndex(stack.getType(src)))
      // Vec<(
      out.writeByte(params.length)
      //todo: assert size & better error handling
      params.foreach({
        case (essential, ref) =>
          //bool
          out.writeBoolean(essential)
          //ValueRef
          out.writeShort(stack.getRef(ref).get.asInstanceOf[Short])
      })
      //)>
      // Exp, Exp -- will be written by traverser
      super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
    }
    //)

    //TODO: Repeat & TryRepeat

    //[23]Project(
    override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(23)
      //TypeRef
      out.writeByte(imports.typeIndex(stack.getType(src).projected(origin)))
      //ValueRef
      //todo: assert size & better error handling
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      super.project(res, src, origin, stack)
    }
    //)

    //[24]UnProject(
    override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
      out.writeByte(24)
      //TypeRef
      stack.getType(src) match {
        case p:Type.Projected => out.writeByte(imports.typeIndex(p.inner))
        case _ => unexpected("Type checker missed a unproject on a not projected type", CodeGen())
      }
      //ValueRef
      //todo: assert size & better error handling
      out.writeShort(stack.getRef(src).get.asInstanceOf[Short])
      super.unproject(res, src, origin, stack)
    }
    //)

    //[25]RollBack(
    override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      out.writeByte(25)
      //Vec<ValueRef>
      out.writeByte(params.length)
      params.foreach(src => out.writeShort(stack.getRef(src).get.asInstanceOf[Short]))
      //Vec<TypeRef>
      out.writeByte(resTypes.length)
      resTypes.foreach(typ => out.writeByte(imports.typeIndex(typ)))
      super.rollback(res, resTypes, params, origin, stack)
    }
    //)
  }

}