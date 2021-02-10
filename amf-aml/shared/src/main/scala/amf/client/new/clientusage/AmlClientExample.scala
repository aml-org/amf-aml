package amf.client.`new`.clientusage

import amf.client.`new`.{AmfEnvironment, AmfResult, AmlEnvironment}
import amf.core.remote.Aml

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AmlClientExample {

  def registerDialects: Future[AmfResult] = {
    AmlEnvironment.withDialect("file://dialect1.yaml").flatMap { e =>
      e.getInstance()
        .parse("file://myDialectInsta.yaml", Some(Aml))
    }
  }
}
