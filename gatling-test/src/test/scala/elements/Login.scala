package elements

// import io.gatling.core.Predef._ is mandatory as there are tools used to convert data types

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

import io.gatling.commons.stats.KO
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import jodd.lagarto.dom.Node


object Login {

  val usersPerSec : Double = System.getProperty("usersPerSec").toDouble
  val rampUpPeriod: Int = System.getProperty("rampUpPeriod").toInt
  val warmUpPeriod: Int = System.getProperty("warmUpPeriod").toInt
  val measurementPeriod :Int = System.getProperty("measurementPeriod").toInt
  val filterResults : Boolean = System.getProperty("filterResults").toBoolean
  val userThinkTime : Int = System.getProperty("userThinkTime").toInt

  // Computed timestamps
  val  simulationStartTime  : Long = System.currentTimeMillis()
  val  warmUpStartTime : Long = simulationStartTime + rampUpPeriod * 1000
  val measurementStartTime  : Long = warmUpStartTime + warmUpPeriod * 1000
  val measurementEndTime : Long = measurementStartTime + measurementPeriod * 1000

  // assertion properties
  val maxFailedRequests : Int = Integer.getInteger("maxFailedRequests", 0).toInt
  val maxMeanReponseTime : Int = Integer.getInteger("maxMeanReponseTime", 300).toInt

//  val usersPerSec=1.0
//  val rampUpPeriod=15
//  val warmUpPeriod=15
//  val measurementPeriod=30
//  val filterResults=false
//  val userThinkTime=0
//  val refreshTokenPeriod=0
//  val refreshTokenCount=1
//  val badLoginAttempts=1

//  val usersPerSec= Integer.getInteger("usersPerSec", 1).toDouble
//  val rampUpPeriod= Integer.getInteger("rampUpPeriod", 15).toInt
//  val warmUpPeriod= Integer.getInteger("warmUpPeriod", 15).toInt
//  val measurementPeriod= Integer.getInteger("measurementPeriod", 30).toInt
//  val filterResults= System.getProperty("filterResults")
//  val userThinkTime= Integer.getInteger("userThinkTime", 0).toInt
//  val refreshTokenPeriod= Integer.getInteger("refreshTokenPeriod", 0).toInt
//  val refreshTokenCount= Integer.getInteger("refreshTokenCount", 1).toInt
//  val badLoginAttempts= Integer.getInteger("badLoginAttempts", 1).toInt

  val userCredentials = csv("user_credentials.csv").random
  val newUserCredentials = csv("new_user_information.csv").random
  // pick user from feeder
  // and set username, password, and deviceId
  //var pickUser = feed(userCredentials)

  /*
        ENDPOINTS definition
   */

  val uri_KeycloakBASE = "https://api.my-kc.site"
  val client_id_db = "product-app"
  val client_secret_db = "cdb2d2cd-f1c1-4fd9-a206-2b10eadc054e"
  val realm = "WebApps"
  //redirect_uri

  val uriRedirect_webdb = "http://api.my-kc-webdb.site/"
  //var backEndService = "http://ec2-34-247-98-48.eu-west-1.compute.amazonaws.com:8080/demo/all"
  val backEndService = "https://api.my-kc-webdb.site/users"
  //val authUri = "https://api.my-kc.site/auth/realms/"+realm+"/protocol/openid-connect/auth"
  val authUri = uri_KeycloakBASE + "/auth/realms/" + realm + "/protocol/openid-connect/auth"
  //val tokenUri = "https://api.my-kc.site/auth/realms/"+realm+"/protocol/openid-connect/token"
  val tokenUri = uri_KeycloakBASE + "/auth/realms/" + realm + "/protocol/openid-connect/token"
  val logoutUri = uri_KeycloakBASE + "/auth/realms/" + realm + "/protocol/openid-connect/logout"

  /*
      Define the HTTP requests sent in your scenario
   */

