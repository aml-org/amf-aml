package amf.aml.client.scala.model.document

trait ComposedInstancesSupport {
  var composedDialects: Map[String, Dialect] = Map()

  def dialectForComposedUnit(dialect: Dialect): Unit =
    composedDialects += (dialect.id -> dialect)
}
