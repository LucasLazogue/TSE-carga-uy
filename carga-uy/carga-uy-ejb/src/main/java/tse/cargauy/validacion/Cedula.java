package tse.cargauy.validacion;

public final class Cedula {

    // cada uno de los primeros 7 digitos se multiplica por su peso, en este orden
    private static final int[] PESOS = { 2, 9, 8, 7, 6, 3, 4 };

    private Cedula() {
    }

    public static boolean esValida(String cedula) {
        // solo numeros, sin puntos ni guion: 7 u 8 digitos contando el verificador
        if (cedula == null || !cedula.matches("\\d{7,8}")) {
            return false;
        }
        // las cedulas viejas tienen 7 digitos, se completa con un 0 adelante
        String numero = cedula.length() == 7 ? "0" + cedula : cedula;

        // ej. 1.234.567-2: 1*2 + 2*9 + 3*8 + 4*7 + 5*6 + 6*3 + 7*4 = 148
        int suma = 0;
        for (int i = 0; i < PESOS.length; i++) {
            suma += (numero.charAt(i) - '0') * PESOS[i];
        }

        // el verificador es lo que le falta a la suma para llegar a la proxima decena
        // 148 -> 150, falta 2; si la suma ya termina en 0 el verificador es 0
        int verificador = (10 - suma % 10) % 10;
        return verificador == numero.charAt(7) - '0';
    }
}
