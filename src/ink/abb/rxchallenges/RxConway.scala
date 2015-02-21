package ink.abb.rxchallenges

import rx.lang.scala.{Observable}

class RxConwayParser {
  var previous = -1
  var sameCount = 0

  def onNext(value: Int): Observable[Int] = {
    var result: Observable[Int] = null
    if (previous == -1 || previous == value) {
      sameCount = sameCount + 1
      result = Observable.empty
    } else {
      result = Observable.just(sameCount, previous)
      sameCount = 1
    }
    previous = value
    return result
  }

  def onCompleted(): Observable[Int] = {
    return Observable.just(sameCount, previous)
  }

  def onError(e: Throwable): Observable[Int] = {
    return Observable.empty
  }
}

class RxConway(start: Observable[Int]) {
  def this() = this(Observable.just(1))

  def next: RxConway = {
    val parser = new RxConwayParser
    val result = start.flatMap(parser.onNext, parser.onError, parser.onCompleted)
    return new RxConway(result)
  }

  start.subscribe(value => print(s"$value "), e => None, () => println())
}

object RxConway {
  def main(args: Array[String]): Unit = {
    (new RxConway).next.next.next.next.next.next.next.next.next.next.next.next
  }
}