  val UI_HEADERS = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml",
    "Accept-Encoding" -> "gzip, deflate",
    "Accept-Language" -> "en-US,en;q=0.5",
    "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0 Chrome/65.0.3325.181")

  val json_headers = Map(
    "Accept" -> "*/*",
    "Accept-Encoding" -> "gzip, deflate, br",
    "Accept-Language" -> "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6"
  )

  // Define HTTP protocol to be used in simulations
  //  val httpProtocol = http
  //    .baseURL(url)
  //    // Check response code is 200
  //    .check(status.is(successStatus))
  //    // Extract more info to ease debugging
  //    .extraInfoExtractor { extraInfo => List(getExtraInfo(extraInfo)) }

  val httpProtocol = http
    .baseURL(uri_KeycloakBASE)
    //.inferHtmlResources() //default protocol is used with the change to fetch all HTTP resources on a page (JS, CSS, images, etc.) with inferHtmlResources()
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6")
    .doNotTrackHeader("1")
    .disableFollowRedirect //Control over execution


  /*
     Send Request for loginPage
     After transformation of the response we can extract loginurl and use it in the next request: perform Login
 */
  val browserOpensLoginPage = exec(http("get_my_login_page")
    .get(authUri)
    .headers(UI_HEADERS)
    .queryParam("login", "true")
    .queryParam("response_type", "code")
    .queryParam("client_id", client_id_db)
    .queryParam("state", UUID.randomUUID().toString())
    .queryParam("redirect_uri", uriRedirect_webdb)
    .queryParam("scope", "openid")
    // if already logged in the check will fail with:
    // status.find.is(200), but actually found 302
    // The reason is that instead of returning the login page we are immediately redirected to the app that requested authentication
    .check(status.is(200))
    .check(css("#kc-form-login")
      .ofType[Node]
      .transform(variabe => {
        variabe.getAttribute("action")
      })
      .saveAs("loginurl")))
      //.regex("href=\"/auth(/realms/[^\"]*/login-actions/registration[^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("registration-link")))
  //
    .exitHereIfFailed

  /*
      Perform Login
      pass our username and password and from the responce,
      exctract the code that we can exckange for the access token.
  */

  val performLoginWithCorrectCredentials = exec(http("execute_login_on_my_login_page")
    .post("${loginurl}")
    .formParam("username", "user")
    .formParam("password", "user")
    .check(status.is(302))
    .check(currentLocation.saveAs("currentloaction"))

    .check(header("Location")
      .transform(t => {
        t.substring(t.indexOf("code=") + 5, t.length)
      })
      .saveAs("code"))
    .check(header("Location").saveAs("nextPage"))
)
    /*.exec { session =>
      println("My nextpage")
      println(session("nextPage").as[String])
      session
    }*/
    .exitHereIfFailed


val performLoginWithFalseCred = exec (http ("execute_login_on_my_login_page_FALSE")
.post ("${loginurl}")
//.headers(headers_3)
.headers (UI_HEADERS)
.formParam ("username", "${username}")
.formParam ("password", "wrongwrongwrong")
.check (status.is (200) )
).exitHereIfFailed


val exchangeCodeForToken = exec (http ("fetch_token")
.post (tokenUri)
.headers (json_headers)
//.headers(UI_HEADERS)
//.header("Referer", "nextPage")
.formParam("code", "${code}")
.formParam ("grant_type", "authorization_code")
.formParam ("client_id", client_id_db)
.formParam ("client_secret", client_secret_db)
.formParam ("redirect_uri", uriRedirect_webdb)
.check (status.is (200) )
.check (jsonPath ("$..access_token").saveAs ("token") )
.check (jsonPath ("$..refresh_token").saveAs ("refreshToken") )
).exitHereIfFailed


val getTokens = exec (http("get_Tokens")
    .post(tokenUri)
    .headers(json_headers)
  .formParam ("client_id", client_id_db)
  .formParam ("client_secret", client_secret_db)
  .formParam ("grant_type", "client_credentials")
    .check(status.is(200))
   .check(jsonPath("$..access_token").saveAs("accessToken"))
    .check (jsonPath ("$..refresh_token").saveAs ("refreshToken"))
)/*.exec { session =>
  println("accessTOKEN BEGIN")
  println(session("accessToken").as[String])
  println("accessTOKEN END")
  session
}*/.exitHereIfFailed


val getWelcomepageAfterSuccessfulLogin = exec (http ("get_welcome_page_after_login")
.get ("${nextPage}")
//.headers(headers_3)
.headers (UI_HEADERS)
.check (status.is (200) )

).exitHereIfFailed

