package pl.fairydeck.authorization.acceptance;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/** Runs every feature under this package against the complete application on real infrastructure. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("pl/fairydeck/authorization/acceptance")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "pl.fairydeck.authorization.acceptance")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
class AcceptanceScenarios {
}
