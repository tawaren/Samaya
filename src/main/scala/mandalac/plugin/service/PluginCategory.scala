package mandalac.plugin.service

import mandalac.plugin.Plugin

trait PluginCategory[T <: Plugin] {
  def name:String
  def interface:Class[T]
}