val callBackendServiceWithCode = exec (http ("call_backend_with_code")
.get (backEndService)
.header ("Authorization", "bearer ${code}")
//.headers(json_headers)
.headers (UI_HEADERS)
.check (status.is (200) )
).exitHereIfFailed

val callBackendServiceWithToken = exec (http ("call_backend_with_token")
.get (backEndService)
//.get("${backendLink}")
.header ("Authorization", "bearer ${newtoken}")
.headers (UI_HEADERS)
  //.headers(json_headers)
.check (status.is (200) )
).exitHereIfFailed

val refreshToken = exec (http ("refresh_token")
.post (tokenUri)
//.headers(json_headers)
.headers (UI_HEADERS)
//.header("Referer", "nextPage")
.formParam ("refresh_token", "${refreshToken}")
.formParam ("grant_type", "refresh_token")
//.formParam("client_id", client_id)
//.formParam("client_secret", client_secret)
//.formParam("redirect_uri", uriRedirect_webapp)
.formParam ("client_id", client_id_db)
.formParam ("client_secret", client_secret_db)
.formParam ("redirect_uri", uriRedirect_webdb)
.check (status.is (200) )
.check (jsonPath ("$..access_token").saveAs ("newtoken") )
.check (jsonPath ("$..refresh_token").saveAs ("refreshToken") )
)/*.exec { session =>
  println("newtoken BEGIN")
  println(session("newtoken").as[String])
  println("newtoken END")
  session}*/
  .exitHereIfFailed

val logout = exec (http ("Browser logout")
.get (logoutUri)
.headers (UI_HEADERS)
.queryParam ("redirect_uri", uriRedirect_webdb)
.check (status.is (302), header ("Location").is (uriRedirect_webdb) )
).exitHereIfFailed

  val browserOpensRegistrationPage = exec(http("Browser to Registration Endpoint")
        .get("${keycloakServer}${registration-link}")
        .headers(UI_HEADERS)
        .check(
          status.is(200),
          regex("action=\"([^\"]*)\"").find.transform(_.replaceAll("&amp;", "&")).saveAs("registration-form-uri"))
      )
      .exitHereIfFailed

  val browserPostsRegistrationDetails = exec(http("Browser posts registration details")
        .post("${registration-form-uri}")
        .headers(UI_HEADERS)
        .formParam("firstName", "${firstName}")
        .formParam("lastName", "${lastName}")
        .formParam("email", "${email}")
        .formParam("username", "${username}")
        .formParam("password", "${password}")
        .formParam("password-confirm", "${password}")
        .check(status.is(302), header("Location").saveAs("nextPage")))
      .exitHereIfFailed
  /*
  val adapterExchangesCodeForTokens() = exec(oauth("Adapter exchanges code for tokens")
        .authorize("${login-redirect}",
          session => List(new Cookie("OAuth_Token_Request_State", session("state").as[String], 0, null, null)))
        .authServerUrl("${keycloakServer}")
        .resource("${clientId}")
        .clientCredentials("${secret}")
        .realm("${realm}")
        //.realmKey(Loader.realmRepresentation.getPublicKey)
      )
  */


val scnReg = scenario ("registerAndLogoutScenario")
.feed (newUserCredentials)
.exec (browserOpensLoginPage
, pause (userThinkTime)
, browserOpensRegistrationPage
  , pause (userThinkTime)
  , browserPostsRegistrationDetails
, pause(userThinkTime)
//, exchangeCodeForToken
//, pause (2)
  ,getWelcomepageAfterSuccessfulLogin
, logout
)

val continue = new AtomicBoolean (true)

val scnLogin = scenario ("LoginAndLogoutWithRandomUser")
.feed (userCredentials)
.exec (
doIf (session => continue.get () ) {
  exec (browserOpensLoginPage
  , pause (userThinkTime)
  //, performLoginWithFalseCred
  //, pause (5)
  , performLoginWithCorrectCredentials
  , pause (userThinkTime)
  , getWelcomepageAfterSuccessfulLogin
  , pause (userThinkTime)
  //, exchangeCodeForToken
    ,getTokens
  , refreshToken
  , pause (userThinkTime)
  //, callBackendServiceWithToken
  //, pause (2)
  , logout
  ).exec ((session: io.gatling.core.session.Session) => {
  if (session.status == KO) {
  continue.set (false)
}
  session
})
})

}
