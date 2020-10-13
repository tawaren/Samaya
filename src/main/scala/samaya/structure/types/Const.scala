package samaya.structure.types

//todo: we need origin (as Optional)
trait Const {
  def bytes:Array[Byte]
  def enforceSize(size:Short):Option[Lit]
}

//todo: make less ugly
case class Lit(bytes: Array[Byte], isNum:Boolean) extends Const {
  def enforceSize(size:Short):Option[Lit] = {
    if(!isNum) {
      if(bytes.length == size){
        Some(this)
      } else {
        None
      }
    } else {
      if(bytes.length == 0 || bytes(0) >= 0){
        //Positiv number
        if(bytes.length == size) {
          Some(this)
        } else if (bytes.length < size) {
          val fills = size - bytes.length
          Some(Lit(Array.tabulate[Byte](size){ p =>
            if(p < fills) {
              0
            } else {
              bytes(p-fills)
            }
          }, isNum=true))
        } else if(bytes.length > size) {
          val (dropped, res) = bytes.splitAt(bytes.length - size)
          if(dropped.exists(_ != 0)){
            None
          } else {
            Some(Lit(res,isNum=true))
          }
        } else {
          None
        }
      } else {
        //Negative number
        if(bytes.length == size) {
          Some(this)
        } else if (bytes.length < size) {
          val fills = size - bytes.length
          Some(Lit(Array.tabulate[Byte](size){ p =>
            if(p < fills) {
              -1
            } else {
              bytes(p-fills)
            }
          }, isNum=true))
        } else if(bytes.length > size) {
          val (dropped, res) = bytes.splitAt(bytes.length - size)
          if(dropped.exists(_ != -1)){
            None
          } else if(res.length == 0 || res(0) >= 0) {
            None
          } else {
            Some(Lit(res,isNum=true))
          }
        } else {
          None
        }
      }
    }
  }
}