package mandalac.codegen

import java.io.DataOutputStream

import mandalac.codegen.CodeSerializer.ValueBindings
import mandalac.codegen.InterfaceSerializer.ImportCollector
import mandalac.structure.{FunctionDef, Param}
import mandalac.structure.types.Code.{Return, Throw}
import mandalac.structure.types.{Code, Const, FetchMode, Func, Id, OpCode, Risk, Type, TypedId, Val}
import mandalac.structure.types.OpCode._
import mandalac.structure.types.Type.NativeTypeKind
import mandalac.structure.types.Type.NativeTypeKind.PrivateId



object CodeSerializer {
  
  //todo: can we share with validator somehow???
  private case class ValueBindings(ids:Map[Id,Val] = Map[Id,Val](),bindings:Map[Val,  (Int, Type)] = Map[Val,  (Int, Type)]()) {
    def provideVal(id:TypedId):ValueBindings = {
      val index = ids.get(id.id).map(v => v.index+1).getOrElse(0)
      val value = Val(id.id,index)
      copy(ids = ids.updated(id.id,value), bindings = bindings.updated(value,(bindings.size,id.typ)))
    }

    def provideAllVals(ids:Seq[TypedId]):ValueBindings = {
      ids.foldLeft(this){ (c, r) => c.provideVal(r)}
    }

    def getType(v:Val):Type = {
      bindings.get(v) match {
        case None =>  throw new SerialisationFailedException(s"Value with name ${v.id} not found")
        case Some(t) => t._2
      }
    }

    def getRef(v:Val):Int = {
      bindings.get(v) match {
        case None =>  throw new SerialisationFailedException(s"Value with name ${v.id} not found")
        case Some(t) => bindings.size - t._1 - 1
      }
    }
  }
  
  class SerialisationFailedException(err:String) extends Exception
  
  private def collectOpCodeDependencies(imports:ImportCollector, op:OpCode, context:ValueBindings): ValueBindings = {
    op match {
      case Lit(res, value) =>
        imports.addType(res.typ)
        context.provideVal(res)
      case Let(res, block) =>
        //assumes that it is type checked
        val types = collectCodeDependencies(imports,block,context)
        context.provideAllVals(res.zip(types).map(tid => TypedId(tid._1,tid._2)))
      case Fetch(res, src, _) => context.provideVal(TypedId(res,context.getType(src)))
      case Unpack(res, src, _)  =>
        imports.addType(context.getType(src))
        context.provideAllVals(res)
      case Field(res, src, _, _) =>
        imports.addType(context.getType(src))
        context.provideVal(res)
      case Switch(res, src, branches, _) =>
        imports.addType(context.getType(src))
        //assumes that it is type checked
        val types = branches.foldLeft(Seq.empty[Type])((r,b) => {
          val res = collectCodeDependencies(imports, b._2, context.provideAllVals(b._1))
          if(r.isEmpty) {
            res
          } else {
            r
          }
        })
        context.provideAllVals(res.zip(types).map(tid => TypedId(tid._1,tid._2)))
      case Pack(res, _, _, _) =>
        imports.addType(res.typ)
        context.provideVal(res)
      case Invoke(res, func, _) =>
        imports.addFunction(func)
        context.provideAllVals(res)
      case Try(res, tryBlock, branches) =>
        val tryTypes = collectCodeDependencies(imports,tryBlock, context)
        //assumes that it is type checked
        val types = branches.foldLeft(tryTypes)((r,b) => {
          imports.addRisk(b._1)
          val res = collectCodeDependencies(imports,b._2,context)
          if(r.isEmpty) {
            res
          } else {
            r
          }
        })
        context.provideAllVals(res.zip(types).map(tid => TypedId(tid._1,tid._2)))
      case Image(res, src) => context.provideVal(TypedId(res,context.getType(src)))
      case ExtractImage(res, src)=>
        //assumes that it is type checked
        val typ = context.getType(src) match {
          case Type.ImageType(t) => t
          case t => t
        }
        context.provideVal(TypedId(res,typ))
      case ModuleIndex(res) => context.provideVal(TypedId(res,Type.NativeType(NativeTypeKind.PrivateId, Seq.empty)))
      case _ => context
    }
  }


