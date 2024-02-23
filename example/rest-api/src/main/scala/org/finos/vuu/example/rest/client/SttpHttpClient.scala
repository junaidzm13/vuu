package org.finos.vuu.example.rest.client

import sttp.client4.Response
import sttp.client4.quick._
import sttp.model.Uri

class SttpHttpClient {
  def get(uri:Uri): Response[String] = {
    quickRequest
      .get(uri)
      .send()
  }
}
