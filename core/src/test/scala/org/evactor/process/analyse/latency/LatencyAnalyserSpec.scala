/*
 * Copyright 2012 Albert Örwall
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.evactor.process.analyse.latency

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import akka.actor.Actor._
import akka.actor.ActorSystem
import akka.testkit.TestActorRef._
import akka.testkit.TestProbe._
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import akka.util.duration._
import org.evactor.model.events.AlertEvent
import org.evactor.model.events.RequestEvent
import org.evactor.model.Success
import org.evactor.process.analyse.window.LengthWindow
import org.evactor.EvactorSpec
import org.evactor.model.Message
import org.evactor.publish.TestPublication

@RunWith(classOf[JUnitRunner])
class LatencyAnalyserSpec(_system: ActorSystem) 
  extends TestKit(_system)
  with EvactorSpec
  with BeforeAndAfterAll{

  def this() = this(ActorSystem("LatencyAnalyserSpec"))

  val name = "name"
  val eventName = "event"
  val correlationid = "correlationid"

  override protected def afterAll(): scala.Unit = {
    _system.shutdown()
  }

  "A LatencyAnalyser" must {

    "alert when the average latency of the incoming activities exceeds the specified max latency" in {
      val probe = TestProbe()

      val latencyActor = TestActorRef(new LatencyAnalyser(Nil, new TestPublication(probe.ref), 5))

      latencyActor ! new Message("", Set(), createRequestEvent(0L, None, None, "corr", "comp", Success, 4)) // avg latency 4ms
      latencyActor ! new Message("", Set(), createRequestEvent(1L, None, None, "corr", "comp", Success, 5))  // avg latency 4.5ms
      probe.expectNoMsg
      latencyActor ! new Message("", Set(), createRequestEvent(3L, None, None, "corr", "comp", Success, 9)) // avg latency 6ms, trig alert!

//      probe.expectMsg(200 millis, new Alert(eventName, "Average latency 6ms is higher than the maximum allowed latency 5ms", true))
      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      latencyActor.stop
    }

    "alert when the average latency of the incoming activities exceeds the max latency within a specified length window" in {
      val probe = TestProbe()

      val latencyActor = TestActorRef(new LatencyAnalyser(Nil, new TestPublication(probe.ref), 60) with LengthWindow {
        override val noOfRequests = 2
      })
      
      latencyActor ! new Message("", Set(), createRequestEvent(1L, None, None, "corr", "comp", Success, 10)) // avg latency 10ms
      latencyActor ! new Message("", Set(), createRequestEvent(2L, None, None, "corr", "comp", Success, 110)) // avg latency 55ms
      latencyActor ! new Message("", Set(), createRequestEvent(3L, None, None, "corr", "comp", Success, 40)) // avg latency 75ms, trig alert!

//      probe.expectMsg(100 millis, new Alert(eventName, "Average latency 75ms is higher than the maximum allowed latency 60ms", true))
      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      latencyActor ! new Message("", Set(), createRequestEvent(4L, None, None, "corr", "comp", Success, 60)) // avg latency 55ms, back to normal!

 //     probe.expectMsg(100 millis, new Alert(eventName, "back to normal!", false))
      probe.expectMsgAllClassOf(200 millis, classOf[AlertEvent])

      latencyActor.stop
    }

  } 
}