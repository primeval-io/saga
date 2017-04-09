package io.primeval.saga.annotations.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.saga.action.Result;
import io.primeval.saga.annotations.QueryParameter;
import io.primeval.saga.annotations.Route;
import io.primeval.saga.controller.Controller;
import io.primeval.saga.http.protocol.HttpMethod;

@Component
@Controller
public final class HelloWorldController {

    @Route(method = HttpMethod.GET, uri = "/hello")
    public String hello() { // @DefaultValue("world") @QueryParameter("who") String who) {
        return "Hello 1"; // + who;
    }

//    @Route(method = HttpMethod.POST, uri = "/uppercase")
//    public String uppercase(@Body String input) {
//        return input.toUpperCase();
//    }

    @Route(method = HttpMethod.GET, uri = "/hello2")
    public Result<String> hello2(@QueryParameter Optional<String> who) {
        return Result.ok("Hello " + who);
    }

    @Route(method = HttpMethod.GET, uri = "/hello3")
    public Promise<String> hello3() { // @DefaultValue("world") @QueryParameter("who") String who) {
        return Promises.resolved("Hello 3"); // + who;
    }

    @Route(method = HttpMethod.GET, uri = "/hello4")
    public Promise<Result<String>> hello4() { // @DefaultValue("world") @QueryParameter("who") String who) {
        return Promises.resolved(Result.ok("Hello 4")); // + who;
    }
}
