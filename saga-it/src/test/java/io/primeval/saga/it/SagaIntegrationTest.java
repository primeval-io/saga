package io.primeval.saga.it;

import static io.primeval.saga.it.TestProvisioningConfig.baseOptions;
import static io.primeval.saga.it.TestProvisioningConfig.dsAndFriends;
import static io.primeval.saga.it.TestProvisioningConfig.extraSnapshotRepository;
import static io.primeval.saga.it.TestProvisioningConfig.guavas;
import static io.primeval.saga.it.TestProvisioningConfig.ninio;
import static io.primeval.saga.it.TestProvisioningConfig.primevalCommonsAndCodex;
import static io.primeval.saga.it.TestProvisioningConfig.primevalCompendium;
import static io.primeval.saga.it.TestProvisioningConfig.primevalJson;
import static io.primeval.saga.it.TestProvisioningConfig.primevalReflexAndAspecio;
import static io.primeval.saga.it.TestProvisioningConfig.saga;
import static io.primeval.saga.it.TestProvisioningConfig.sagaNinio;
import static io.primeval.saga.it.TestProvisioningConfig.sagaThymeleaf;
import static io.primeval.saga.it.TestProvisioningConfig.slf4jLogging;
import static io.primeval.saga.it.TestProvisioningConfig.testingBundles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.http.client.HttpClientRawResponse;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.server.HttpServer;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.router.Router;
import io.primeval.saga.serdes.deserializer.Deserializer;
import reactor.core.publisher.Flux;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SagaIntegrationTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(SagaIntegrationTest.class);

    public static Option exampleApplication() {
        return composite(dsAndFriends(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("io.primeval.saga", "saga-examples").versionAsInProject());
    }

    @Configuration
    public Option[] config() throws Throwable {

        return options(
                extraSnapshotRepository(),
                baseOptions(),
                testingBundles(),
                slf4jLogging(),
                guavas(),
                ninio(),
                primevalCommonsAndCodex(),
                primevalCompendium(),
                primevalReflexAndAspecio(),
                primevalJson(),
                saga(),
                sagaThymeleaf(),
                sagaNinio(),
                exampleApplication());
    }

    @Inject
    Router router;

    @Inject
    ServiceComponentRuntime scr;

    @Inject
    ConfigurationAdmin configAdmin;

    @Inject
    HttpServer httpServer;

    @Inject
    HttpClient httpClient;

    @Inject
    Deserializer deserializer;

    @Test
    // @Ignore
    public void someTest() throws Exception {

        BundleContext bundleContext = FrameworkUtil.getBundle(SagaIntegrationTest.class).getBundleContext();
        // Collection<ComponentDescriptionDTO> componentDescriptionDTOs = scr
        // .getComponentDescriptionDTOs(bundleContext.getBundles());
        //
        // componentDescriptionDTOs.stream().forEach(dto -> {
        // scr.getComponentConfigurationDTOs(dto).stream()
        // .filter(ccd -> ccd.unsatisfiedReferences.length > 0)
        // .forEach(ccd -> System.out.println(dto.name + " " + ccd));
        // });
        //
        Stream.of(bundleContext.getBundles()).forEach(bundle -> {
            System.out.println(bundle.getSymbolicName() + " " +
                    bundle.getVersion() + " " + bundle.getBundleId());
        });

        Hashtable<String, Object> corsConfig = new Hashtable<>();
        corsConfig.put("allowed.hosts", "*");
        corsConfig.put("max.age", 86400);
        corsConfig.put("exposed.headers", new String[0]);
        configAdmin.getConfiguration("saga.cors.filter", "?").update(corsConfig);

        Hashtable<String, Object> i18nConfig = new Hashtable<>();
        i18nConfig.put("defaultLocale", "en");
        i18nConfig.put("supportedLocales", ImmutableList.of("en", "fr"));
        configAdmin.getConfiguration("primeval.compendium.i18n", "?").update(i18nConfig);

        int port = httpServer.start(findRandomOpenPortOnAllLocalInterfaces()).flatMap(x -> httpServer.port())
                .getValue();
        LOGGER.info("Starting Saga server on port {}", port);

        String response = httpClient.to("localhost", port).get("/hello?who=World").execMap(String.class).getValue();

        assertThat(response).isEqualTo("Hello World");

        System.out.println(ImmutableList.class.getClassLoader());

        HttpClientRawResponse rawResp = httpClient.to("localhost", port).get("/ingredients")
                .withHeader(HeaderNames.ACCEPT, MimeTypes.JSON).exec().getValue();
        LOGGER.info(getAsString(rawResp));

        Promise<ImmutableList<String>> igtPms = PromiseHelper
                .wrapPromise(() -> httpClient.to("localhost", port).get("/ingredients")
                        .withHeader(HeaderNames.ACCEPT, MimeTypes.JSON)
                        .execMap(new TypeTag<ImmutableList<String>>() {
                        }));
        PromiseHelper.onFailure(igtPms, t -> t.printStackTrace());

        ImmutableList<String> ingredients = igtPms.getValue();

        assertThat(ingredients).contains("Milk");

        String item42 = httpClient.to("localhost", port).get("/item?id=42").execMap(String.class).getValue();
        assertThat(item42).isEqualTo("Foo");

        HttpClientRawResponse item37 = httpClient.to("localhost", port).get("/item?id=37").exec()
                .getValue();
        assertThat(item37.code).isEqualTo(404);
        assertThat(getAsString(item37)).isEqualTo("Unknown item 37");

        HttpClientRawResponse emptyResp = httpClient.to("localhost", port).get("/emptyResult").exec().getValue();
        assertThat(emptyResp.code).isEqualTo(Status.GONE);
        assertThat(Flux.from(emptyResp.payload.content).collectList().block()).isEmpty();

        HttpClientRawResponse emptyRespFluent = httpClient.to("localhost", port).get("/emptyFluent")
                .withHeader(HeaderNames.ACCEPT, MimeTypes.JSON).exec().getValue();
        assertThat(emptyRespFluent.code).isEqualTo(Status.OK);
        assertThat(Flux.from(emptyRespFluent.payload.content).collectList().block()).isEmpty();

        UUID randomUUID = UUID.randomUUID();
        UUID actualUUID = httpClient.to("localhost", port).get("/location/" + randomUUID + "/foo").execMap(UUID.class)
                .getValue();
        assertThat(actualUUID).isEqualTo(randomUUID);

        if (Boolean.getBoolean("KEEP_SAGA_UP")) {
            keepTestsUp();
        }
        httpServer.stop().getValue();

    }

    private String getAsString(HttpClientRawResponse rawResp) throws InvocationTargetException, InterruptedException {
        return deserializer
                .deserialize(rawResp.payload, TypeTag.of(String.class),
                        SagaIntegrationTest.class.getClassLoader(), MimeTypes.TEXT, Collections.emptyMap())
                .getValue();
    }

    private void keepTestsUp() throws InterruptedException {
        CountDownLatch cd = new CountDownLatch(1);
        cd.await();
    }

    private Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

}