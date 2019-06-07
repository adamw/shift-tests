package com.softwaremill.tests

import java.util.concurrent.Executors

import cats.effect.IO
import cats.implicits._

import scala.concurrent.ExecutionContext

object UsingCatsEffect extends App {
  val ec1 = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("ec1", true)))
  val ec2 = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("ec2", true)))
  val ec3 = Executors.newCachedThreadPool(new NamedThreadFactory("ec3", true))

  val cs1 = IO.contextShift(ec1)
  val cs2 = IO.contextShift(ec2)

  val printThread = IO { println(Thread.currentThread().getName) }

  val a = IO.async[Unit] { cb =>
    ec3.submit(new Runnable {
      override def run(): Unit = {
        println(Thread.currentThread().getName + " (async)")
        cb(Right(()))
      }
    })
  }

  def run(name: String)(th: IO[_]): Unit = {
    println(s"-- $name --")
    th.unsafeRunSync()
    println()
  }

  run("Plain") {
    printThread
  }

  run("Shift") {
    printThread *> IO.shift(ec1) *> printThread *> IO.shift(ec2) *> printThread
  }

  run("Shift shift") {
    IO.shift(ec1) *> IO.shift(ec2) *> printThread
  }

  run("Eval on") {
    printThread *> cs1.evalOn(ec2)(printThread) *> printThread
  }

  run("Eval on eval on") {
    cs2.evalOn(ec2)(cs1.evalOn(ec1)(printThread))
  }

  run("caveat 1") {
    val someEffect = IO.shift(ec1) *> printThread
    printThread *> someEffect *> printThread
  }

  run("async") {
    printThread *> a *> printThread
  }

  run("async 2") {
    cs1.evalOn(ec1)(a *> printThread)
  }

  val ae = IO.async[Unit] { cb =>
    ec3.submit(new Runnable {
      override def run(): Unit = {
        println(Thread.currentThread().getName + " (async)")
        cb(Left(new IllegalStateException()))
      }
    })
  }

  run("async shift error") {
    ae.guarantee(IO.shift(ec1)).guarantee(printThread)
  }
}
