package samaya.types

sealed trait ProcessingMode
case object Deep extends ProcessingMode
case object Fresh extends ProcessingMode
case object Shallow extends ProcessingMode
