package samaya.plugin.service

import samaya.plugin.Plugin

trait PluginCategory[T <: Plugin] {
  def name:String
  def interface:Class[T]
}
