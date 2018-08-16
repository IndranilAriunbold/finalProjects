package elements

import io.gatling.core.Predef._
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.request.ExtraInfo
import io.gatling.core
import io.gatling.http.check.HttpCheckScope.Status

import scala.concurrent.duration._

object Constants {
  val numberOfUsers: Int = 10//System.getProperty("numberOfUsers").toInt //10
  val duration: FiniteDuration = 1//System.getProperty("durationMinutes").toInt.minutes //1
  val pause: FiniteDuration = 3000//System.getProperty("pauseBetweenRequestsMs").toInt.millisecond //3000
  val responseTimeMs = 500
  val responseSuccessPercentage = 99
  //private val url: String = System.getProperty("url")
  val url = "https://api.my-kc.site"
  private val repeatTimes: Int = 1 //System.getProperty("numberOfRepetitions").toInt //1
  private val successStatus: Int = 200
  private val isDebug = false// System.getProperty("debug").toBoolean


  // Define HTTP protocol to be used in simulations
//  val httpProtocol = http
//    .baseURL(url)
//    // Check response code is 200
//    .check(status.is(successStatus))
//    // Extract more info to ease debugging
//    .extraInfoExtractor { extraInfo => List(getExtraInfo(extraInfo)) }

  val httpProtocol = http
    .baseURL(url)
    //.inferHtmlResources() //default protocol is used with the change to fetch all HTTP resources on a page (JS, CSS, images, etc.) with inferHtmlResources()
    .acceptHeader("application/json")
    .disableFollowRedirect //Control over execution




}