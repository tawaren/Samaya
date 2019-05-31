package mandalac.structure.types

sealed trait Visibility
object Visibility {
  case object Public extends Visibility
  case object Private extends Visibility
  case object Protected extends Visibility
}
