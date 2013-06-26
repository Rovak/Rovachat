package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import rovachat.services.HubotAdapter

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class HubotSpec extends Specification {
  
  "Hubot" should {
    
    "return a ship it message" in {
      var hubot = new HubotAdapter
      hubot.shipit
    }
  }
}