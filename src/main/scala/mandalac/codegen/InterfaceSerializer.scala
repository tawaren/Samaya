package mandalac.codegen

import java.io.{ByteArrayOutputStream, DataOutputStream}

import mandalac.structure.Module
import mandalac.structure.types.Hash
import mandalac.structure.types.Type.{GenericType, ImageType, NativeTypeKind, RealType}
import mandalac.structure.types._

import scala.collection.mutable

//Serializes a Module interface without the body
//Todo: rename this + hierarchy to code generator
object InterfaceSerializer {

  class ImportCollector() {
    val modules = new mutable.HashMap[Hash,Int]()
    val types = new mutable.LinkedHashMap[Type,Int]()
    val errors = new mutable.LinkedHashMap[Risk,Int]()
    val funs = new mutable.LinkedHashMap[Func,Int]()

    //todo: asserts for lenghts
    def addModule(hash:Hash):Unit = modules.getOrElseUpdate(hash,modules.size+1) //Note: Module0 is the local thats way +1
    def addType(typ:Type){
      if(!types.contains(typ)){
        typ match {
          case RealType(module,_,applies) =>
            addModule(module)
            applies.foreach(appl => addType(appl))
          case ImageType(innerTyp) => addType(innerTyp)
          case _ =>
        }
        types.put(typ,types.size)
      }
    }

    def addFunction(func:Func){
      if(!funs.contains(func)){
        func match {
          case Func.Local(_,applies) =>
            applies.foreach(addType)
          case Func.Module(mod,_,applies) =>
            addModule(mod)
            applies.foreach(addType)
          case Func.Native(_,applies) =>
            applies.foreach(addType)
        }
        funs.put(func,funs.size)
      }
    }

    def addRisk(risk:Risk){
      if(!errors.contains(risk)){
        risk match {
          case Risk.Module(module, _) => addModule(module)
          case _ =>
        }
        errors.put(risk,errors.size)
      }
    }

    def modIndex(hash:Hash):Byte = modules(hash).asInstanceOf[Byte]
    def funIndex(func:Func):Byte = funs(func).asInstanceOf[Byte]
    def typeIndex(typ:Type):Byte = types(typ).asInstanceOf[Byte]
    def riskIndex(risk:Risk):Byte = errors(risk).asInstanceOf[Byte]
  }

  def serializeModule(mod:Module):Array[Byte] = {
    val inner = new ByteArrayOutputStream()
    val out = new DataOutputStream(inner)
    mod.serialize(out)
    out.close()
    inner.toByteArray
  }

  def serializeImports(out:DataOutputStream, imports:ImportCollector, includeFuns:Boolean): Unit ={
    out.writeByte(imports.modules.size-1) //this is implicit
    imports.modules.drop(1).foreach( hp => out.write(hp._1.data))
    out.writeByte(imports.errors.size)
    imports.errors.foreach( err => {
      err._1 match {
        case Risk.Local(offset) =>
          out.writeByte(0);                       //Mark Module
          out.writeByte(0)                        //ModRef
          out.writeByte(offset)                   //Offset
        case Risk.Module(module, offset) =>
          out.writeByte(0);                       //Mark Module
          out.writeByte(imports.modIndex(module)) //ModRef
          out.writeByte(offset)                   //Offset
        case Risk.Native(kind) =>
          out.writeByte(1);                       //Mark Module
          out.writeByte(kind.ident)               //Native ident
      }
    })

    out.writeByte(imports.types.size)
    imports.types.foreach( {
      case (Type.LocalType(offset, applies),_) =>
        out.writeByte(0) //Mark Real
        out.writeByte(0) //Mark Module
        out.writeByte(0) //ModRef
        out.writeByte(offset) //Offset
        out.writeByte(applies.length) //applies len
      case (Type.RealType(module, offset, applies),_) =>
        out.writeByte(0) //Mark Real
        out.writeByte(0) //Mark Module
        out.writeByte(imports.modIndex(module)) //ModRef
        out.writeByte(offset) //Offset
        out.writeByte(applies.length) //applies len
        applies.foreach(t => out.writeByte(imports.typeIndex(t))) //applies
      case (Type.NativeType(kind,applies),_) =>
        out.writeByte(0) //Mark Real
        out.writeByte(1) //Mark Native
        out.writeByte(kind.ident) //KindType
        kind match {
          case NativeTypeKind.Data(size) => out.writeShort(size)
          case NativeTypeKind.SInt(size) => out.writeByte(size)
          case NativeTypeKind.UInt(size) => out.writeByte(size)
          case NativeTypeKind.Tuple(elems) => out.writeByte(elems)
          case NativeTypeKind.Alternative(elems) => out.writeByte(elems)
          case _ =>
        }

        out.writeByte(applies.length) //applies len
        applies.foreach(t => out.writeByte(imports.typeIndex(t))) //applies

      case (Type.ImageType(inner),_) =>
        out.writeByte(1) //Mark Image
        out.writeByte(imports.typeIndex(inner)) //Non Image type
      case (Type.GenericType(_,offset),_) =>
        out.writeByte(2) //Mark Generic
        out.writeByte(offset) //Offset
    })

    if(includeFuns){
      out.writeByte(imports.funs.size)
      imports.funs.foreach( fun => {
        fun._1 match {
          case Func.Local(offset, applies) =>
            out.writeByte(0)      //Mark Module
            out.writeByte(0)      //ModRef
            out.writeByte(offset) //Offset
            applies.foreach(appl => out.writeByte(imports.typeIndex(appl))) //applies
          case Func.Module(module, offset, applies) =>
            out.writeByte(0)//Mark Module
            out.writeByte(imports.modIndex(module))//ModRef
            out.writeByte(offset)//Offset
            applies.foreach(appl => out.writeByte(imports.typeIndex(appl))) //applies
          case Func.Native(kind, applies) =>
            out.writeByte(1)//Mark Native
            out.writeByte(kind.ident)//Native ident
            applies.foreach(appl => out.writeByte(imports.typeIndex(appl))) //applies
        }

      })
    }
  }
}
