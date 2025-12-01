package io.andrsn

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchitectureTest {

  private val classes: JavaClasses = ClassFileImporter()
    .withImportOption(ImportOption.DoNotIncludeTests())
    .importPackages("io.andrsn")

  @Test
  fun `matrix package should not access http package`() {
    noClasses()
      .that()
      .resideInAPackage("io.andrsn.matrix..")
      .should()
      .dependOnClassesThat()
      .resideInAPackage("io.andrsn.http..")
      .because("Matrix domain logic should not depend on HTTP infrastructure")
      .check(classes)
  }
}
