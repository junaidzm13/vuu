package org.finos.vuu.example.ignite.store

trait IgniteStore[T <: Product] {
  def findEntities(sqlFilterQueries: String, sqlSortQueries: String, rowCount: Int, startIndex: Long): Iterator[Product]
}
