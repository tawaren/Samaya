package samaya.structure

import samaya.structure.types.{Accessibility, Capability, Permission}

//Note called FunctionDef instead of function as Function is imported in scala prelude
trait FunctionSig extends TypeParameterized with ModuleEntry{
  def index:Int
  def name:String
  def attributes:Seq[Attribute]
  def accessibility:Map[Permission,Accessibility]
  def transactional:Boolean
  def capabilities:Set[Capability]
  def generics:Seq[Generic]
  def generic(index:Int):Option[Generic] = generics.find(gi => gi.index == index)
  def params:Seq[Param]
  def param(index:Int):Option[Param] = params.find(p => p.index == index)
  def results:Seq[Result]
  def result(index:Int):Option[Result]  = results.find(p => p.index == index)
}