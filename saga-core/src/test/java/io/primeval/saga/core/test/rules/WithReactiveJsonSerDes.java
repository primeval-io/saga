package io.primeval.saga.core.test.rules;

import org.junit.rules.ExternalResource;

import io.primeval.common.test.rules.TestResource;
import io.primeval.json.jackson.test.rules.WithJacksonMapper;
import io.primeval.saga.core.internal.serdes.deserializer.base.json.JsonReactiveDeserializer;
import io.primeval.saga.core.internal.serdes.serializer.base.json.JsonReactiveSerializer;

public class WithReactiveJsonSerDes extends ExternalResource implements TestResource {

    private WithJacksonMapper withJacksonMapper;
    private JsonReactiveDeserializer jsonReactiveDeserializer;
    private JsonReactiveSerializer jsonReactiveSerializer;

    public WithReactiveJsonSerDes(WithJacksonMapper withJacksonMapper) {
        this.withJacksonMapper = withJacksonMapper;
    }

    @Override
    public void before() throws Throwable {
        JsonReactiveDeserializer jsonReactiveDeserializer = new JsonReactiveDeserializer();
        jsonReactiveDeserializer.setJsonDeserializer(withJacksonMapper.getJacksonMapper());

        JsonReactiveSerializer jsonReactiveSerializer = new JsonReactiveSerializer();
        jsonReactiveSerializer.setJsonSerializer(withJacksonMapper.getJacksonMapper());

        this.jsonReactiveDeserializer = jsonReactiveDeserializer;
        this.jsonReactiveSerializer = jsonReactiveSerializer;
    }

    @Override
    public void after() {
    }

    public JsonReactiveDeserializer getJsonReactiveDeserializer() {
        return jsonReactiveDeserializer;
    }

    public JsonReactiveSerializer getJsonReactiveSerializer() {
        return jsonReactiveSerializer;
    }

}
