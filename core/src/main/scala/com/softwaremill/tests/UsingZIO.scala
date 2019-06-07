package com.softwaremill.tests

import java.util.concurrent.Executors

import scalaz.zio._

import scala.concurrent.ExecutionContext

object UsingZIO extends _root_.scala.App {
  val ec1 = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("ec1", true)))
  val ec2 = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("ec2", true)))
  val ec3 = Executors.newCachedThreadPool(new NamedThreadFactory("ec3", true))

  val printThread = IO { println(Thread.currentThread().getName) }

  val a = IO.effectAsync[Any, Nothing, Unit] { cb =>
    ec3.submit(new Runnable {
      override def run(): Unit = {
        println(Thread.currentThread().getName + " (async)")
        cb(UIO.unit)
      }
    })
  }

  def run(name: String)(th: => IO[_, _]): Unit = {
    println(s"-- $name --")
    new DefaultRuntime {}.unsafeRun(th)
    println()
  }

  run("Plain") {
    printThread
  }

  run("Eval on") {
    printThread.on(ec1)
  }

  run("Eval on and back") {
    printThread *> printThread.on(ec1) *> printThread
  }

  run("Eval on eval on") {
    printThread *> printThread.on(ec1).on(ec2) *> printThread
  }

  run("async") {
    printThread *> a *> printThread
  }

  run("async 2") {
    (printThread *> a *> printThread).on(ec1)
  }
}
