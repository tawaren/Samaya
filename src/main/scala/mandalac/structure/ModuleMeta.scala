package mandalac.structure

import mandalac.structure.types.Hash
import mandalac.types.{Identifier, InputSource}

case class ModuleMeta (
  path:Seq[Identifier],
  name:String,
  codeHash:Hash,
  interfaceHash:Hash,
  sourceHash:Hash,
  interface:InputSource,
  code:Option[InputSource],
  sourceCode:Option[InputSource]
)
