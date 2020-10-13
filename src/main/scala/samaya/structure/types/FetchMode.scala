package samaya.structure.types

sealed trait FetchMode {}
object FetchMode {
  case object Copy extends FetchMode
  case object Move extends FetchMode
  //Tells transformers that they are free to change this value
  case object Infer extends FetchMode
}
