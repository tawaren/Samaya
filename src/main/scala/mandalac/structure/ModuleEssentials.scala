package mandalac.structure

import java.io.DataOutputStream

import mandalac.compilation.ErrorHandler
import mandalac.structure.meta.ModuleAttribute
import mandalac.structure.types.Hash

object ModuleEssentials {
   def defaultLookup[T <: Indexed](seq:Seq[T], index:Int):Option[T] = {
     if(seq.size > index) {
       val res = seq(index)
       if(res.index != index) {
         None
       } else {
         Some(res)
       }
     } else {
       None
     }
   }
}

//Todo: Make the interfaces more general so that they can represent Interfaces and concrete code classes
trait ModuleEssentials {
  def name:String
  def language:String
  def version:String
  def classifier:String
  def attributes:Seq[ModuleAttribute]
  def functions:Seq[FunctionDef]
  def function(index:Int):Option[FunctionDef] = ModuleEssentials.defaultLookup(functions,index)
  def dataTypes:Seq[DataType]
  def dataType(index:Int):Option[DataType] = ModuleEssentials.defaultLookup(dataTypes,index)
  def risks:Seq[Risk]
  def risk(index:Int):Option[Risk] = ModuleEssentials.defaultLookup(risks,index)

  //todo: Better on Object???
  def serialize(out:DataOutputStream){
    //Writing the Meta (LargeVec)
    //todo: Something else here???
    out.writeShort(0)
    val maxDataIndex = dataTypes.map(d => d.index).max
    //Writing the Data types
    out.writeByte(maxDataIndex-1)
    for(d <- dataTypes) {
      d.serialize(out)
    }
    val maxFunIndex = functions.map(d => d.index).max
    //Writing the Functions
    out.writeByte(maxFunIndex-1)
    for(i <- 0 to maxFunIndex) {
      function(i) match {
        case Some(f) => f.serialize(out)
        case None => serializeMissingFunction(out,i)
      }
    }
    //Writing the Errors
    out.writeByte(risks.length)
  }

  protected def serializeMissingDatatype(out:DataOutputStream, index:Int): Unit = {
    ErrorHandler.unexpected("Missing implementation for DataType")
  }

  protected def serializeMissingFunction(out:DataOutputStream, index:Int): Unit = {
    ErrorHandler.unexpected("Missing implementation for Function")
  }

}
