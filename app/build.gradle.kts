plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.ktlint)
  application
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.vertx.lang.kotlin)
  implementation(libs.vertx.web)
  runtimeOnly(
    group = "io.netty",
    name = "netty-resolver-dns-native-macos",
    version = libs.versions.netty.get(),
    classifier = "osx-aarch_64",
  )

  implementation(libs.dsljson)
  kapt(libs.dsljson)

  implementation(libs.argon2.jvm)

  testImplementation(libs.assertj.core)
  testImplementation(libs.archunit.junit5)
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter(libs.versions.junit.get())
    }
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

application {
  mainClass = "io.andrsn.AndrsnMainKt"
}
