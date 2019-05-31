package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.ConstructorAttribute
import mandalac.structure.{Constructor, Field}

case class ConstructorImpl(ctr: JsonModel.Constructor, override val tag: Int) extends Constructor{
  override val name: String = ctr.name

  override def attributes: Seq[ConstructorAttribute] = ???

  override val fields: Seq[Field] = ctr.fields.zipWithIndex.map(fi => FieldImpl(fi._1,fi._2))

  override def field(index: Int): Option[Field] = fields.find(r => r.pos == index)
}