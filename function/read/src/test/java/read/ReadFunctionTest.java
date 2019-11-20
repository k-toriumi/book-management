package read;

import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import read.model.Read;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
public class ReadFunctionTest {

    @Inject
    ReadClient client;

    @Test
    public void testFunction() throws Exception {
    	Read body = new Read();
    	body.setName("read");
        assertEquals("read", client.apply(body).blockingGet().getName());
    }
}
