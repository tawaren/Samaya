package samaya.plugin.impl.inter.json


import samaya.structure.{Attribute, Constructor, Field}

case class ConstructorImpl(override val location: JsonLocation, ctr: JsonModel.Constructor, override val tag: Int) extends Constructor with JsonSource{
  override val name: String = ctr.name

  override def attributes: Seq[Attribute] = ctr.attributes

  override val fields: Seq[Field] = {
    val fieldLoc = location.descendProperty("field")
    ctr.fields.zipWithIndex.map(fi => FieldImpl(fieldLoc.descendProperty(fi._1.name), fi._1,fi._2))
  }
}