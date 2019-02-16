package teammates.test.driver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Settings for component tests.
 */
public final class TestProperties {

    /** The directory where HTML files for testing pages are stored. */
    public static final String TEST_PAGES_FOLDER = "src/test/resources/pages";

    /** The directory where HTML files for testing email contents are stored. */
    public static final String TEST_EMAILS_FOLDER = "src/test/resources/emails";

    /** The directory where CSV files for testing CSV contents are stored. */
    public static final String TEST_CSV_FOLDER = "src/test/resources/csv";

    /** The directory where JSON files used to create data bundles are stored. */
    public static final String TEST_DATA_FOLDER = "src/test/resources/data";

    /** The value of "test.persistence.timeout" in test.properties file. */
    public static final int PERSISTENCE_RETRY_PERIOD_IN_S;

    /** Indicates whether "God mode" is activated. */
    public static final boolean IS_GODMODE_ENABLED;

    private TestProperties() {
        // access static fields directly
    }

    static {
        Properties prop = new Properties();
        try {
            try (InputStream testPropStream = Files.newInputStream(Paths.get("src/test/resources/test.properties"))) {
                prop.load(testPropStream);
            }

            IS_GODMODE_ENABLED = Boolean.parseBoolean(prop.getProperty("test.godmode.enabled", "false"));

            PERSISTENCE_RETRY_PERIOD_IN_S = Integer.parseInt(prop.getProperty("test.persistence.timeout"));

        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

}
