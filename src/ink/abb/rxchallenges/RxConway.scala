package ink.abb.rxchallenges

import rx.lang.scala.Observable

/*
 * Naive parser of an Observable[Int] to create the next sequence (to be used with flatMap)
 * Is cold observable for flatMap and needs to reset its internal state when onCompleted is triggered
 * otherwise the returned sequence will be wrong
*/
class RxConwayParser {
  var previous = -1
  var sameCount = 0

  def onNext(value: Int) = {
    // variable to store result
    var result: Observable[Int] = null
    if (previous == -1 || previous == value) {
      sameCount = sameCount + 1
      // we still got the same number, so return nothing at all
      result = Observable.empty
    } else {
      // new number, return an observable with two numbers from the next sequence
      result = Observable.just(sameCount, previous)
      sameCount = 1
    }
    previous = value
    result
  }

  def onCompleted() = {
    val result = Observable.just(sameCount, previous)
    reset()
    result
  }

  def reset() = {
    previous = -1
    sameCount = 0
  }

  def onError(e: Throwable) = {
    Observable.empty
  }
}

class RxConway(start: Observable[Int]) {
  def this() = this(Observable.just(1))

  def next = {
    val parser = new RxConwayParser
    val result = start.flatMap(parser.onNext, parser.onError, parser.onCompleted)
    new RxConway(result)
  }

  def subscribe(onNext: Int => Unit, onError: Throwable => Unit, onCompleted: () => Unit) = {
    start.subscribe(onNext, onError, onCompleted)
  }
}

object RxConway {
  def main(args: Array[String]): Unit = {
    val repetitions = Observable.just(0).repeat(20)

    var conwaySequence = new RxConway
    repetitions.subscribe(_ => {
      conwaySequence.subscribe(value => print(value), e => None, () => println())
      conwaySequence = conwaySequence.next
    })
  }
}