package pe.edu.upeu.sysvstarbucks.dto;

public record PersonaDni(String dni, String nombres, String apellidoPaterno, String apellidoMaterno) {
    public String nombreCompleto() {
        return String.join(" ", nombres, apellidoPaterno, apellidoMaterno).trim().replaceAll("\\s+", " ");
    }
}
