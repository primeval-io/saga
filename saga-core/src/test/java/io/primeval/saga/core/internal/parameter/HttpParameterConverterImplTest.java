package io.primeval.saga.core.internal.parameter;

import io.primeval.common.type.TypeTag;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpParameterConverterImplTest {

    private HttpParameterConverterImpl tested;

    @Before
    public void setUp() throws Exception {
        tested = new HttpParameterConverterImpl();
    }

    @Test
    public void shouldCreateStringParam() throws Exception {
        String actual = tested.createParameter("hello", TypeTag.of(String.class)).getValue();
        assertThat(actual).isEqualTo("hello");
    }

    @Test
    public void shouldCreateUUIDParam() throws Exception {
        UUID expected = UUID.randomUUID();
        UUID actual = tested.createParameter(expected.toString(), TypeTag.of(UUID.class)).getValue();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldCreateIntegerParam() throws Exception {
        Integer expected = 42;
        Integer actual = tested.createParameter(expected.toString(), TypeTag.of(Integer.class)).getValue();
        assertThat(actual).isEqualTo(expected);
    }

}
