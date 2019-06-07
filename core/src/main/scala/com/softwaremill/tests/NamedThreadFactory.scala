package com.softwaremill.tests

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(name: String, daemon: Boolean) extends ThreadFactory {

  private val parentGroup = Option(System.getSecurityManager).fold(Thread.currentThread().getThreadGroup)(_.getThreadGroup)

  private val threadGroup = new ThreadGroup(parentGroup, name)
  private val threadCount = new AtomicInteger(1)
  private val threadHash = Integer.toUnsignedString(this.hashCode())

  override def newThread(r: Runnable): Thread = {
    val newThreadNumber = threadCount.getAndIncrement()

    val thread = new Thread(threadGroup, r)
    thread.setName(s"$name-$newThreadNumber-$threadHash")
    thread.setDaemon(daemon)

    thread
  }

}
