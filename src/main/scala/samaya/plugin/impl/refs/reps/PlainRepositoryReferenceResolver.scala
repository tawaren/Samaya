package samaya.plugin.impl.refs.reps

import samaya.plugin.impl.refs.PlainReferenceResolver
import samaya.plugin.service.ReferenceResolver

class PlainRepositoryReferenceResolver extends PlainReferenceResolver{
  override protected val ext: String = "reps"
  override protected val TYP: ReferenceResolver.ReferenceType = ReferenceResolver.Repository
}
