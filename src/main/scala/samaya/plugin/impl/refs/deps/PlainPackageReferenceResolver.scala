package samaya.plugin.impl.refs.deps

import samaya.plugin.impl.refs.PlainReferenceResolver
import samaya.plugin.service.ReferenceResolver

class PlainPackageReferenceResolver extends PlainReferenceResolver{
  override protected val ext: String = "deps"
  override protected val TYP: ReferenceResolver.ReferenceType = ReferenceResolver.Package
}
