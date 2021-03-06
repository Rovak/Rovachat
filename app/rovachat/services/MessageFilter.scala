package rovachat.services

import scala.util.matching.Regex
import play.api.Play
import play.api.Play.current
import java.io.File
import org.apache.commons.lang3.StringEscapeUtils

object MessageFilter {

  val publicFolder = Play.application.path.getPath + "/public/icons"

  var filters = List[String => String](
    filterHtml,
    injectIcons,
    injectImages)

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

  def injectImages(msg: String) = {
    val imgUrlPattern = new Regex("(https?:\\/\\/.*\\.(?:png|jpg|jpeg|gif))")
    imgUrlPattern.findAllIn(msg).foldLeft(msg) {
      case (result, imageUrl) => result.replace(imageUrl, s"<img src='$imageUrl'>")
    }
  }

  def filterHtml(msg: String) = {
    StringEscapeUtils.escapeHtml4(msg)
  }


}
