package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SesionExpiracionScheduler {

    private final ChatbotService chatbotService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    private static final long TIMEOUT_MINUTOS = 30;

    // Se ejecuta cada 5 minutos
    @Scheduled(fixedRate = 300000)
    public void revisarSesionesInactivas() {
        System.out.println(">>> Revisando sesiones inactivas...");

        // Obtenemos el mapa de sesiones del ChatbotService
        Map<Long, SesionUsuario> sesiones = chatbotService.obtenerSesiones();

        for (Map.Entry<Long, SesionUsuario> entry : sesiones.entrySet()) {
            Long chatId = entry.getKey();
            SesionUsuario sesion = entry.getValue();

            // Si está autenticado y ha pasado el tiempo de inactividad
            if (sesion.isAutenticado() &&
                    Duration.between(sesion.getUltimaActividad(), LocalDateTime.now()).toMinutes() >= TIMEOUT_MINUTOS) {

                String mensaje = "⏰ *Sesión cerrada por inactividad*\n\n" +
                        "📌 Han pasado más de 30 minutos sin actividad.\n\n" +
                        "👋 ¡Nos vemos después, " + sesion.getNombre() + "!\n\n" +
                        "🤖 *ITI se despide... ¡Que tengas un excelente día!* 🌟\n\n" +
                        "👉 Cuando quieras volver, solo escribe *hola* para iniciar sesión de nuevo.";

                // Enviamos el mensaje a Telegram
                enviarMensajeTelegram(chatId, mensaje);

                // Cerramos la sesión
                sesiones.remove(chatId);
                System.out.println(">>> Sesión cerrada automáticamente para: " + sesion.getNombre());
            }
        }
    }

    private void enviarMensajeTelegram(Long chatId, String texto) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", texto);
            body.put("parse_mode", "Markdown");

            restTemplate.postForObject(url, body, String.class);
            System.out.println(">>> 📩 Mensaje de despedida enviado a chatId: " + chatId);
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje de despedida: " + e.getMessage());
        }
    }
}