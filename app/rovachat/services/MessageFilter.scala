package rovachat.services

import scala.util.matching.Regex
import play.api.Play
import play.api.Play.current
import java.io.File

object MessageFilter {

  val publicFolder = Play.application.path.getPath + "/public/icons"

  var filters = List[String => String](injectIcons)

  def filter(msg: String) = {
    filters.foldLeft(msg) {
      case (result, filter) => filter(result)
    }
  }

  def injectIcons(msg: String) = {
    val icoPattern = new Regex("(:[a-zA-Z_]+:)")
    icoPattern.findAllIn(msg).foldLeft(msg) {
      case (result, ico) =>
        val fileName = ico.substring(1, ico.length - 1) + ".png"
        if (new File(publicFolder + "/" + fileName).exists()) {
           result.replace(ico, s"<img class='ico' src='/assets/icons/$fileName'>")
        } else result
    }
  }


}
