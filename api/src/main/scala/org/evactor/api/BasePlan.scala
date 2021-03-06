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
package org.evactor.api

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import unfiltered.response.BadRequest
import unfiltered.request.Path
import unfiltered.request.Seg
import scala.io.Source
import unfiltered.response.ResponseString
import unfiltered.request.Params

class BasePlan (system: ActorSystem) extends NettyPlan with Logging {

  val api = new EventAPI(system)
  
//  val indexFile = Source.fromFile("/index.html", "UTF-8").mkString
  
  def intent = {
//    case req @ Path(Seg(Nil)) => try {
//      ResponseString(indexFile)
//    } catch { case e => warn("error while getting index page", e); BadRequest }
    case req @ Path(Seg("api" :: path)) => try {
      val Params(params) = req
      api.doRequest(path, params)
    } catch { case e => warn("error while calling event api", e); BadRequest }
    case _ => ResponseString("Couldn't handle request")
      
  }
  
}