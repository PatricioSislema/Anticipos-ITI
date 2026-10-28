package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.OrdenTransferenciaRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final OrdenTransferenciaRepository ordenTransferenciaRepository;
    private final UsuarioService usuarioService;

    private static final int DIAS_PLAZO = 5;

    // Se ejecuta cada 6 horas (00:00, 06:00, 12:00, 18:00)
    @Scheduled(cron = "0 0 0/6 * * *")
    public void revisarLiquidacionesProximasAVencer() {
        System.out.println("=== RECORDATORIO: Revisando vencimientos de anticipos ===");

        List<Tramite> transferidos = tramiteRepository.findByEstado("Transferido");

        if (transferidos.isEmpty()) {
            System.out.println("No hay trámites transferidos pendientes de liquidar.");
            return;
        }

        LocalDate hoy = LocalDate.now();
        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");

        for (Tramite tramite : transferidos) {
            LocalDate fechaAcreditacion = obtenerFechaAcreditacion(tramite.getTramiteId());
            if (fechaAcreditacion == null) continue;

            long diasTranscurridos = ChronoUnit.DAYS.between(fechaAcreditacion, hoy);
            long diasRestantes = DIAS_PLAZO - diasTranscurridos;

            // Aviso preventivo a los 3 días (faltan 2 días para vencer)
            if (diasRestantes == 2) {
                enviarNotificacion(tramite, "⚠️ Aviso: Anticipo próximo a vencer",
                        "Quedan 2 días para liquidar el anticipo. Evita el descuento en tu rol de pagos.",
                        correoContadora);
            }
            // Aviso de VENCIMIENTO (pasaron los 5 días)
            else if (diasTranscurridos >= DIAS_PLAZO) {
                enviarNotificacion(tramite, "🚨 URGENTE: Anticipo Vencido",
                        "El plazo de 5 días ha vencido. El valor será descontado en tu rol de pagos.",
                        correoContadora);
            }
        }
    }

    private LocalDate obtenerFechaAcreditacion(Long tramiteId) {
        try {
            List<ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia> ordenes =
                    ordenTransferenciaRepository.findByTramiteTramiteId(tramiteId);
            if (!ordenes.isEmpty() && ordenes.get(0).getFechaGeneracion() != null) {
                return ordenes.get(0).getFechaGeneracion();
            }
        } catch (Exception e) {
            System.err.println("Error al obtener fecha de acreditación: " + e.getMessage());
        }
        return null;
    }

    private void enviarNotificacion(Tramite tramite, String asunto, String mensajeBase, String correoContadora) {
        // 1. Correo del funcionario
        String correoFuncionario = tramite.getUsuario().getCorreo();

        String mensajeCompleto = asunto + "\n\n" +
                "🆔 Trámite: #" + tramite.getTramiteId() + "\n" +
                "👤 Funcionario: " + tramite.getUsuario().getNombre() + "\n" +
                "📌 Detalle: " + mensajeBase;

        // Notificar al Funcionario (Correo y Telegram)
        if (correoFuncionario != null) {
            NotificacionRequestDTO notifFun = new NotificacionRequestDTO();
            notifFun.setTramiteId(tramite.getTramiteId());
            notifFun.setDestinatario(correoFuncionario);
            notifFun.setTipo(asunto);
            notifFun.setEstado("Enviado");
            notifFun.setMensajePersonalizado(mensajeCompleto);
            notifFun.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramite.getTramiteId());
            notificacionService.guardar(notifFun);
        }

        // Notificar a la Contadora (Copia)
        if (correoContadora != null) {
            NotificacionRequestDTO notifCon = new NotificacionRequestDTO();
            notifCon.setTramiteId(tramite.getTramiteId());
            notifCon.setDestinatario(correoContadora);
            notifCon.setTipo(asunto + " (Info Contadora)");
            notifCon.setEstado("Enviado");
            notifCon.setMensajePersonalizado("Aviso automático: El trámite #" + tramite.getTramiteId() + " de " + tramite.getUsuario().getNombre() + " " + mensajeBase.toLowerCase());
            notifCon.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramite.getTramiteId());
            notificacionService.guardar(notifCon);
        }
    }
}