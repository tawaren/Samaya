package samaya.structure.types

import samaya.structure.Attribute

case class TypedId(id:Id, attributes:Seq[Attribute], typ:Type)
