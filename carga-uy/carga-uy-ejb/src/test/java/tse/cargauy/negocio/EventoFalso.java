package tse.cargauy.negocio;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;

// junta lo que se dispara; en el servidor lo reciben los observadores cuando se confirma la transaccion
public class EventoFalso<T> implements Event<T> {

    public final List<T> disparados = new ArrayList<>();

    @Override
    public void fire(T evento) {
        disparados.add(evento);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U evento) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U evento, NotificationOptions opciones) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Event<T> select(Annotation... calificadores) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtipo, Annotation... calificadores) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtipo, Annotation... calificadores) {
        throw new UnsupportedOperationException();
    }
}
