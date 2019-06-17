package com.softwaremill.tests

import java.util.concurrent.Executors

import cats.implicits._
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.ExecutionContext

object UsingMonix extends App {
  val ec1 = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("ec1", true)))
  val ec2 = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new NamedThreadFactory("ec2", true)))
  val ec3 = Executors.newCachedThreadPool(new NamedThreadFactory("ec3", true))

  val cs1 = Task.contextShift(Scheduler(ec1))
  val cs2 = Task.contextShift(Scheduler(ec2))

  val printThread = Task { println(Thread.currentThread().getName) }

  val a = Task.async[Unit] { cb =>
    ec3.submit(new Runnable {
      override def run(): Unit = {
        println(Thread.currentThread().getName + " (async)")
        cb(Right(()))
      }
    })
  }

  def run(name: String)(th: Task[_]): Unit = {
    println(s"-- $name --")
    try {
      import monix.execution.Scheduler.Implicits.global
      th.runSyncUnsafe()
    } catch {
      case e: Exception => e.printStackTrace()
    }
    println()
  }

  run("Plain") {
    printThread
  }

  run("Shift") {
    printThread *> Task.shift(ec1) *> printThread *> Task.shift(ec2) *> printThread
  }

  run("Shift shift") {
    Task.shift(ec1) *> Task.shift(ec2) *> printThread
  }

  run("Eval on") {
    printThread *> cs1.evalOn(ec2)(printThread) *> printThread
  }

  run("Eval on eval on") {
    cs2.evalOn(ec2)(cs1.evalOn(ec1)(printThread))
  }

  run("caveat 1") {
    val someEffect = Task.shift(ec1) *> printThread
    printThread *> someEffect *> printThread
  }

  run("async") {
    printThread *> a *> printThread
  }

  run("async shift") {
    a *> Task.shift(ec1) *> printThread
  }

  run("async 2") {
    cs1.evalOn(ec1)(a *> printThread)
  }

  val ae = Task.async[Unit] { cb =>
    ec3.submit(new Runnable {
      override def run(): Unit = {
        println(Thread.currentThread().getName + " (async)")
        cb(Left(new IllegalStateException()))
      }
    })
  }

  run("async error") {
    ae.guarantee(printThread)
  }

  run("async shift error") {
    ae.guarantee(Task.shift(ec1)).guarantee(printThread)
  }
}
