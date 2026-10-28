package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ResponseBot {
    private String texto;
    private List<List<Boton>> botones;
    private byte[] archivoPdf; // <--- NUEVO CAMPO
    private String nombreArchivo; // <--- NUEVO CAMPO

    // Constructor existente
    public ResponseBot(String texto, List<List<Boton>> botones) {
        this.texto = texto;
        this.botones = botones;
    }

    public ResponseBot(String texto) {
        this.texto = texto;
    }

    // Nuevo constructor para PDF
    public ResponseBot(String texto, byte[] archivoPdf, String nombreArchivo) {
        this.texto = texto;
        this.archivoPdf = archivoPdf;
        this.nombreArchivo = nombreArchivo;
    }

    public boolean tieneBotones() {
        return botones != null && !botones.isEmpty();
    }

    public boolean tieneArchivo() {
        return archivoPdf != null && archivoPdf.length > 0;
    }

    // ... tus métodos estáticos fila() y boton() se quedan igual
    public static List<Boton> fila(Boton... b) {
        return List.of(b);
    }

    public static Boton boton(String texto, String callback) {
        return new Boton(texto, callback);
    }

    @Data
    @NoArgsConstructor
    public static class Boton {
        private String texto;
        private String callbackData;

        public Boton(String texto, String callbackData) {
            this.texto = texto;
            this.callbackData = callbackData;
        }
    }
}