package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.FunctionAttribute
import mandalac.structure.types.{Code, Hash, Risk, Visibility}
import mandalac.structure
import mandalac.structure.types.Risk.NativeRisk
import mandalac.structure.{Generic, Param, Result}

case class FunctionImpl(input:JsonModel.Function) extends structure.FunctionDef{
  override val index: Int = input.offset
  override val name: String = input.name

  override def attributes: Seq[FunctionAttribute] = ???

  override val risks: Set[Risk] = input.risks.flatMap(r => if(r.module == "$native") {
    //todo: produce error if not their
    val offset = r.offset.get
    NativeRisk.fromIdent(offset).map(Risk.Native)
  } else {
    //todo: produce error if not their
    val offset = r.offset.get
    Some(Risk.Module(Hash.fromString(r.module), offset))
  })

  override val visibility: Visibility =  if(input.generics.exists(g => g.isProtected)) Visibility.Protected else Visibility.Private
  override val generics: Seq[Generic] = input.generics.zipWithIndex.map(gi => GenericImpl(gi._1,gi._2))
  override def generic(index: Int): Option[Generic] = generics.find(gi => gi.pos == index)

  override val params: Seq[Param] = TypeBuilder.inContext(generics){
    input.params.zipWithIndex.map(pi => ParamImpl(pi._1,pi._2))
  }
  override def param(pos: Int): Option[Param] = params.find(p => p.pos == index)

  override val results: Seq[Result] = TypeBuilder.inContext(generics){
    input.returns.zipWithIndex.map(ri => ReturnImpl(ri._1,input,ri._2))
  }

  override def result(index: Int): Option[Result] =  results.find(r => r.pos == index)
  override val code: Option[Code] = None
}
