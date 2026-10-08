package tse.cargauy.nodotracking;

public class PublicadorException extends Exception{
    public PublicadorException(String mensaje, Throwable causa){
        super(mensaje, causa);
    }
}
