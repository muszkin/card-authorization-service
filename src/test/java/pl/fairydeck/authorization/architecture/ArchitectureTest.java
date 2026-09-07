package pl.fairydeck.authorization.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The package boundaries this single-module project relies on instead of separate build modules.
 */
@AnalyzeClasses(packages = "pl.fairydeck.authorization", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFreeOfFrameworksAndOuterLayers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "..application..", "..adapter..", "..technical..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule inboundAdaptersDoNotReachIntoOutboundAdapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule outboundAdaptersAreIndependentOfEachOther = slices()
            .matching("..adapter.out.(*)..")
            .should().notDependOnEachOther();

    @ArchTest
    static final ArchRule technicalCodeKnowsNothingAboutTheBusiness = noClasses()
            .that().resideInAPackage("..technical..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..application..");

    @ArchTest
    static final ArchRule dependenciesArriveThroughConstructors = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
