package ink.abb.rxchallenges.snake

import javafx.application.{Application, Platform}
import javafx.scene.Scene
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage

import rx.lang.scala.{Observable, Subject}

import scala.collection.mutable.{ListBuffer, Map}
import scala.concurrent.duration._
import scala.util.Random

class RxSnake extends Application {

  import utils._

  val gridSize = 20

  override def start(primaryStage: Stage): Unit = {

    val pane = new GridPane

    val gridModel = Map[(Int, Int), Rectangle]()

    Observable.from(0 until gridSize).subscribe(x => {
      Observable.from(0 until gridSize).subscribe(y => {
        val rectangle = new Rectangle(25, 25)
        rectangle setArcHeight 5
        rectangle setArcWidth 5
        rectangle setFill Color.WHITE
        rectangle setStroke Color.BLACK

        gridModel += (((x, y), rectangle))

        pane.add(rectangle, x, y)
      })
    })

    var snakeModel = ListBuffer((gridSize / 2, gridSize / 2), (gridSize / 2, gridSize / 2 + 1))

    val scene = new Scene(pane, (gridSize + 1) * 25, (gridSize + 1) * 25)

    val updates = Subject[(KeyCode, Boolean)]()

    val eaten = Subject[Boolean]()
    val food = Subject[(Int, Int)]()
    val gameOver = Subject[Boolean]()

    val newPosition = Subject[(Int, Int)]()

    newPosition.combineLatest(food).filter(combined => {
      combined._1 == combined._2
    }).subscribe(_ => {
      eaten.onNext(true)
    })

    newPosition.subscribe(e => {
      if (snakeModel.contains(e)) {
        gameOver.onCompleted()
      }
    })

    food.subscribe(coordinates => {
      gridModel.get(coordinates) match {
        case Some(r) =>
          Platform.runLater(new Runnable() {
            def run = r.setFill(Color.GREEN)
          })
          // we just had food, set eaten to false
          eaten.onNext(false)
        case None =>
      }
    })

    val clock = Observable.interval(150 millis)

    // combine clock with arrow key events
    clock.combineLatest(eaten).combineLatest(arrows(scene)).takeUntil(gameOver)
      .subscribe((data: (((Long, Boolean), KeyEvent))) => {
      // tell the updates subject that there is a new event for us
      val e = data._2
      // (direction, got food?)
      if (e.getEventType == KeyEvent.KEY_PRESSED) {
        updates.onNext((e.getCode, data._1._2))
      }
    })

    updates.subscribe(a => {
      val head = newHead(snakeModel.head, a._1)
      if (head != snakeModel.head && head != snakeModel(1)) {
        if (!a._2) {
          gridModel.get(snakeModel.last) match {
            case Some(r) =>
              Platform.runLater(new Runnable() {
                def run = r.setFill(Color.WHITE)
              })
            case None =>
          }
          snakeModel -= snakeModel.last
        }
        newPosition.onNext(head)
        snakeModel.prepend(head)
        gridModel.get(head) match {
          case Some(r) =>
            Platform.runLater(new Runnable() {
              def run = r.setFill(Color.RED)
            })
          case None =>
        }
      }
    })

    eaten.filter(e => e).subscribe(a => {
      // tell the food subject there is a new food item
      food.onNext(Random.nextInt(gridSize), Random.nextInt(gridSize))
    })

    // spawn some initial food
    eaten.onNext(true)

    primaryStage setTitle "RxSnake"
    primaryStage setScene scene
    primaryStage show
  }

  def newHead(head: (Int, Int), direction: KeyCode): (Int, Int) = {
    var result = head
    direction match {
      case KeyCode.LEFT =>
        result = (head._1 - 1, head._2)
      case KeyCode.RIGHT =>
        result = (head._1 + 1, head._2)
      case KeyCode.UP =>
        result = (head._1, head._2 - 1)
      case KeyCode.DOWN =>
        result = (head._1, head._2 + 1)
      case _ => return result
    }

    // Fix out of bounds positions
    if (result._1 < 0) {
      result = (gridSize - 1, result._2)
    } else if (result._1 >= gridSize) {
      result = (0, result._2)
    }
    if (result._2 < 0) {
      result = (result._1, gridSize - 1)
    } else if (result._2 >= gridSize) {
      result = (result._1, 0)
    }
    result
  }
}

object RxSnake {
  def main(args: Array[String]) = {
    Application.launch(classOf[RxSnake], args: _*)
  }
}