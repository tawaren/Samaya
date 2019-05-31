package mandalac.types

import java.io.InputStream

trait InputSource {
  def location:Location
  def identifier:Identifier
  def content:InputStream
}
