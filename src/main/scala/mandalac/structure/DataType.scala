package mandalac.structure

import java.io.DataOutputStream

import mandalac.codegen.InterfaceSerializer
import mandalac.structure.meta.DataTypeAttribute
import mandalac.structure.types.Capability

trait DataType extends TypeParameterized with Indexed{

  def index:Int
  def name:String
  def attributes:Seq[DataTypeAttribute]
  def generics:Seq[Generic]
  def generic(index:Int):Option[Generic]
  def constructors:Seq[Constructor]
  def constructor(tag:Int):Option[Constructor]
  def capabilities:Set[Capability]

  def serialize(out: DataOutputStream): Unit ={
    val caps = capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
    //capset
    out.writeByte(caps)
    //generics
    out.writeByte(generics.length)
    for(i <- generics.indices) {
      generic(i).get.serialize(out)
    }

    //imports
    val collector = new InterfaceSerializer.ImportCollector
    constructors.foreach(ctr => ctr.fields.foreach(field => collector.addType(field.typ)))
    InterfaceSerializer.serializeImports(out, collector, includeFuns = false)

    // risks
    out.writeByte(0)
    //constructors
    val ctrs = constructors.length
    out.writeByte(ctrs)
    for(i <- 0 until ctrs) {
      val ctr = constructor(i).get
      //fields
      val fields = ctr.fields.length
      out.writeByte(fields)
      for(i <- 0 until fields) {
        val field = ctr.field(i).get
        //todo: fetch type index
        val typIdx:Byte = collector.typeIndex(field.typ)
        out.writeByte(typIdx)
      }
    }
  }

}

object DataType {
  def serializeDefault(out: DataOutputStream): Unit = {
    //6B = 1B capabilities + 1B generics +3B imports (1B modules, 1B Errors, 1B Types) +1B Constructors
    // In default all these bytes are 0
    out.writeInt(0)   //4 Zero Bytes
    out.writeShort(0) //2 Zero Bytes
  }
}
