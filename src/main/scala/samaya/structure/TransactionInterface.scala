package samaya.structure

import samaya.structure.types.{CompLink, SourceId}

class TransactionInterface(override val meta:Meta, private val txt: Transaction) extends Transaction with Interface[Transaction] {
  override def toInterface(meta: Meta): Interface[Transaction] = new TransactionInterface(meta,txt)
  override def link: CompLink = meta.link

  override def name: String = txt.name
  override def language: String = txt.language
  override def version: String = txt.version
  override def classifier: Set[String] = txt.classifier
  override def attributes: Seq[Attribute]= txt.attributes
  override def transactional: Boolean = txt.transactional
  override def params: Seq[Param] = txt.params
  override def results: Seq[Result] = txt.results
  override def src:SourceId = txt.src
  override def isVirtual: Boolean = txt.isVirtual

}
