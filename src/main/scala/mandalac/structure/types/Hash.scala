package mandalac.structure.types


case class Hash(data:Array[Byte]) {
  //todo: hexencode
  override def toString:String = null
}

object Hash {
  def fromBytes(data: Array[Byte]): Hash = Hash(data)
  //todo: hexdecode
  def fromString(hash:String):Hash = null

  implicit val byteOrdering:Ordering[Hash] = new Ordering[Hash] {
    def compare(ah:Hash, bh: Hash): Int = {
      val a = ah.data
      val b = bh.data
      if (a eq null) {
        if (b eq null) 0
        else -1
      }
      else if (b eq null) 1
      else {
        val L = math.min(a.length, b.length)
        var i = 0
        while (i < L) {
          if (a(i) < b(i)) return -1
          else if (b(i) < a(i)) return 1
          i += 1
        }
        if (L < b.length) -1
        else if (L < a.length) 1
        else 0
      }
    }
  }
}
