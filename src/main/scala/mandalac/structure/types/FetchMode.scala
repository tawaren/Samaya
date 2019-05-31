package mandalac.structure.types

sealed trait FetchMode {}
object FetchMode {
  case object Copy extends FetchMode
  case object Borrow extends FetchMode
  case object Move extends FetchMode
}
