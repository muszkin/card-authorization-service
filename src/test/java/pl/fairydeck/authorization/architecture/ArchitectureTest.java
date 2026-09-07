package pl.fairydeck.authorization.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The package boundaries this single-module project relies on instead of separate build modules.
 * Layers are optional so the rules hold from the first commit and tighten as packages appear.
 */
@AnalyzeClasses(packages = "pl.fairydeck.authorization", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule dependenciesPointInwards = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Adapter").definedBy("..adapter..")
            .layer("Technical").definedBy("..technical..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter")
            .whereLayer("Technical").mayOnlyBeAccessedByLayers("Adapter");

    @ArchTest
    static final ArchRule domainIsFreeOfFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..");

    @ArchTest
    static final ArchRule inboundAndOutboundAdaptersAreSeparate = slices()
            .matching("..adapter.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule outboundAdaptersAreIndependentOfEachOther = slices()
            .matching("..adapter.out.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dependenciesArriveThroughConstructors = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
}
