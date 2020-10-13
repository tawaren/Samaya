package samaya.structure

import samaya.structure.types.{CompLink, Hash}
import samaya.types.InputSource



case class Meta(
  codeHash:Option[Hash],
  interfaceHash:Hash,
  sourceHash:Hash,
  interface:InputSource,
  code:Option[InputSource],
  sourceCode:Option[InputSource]
) {
  def link:CompLink = codeHash match {
    case Some(value) => CompLink.ByCode(value)
    case None => CompLink.ByInterface(interfaceHash)
  }
}

