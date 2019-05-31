package mandalac.structure.types

sealed trait OpCode {

}

//todo: Try to get rid of TypeId where possible
//      May require some more inferenz in let, switch, try
//      Pack and Unpack last if at all

//todo: some have still a TypeId as res, branch input, because it would need a ctr/function lookup in serializer otherwise
object OpCode {
  case class Lit(res:TypedId, value:Const) extends OpCode
  case class Let(res:Seq[Id], block:Code) extends OpCode
  case class Fetch(res:Id, src:Val, mode:FetchMode) extends OpCode
  case class Discard(trg:Val) extends OpCode
  case class DiscardMany(trg:Seq[Val]) extends OpCode
  case class DiscardBorrowed(value:Val, borrowedBy:Seq[Val]) extends OpCode
  case class Unpack(res:Seq[TypedId], src:Val, mode:FetchMode) extends OpCode
  case class Field(res:TypedId, src:Val, pos:Int, mode:FetchMode) extends OpCode
  case class Switch(res:Seq[Id], src:Val, branches:Seq[(Seq[TypedId],Code)], mode:FetchMode) extends OpCode
  case class Pack(res:TypedId, src:Seq[Val], tag:Int, mode:FetchMode) extends OpCode
  case class Invoke(res:Seq[TypedId], func:Func, param:Seq[Val]) extends OpCode
  case class Try(res:Seq[Id], tryBlock:Code, branches:Seq[(Risk,Code)]) extends OpCode
  case class ModuleIndex(res: Id) extends OpCode
  case class Image(res:Id, src:Val) extends OpCode
  case class ExtractImage(res:Id, src:Val) extends OpCode

  //we put this here to ensure that if order changes we do not miss this
  def ordinal(code:OpCode):Byte = {

    def fetchOffset(start:Byte, mode:FetchMode): Byte = {
      (start + (mode match {
        case FetchMode.Copy => 0
        case FetchMode.Borrow => 1
        case FetchMode.Move => 2
      })).toByte
    }

    code match {
      case Lit(_, _) => 0
      case Let(_, _) => 1
      case Fetch(_, _, m) => fetchOffset(2, m)
      case Discard(_) => 5
      case DiscardMany(_) => 6
      case DiscardBorrowed(_, _) => 7
      case Unpack(_, _, m) => fetchOffset(8, m)
      case Field(_, _, _, m) => fetchOffset(11, m)
      case Switch(_, _, _, m) => fetchOffset(14, m)
      case Pack(_, _, _, m) => fetchOffset(17, m)
      case Invoke(_, _, _) => 20
      case Try(_, _, _) => 21
      case ModuleIndex(_) => 22
      case Image(_, _) => 23
      case ExtractImage(_, _) => 24
    }
  }
}