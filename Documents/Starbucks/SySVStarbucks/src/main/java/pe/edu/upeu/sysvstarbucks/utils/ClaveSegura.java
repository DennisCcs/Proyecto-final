package pe.edu.upeu.sysvstarbucks.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Genera y verifica hashes PBKDF2 para no guardar contraseñas en texto plano. */
public final class ClaveSegura {
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int BITS_HASH = 256;
    private static final int BYTES_SALT = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClaveSegura() { }

    public static String hash(String clave) {
        byte[] salt = new byte[BYTES_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(clave.toCharArray(), salt, ITERACIONES);
        return "pbkdf2-sha256$" + ITERACIONES + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificar(String clave, String hashGuardado) {
        if (clave == null || hashGuardado == null) return false;
        try {
            String[] partes = hashGuardado.split("\\$", -1);
            if (partes.length != 4 || !"pbkdf2-sha256".equals(partes[0])) return false;
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[3]);
            byte[] hashCalculado = derivar(clave.toCharArray(), salt, iteraciones);
            return MessageDigest.isEqual(hashEsperado, hashCalculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] clave, byte[] salt, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(clave, salt, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", e);
        } finally {
            especificacion.clearPassword();
            java.util.Arrays.fill(clave, '\0');
        }
    }
}
