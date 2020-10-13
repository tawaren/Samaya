package samaya.structure

import samaya.structure.types.CompLink

trait Interface[+T] extends Component {
  this:T =>
  def name: String
  def language: String
  def version: String
  def classifier: Set[String]
  def meta:Meta
  override def toString: String = s"$name -- $meta"

  def link:CompLink
}
