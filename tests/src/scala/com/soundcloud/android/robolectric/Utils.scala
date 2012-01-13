package com.soundcloud.android.robolectric

import android.database.Cursor
import com.xtremelabs.robolectric.Robolectric
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse
import org.apache.http.HttpResponse

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
    def getString(s: String) = underlying.getString(underlying.getColumnIndex(s));
    def getLong(s: String) = underlying.getLong(underlying.getColumnIndex(s));
  }

  case class Response(code: Int, body: String)
  implicit def response2HttpResponse(r: Response): HttpResponse = new TestHttpResponse(r.code, r.body)


  def respond(uri: String, response: Response) {
    Robolectric.addHttpResponseRule(uri, response)
  }

}
