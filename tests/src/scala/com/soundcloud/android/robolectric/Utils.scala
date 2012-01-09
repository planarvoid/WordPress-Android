package com.soundcloud.android.robolectric

import android.database.Cursor

object Utils {
  implicit def RichCursor(c: Cursor) = new RichCursor(c)

  class RichCursor(underlying: Cursor) extends Iterable[Cursor] {
    def iterator = new Iterator[Cursor] {
      def hasNext = underlying.getCount > 0 && !underlying.isLast
      def next() = {
        underlying.moveToNext()
        underlying
      }
    }
  }
}
