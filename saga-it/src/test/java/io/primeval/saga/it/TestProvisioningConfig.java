package io.primeval.saga.it;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.url;

import org.ops4j.pax.exam.Constants;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;

public final class TestProvisioningConfig {
    public static Option baseOptions() {
        return composite(
                frameworkStartLevel(org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE),
                bootDelegationPackage("sun.*"), systemPackage("sun.misc"), cleanCaches(),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link")
                        .startLevel(START_LEVEL_SYSTEM_BUNDLES));
    }

    public static Option extraSnapshotRepository() {
        String SNAPSHOT_REPO = System.getenv("EXTRA_SNAPSHOT_REPOSITORY");
        if (SNAPSHOT_REPO != null) {
            return CoreOptions.repository(SNAPSHOT_REPO).allowSnapshots();
        }
        return CoreOptions.composite();
    }

    public static Option testingBundles() {
        return composite(junitBundles(), mavenBundle("org.assertj", "assertj-core").versionAsInProject());
    }

    public static Option slf4jLogging() {
        return composite(systemProperty("logback.configurationFile").value("file:" + PathUtils.getBaseDir() +
                "/src/test/resources/logback.xml"),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject().startLevel(START_LEVEL_SYSTEM_BUNDLES),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject()
                        .startLevel(Constants.START_LEVEL_SYSTEM_BUNDLES),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject()
                        .startLevel(Constants.START_LEVEL_SYSTEM_BUNDLES));
    }

    public static Option dsAndFriends() {
        return composite(mavenBundle("org.apache.felix", "org.apache.felix.log", "1.0.1"),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.1.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.8"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.2"));
    }

    public static Option ninio() {
        return composite(mavenBundle("com.google.guava", "guava", "18.0"),
                mavenBundle("com.typesafe", "config").versionAsInProject(),
                mavenBundle("com.davfx.ninio", "ninio-util").versionAsInProject(),
                mavenBundle("com.davfx.ninio", "ninio-core").versionAsInProject(),
                mavenBundle("com.davfx.ninio", "ninio-dns").versionAsInProject(),
                mavenBundle("com.davfx.ninio", "ninio-http").versionAsInProject());
    }

    public static Option primevalCommonsAndCodex() {
        return composite(mavenBundle("org.reactivestreams", "reactive-streams").versionAsInProject(),
                mavenBundle("io.projectreactor", "reactor-core").versionAsInProject(),
                mavenBundle("io.primeval", "primeval-commons").versionAsInProject(),
                mavenBundle("io.primeval", "primeval-codex").versionAsInProject());
    }

    public static Option primevalJson() {
        return composite(mavenBundle("io.primeval", "primeval-json").versionAsInProject(),
                mavenBundle("io.primeval", "primeval-jackson-guava18", "1.0.0-SNAPSHOT"),
                mavenBundle("io.primeval", "primeval-jackson-guava21", "1.0.0-SNAPSHOT"),
                mavenBundle("io.primeval", "primeval-json-jackson").versionAsInProject());
    }

    public static Option saga() {
        return composite(mavenBundle("io.primeval.saga", "saga-api").versionAsInProject(),
                mavenBundle("io.primeval.saga", "saga-guava").versionAsInProject(),
                mavenBundle("io.primeval.saga", "saga-core").versionAsInProject(),
                mavenBundle("io.primeval.saga", "saga-base-annotations").versionAsInProject());
    }

    public static Option sagaNinio() {
        return mavenBundle("io.primeval.saga", "saga-ninio").versionAsInProject();
    }

}
