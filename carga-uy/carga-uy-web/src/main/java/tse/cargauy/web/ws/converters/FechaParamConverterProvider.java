package tse.cargauy.web.ws.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FechaParamConverterProvider implements ParamConverterProvider {

    private static final ParamConverter<LocalDate> FECHA = new ParamConverter<>() {
        @Override
        public LocalDate fromString(String valor) {
            return valor == null || valor.isBlank() ? null : LocalDate.parse(valor);
        }

        @Override
        public String toString(LocalDate valor) {
            return valor.toString();
        }
    };

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> tipo, Type generico, Annotation[] anotaciones) {
        return tipo == LocalDate.class ? (ParamConverter<T>) FECHA : null;
    }
}
