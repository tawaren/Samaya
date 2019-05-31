package mandalac.structure.types

sealed trait Code { }

object Code {
  case class Return(ops:Seq[OpCode], returns:Seq[Val]) extends Code
  case class Throw(err:Risk) extends Code
}