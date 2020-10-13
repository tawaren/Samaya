package samaya.structure

import samaya.structure.Module.Mode

object Module {
  sealed trait Mode
  case class Precompile(id:Int) extends Mode
  case object Elevated extends Mode
  case object Normal extends Mode



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

trait Module extends Component {
  def name:String
  def language:String
  def version:String
  def classifier:Set[String]
  def mode: Mode
  def attributes:Seq[Attribute]
  def dataTypes:Seq[DataDef]
  def dataType(index:Int):Option[DataDef] = Module.defaultLookup(dataTypes,index)
  def signatures:Seq[FunctionSig]
  def signature(index:Int):Option[FunctionSig] = Module.defaultLookup(signatures,index)

  def functions:Seq[FunctionSig]
  def function(index:Int):Option[FunctionSig] = Module.defaultLookup(functions,index)
  def implements:Seq[FunctionSig]
  def implement(index:Int):Option[FunctionSig] = Module.defaultLookup(implements,index)
  def toInterface(meta: Meta): Interface[Module]
}
