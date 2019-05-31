package mandalac.structure

import java.io.DataOutputStream

import mandalac.codegen.{CodeSerializer, InterfaceSerializer}
import mandalac.structure.meta.FunctionAttribute
import mandalac.structure.types.Visibility.{Private, Protected, Public}

import scala.collection.mutable

//Note called FunctionDef instead of function as Function is imported in scala prelude
trait FunctionDef extends TypeParameterized with Indexed{
  def index:Int
  def name:String
  def attributes:Seq[FunctionAttribute]
  def risks:Set[mandalac.structure.types.Risk]
  def visibility:mandalac.structure.types.Visibility
  def generics:Seq[Generic]
  def generic(index:Int):Option[Generic]
  def params:Seq[Param]
  def param(pos:Int):Option[Param]
  def results:Seq[Result]
  def result(index:Int):Option[Result]
  def code:Option[mandalac.structure.types.Code]

  def serialize(out: DataOutputStream): Unit ={
    //generics
    out.writeByte(generics.length)
    for(i <- generics.indices) {
      generic(i).get.serialize(out)
    }

    //visibility
    visibility match {
      case Private => out.writeByte(0)
      case Protected =>
        val protects = generics.filter(g => g.protection).map(g => g.pos)
        out.writeByte(1)
        out.writeByte(protects.length)
        protects.foreach(p => out.writeByte(p))
      case Public => out.writeByte(2)
    }

    //imports
    val collector = new InterfaceSerializer.ImportCollector()
    risks.foreach(risk => collector.addRisk(risk))
    params.foreach(param => collector.addType(param.typ))
    results.foreach(result => collector.addType(result.typ))
    code.foreach(c => CodeSerializer.collectFunctionBodyDependencies(collector, c, params))
    InterfaceSerializer.serializeImports(out, collector, includeFuns = true)

    //Risks
    out.writeByte(risks.size)
    for(risk <- risks) {
      out.writeByte(collector.riskIndex(risk))
    }

    //needed to resolve offsets of borrows
    val tempStackMap = mutable.Map[String,Int]()

    //Param
    out.writeByte(params.size)
    for(i <- params.indices) {
      val p = param(i).get
      tempStackMap.put(p.name,i)
      out.writeBoolean(p.consumes)
      out.writeByte(collector.typeIndex(p.typ))
    }

    //base offset ofn params
    val resultOffset = params.size

    //Return
    out.writeByte(results.size)
    for(i <- results.indices) {
      val r = result(i).get
      tempStackMap.put(r.name,i+resultOffset)

      //borrows
      //todo: what if not exists
      val borrowSet = r.borrows.map(b => resultOffset + i - tempStackMap(b) - 1)
      out.writeByte(borrowSet.size)
      borrowSet.foreach(b => out.writeByte(b))
      //type
      out.writeByte(collector.typeIndex(r.typ))
    }

    //code
    code match {
      case Some(c) => CodeSerializer.serializeCode(out,collector,c, this)
      case None =>
        // Note this will not compile, but it can be used as dependency
        // If we want to allow compile, we could throw -- but this only works if their is a risk we can throw
        // If we can not throw the last resort would be to return nothing and drop each param (only works on empty return)

        // 5B Body = 1B Kind + 2B Opcodes + 1B returns + 1B drops) -- Same as default
        out.writeInt(0)
        out.writeByte(0)
    }
  }


}

object FunctionDef {
  def serializeDefault(out: DataOutputStream): Unit = {
    //14B = 1B generics + 1B visibility +4B imports (1B modules, 1B Errors, 1B Types, 1B Functions)
    // +1B Risks + 1B Parameters + 1B Returns
    // +5B Body (1B Kind, 2B Opcodes, 1B returns, 1B drops)
    // In default all these bytes are 0
    out.writeLong(0)  //8 Zero Bytes
    out.writeInt(0)   //4 Zero Bytes
    out.writeShort(0) //2 Zero Bytes
  }
}