  def collectFunctionBodyDependencies(imports:ImportCollector, code:Code, params:Seq[Param]): Unit ={
    collectCodeDependencies(imports, code, ValueBindings().provideAllVals(params.map(p => TypedId(Id(p.name),p.typ))))
  }

  def collectCodeDependencies(imports:ImportCollector, code:Code, context:ValueBindings): Seq[Type] ={
    code match {
      case Return(ops, rets) => {
        val afterBody = ops.foldLeft(context){(c,o) => collectOpCodeDependencies(imports,o,c)}
        rets.map(v => afterBody.getType(v))
      }
      case Throw(err) => {
        imports.addRisk(err)
        Seq.empty
      }
    }
  }

  def serializeCode(out:DataOutputStream, imports:ImportCollector, code:Code, fun:FunctionDef): Unit ={
    new CodeSerializer(out,imports,fun).serializeFunctionBody(code)
  }
}


private class CodeSerializer(out:DataOutputStream, imports:ImportCollector, fun:FunctionDef) {

  def serializeFunctionBody(code: Code): Unit = {
    //push the params
    val bindings = ValueBindings()
    val bindingWithParams = bindings.provideAllVals(fun.params.map(p => TypedId(Id(p.name), p.typ)))
    serializeExp(code,bindingWithParams)
  }

  private def serializeExp(code: Code, bindings:ValueBindings): Seq[Type] = {
    code match {
      case Return(ops, returns) =>
        out.writeByte(0)
        //todo: assert size
        out.writeShort(ops.length)
        val bindingsAfterBody = ops.foldLeft(bindings){(b,op) => serializeOpCode(op,b)}
        //todo: assert size
        out.writeByte(returns.length)
        returns.foreach(ret => out.writeShort(bindingsAfterBody.getRef(ret)))
        returns.map(ret => bindingsAfterBody.getType(ret))
      case Throw(err) =>
        out.writeByte(1)
        out.writeByte(imports.riskIndex(err))
        Seq.empty
    }
  }

  private def serializeOpCode(op: OpCode, bindings:ValueBindings): ValueBindings = {
    out.writeByte(OpCode.ordinal(op))
    op match {
      case Lit(res, value) => serializeLit(res,value,bindings)
      case Let(res, block) => serializeLet(res,block,bindings)
      case Fetch(res, src, mode) => serializeFetch(res, src, mode,bindings)
      case Discard(trg) => serializeDiscard(trg,bindings)
      case DiscardMany(trgs) => serializeDiscardMany(trgs,bindings)
      case DiscardBorrowed(src, trg) => serializeSteal(src, trg,bindings)
      case Unpack(res, src, mode) => serializeUnpack(res, src, mode,bindings)
      case Field(res, src, pos, mode) => serializeField(res, src, pos, mode,bindings)
      case Switch(res, src, branches, mode) => serializeSwitch(res, src, branches, mode,bindings)
      case Pack(res, src, tag, mode) => serializePack(res, src, tag, mode,bindings)
      case Invoke(res, func, param) => serializeInvoke(res, func, param,bindings)
      case Try(res, tryBlock, branches) => serializeTry(res, tryBlock, branches,bindings)
      case ModuleIndex(res) => serializeModuleIndex(res,bindings)
      case Image(res, src) => serializeImage(res, src,bindings)
      case ExtractImage(res, src) => serializeExtractImage(res, src,bindings)
    }
  }

  private def serializeLit(res: TypedId, value: Const, bindings:ValueBindings): ValueBindings =  {
    //todo: assert size
    out.writeShort(value.bytes.length)
    out.write(value.bytes)
    out.writeByte(imports.typeIndex(res.typ))
    bindings.provideVal(res)
  }

  private def serializeLet(res: Seq[Id], block: Code, bindings:ValueBindings): ValueBindings =  {
    val types = serializeExp(block,bindings)
    //we assume here that it is successfully typed
    bindings.provideAllVals(res.zip(types).map(tid => TypedId(tid._1,tid._2)))
  }

  private def serializeFetch(res: Id, src: Val, mode: FetchMode, bindings:ValueBindings): ValueBindings =  {
    out.writeShort(bindings.getRef(src))
    bindings.provideVal(TypedId(res, bindings.getType(src)))
  }

