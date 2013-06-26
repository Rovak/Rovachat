package rovachat.services

import java.io.{OutputStreamWriter, BufferedWriter, BufferedReader, InputStreamReader}

class ProcessWrapper(cmd: String) {
  var inp: BufferedReader = null
  var out: BufferedWriter = null

  val p = Runtime.getRuntime.exec(cmd)

  inp = new BufferedReader(new InputStreamReader(p.getInputStream()))
  out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()))

  var line = ""

  while (line != null) {
    println(line)
    line = inp.readLine()
  }

  def pipe(msg: String) {
    out.write(msg + "\n")
    out.flush()
    var line = ""
    while (line != null) {
      println(line)
      line = inp.readLine()
    }
  }

  def quite = {
    inp.close()
    out.close()
  }
}

class HubotAdapter {

  var path = "/bin/sh /home/rovak/hubot/bot/bin/hubot"


  def shipit = {

    var wrapper = new ProcessWrapper(path)
    //println(wrapper.pipe("ship it"))

  }

}
