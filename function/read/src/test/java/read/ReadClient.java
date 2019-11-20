package read;

import io.micronaut.function.client.FunctionClient;
import io.micronaut.http.annotation.Body;
import io.reactivex.Single;
import read.model.Read;

import javax.inject.Named;

@FunctionClient
public interface ReadClient {

    @Named("read")
    Single<Read> apply(@Body Read body);

}
