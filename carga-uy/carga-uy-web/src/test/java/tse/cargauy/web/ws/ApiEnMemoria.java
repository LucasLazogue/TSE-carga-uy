package tse.cargauy.web.ws;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;

import jakarta.ws.rs.core.MediaType;
import tse.cargauy.web.ws.converters.FechaParamConverterProvider;

public class ApiEnMemoria {

    private final Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

    public ApiEnMemoria(Object recurso) {
        dispatcher.getProviderFactory().registerProvider(FechaParamConverterProvider.class);
        dispatcher.getProviderFactory().registerProvider(CargaUYExceptionMapper.class);
        dispatcher.getRegistry().addSingletonResource(recurso);
    }

    public MockHttpResponse get(String uri) throws URISyntaxException {
        return invocar(MockHttpRequest.get(uri));
    }

    public MockHttpResponse post(String uri, String json) throws URISyntaxException {
        return invocar(MockHttpRequest.post(uri).contentType(MediaType.APPLICATION_JSON).content(json.getBytes(StandardCharsets.UTF_8)));
    }

    private MockHttpResponse invocar(MockHttpRequest request) {
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        return response;
    }
}
