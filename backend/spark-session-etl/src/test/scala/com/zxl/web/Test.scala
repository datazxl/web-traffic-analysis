package com.zxl.web

object Test {
  def main(args: Array[String]): Unit = {
    val date = new java.util.Date(1529235807000L);
    val date2 = new java.util.Date()
    println(date2.getTime);
    println((1557324877639L - 1529235807000L) / 1000 / 60 / 60 / 24)
  }
}
