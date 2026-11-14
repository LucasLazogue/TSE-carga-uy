package tse.cargauy.adaptadores.pdi;

// lo que el central usa de la respuesta de DNIC: sexo, nacionalidad y el resto no hacen falta y no se guardan
public record PersonaPdi(String nombres, String apellidos, String fechaNacimiento) {
}
