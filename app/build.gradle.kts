plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
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
    classifier = "osx-aarch_64"
  )

  implementation(libs.dsljson)
  kapt(libs.dsljson)
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter("5.13.3")
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
