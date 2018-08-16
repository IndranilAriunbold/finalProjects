package simulations

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import java.net.URLEncoder
import java.util.UUID

import elements.Login
import elements.Constants
import io.gatling.core.controller.inject.InjectionStep
import jodd.lagarto.dom.Node

class login extends Simulation {

  var defaultInjectionProfile = Array[InjectionStep] (
    rampUsersPerSec(0.001) to Login.usersPerSec during (Login.rampUpPeriod),
    constantUsersPerSec(Login.usersPerSec) during (Login.warmUpPeriod + Login.measurementPeriod)
  )

    setUp(Login.scnLogin.inject(defaultInjectionProfile).protocols(Login.httpProtocol))
      //setUp(Login.scnLogin.inject(atOnceUsers(100))).protocols(Login.httpProtocol)
      //setUp(Login.scnOWNUsername.inject(rampUsersPerSec(10) to 20 during (2 minutes)randomized).protocols(Login.httpProtocol))
      .assertions(
      global.failedRequests.count.lessThan(Login.maxFailedRequests + 1),
      global.responseTime.mean.lessThan(Login.maxMeanReponseTime)
    )


}
