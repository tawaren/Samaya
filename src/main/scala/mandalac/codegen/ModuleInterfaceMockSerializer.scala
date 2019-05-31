package mandalac.codegen

import java.io.{ByteArrayOutputStream, DataOutputStream}

import mandalac.structure.types.Hash
import mandalac.structure.types.Type
import mandalac.structure.types.Type.{GenericType, ImageType, RealType}
import mandalac.structure.{DataType, FunctionDef, Module}

import scala.collection.mutable

//Serializes a Module interface without the body
// Allows to use the Rust Code Validator to target interfaces on import
object ModuleInterfaceMockSerializer {

  class ImportCollector(self:Hash) {
    val modules = new mutable.HashMap[Hash,Int]()
    addModule(self)
    val types = new mutable.HashMap[Type,Int]()

    def addModule(hash:Hash):Unit = {
      modules.getOrElseUpdate(hash,{
        assert(modules.size <= 255)
        modules.size
      })
    }
    def addType(typ:Type){
      if(!types.contains(typ)){
        typ match {
          case RealType(module,_,applies) => {
            addModule(module)
            applies.foreach(appl => addType(appl))
          }
          case ImageType(innerTyp) => addType(innerTyp)
          case _ =>
        }
        assert(types.size <= 255)
        types.put(typ,types.size)
      }
    }
    def typeIndex(typ:Type):Byte = types(typ).asInstanceOf[Byte]
  }

  def serialize(mod:Module):Array[Byte] = {
    val inner = new ByteArrayOutputStream()
    val out = new DataOutputStream(inner)
    //Writing the Meta (LargeVec)
    out.writeShort(0)
    val maxDataIndex = mod.dataTypes.map(d => d.index).max
    //Writing the Data types
    out.writeByte(maxDataIndex-1)
    for(i <- 0 to maxDataIndex) {
      mod.dataType(i) match {
        case Some(d) => serializeDataType(out,d, mod.hash)
        case None => serializeDefaultDataType(out)
      }
    }
    val maxFunIndex = mod.functions.map(d => d.index).max
    //Writing the Functions
    out.writeByte(maxFunIndex-1)
    for(i <- 0 to maxFunIndex) {
      mod.function(i) match {
        case Some(f) => serializeFunction(out,f)
        case None => serializeDefaultFunction(out)
      }
    }
    val risks = mod.risks.length
    //Writing the Errors
    out.writeByte(risks)

    //return
    out.close()
    inner.toByteArray
  }

  private def serializeDataType(out:DataOutputStream, data:DataType, self:Hash): Unit ={
    val caps = data.capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){(a,m) => (a|m).asInstanceOf[Byte]}
    //capset
    out.writeByte(caps)
    //generics
    val generics = data.generics.length
    out.writeByte(generics)
    for(i <- 0 until generics) {
      val generic = data.generic(i).get
      if(generic.phantom) {
        //write phantom
        out.writeByte(0)
      } else {
        //write physical
        out.writeByte(1)
        val caps = generic.capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){(a,m) => (a|m).asInstanceOf[Byte]}
        out.writeByte(caps)
      }
    }

    //imports
    val collector = new ImportCollector(self)
    data.constructors.foreach(ctr => ctr.fields.foreach(field => collector.addType(field.typ)))
    //todo: Write import of modules and types

    // risky
    out.writeByte(0)
    //constructors
    val ctrs = data.constructors.length
    out.writeByte(ctrs)
    for(i <- 0 until ctrs) {
      val ctr = data.constructor(i).get
      //fields
      val fields = ctr.fields.length
      out.writeByte(fields)
      for(i <- 0 until fields) {
          val field = ctr.field(i).get
          val typIdx = collector.typeIndex(field.typ)
          out.writeByte(typIdx)
      }
    }
  }

  private def serializeFunction(out:DataOutputStream,function:FunctionDef): Unit = {


  }

  private def serializeDefaultDataType(out:DataOutputStream){
    //todo: shortcut over 6 zeroes (int + short)
    //capset
    out.writeByte(0)
    //generics
    out.writeByte(0)
    //imports
    //modules
    out.writeByte(0)
    //errors
    out.writeByte(0)
    //types
    out.writeByte(0)
    //constructors
    out.writeByte(0)
  }

  private def serializeDefaultFunction(out:DataOutputStream): Unit = {
    //todo: shortcut over 14 zeroes (long + int + short)
    //generics
    out.writeByte(0)
    //visibility (private)
    out.writeByte(0)
    //imports
    //modules
    out.writeByte(0)
    //errors
    out.writeByte(0)
    //types
    out.writeByte(0)
    //functions
    out.writeByte(0)
    //risks
    out.writeByte(0)
    //params
    out.writeByte(0)
    //returns
    out.writeByte(0)
    //code (Ret nothing)
    // ret
    out.writeByte(0)
    // statements
    out.writeShort(0)
    // returns
    out.writeByte(0)
    // drops
    out.writeByte(0)
  }
}