  private def serializeDiscard(trg: Val, bindings:ValueBindings): ValueBindings =  {
    out.writeShort(bindings.getRef(trg))
    bindings
  }

  private def serializeDiscardMany(trgs: Seq[Val], bindings:ValueBindings): ValueBindings =  {
    trgs.foreach(trg => out.writeShort(bindings.getRef(trg)))
    bindings
  }

  private def serializeSteal(src: Val, borrowedBy: Seq[Val], bindings:ValueBindings): ValueBindings = {
    out.writeShort(bindings.getRef(src))
    borrowedBy.foreach(trg => out.writeShort(bindings.getRef(trg)))
    bindings
  }

  private def serializeUnpack(res: Seq[TypedId], src: Val, mode: FetchMode, bindings:ValueBindings): ValueBindings =  {
    out.writeShort(bindings.getRef(src))
    out.writeByte(imports.typeIndex(bindings.getType(src)))
    bindings.provideAllVals(res)
  }

  private def serializeField(res: TypedId, src: Val, pos: Int, mode: FetchMode, bindings:ValueBindings): ValueBindings =  {
    out.writeShort(bindings.getRef(src))
    out.writeByte(imports.typeIndex(bindings.getType(src)))
    out.writeByte(pos)
    bindings.provideVal(res)
  }

  private def serializeSwitch(res: Seq[Id], src: Val, branches: Seq[(Seq[TypedId],Code)], mode: FetchMode, bindings:ValueBindings): ValueBindings =  {
    out.writeShort(bindings.getRef(src))
    out.writeByte(imports.typeIndex(bindings.getType(src)))
    out.writeByte(branches.length)
    //Assumes that it is type checked
    val types = branches.foldLeft(Seq.empty[Type])((r,b) => {
      val res = serializeExp(b._2,b._1.foldLeft(bindings){(b,r) => b.provideVal(r)})
      if(r.isEmpty) {
        res
      } else {
        r
      }
    })
    bindings.provideAllVals(res.zip(types).map(tid => TypedId(tid._1,tid._2)))
  }

  private def serializePack(res: TypedId, srcs: Seq[Val], tag: Int, mode: FetchMode, bindings:ValueBindings): ValueBindings = {
    out.writeByte(imports.typeIndex(res.typ))
    out.writeByte(tag)
    out.writeByte(srcs.length)
    srcs.foreach(src => out.writeShort(bindings.getRef(src)))
    bindings.provideVal(res)
  }

  private def serializeInvoke(res: Seq[TypedId], func: Func, param: Seq[Val], bindings:ValueBindings): ValueBindings =  {
    out.writeByte(imports.funIndex(func))
    out.writeByte(param.length)
    param.foreach(p => out.writeShort(bindings.getRef(p)))
    bindings.provideAllVals(res)
  }

  private def serializeTry(res: Seq[Id], tryBlock: Code, branches: Seq[(Risk, Code)], bindings:ValueBindings): ValueBindings =  {
    val tryTypes = serializeExp(tryBlock,bindings)
    out.writeByte(branches.length)
    //Assumes that it is type checked
    val types = branches.foldLeft(tryTypes)((r,rc) => {
      out.writeByte(imports.riskIndex(rc._1))
      val branchType = serializeExp(rc._2,bindings)
      if(r.isEmpty) {
        branchType
      } else {
        r
      }
    })
    bindings.provideAllVals(res.zip(types).map(tid => TypedId(tid._1,tid._2)))
  }

  private def serializeModuleIndex(res: Id, bindings:ValueBindings): ValueBindings =  {
    bindings.provideVal(TypedId(res, Type.NativeType(NativeTypeKind.PrivateId, Seq.empty)))
  }

  private def serializeImage(res: Id, src: Val, bindings:ValueBindings): ValueBindings =  {
    bindings.getRef(src)
    val typ = Type.ImageType(bindings.getType(src))
    bindings.provideVal(TypedId(res,typ))
  }

  private def serializeExtractImage(res: Id, src: Val, bindings:ValueBindings): ValueBindings =  {
    bindings.getRef(src)
    //we assume here that everything is type checked and if not an error was produced
    val typ = bindings.getType(src) match {
      case Type.ImageType(t) => t
      case t => t
    }
    bindings.provideVal(TypedId(res,typ))
  }

}