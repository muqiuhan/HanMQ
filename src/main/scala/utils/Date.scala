package utils

import java.text.SimpleDateFormat
import java.util

object Date:
  inline def getLocalTime(): String =
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new util.Date())
end Date
