package se.aorwall.bam.storage.cassandra
import akka.actor.ActorSystem
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.cassandra.service.CassandraHostConfigurator
import me.prettyprint.cassandra.utils.TimeUUIDUtils
import me.prettyprint.hector.api.factory.HFactory
import se.aorwall.bam.model.attributes.HasState
import se.aorwall.bam.model.events.Event
import se.aorwall.bam.model.events.KeywordEvent
import se.aorwall.bam.storage.EventStorage
import me.prettyprint.cassandra.serializers.UUIDSerializer
import org.joda.time.DateTime
import me.prettyprint.hector.api.beans.ColumnSlice
import se.aorwall.bam.storage.EventStorage
import se.aorwall.bam.model.events.EventRef


class KeywordEventStorage(system: ActorSystem, prefix: String) extends CassandraStorage (system, prefix) with EventStorage {

   type EventType = KeywordEvent
  
	def this(system: ActorSystem) = this(system, "KeywordEvent")

	override val columnNames = List("name", "id", "timestamp", "keyword", "eventRef")

	override def storeEvent(event: Event): Boolean = event match {
		  
		case keywordEvent: KeywordEvent => {
	
			val mutator = HFactory.createMutator(keyspace, StringSerializer.get)
			val timeuuid = TimeUUIDUtils.getTimeUUID(event.timestamp)
	
			val key = "%s/%s".format(keywordEvent.name, keywordEvent.keyword)
	
			// column family: KeywordEvent
			// row key: keyword name
			// column key: timeuuid
			// column value: keyword value			
			mutator.incrementCounter(keywordEvent.name, EVENT_CF, keywordEvent.keyword, 1)
			
			// column family: KeywordEventTimeline
			// row key: keyword name + value
			// column key: event timestamp
			// value: ref event id		
			mutator.insert(key, TIMELINE_CF, HFactory.createColumn(timeuuid, keywordEvent.eventRef.getOrElse("").toString, UUIDSerializer.get, StringSerializer.get))
	
			// column family: EventCount
			// row key: event name + state + ["year";"month":"day":"hour"]
			// column key: timestamp
			// value: counter
			val time = new DateTime(event.timestamp)
			val count = new java.lang.Long(1L)
			val year = new java.lang.Long(new DateTime(time.getYear, 1, 1, 0, 0).toDate.getTime)
			val month = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, 1, 0, 0).toDate.getTime)
			val day = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, 0, 0).toDate.getTime)
			val hour = new java.lang.Long(new DateTime(time.getYear, time.getMonthOfYear, time.getDayOfMonth, time.getHourOfDay, 0).toDate.getTime)
	
			mutator.incrementCounter("%s/%s".format(key, YEAR), COUNT_CF, year, count)
			mutator.incrementCounter("%s/%s".format(key, MONTH), COUNT_CF, month, count)
			mutator.incrementCounter("%s/%s".format(key, DAY), COUNT_CF, day, count)
			mutator.incrementCounter("%s/%s".format(key, HOUR), COUNT_CF, hour, count)
	
			true
		}
		case _ => false
	}
	
	def eventToColumns(event: Event): List[(String, String)] = {		  
		event match {  
			case keywordEvent: KeywordEvent => 
				("name", event.name) :: 
				("id", event.id) :: 
				("timestamp", event.timestamp.toString) :: 
				("keyword", keywordEvent.keyword) :: 
				getEventRefCol("eventRef", keywordEvent.eventRef)			
		}
	}
	
	def getEventRef(keywordEvent: KeywordEvent): List[(String, String)] = keywordEvent.eventRef match {
	  case e: EventRef => ("eventRef", e.toString) :: Nil
	  case None => Nil
	} 
	
   def columnsToEvent(columns: ColumnSlice[String, String]): Event = {
	 val get = getValue(columns) _	
	 new KeywordEvent(get("name"), 
			 			get("id"),
			 			get("timestamp").toLong,
			 			get("keyword"),
			 			getEventRef(columns, "eventRef"))
	}
}