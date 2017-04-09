package io.primeval.saga.it;

import static io.primeval.saga.it.TestProvisioningConfig.baseOptions;
import static io.primeval.saga.it.TestProvisioningConfig.dsAndFriends;
import static io.primeval.saga.it.TestProvisioningConfig.ninio;
import static io.primeval.saga.it.TestProvisioningConfig.primevalCommonsAndCodex;
import static io.primeval.saga.it.TestProvisioningConfig.primevalJson;
import static io.primeval.saga.it.TestProvisioningConfig.saga;
import static io.primeval.saga.it.TestProvisioningConfig.sagaNinio;
import static io.primeval.saga.it.TestProvisioningConfig.slf4jLogging;
import static io.primeval.saga.it.TestProvisioningConfig.testingBundles;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import com.google.common.collect.ImmutableList;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.client.HttpClient;
import io.primeval.saga.http.server.HttpServer;
import io.primeval.saga.router.Router;
import io.primeval.saga.serdes.deserializer.spi.MediaDeserializer;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SagaIntegrationTest {

    public static Option exampleApplication() {
        return composite(dsAndFriends(),
                mavenBundle("com.google.guava", "guava").versionAsInProject(),
                mavenBundle("io.primeval.saga", "saga-examples").versionAsInProject());
    }

    @Configuration
    public Option[] config() throws Throwable {
        return new Option[] {
                baseOptions(),
                testingBundles(),
                slf4jLogging(),
                ninio(),
                primevalCommonsAndCodex(),
                primevalJson(),
                saga(),
                sagaNinio(),
                exampleApplication(),
        };
    }

    @Inject
    Router router;

    @Inject
    ServiceComponentRuntime scr;

    @Inject
    HttpServer httpServer;

    @Inject
    HttpClient httpClient;

    @Inject
    MediaDeserializer mtd;

    @Test
    // @Ignore
    public void someTest() throws Exception {

        // System.out.println(mtd);
        //
        // Collection<ComponentDescriptionDTO> componentDescriptionDTOs = scr
        // .getComponentDescriptionDTOs(bundleContext.getBundles());
        //
        // componentDescriptionDTOs.stream().forEach(dto -> {
        // scr.getComponentConfigurationDTOs(dto).stream()
        // .filter(ccd -> ccd.unsatisfiedReferences.length > 0)
        // .forEach(ccd -> System.out.println(dto.name + " " + ccd));
        // });
        //
        // Stream.of(bundleContext.getBundles()).forEach(bundle -> {
        // System.out.println(bundle.getSymbolicName() + " " + bundle.getVersion());
        // });

        int port = httpServer.start(findRandomOpenPortOnAllLocalInterfaces()).flatMap(x -> httpServer.port())
                .getValue();

        String response = httpClient.to("localhost", port).get("/hello?who=World").execMap(String.class).getValue();

        assertThat(response).isEqualTo("Hello World");
        
        ImmutableList<String> ingredients = httpClient.to("localhost", port).get("/ingredients").execMap(new TypeTag<ImmutableList<String>>() {}).getValue();

        assertThat(ingredients).contains("Milk");

        httpServer.stop().getValue();

    }

    private Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

}