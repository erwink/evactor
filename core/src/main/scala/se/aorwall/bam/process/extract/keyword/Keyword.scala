package se.aorwall.bam.process.extract.keyword

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import akka.actor.Actor
import grizzled.slf4j.Logging
import se.aorwall.bam.model.attributes.HasMessage
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.EventRef
import se.aorwall.bam.model.events.KeywordEvent
import se.aorwall.bam.process.extract.Extractor
import se.aorwall.bam.process.ProcessorConfiguration
import se.aorwall.bam.process.Processor

/**
 * Extracts a path from a message and creates a KeywordEvent object.
 * 
 * Only supports Json and takes the first occurrence of an element. 
 * Will add more functionality and support for Regex and Xpath later.
 */
class Keyword (override val name: String, val eventName: Option[String], val fieldName: String) extends ProcessorConfiguration(name: String){

	def extract (event: Event with HasMessage): Option[KeywordEvent] = {
	  	  
	  lazy val getJsonKeyword: (JsonParser => Option[KeywordEvent]) = (jsonParser: JsonParser) => {
	     if(fieldName == jsonParser.getCurrentName) {
	       jsonParser.nextToken()
	       Some(new KeywordEvent(name, event.id, event.timestamp, jsonParser.getText, Some(EventRef(event))))
	     } else if (jsonParser.nextToken() != JsonToken.END_OBJECT){
	       getJsonKeyword(jsonParser)
	     } else {
	       jsonParser.close()
	       None
	     }
	  }
	  
     val f = new JsonFactory();
	  val jp = f.createJsonParser(event.message);
	  getJsonKeyword(jp)
	}

   override def getProcessor(): Processor = {
     new Extractor(name, eventName, extract)
   }

}
