import sbt._

object Dependencies {

  object Versions {
    val monix = "3.2.1"
    val mongoScala = "2.9.0"
    val monixKafka = "1.0.0-RC6"
    val kafkaClient = "2.1.0"
    val reactiveStreams = "1.0.3"
    val sphereMongo = "0.10.1"
    val logBackClassic = "1.2.3"
    val logstashEncoder = "5.3"
    val slf4j = "1.7.26"
  }

  lazy val monix = Seq(
    "io.monix" %% "monix" % Versions.monix
  )

  lazy val mongo = Seq(
    "org.mongodb.scala" %% "mongo-scala-driver" % Versions.mongoScala
  )

  /*
  ToDo: Write the stream to Kafka topic. Kafka not used at the moment.
  lazy val kafka = Seq(
    "io.monix" %% "monix-kafka-1x" % Versions.monixKafka,
    "org.apache.kafka" % "kafka-clients" % Versions.kafkaClient exclude("org.slf4j","slf4j-log4j12") exclude("log4j", "log4j")
  )
 */

  lazy val rxStreams = Seq(
    "org.reactivestreams" % "reactive-streams" % Versions.reactiveStreams
  )

  lazy val logging = Seq(
    // we use logback over slf4j
    "ch.qos.logback" % "logback-classic" % Versions.logBackClassic,
    // to output logs as json
    "net.logstash.logback" % "logstash-logback-encoder" % Versions.logstashEncoder,
    // so that lib using jcl will be redirected to slf4j
    "org.slf4j" % "jcl-over-slf4j" % Versions.slf4j)
}