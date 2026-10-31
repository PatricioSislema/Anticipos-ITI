package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.*;
import ec.edu.iti.anticipos.chatbot.service.chat.*;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ec.edu.iti.anticipos.chatbot.dto.request.OrdenTransferenciaRequestDTO;

import java.time.LocalDate;
import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final OpenAIService openAIService;
    private final UsuarioService usuarioService;
    private final FuncionarioChatService funcionarioChatService;
    private final AprobadorChatService aprobadorChatService;
    private final ColectoraChatService colectoraChatService;
    private final AsistenteContableChatService asistenteContableChatService;
    private final ContadoraChatService contadoraChatService;
    private final DevolucionService devolucionService;
    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final Map<Long, SesionUsuario> sesiones = new HashMap<>();
    private static final long TIMEOUT_MINUTOS = 30;

    private final OrdenTransferenciaService ordenTransferenciaService;
    private final SolicitudService solicitudService;
    private final ReembolsoService reembolsoService;

    // ============================================================
    // PROCESAR MENSAJE (TEXTO PLANO)
    // ============================================================
    public String procesarMensaje(String mensaje, Long chatId) {
        System.out.println("=== PROCESAR MENSAJE ===");
        System.out.println("ChatId: [" + chatId + "]");
        System.out.println("Mensaje: [" + mensaje + "]");

        String mensajeLower = mensaje.toLowerCase().trim();

        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null) {
            sesion = new SesionUsuario();
            sesiones.put(chatId, sesion);
        }

        if (sesion.isAutenticado() && expiroSesion(sesion)) {
            String nombre = sesion.getNombre();
            sesiones.remove(chatId);

            return "⏰ *Sesión cerrada por inactividad*\n\n" +
                    "📌 Han pasado más de 30 minutos sin actividad.\n\n" +
                    "👋 ¡Nos vemos después, " + nombre + "!\n\n" +
                    "🤖 *ITI se despide... ¡Que tengas un excelente día!* 🌟\n\n" +
                    "👉 Cuando quieras volver, solo escribe *hola* para iniciar sesión de nuevo.";
        }

        sesion.setUltimaActividad(LocalDateTime.now());
        sesion.incrementarContador();

        // Verificar si debe mostrar el recordatorio (cada 5 mensajes)
        boolean mostrarRecordatorio = sesion.debeMostrarRecordatorio();
        if (mostrarRecordatorio) {
            sesion.setRecordatorioMostrado(true);
            System.out.println(">>> Recordatorio de finalidad (contador: " + sesion.getContadorMensajes() + ")");
        }

        if (sesion.isAutenticado() && sesion.isEsperandoNuevaContrasena()) {
            String nuevaContrasena = mensaje.trim();

            if (nuevaContrasena.length() < 8) {
                return "⚠️ *Contraseña muy corta.*\n\n" +
                        "📌 Debe tener al menos 8 caracteres.\n\n" +
                        "👉 Intenta de nuevo.";
            }

            usuarioService.actualizarContrasena(sesion.getCorreo(), nuevaContrasena);
            sesion.setEsperandoNuevaContrasena(false);

            return "✅ *Contraseña actualizada correctamente.*\n\n" +
                    "🎉 Ya puedes usar el sistema.\n\n" +
                    menuPrincipal(sesion);
        }

        String msgLimpio = mensajeLower.trim();
        if (msgLimpio.equals("ver_solicitudes_pendientes")
                || msgLimpio.equals("ver_ordenes_anticipo")
                || msgLimpio.equals("ver_liquidaciones_pendientes")
                || msgLimpio.equals("registrar_comprobante")
                || msgLimpio.equals("generar_orden_anticipo")
                || msgLimpio.equals("ver_devoluciones")
                || msgLimpio.equals("revisar_liquidaciones_asistente")
                || msgLimpio.startsWith("revisar_")
                || msgLimpio.startsWith("pag_")
                || msgLimpio.startsWith("aprobar_tramite_")
                || msgLimpio.startsWith("rechazar_tramite_")
                || msgLimpio.startsWith("aprobar_orden_tramite_")
                || msgLimpio.startsWith("rechazar_orden_tramite_")
                || msgLimpio.startsWith("registrar_comprobante_tramite_")
                || msgLimpio.startsWith("validar_devolucion_")
                || msgLimpio.startsWith("rechazar_devolucion_")
                || msgLimpio.startsWith("enviar_contadora_")
                || msgLimpio.startsWith("devolver_funcionario_")
                || msgLimpio.startsWith("generar_orden_tramite_")
                || msgLimpio.startsWith("autorizar_reembolso_tramite_")
                || msgLimpio.startsWith("autorizar_reembolso_director_")
                || msgLimpio.startsWith("solicitar_pdf_orden_reembolso_")
                || msgLimpio.startsWith("aprobar_orden_reembolso_")
                || msgLimpio.startsWith("registrar_comprobante_reembolso_")
                || msgLimpio.startsWith("informar_colectora_")) {
            return "";
        }

        if (sesion.isAutenticado()
                && sesion.getAccionPendiente() != null
                && (sesion.getFormularioSolicitud() != null
                || sesion.getFormularioLiquidacion() != null
                || sesion.isSubiendoFacturas())) {
            if (mensajeLower.equals("1")) {
                mensajeLower = "si";
                mensaje = "si";
            } else if (mensajeLower.equals("2")) {
                mensajeLower = "no";
                mensaje = "no";
            }
        }

        if (mensajeLower.equals("subir_comprobante")) {
            if (sesion.isEsperandoComprobanteDevolucion()) {
                return "📎 Perfecto.\n\n" +
                        "👉 Adjunta el comprobante de depósito en formato PDF desde este chat.";
            }
            return "ℹ️ En este momento no necesitas subir un comprobante.\n\n" +
                    "👉 Sigue las instrucciones del bot.";
        }

        if (mensajeLower.equals("ver_documentacion_anexada")) {
            return "📄 *Documentación del trámite #" + sesion.getTramiteEnRevision() + "*\n\n" +
                    "📌 *Razón:* Los archivos no se almacenan en el servidor (limitación del prototipo).\n\n" +
                    "📎 *¿Dónde encontrarlos?*\n" +
                    "Los documentos originales (formulario, facturas, comprobantes) están en el chat de Telegram donde el Funcionario los subió.\n\n" +
                    "👉 *Trabajo futuro:* Implementar almacenamiento en Azure Blob Storage.";
        }

        if (sesion.isAutenticado()
                && sesion.getAccionPendiente() == null
                && !sesion.isSubiendoFacturas()
                && !sesion.isEsperandoComprobanteDevolucion()
                && !sesion.isEsperandoConfirmacionCancelacion()
                && !sesion.isEsperandoConfirmacionCerrarSesion()
                && !sesion.isEsperandoMotivoRechazo()
                && !mensajeLower.startsWith("cerrar_sesion")
                && !mensajeLower.matches("\\d+")
                && !mensajeLower.matches("revisar\\s+\\d+")
                && !mensajeLower.matches("validar\\s+devolucion\\s+\\d+")
                && !mensajeLower.matches("registrar\\s+comprobante\\s+\\d+")
                && !mensajeLower.matches("ver\\s+detalles\\s+\\d+")
                && !mensajeLower.matches("rechazar\\s+devolucion\\s+\\d+")
                && !mensajeLower.matches("autorizar\\s+reembolso\\s+\\d+")
                && !mensajeLower.matches("informar\\s+colectora\\s+\\d+")
                && !mensajeLower.matches("cerrar\\s+reembolso\\s+\\d+")
                && !mensajeLower.matches("aprobar\\s+orden\\s+\\d+")
                && !mensajeLower.contains("pendiente")
                && !mensajeLower.contains("revisar")
                && !mensajeLower.contains("transferencia")
                && !mensajeLower.contains("confirmar")
                && !mensajeLower.equals("listo")
                && !mensajeLower.equals("reembolsos")
                && !mensajeLower.equals("ordenes")) {

            boolean esComandoDirecto = esComando(mensajeLower,
                    "menu", "menú", "inicio", "ayuda", "opciones", "help", "?",
                    "salir", "cancelar", "cancela", "volver", "atras", "atrás", "regresar",
                    "1", "2", "3", "4", "5", "6", "si", "sí", "no", "listo",
                    "pendientes", "pendiente", "revisar", "aprobar", "rechazar",
                    "registrar", "transferencia", "confirmar", "enviar", "devolver",
                    "subir_comprobante");

            if (!esComandoDirecto) {
                String intencion = openAIService.clasificarIntencion(mensaje);
                System.out.println("[IA] Mensaje: [" + mensaje + "] → Intención: " + intencion);

                switch (intencion) {
                    case "SOLICITAR":
                        // ✅ NUEVO: Guiar paso a paso con confirmación
                        return "📝 *¡Perfecto! Veo que quieres solicitar un anticipo.*\n\n" +
                                "Para continuar, necesito que subas el formulario de solicitud en formato PDF (ya llenado y firmado).\n\n" +
                                "👉 Adjúntalo en este chat cuando esté listo.";
                    case "LIQUIDAR":
                        // ✅ NUEVO: Guiar paso a paso con confirmación
                        return "💰 *¡Perfecto! Veo que quieres liquidar un anticipo.*\n\n" +
                                "Para continuar, necesito que subas el formulario de liquidación en formato PDF.\n\n" +
                                "👉 Adjúntalo en este chat cuando esté listo.";
                    case "CONSULTAR":
                        mensajeLower = "estado";
                        mensaje = "estado";
                        break;
                    case "AYUDA":
                        mensajeLower = "ayuda";
                        mensaje = "ayuda";
                        break;
                    case "SALIR":
                        mensajeLower = "salir";
                        mensaje = "salir";
                        break;
                    case "MENU":
                        mensajeLower = "menu";
                        mensaje = "menu";
                        break;
                    case "CANCELAR":
                        mensajeLower = "cancelar";
                        mensaje = "cancelar";
                        break;
                    case "CONSULTA_GENERAL":
                        // ✅ NUEVO: Usar la IA para responder preguntas generales
                        return openAIService.responderConsultaGeneral(mensaje);
                    case "DESCONOCIDA":
                        return openAIService.responderConsultaGeneral(mensaje);
                }
            }
        }

        if (sesion.isEsperandoConfirmacionCancelacion()) return procesarConfirmacionCancelacion(mensajeLower, sesion);
        if (sesion.isEsperandoConfirmacionCerrarSesion()) return procesarConfirmacionCerrarSesion(mensajeLower, sesion, chatId);

        if (mensajeLower.equals("cambiar_contrasena")) {
            sesion.setEsperandoNuevaContrasena(true);
            return "🔐 *Cambio de contraseña*\n\n👉 Por favor, escribe tu *nueva contraseña* (mínimo 8 caracteres).\n\n⚠️ Si deseas cancelar, escribe *cancelar*.";
        }
        if (mensajeLower.equals("cerrar_sesion_confirmar")) {
            sesion.setEsperandoConfirmacionCerrarSesion(true);
            return "⚠️ ¿Estás seguro de que quieres cerrar sesión?\n\n📌 Perderás el estado de la conversación actual.";
        }
        if (mensajeLower.equals("cerrar_sesion_si")) {
            sesiones.remove(chatId);
            return "👋 Sesión cerrada correctamente. Hasta luego.\n\nPara volver a ingresar, escribe *hola*.";
        }
        if (mensajeLower.equals("cerrar_sesion_no")) {
            sesion.setEsperandoConfirmacionCerrarSesion(false);
            return "👍 Perfecto, continúo con tu sesión.";
        }
        if (mensajeLower.equals("descargar formulario")) {
            limpiarOperacion(sesion);
            return "⚠️ *Formulario no disponible por el momento.*\n\n📌 Esta función estará habilitada en una próxima versión.\n\n👉 Por favor, continúa con el proceso o usa el menú principal para volver.";
        }
        if (mensajeLower.equals("descargar facturas")) {
            limpiarOperacion(sesion);
            return "⚠️ *Facturas no disponibles por el momento.*\n\n📌 Esta función estará habilitada en una próxima versión.\n\n👉 Por favor, continúa con el proceso o usa el menú principal para volver.";
        }
        if (mensajeLower.equals("ver documentos")) {
            if (sesion.getRolNombre() == null || !sesion.getRolNombre().equalsIgnoreCase("Funcionario")) {
                return "⚠️ Esta opción es solo para Funcionarios.";
            }
            return "📄 *Descargar mis documentos*\n\nPara descargar un documento, primero consulta el estado de tus trámites:\n\n👉 Escribe *3* o *consultar estado*\n👉 Luego escribe *ver detalles X* (donde X es el número del trámite)\n\n📌 Los botones de descarga aparecerán automáticamente al ver el detalle.\n\n⚠️ *Nota:* Los PDFs no se almacenan en el servidor (limitación del prototipo).";
        }

        if (mensajeLower.equals("pendientes")) sesion.setContextoActual("PENDIENTES");
        if (mensajeLower.equals("ordenes")) sesion.setContextoActual("ORDENES");
        if (mensajeLower.equals("reembolsos")) sesion.setContextoActual("REEMBOLSOS");

        if (esComando(mensajeLower, "salir", "cerrar sesion", "cerrar sesión", "logout")) {
            if (sesion.isAutenticado()) {
                sesion.setEsperandoConfirmacionCerrarSesion(true);
                return "⚠️ ¿Estás seguro de que quieres cerrar sesión?\n\n📌 Perderás el estado de la conversación actual.";
            }
        }

        if (esComando(mensajeLower, "ayuda", "opciones", "puedo hacer", "help", "?")) {
            if (sesion.isAutenticado()) {
                return menuAyudaContextual(sesion);
            } else {
                return "👋 Hola. Soy ITI, el asistente virtual del Instituto Tecnológico Internacional.\n\n👉 Para continuar, ingresa tu correo electrónico.";
            }
        }

        if (esComando(mensajeLower, "menu", "menú", "inicio", "volver al menu", "volver al menú")) {
            if (sesion.isAutenticado()) {
                limpiarOperacion(sesion);
                return menuPrincipal(sesion);
            }
        }

        if (esComando(mensajeLower, "cancelar", "cancela", "abortar")) {
            if (sesion.isAutenticado() && sesion.getAccionPendiente() != null) {
                sesion.setEsperandoConfirmacionCancelacion(true);
                return "⚠️ ¿Estás seguro de cancelar la operación actual?\n\n📌 Operación en curso: *" + sesion.getAccionPendiente() + "*";
            }
            if (sesion.isAutenticado()) {
                return "ℹ️ No hay ninguna operación en curso.\n\n" + menuPrincipal(sesion);
            }
            return "ℹ️ No hay ninguna operación en curso.";
        }

        if (esComando(mensajeLower, "volver", "atras", "atrás", "regresar")) {
            if (sesion.isAutenticado()) {
                limpiarOperacion(sesion);
                return menuPrincipal(sesion);
            }
        }

        if (!sesion.isAutenticado()) {
            ResponseBot respuestaLogin = manejarLogin(mensaje, mensajeLower, sesion, chatId);
            return respuestaLogin.getTexto();
        }

        return delegarPorRol(mensaje, mensajeLower, sesion, chatId);
    }


    // ============================================================
    // PROCESAR MENSAJE CON BOTONES
    // ============================================================
    public ResponseBot procesarMensajeConBotones(String mensaje, Long chatId) {
        SesionUsuario sesionAntes = sesiones.get(chatId);
        boolean teniaFormularioSolicitud = sesionAntes != null && sesionAntes.getFormularioSolicitud() != null;
        boolean estabaSubiendoFacturas = sesionAntes != null && sesionAntes.isSubiendoFacturas();

        if (mensaje.startsWith("deep_link_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("deep_link_", ""));
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) {
                sesion = new SesionUsuario();
                sesiones.put(chatId, sesion);
            }
            sesion.setTramiteRedirigido(tramiteId);
            sesion.setAutenticado(false);
            sesion.setEsperandoContrasena(false);
            return new ResponseBot("👋 ¡Hola! Has sido redirigido para revisar el **Trámite #" + tramiteId + "**.\n\n🔐 Para continuar, por favor ingresa tu **correo institucional**.");
        }

        if (mensaje.startsWith("APROBAR_ORDEN_")) {
            Long ordenId = Long.parseLong(mensaje.replace("APROBAR_ORDEN_", ""));
            return aprobarOrdenConPdf(ordenId, chatId);
        }
        if (mensaje.startsWith("RECHAZAR_ORDEN_")) {
            Long ordenId = Long.parseLong(mensaje.replace("RECHAZAR_ORDEN_", ""));
            return rechazarOrden(ordenId, chatId);
        }

        if (mensaje.startsWith("aprobar_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("aprobar_tramite_", ""));
            return aprobarTramiteConNotificacion(tramiteId, chatId);
        }
        if (mensaje.startsWith("rechazar_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("rechazar_tramite_", ""));
            return iniciarRechazoConMotivo(tramiteId, chatId);
        }

        if (mensaje.startsWith("aprobar_orden_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("aprobar_orden_tramite_", ""));
            return aprobarOrdenConNotificacion(tramiteId, chatId);
        }
        if (mensaje.startsWith("rechazar_orden_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("rechazar_orden_tramite_", ""));
            return iniciarRechazoConMotivo(tramiteId, chatId);
        }

        if (mensaje.startsWith("registrar_comprobante_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("registrar_comprobante_tramite_", ""));
            return registrarComprobanteContadora(tramiteId, chatId);
        }

        if (mensaje.startsWith("validar_devolucion_")) {
            Long devolucionId = Long.parseLong(mensaje.replace("validar_devolucion_", ""));
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null || !sesion.isAutenticado()) {
                return new ResponseBot("⚠️ Sesión expirada.");
            }

            Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorId(devolucionId);
            if (devolucionOpt.isEmpty()) {
                return new ResponseBot("⚠️ No encontré la devolución #" + devolucionId + ".");
            }
            DevolucionResponseDTO devolucion = devolucionOpt.get();
            Long tramiteId = devolucion.getTramiteId();

            devolucionService.actualizarEstado(devolucionId, "Validado");
            tramiteService.actualizarEstado(tramiteId, "Liquidación cerrada");

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            String nombreFuncionario = detalleOpt.map(TramiteDetalleDTO::getNombreUsuario).orElse("N/A");
            String correoFuncionario = detalleOpt.map(TramiteDetalleDTO::getCorreoUsuario).orElse(null);
            String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

            String mensajeNotif = "🎉 *Liquidación cerrada exitosamente*\n\n" +
                    "🆔 Trámite: #" + tramiteId + "\n" +
                    "👤 Funcionario: " + nombreFuncionario + "\n" +
                    "📊 Estado: Liquidación cerrada\n\n" +
                    "✅ El trámite ha concluido satisfactoriamente.";

            if (correoFuncionario != null) {
                NotificacionRequestDTO notifFun = new NotificacionRequestDTO();
                notifFun.setTramiteId(tramiteId);
                notifFun.setDestinatario(correoFuncionario);
                notifFun.setTipo("Liquidación cerrada");
                notifFun.setMensajePersonalizado(mensajeNotif);
                notificacionService.guardar(notifFun);
            }
            if (correoDirector != null) {
                NotificacionRequestDTO notifDir = new NotificacionRequestDTO();
                notifDir.setTramiteId(tramiteId);
                notifDir.setDestinatario(correoDirector);
                notifDir.setTipo("Liquidación cerrada");
                notifDir.setMensajePersonalizado(mensajeNotif);
                notificacionService.guardar(notifDir);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Devolución #" + devolucionId + " validada correctamente.*\n\n📬 Se ha notificado al *Funcionario* y al *Director Financiero*.\n\n🎉 La liquidación ha sido cerrada.", botones);
        }

        if (mensaje.startsWith("rechazar_devolucion_")) {
            Long devolucionId = Long.parseLong(mensaje.replace("rechazar_devolucion_", ""));
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            sesion.setTramiteEnRevision(devolucionId);
            sesion.setEsperandoMotivoRechazo(true);
            return new ResponseBot("✍️ ¿Cuál es el motivo del rechazo de la devolución #" + devolucionId + "?\n\n👉 Escribe el motivo.");
        }

        if (mensaje.startsWith("enviar_contadora_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("enviar_contadora_", ""));
            return enviarAContadora(tramiteId, chatId);
        }
        if (mensaje.startsWith("devolver_funcionario_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("devolver_funcionario_", ""));
            return iniciarDevolucionFuncionario(tramiteId, chatId);
        }

        if (mensaje.startsWith("solicitar_pdf_orden_reembolso_")) {
            String idStr = mensaje.replace("solicitar_pdf_orden_reembolso_", "");
            Long tramiteId = Long.parseLong(idStr);
            SesionUsuario sesion = sesiones.get(chatId);

            sesion.setTramiteEnRevision(tramiteId);
            sesion.setAccionPendiente("GENERAR_ORDEN_REEMBOLSO");

            List<List<ResponseBot.Boton>> botones = new ArrayList<>();
            botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
            return new ResponseBot("📎 *Para generar la orden de reembolso*, por favor adjunta el respaldo en formato PDF.\n\n👉 Envía el archivo por este chat.", botones);
        }

        if (mensaje.startsWith("solicitar_pdf_orden_") && !mensaje.startsWith("solicitar_pdf_orden_reembolso_")) {
            String idStr = mensaje.replace("solicitar_pdf_orden_", "");
            if (idStr.equals("null") || idStr.isEmpty()) {
                return new ResponseBot("⚠️ Error: No se encontró el ID del trámite. Por favor, abre el trámite de nuevo desde la lista.");
            }
            Long tramiteId = Long.parseLong(idStr);
            SesionUsuario sesion = sesiones.get(chatId);

            sesion.setTramiteEnRevision(tramiteId);
            sesion.setAccionPendiente("GENERAR_ORDEN");

            List<List<ResponseBot.Boton>> botones = new ArrayList<>();
            botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
            return new ResponseBot("📎 *Para generar la orden de transferencia*, por favor adjunta el respaldo en formato PDF (comprobante de transferencia o captura de la orden).\n\n👉 Envía el archivo por este chat.", botones);
        }

        if (mensaje.startsWith("generar_orden_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("generar_orden_tramite_", ""));
            return generarOrdenColectora(tramiteId, chatId);
        }

        if (mensaje.startsWith("autorizar_reembolso_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("autorizar_reembolso_tramite_", ""));
            SesionUsuario sesion = sesiones.get(chatId);

            tramiteService.actualizarEstado(tramiteId, "Liquidación aprobada");

            String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");
            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            String nombreSolicitante = detalleOpt.map(TramiteDetalleDTO::getNombreUsuario).orElse("N/A");
            String montoReembolso = detalleOpt.map(d -> d.getSaldo() != null ? d.getSaldo().abs().toString() : "N/A").orElse("N/A");

            if (correoDirector != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoDirector);
                notifDTO.setTipo("Reembolso pendiente de autorización");
                notifDTO.setMensajePersonalizado("💸 *Reembolso pendiente de autorización*\n\n🆔 Trámite: #" + tramiteId + "\n👤 Funcionario: " + nombreSolicitante + "\n💰 Monto a reembolsar: $" + montoReembolso + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para autorizar el reembolso.");
                notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
                notificacionService.guardar(notifDTO);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Reembolso solicitado al Director Financiero.*\n\n📬 Se ha notificado al *Director* para que autorice el reembolso.", botones);
        }

        if (mensaje.startsWith("autorizar_reembolso_director_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("autorizar_reembolso_director_", ""));
            SesionUsuario sesion = sesiones.get(chatId);

            tramiteService.actualizarEstado(tramiteId, "Reembolso autorizado");

            String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
            if (correoContadora != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoContadora);
                notifDTO.setTipo("Reembolso autorizado - Informar a Colectora");
                notifDTO.setMensajePersonalizado("✅ *Reembolso autorizado por el Director*\n\n🆔 Trámite: #" + tramiteId + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* y presiona *Informar a Colectora* para que genere la orden.");
                notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
                notificacionService.guardar(notifDTO);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Reembolso autorizado.*\n\n📬 Se ha notificado a la *Contadora* para que informe a la Colectora.", botones);
        }

        if (mensaje.startsWith("informar_colectora_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("informar_colectora_", ""));
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null || !sesion.isAutenticado()) {
                return new ResponseBot("⚠️ Sesión expirada.");
            }

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            if (detalleOpt.isEmpty() || !"Reembolso autorizado".equals(detalleOpt.get().getEstado())) {
                return new ResponseBot("⚠️ El trámite #" + tramiteId + " no está en estado *Reembolso autorizado*.\n\n📌 No se puede informar a la Colectora todavía.");
            }

            String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
            if (correoColectora != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoColectora);
                notifDTO.setTipo("Orden de reembolso pendiente");
                notifDTO.setMensajePersonalizado("🏦 *Orden de reembolso pendiente*\n\n🆔 Trámite: #" + tramiteId + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para generar la orden.");
                notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
                notificacionService.guardar(notifDTO);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Colectora notificada.*\n\n📬 Se ha notificado a la *Colectora* para que genere la orden de reembolso.", botones);
        }

        if (mensaje.startsWith("aprobar_orden_reembolso_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("aprobar_orden_reembolso_", ""));
            SesionUsuario sesion = sesiones.get(chatId);

            tramiteService.actualizarEstado(tramiteId, "Orden de reembolso aprobada");

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            if (detalleOpt.isPresent() && detalleOpt.get().getSaldo() != null) {
                OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
                ordenDTO.setTramiteId(tramiteId);
                ordenDTO.setColectoraId(sesion.getUsuarioId());
                ordenDTO.setDirectorId(sesion.getUsuarioId());
                ordenDTO.setNumeroCuenta("1234567890");
                ordenDTO.setValor(detalleOpt.get().getSaldo().abs());
                ordenDTO.setConcepto("Reembolso al funcionario - Trámite #" + tramiteId);
                ordenDTO.setFechaGeneracion(LocalDate.now());
                ordenDTO.setFechaAprobacion(LocalDate.now());
                ordenDTO.setEstado("Aprobada");
                ordenTransferenciaService.guardar(ordenDTO);
            }

            String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
            String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");

            if (correoContadora != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoContadora);
                notifDTO.setTipo("Orden de reembolso aprobada - Registrar comprobante");
                notifDTO.setMensajePersonalizado("📄 *Orden de reembolso aprobada - Registrar comprobante*\n\n🆔 Trámite: #" + tramiteId + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para registrar la transferencia.");
                notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
                notificacionService.guardar(notifDTO);
            }

            if (correoColectora != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoColectora);
                notifDTO.setTipo("Orden de reembolso aprobada");
                notifDTO.setMensajePersonalizado("✅ *Orden de reembolso aprobada*\n\n🆔 Trámite: #" + tramiteId + "\n\n👉 El dinero ha sido transferido.");
                notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
                notificacionService.guardar(notifDTO);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Orden de reembolso #" + tramiteId + " aprobada.*\n\n📬 Se ha notificado a la *Colectora* y a la *Contadora*.", botones);
        }

        if (mensaje.startsWith("registrar_comprobante_reembolso_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("registrar_comprobante_reembolso_", ""));
            SesionUsuario sesion = sesiones.get(chatId);

            tramiteService.actualizarEstado(tramiteId, "Liquidación cerrada");

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            String nombreFuncionario = detalleOpt.map(TramiteDetalleDTO::getNombreUsuario).orElse("N/A");
            String correoFuncionario = detalleOpt.map(TramiteDetalleDTO::getCorreoUsuario).orElse(null);
            String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

            String mensajeNotif = "🎉 *Liquidación cerrada exitosamente*\n\n🆔 Trámite: #" + tramiteId + "\n👤 Funcionario: " + nombreFuncionario + "\n📊 Estado: Liquidación cerrada\n\n✅ El trámite ha concluido satisfactoriamente.";

            if (correoFuncionario != null) {
                NotificacionRequestDTO notifFun = new NotificacionRequestDTO();
                notifFun.setTramiteId(tramiteId);
                notifFun.setDestinatario(correoFuncionario);
                notifFun.setTipo("Liquidación cerrada");
                notifFun.setMensajePersonalizado(mensajeNotif);
                notificacionService.guardar(notifFun);
            }
            if (correoDirector != null) {
                NotificacionRequestDTO notifDir = new NotificacionRequestDTO();
                notifDir.setTramiteId(tramiteId);
                notifDir.setDestinatario(correoDirector);
                notifDir.setTipo("Liquidación cerrada");
                notifDir.setMensajePersonalizado(mensajeNotif);
                notificacionService.guardar(notifDir);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Liquidación cerrada correctamente.*\n\n📬 Se ha notificado al *Funcionario* y al *Director Financiero*.\n\n🎉 El trámite ha concluido.", botones);
        }

        if (mensaje.equals("revisar_liquidaciones_asistente")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaTramitesLiquidacion(sesion, "Liquidación en proceso", 0);
        }

        if (mensaje.startsWith("revisar_")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");

            Long tramiteId = Long.parseLong(mensaje.replace("revisar_", ""));
            sesion.setTramiteEnRevision(tramiteId);

            String detalle = verDetallesTramite(tramiteId);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot(detalle, botones);
        }

        if (mensaje.startsWith("pag_")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");

            String[] partes = mensaje.split("_");
            int nuevaPagina = Integer.parseInt(partes[partes.length - 1]);
            StringBuilder nombreLista = new StringBuilder();
            for (int i = 1; i < partes.length - 1; i++) {
                if (i > 1) nombreLista.append("_");
                nombreLista.append(partes[i]);
            }
            String estado = sesion.getEstadoFiltroLista();
            String titulo = obtenerTituloLista(nombreLista.toString());
            return mostrarListaPaginada(sesion, estado, nombreLista.toString(), nuevaPagina, titulo);
        }

        if (mensaje.startsWith("cerrar_liquidacion_tramite_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("cerrar_liquidacion_tramite_", ""));
            SesionUsuario sesion = sesiones.get(chatId);

            Optional<DevolucionResponseDTO> devOpt = devolucionService.buscarPorTramiteYEstado(tramiteId, "Comprobante subido");
            if (devOpt.isPresent()) {
                devolucionService.actualizarEstado(devOpt.get().getDevolucionId(), "Validado");
            }

            tramiteService.actualizarEstado(tramiteId, "Liquidación cerrada");

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            String nombreFuncionario = detalleOpt.map(TramiteDetalleDTO::getNombreUsuario).orElse("N/A");
            String correoFuncionario = detalleOpt.map(TramiteDetalleDTO::getCorreoUsuario).orElse(null);
            String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

            String mensajeNotif = "🎉 *Liquidación cerrada exitosamente*\n\n🆔 Trámite: #" + tramiteId + "\n👤 Funcionario: " + nombreFuncionario + "\n📊 Estado: Liquidación cerrada\n\n✅ El trámite ha concluido satisfactoriamente.";

            if (correoFuncionario != null) {
                NotificacionRequestDTO notifFun = new NotificacionRequestDTO();
                notifFun.setTramiteId(tramiteId);
                notifFun.setDestinatario(correoFuncionario);
                notifFun.setTipo("Liquidación cerrada");
                notifFun.setMensajePersonalizado(mensajeNotif);
                notificacionService.guardar(notifFun);
            }
            if (correoDirector != null) {
                NotificacionRequestDTO notifDir = new NotificacionRequestDTO();
                notifDir.setTramiteId(tramiteId);
                notifDir.setDestinatario(correoDirector);
                notifDir.setTipo("Liquidación cerrada");
                notifDir.setMensajePersonalizado(mensajeNotif);
                notificacionService.guardar(notifDir);
            }

            sesion.setTramiteEnRevision(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("✅ *Liquidación cerrada correctamente.*\n\n📬 Se ha notificado al *Funcionario* y al *Director Financiero*.\n\n🎉 El trámite ha concluido.", botones);
        }

        if (mensaje.equals("ver_solicitudes_pendientes")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");

            String rol = sesion.getRolNombre();
            String estado;
            String nombreLista;
            String titulo;

            if (rol.equalsIgnoreCase("Jefe Inmediato")) {
                estado = "Pendiente";
                nombreLista = "SOLICITUDES_JEFE";
                titulo = "Solicitudes pendientes";
            } else if (rol.equalsIgnoreCase("Contadora")) {
                estado = "Aprobado por Director";
                nombreLista = "SOLICITUDES_CONTADORA";
                titulo = "Solicitudes pendientes";
            } else {
                estado = "Aprobado por Jefe";
                nombreLista = "SOLICITUDES_DIRECTOR";
                titulo = "Solicitudes de anticipo pendientes";
            }
            return mostrarListaPaginada(sesion, estado, nombreLista, 0, titulo);
        }

        if (mensaje.equals("ver_solicitudes_anticipo")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Aprobado por Jefe", "SOLICITUDES_DIRECTOR", 0, "Solicitudes de anticipo pendientes");
        }

        if (mensaje.equals("ver_ordenes_anticipo")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Orden generada", "ORDENES_DIRECTOR", 0, "Órdenes de anticipo pendientes");
        }

        if (mensaje.equals("ver_reembolsos")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Liquidación aprobada", "REEMBOLSOS_DIRECTOR", 0, "Reembolsos pendientes de autorización");
        }

        if (mensaje.equals("ver_ordenes_reembolso")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Orden de reembolso generada", "ORDENES_REEMBOLSO_DIRECTOR", 0, "Órdenes de reembolso pendientes");
        }

        if (mensaje.equals("ver_liquidaciones_pendientes")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Liquidación revisada", "LIQUIDACIONES_CONTADORA", 0, "Liquidaciones pendientes");
        }

        if (mensaje.equals("ver_devoluciones")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaDevolucionesPaginada(sesion, 0);
        }

        if (mensaje.equals("registrar_comprobante")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Orden aprobada", "COMPROBANTES_CONTADORA", 0, "Comprobantes por registrar");
        }

        if (mensaje.equals("generar_orden_anticipo")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Aprobado por Contadora", "ANTICIPOS_COLECTORA", 0, "Anticipos por generar orden");
        }

        if (mensaje.equals("ver_reembolsos_colectora")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Reembolso autorizado", "REEMBOLSOS_COLECTORA", 0, "Reembolsos listos para generar orden");
        }

        if (mensaje.equals("generar_orden_reembolso")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return new ResponseBot("🏦 *Reembolsos por generar orden*\n\n📌 La paginación de reembolsos estará disponible en la próxima versión.\n\n👉 Por ahora, escribe *menu* para volver al menú principal.");
        }

        if (mensaje.equals("revisar_liquidaciones_asistente")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");
            return mostrarListaPaginada(sesion, "Liquidación en proceso", "LIQUIDACIONES_ASISTENTE", 0, "Liquidaciones por revisar");
        }

        if (mensaje.equals("corregir_factura")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion != null && sesion.getFacturasExtraidas() != null && !sesion.getFacturasExtraidas().isEmpty()) {
                sesion.getFacturasExtraidas().remove(sesion.getFacturasExtraidas().size() - 1);
                return new ResponseBot("🗑️ Se ha borrado la última factura.\n\n📊 Facturas restantes: " + sesion.getFacturasExtraidas().size() + "\n\n👉 Por favor, sube la factura correcta.");
            }
            return new ResponseBot("⚠️ No hay facturas para borrar.");
        }

        if (mensaje.startsWith("iniciar_liq_")) {
            Long tramiteId = Long.parseLong(mensaje.replace("iniciar_liq_", ""));
            SesionUsuario sesion = sesiones.get(chatId);

            sesion.setTramiteEnRevision(tramiteId);
            sesion.setAccionPendiente("LIQUIDAR");
            sesion.setSubiendoFacturas(false);
            return new ResponseBot("✅ Has seleccionado el Trámite #" + tramiteId + ".\n\n📎 Ahora adjunta el formulario de liquidación en formato PDF.");
        }

        if (mensaje.equals("liquidar_lista")) {
            SesionUsuario sesion = sesiones.get(chatId);
            if (sesion == null) return new ResponseBot("⚠️ Sesión expirada.");

            sesion.setTramiteEnRevision(null);
            sesion.setAccionPendiente(null);
            return mostrarListaTramitesPorLiquidar(sesion);
        }

        String texto = procesarMensaje(mensaje, chatId);
        SesionUsuario sesion = sesiones.get(chatId);

        // Agregar recordatorio si es necesario (cada 5 mensajes)
        SesionUsuario sesionActual = sesiones.get(chatId);
        if (sesionActual != null && sesionActual.isAutenticado()
                && sesionActual.getAccionPendiente() == null
                && !sesionActual.isSubiendoFacturas()
                && !sesionActual.isEsperandoComprobanteDevolucion()
                && sesionActual.getContadorMensajes() > 0
                && sesionActual.getContadorMensajes() % 5 == 0) {
            texto += "\n\n💡 *Recuerda:* Soy ITI, tu asistente para gestionar anticipos y liquidaciones de fondos del ITI.";
        }
        if (sesion == null) return new ResponseBot(texto);

        List<List<ResponseBot.Boton>> botones = new ArrayList<>();

        if (sesion.isEsperandoConfirmacionCerrarSesion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cerrar sesión", "cerrar_sesion_si"),
                    ResponseBot.boton("❌ No, continuar", "cerrar_sesion_no")
            ));
            return new ResponseBot(texto, botones);
        }
        if (sesion.isEsperandoConfirmacionCancelacion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cancelar", "1"),
                    ResponseBot.boton("❌ No, continuar", "2")
            ));
            return new ResponseBot(texto, botones);
        }
        if (sesion.isEsperandoComprobanteDevolucion()) {
            botones.add(ResponseBot.fila(ResponseBot.boton("📎 Subir comprobante de depósito", "subir_comprobante")));
            botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
            return new ResponseBot(texto, botones);
        }
        if (texto != null && texto.contains("¿Los datos son correctos?") && teniaFormularioSolicitud) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, continuar", "1"),
                    ResponseBot.boton("❌ No, corregir", "2")
            ));
            return new ResponseBot(texto, botones);
        }
        if (texto != null && texto.contains("¿Confirmas la liquidación?") && estabaSubiendoFacturas) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, continuar", "1"),
                    ResponseBot.boton("❌ No, corregir", "2")
            ));
            return new ResponseBot(texto, botones);
        }
        if (texto != null && texto.contains("sube el comprobante")) {
            botones.add(ResponseBot.fila(ResponseBot.boton("📎 Subir comprobante de depósito", "subir_comprobante")));
            botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
            return new ResponseBot(texto, botones);
        }

        // ✅ FIX: Si el Funcionario pidió consultar sus trámites, inyectamos los botones
        if (sesion.isAutenticado() && sesion.getRolNombre() != null
                && sesion.getRolNombre().equalsIgnoreCase("Funcionario")
                && texto != null && texto.contains("Tus trámites recientes")) {

            List<TramiteResponseDTO> tramites = tramiteService.listarPorUsuario(sesion.getUsuarioId());

            // Tomamos los 5 más recientes
            List<TramiteResponseDTO> recientes = tramites.stream()
                    .sorted((a, b) -> b.getTramiteId().compareTo(a.getTramiteId()))
                    .limit(5)
                    .toList();

            for (TramiteResponseDTO t : recientes) {
                botones.add(ResponseBot.fila(ResponseBot.boton("🔍 Ver trámite #" + t.getTramiteId(), "revisar_" + t.getTramiteId())));
            }

            botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
            return new ResponseBot(texto, botones);
        }

        botones = determinarBotones(sesion);
        return new ResponseBot(texto, botones);
    }

    // ============================================================
    // PROCESAR DOCUMENTO CON BOTONES
    // ============================================================
    public ResponseBot procesarDocumentoConBotones(MultipartFile archivo, Long chatId) {
        String texto = procesarDocumento(archivo, chatId);
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null) return new ResponseBot(texto);

        if (texto != null) {
            if (texto.contains("¿Los datos son correctos?")) {
                List<List<ResponseBot.Boton>> botones = new ArrayList<>();
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return new ResponseBot(texto, botones);
            }
            if (texto.contains("¿Confirmas la liquidación?")) {
                List<List<ResponseBot.Boton>> botones = new ArrayList<>();
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return new ResponseBot(texto, botones);
            }
        }

        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot(texto, botones);
    }

    // ============================================================
    // DETERMINAR BOTONES
    // ============================================================
    private List<List<ResponseBot.Boton>> determinarBotones(SesionUsuario sesion) {
        List<List<ResponseBot.Boton>> botones = new ArrayList<>();

        if (sesion.isEsperandoConfirmacionCerrarSesion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cerrar sesión", "cerrar_sesion_si"),
                    ResponseBot.boton("❌ No, continuar", "cerrar_sesion_no")
            ));
            return botones;
        }
        if (sesion.isEsperandoConfirmacionCancelacion()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Sí, cancelar", "1"),
                    ResponseBot.boton("❌ No, continuar", "2")
            ));
            return botones;
        }
        if (sesion.isAutenticado() && sesion.isEsperandoNuevaContrasena()) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("❓ Ayuda", "5"),
                    ResponseBot.boton("❌ Cancelar cambio", "cancelar")
            ));
            return botones;
        }

        if (sesion.isAutenticado()) {
            String rol = sesion.getRolNombre();

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && sesion.isEsperandoComprobanteDevolucion()) {
                botones.add(ResponseBot.fila(ResponseBot.boton("📎 Subir comprobante de depósito", "subir_comprobante")));
                botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && "SOLICITAR".equals(sesion.getAccionPendiente()) && sesion.getFormularioSolicitud() != null) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && "LIQUIDAR".equals(sesion.getAccionPendiente()) && sesion.getFormularioLiquidacion() != null && !sesion.isSubiendoFacturas()) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("✅ Sí, continuar", "1"),
                        ResponseBot.boton("❌ No, corregir", "2")
                ));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && sesion.getTramiteEnRevision() != null) {
                Long tramiteId = sesion.getTramiteEnRevision();

                if (sesion.isSubiendoFacturas()) {
                    if (sesion.getFacturasExtraidas() != null && !sesion.getFacturasExtraidas().isEmpty()) {
                        botones.add(ResponseBot.fila(ResponseBot.boton("✅ Terminar y calcular saldo", "listo")));
                        botones.add(ResponseBot.fila(ResponseBot.boton("↩️ Corregir (Borrar última factura)", "corregir_factura")));
                    } else {
                        botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
                    }
                    return botones;
                }

                Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
                if (detalleOpt.isPresent()) {
                    TramiteDetalleDTO detalle = detalleOpt.get();
                    if ("Liquidación en proceso".equals(detalle.getEstado()) || "Liquidación revisada".equals(detalle.getEstado()) || "Liquidación aprobada".equals(detalle.getEstado()) || ("Transferido".equals(detalle.getEstado()) == false && detalle.getTotalGastado() != null)) {
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }
                }

                botones.add(ResponseBot.fila(ResponseBot.boton("📄 Iniciar Liquidación", "iniciar_liq_" + tramiteId)));
                botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && sesion.getAccionPendiente() == null) {
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("📝 Solicitar anticipo", "1"),
                        ResponseBot.boton("💰 Liquidar anticipo", "liquidar_lista")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("📋 Consultar estado", "3"),
                        ResponseBot.boton("📄 Ver mis documentos", "ver documentos")
                ));
                botones.add(ResponseBot.fila(
                        ResponseBot.boton("❓ Ayuda", "5"),
                        ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                        ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                ));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && sesion.isSubiendoFacturas()) {
                if (sesion.getFacturasExtraidas() != null && !sesion.getFacturasExtraidas().isEmpty()) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("✅ Terminar y calcular saldo", "listo")));
                }
                botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
                return botones;
            }

            if (rol != null && rol.equalsIgnoreCase("Funcionario") && sesion.getAccionPendiente() != null) {
                if ("SOLICITAR".equals(sesion.getAccionPendiente())) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("📄 Descargar formulario de solicitud", "descargar formulario")));
                } else if ("LIQUIDAR".equals(sesion.getAccionPendiente())) {
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("📄 Descargar formulario de liquidación", "descargar formulario"),
                            ResponseBot.boton("📎 Descargar facturas", "descargar facturas")
                    ));
                }
                botones.add(ResponseBot.fila(ResponseBot.boton("❌ Cancelar operación", "cancelar")));
                return botones;
            }

            if (rol != null && (rol.equalsIgnoreCase("Jefe Inmediato")
                    || rol.equalsIgnoreCase("Director Financiero")
                    || rol.equalsIgnoreCase("Contadora")
                    || rol.equalsIgnoreCase("Colectora")
                    || rol.equalsIgnoreCase("Asistente Contable"))
                    && sesion.getTramiteEnRevision() == null
                    && !sesion.isEsperandoMotivoRechazo()) {

                if (rol.equalsIgnoreCase("Jefe Inmediato")) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("📋 Ver solicitudes pendientes", "ver_solicitudes_pendientes")));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("❓ Ayuda", "5"),
                            ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                            ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                    ));
                    return botones;
                }

                if (rol.equalsIgnoreCase("Director Financiero")) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("📋 Aprobar Solicitudes de Anticipo", "ver_solicitudes_pendientes")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏦 Aprobar Órdenes de Anticipo", "ver_ordenes_anticipo")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("💰 Autorizar Reembolsos", "ver_reembolsos")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏦 Aprobar Órdenes de Reembolso", "ver_ordenes_reembolso")));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("❓ Ayuda", "5"),
                            ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                            ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                    ));
                    return botones;
                }

                if (rol.equalsIgnoreCase("Contadora")) {
                    if (sesion.getTramiteEnRevision() != null) {
                        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(sesion.getTramiteEnRevision());
                        if (detalleOpt.isPresent()) {
                            TramiteDetalleDTO detalle = detalleOpt.get();
                            if (detalle.getTotalGastado() != null) {
                                if (detalle.getSaldo().compareTo(java.math.BigDecimal.ZERO) > 0) {
                                    botones.add(ResponseBot.fila(ResponseBot.boton("✅ Cerrar Liquidación (Sobrante)", "cerrar_liquidacion_tramite_" + sesion.getTramiteEnRevision())));
                                } else if (detalle.getSaldo().compareTo(java.math.BigDecimal.ZERO) < 0) {
                                    botones.add(ResponseBot.fila(ResponseBot.boton("💸 Autorizar Reembolso (Faltante)", "autorizar_reembolso_tramite_" + sesion.getTramiteEnRevision())));
                                } else {
                                    botones.add(ResponseBot.fila(ResponseBot.boton("✅ Cerrar Liquidación (Exacto)", "cerrar_liquidacion_tramite_" + sesion.getTramiteEnRevision())));
                                }
                                botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                                return botones;
                            }
                        }
                    }

                    botones.add(ResponseBot.fila(ResponseBot.boton("📋 Revisar Solicitudes Pendientes", "ver_solicitudes_pendientes")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("💸 Revisar Liquidaciones Pendientes", "ver_liquidaciones_pendientes")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("🔍 Validar Devoluciones", "ver_devoluciones")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("📄 Registrar Comprobante", "registrar_comprobante")));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("❓ Ayuda", "5"),
                            ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                            ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                    ));
                    return botones;
                }

                if (rol.equalsIgnoreCase("Colectora")) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏦 Generar Orden de Anticipo", "generar_orden_anticipo")));
                    botones.add(ResponseBot.fila(ResponseBot.boton("💸 Ver Reembolsos Autorizados", "ver_reembolsos_colectora")));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("❓ Ayuda", "5"),
                            ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                            ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                    ));
                    return botones;
                }

                if (rol.equalsIgnoreCase("Asistente Contable")) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("📋 Revisar Liquidaciones Pendientes", "revisar_liquidaciones_asistente")));
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("❓ Ayuda", "5"),
                            ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                            ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                    ));
                    return botones;
                }

                botones.add(ResponseBot.fila(
                        ResponseBot.boton("❓ Ayuda", "5"),
                        ResponseBot.boton("🔐 Cambiar contraseña", "cambiar_contrasena"),
                        ResponseBot.boton("🚪 Cerrar sesión", "cerrar_sesion_confirmar")
                ));
                return botones;
            }

            if (sesion.getTramiteEnRevision() != null && !sesion.isEsperandoMotivoRechazo()) {
                if (!rol.equalsIgnoreCase("Funcionario")) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("📄 Ver documentación anexada", "ver_documentacion_anexada")));
                }

                if (rol != null && rol.equalsIgnoreCase("Colectora")) {
                    Long tramiteId = sesion.getTramiteEnRevision();
                    Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);

                    if (detalleOpt.isPresent() && "Reembolso autorizado".equals(detalleOpt.get().getEstado())) {
                        botones.add(ResponseBot.fila(ResponseBot.boton("📎 Subir PDF de Orden de Reembolso", "solicitar_pdf_orden_reembolso_" + tramiteId)));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    if (detalleOpt.isPresent() && "Esperando orden de reembolso".equals(detalleOpt.get().getEstado())) {
                        botones.add(ResponseBot.fila(ResponseBot.boton("📎 Subir PDF de Orden de Reembolso", "solicitar_pdf_orden_reembolso_" + tramiteId)));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    if (detalleOpt.isPresent() && "Orden de reembolso aprobada".equals(detalleOpt.get().getEstado())) {
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    botones.add(ResponseBot.fila(ResponseBot.boton("📄 Generar Orden de Transferencia", "solicitar_pdf_orden_" + tramiteId)));
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                    return botones;
                }

                if (rol != null && rol.equalsIgnoreCase("Asistente Contable")) {
                    Long tramiteId = sesion.getTramiteEnRevision();
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("📤 Enviar a Contadora", "enviar_contadora_" + tramiteId),
                            ResponseBot.boton("↩️ Devolver al Funcionario", "devolver_funcionario_" + tramiteId)
                    ));
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                    return botones;
                }

                if (rol != null && rol.equalsIgnoreCase("Contadora")) {
                    Optional<TramiteDetalleDTO> detalleLiqOpt = tramiteService.obtenerDetalle(sesion.getTramiteEnRevision());

                    if (detalleLiqOpt.isPresent() && "Liquidación revisada".equals(detalleLiqOpt.get().getEstado())) {
                        TramiteDetalleDTO detalleLiq = detalleLiqOpt.get();
                        if (detalleLiq.getSaldo().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            botones.add(ResponseBot.fila(ResponseBot.boton("✅ Cerrar Liquidación (Sobrante)", "cerrar_liquidacion_tramite_" + sesion.getTramiteEnRevision())));
                        } else if (detalleLiq.getSaldo().compareTo(java.math.BigDecimal.ZERO) < 0) {
                            botones.add(ResponseBot.fila(ResponseBot.boton("💸 Autorizar Reembolso (Faltante)", "autorizar_reembolso_tramite_" + sesion.getTramiteEnRevision())));
                        } else {
                            botones.add(ResponseBot.fila(ResponseBot.boton("✅ Cerrar Liquidación (Exacto)", "cerrar_liquidacion_tramite_" + sesion.getTramiteEnRevision())));
                        }
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorTramite(sesion.getTramiteEnRevision());
                    if (devolucionOpt.isPresent() && "Comprobante subido".equals(devolucionOpt.get().getEstado())) {
                        Long devolucionId = devolucionOpt.get().getDevolucionId();
                        botones.add(ResponseBot.fila(
                                ResponseBot.boton("✅ Validar devolución", "validar devolucion " + devolucionId),
                                ResponseBot.boton("❌ Rechazar devolución", "rechazar devolucion " + devolucionId)
                        ));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(sesion.getTramiteEnRevision());

                    if (detalleOpt.isPresent() && "Reembolso autorizado".equals(detalleOpt.get().getEstado())) {
                        Long tramiteId = sesion.getTramiteEnRevision();
                        botones.add(ResponseBot.fila(ResponseBot.boton("📤 Informar a Colectora", "informar_colectora_" + tramiteId)));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    if (detalleOpt.isPresent() && "Orden de reembolso aprobada".equals(detalleOpt.get().getEstado())) {
                        Long tramiteId = sesion.getTramiteEnRevision();
                        botones.add(ResponseBot.fila(ResponseBot.boton("📄 Registrar Comprobante y Cerrar", "registrar_comprobante_reembolso_" + tramiteId)));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    if (detalleOpt.isPresent() && "Orden aprobada".equals(detalleOpt.get().getEstado())) {
                        Long tramiteId = sesion.getTramiteEnRevision();
                        botones.add(ResponseBot.fila(ResponseBot.boton("📄 Registrar Comprobante #" + tramiteId, "registrar_comprobante_tramite_" + tramiteId)));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }
                }

                if (rol != null && rol.equalsIgnoreCase("Director Financiero")) {
                    Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(sesion.getTramiteEnRevision());

                    if (detalleOpt.isPresent() && "Liquidación aprobada".equals(detalleOpt.get().getEstado())) {
                        Long tramiteId = sesion.getTramiteEnRevision();
                        botones.add(ResponseBot.fila(ResponseBot.boton("✅ Autorizar Reembolso #" + tramiteId, "autorizar_reembolso_director_" + tramiteId)));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    if (detalleOpt.isPresent() && "Orden generada".equals(detalleOpt.get().getEstado())) {
                        Long tramiteId = sesion.getTramiteEnRevision();
                        botones.add(ResponseBot.fila(
                                ResponseBot.boton("✅ Aprobar Orden #" + tramiteId, "aprobar_orden_tramite_" + tramiteId),
                                ResponseBot.boton("❌ Rechazar Orden #" + tramiteId, "rechazar_orden_tramite_" + tramiteId)
                        ));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }

                    if (detalleOpt.isPresent() && "Orden de reembolso generada".equals(detalleOpt.get().getEstado())) {
                        Long tramiteId = sesion.getTramiteEnRevision();
                        botones.add(ResponseBot.fila(
                                ResponseBot.boton("✅ Aprobar Orden de Reembolso #" + tramiteId, "aprobar_orden_reembolso_" + tramiteId),
                                ResponseBot.boton("❌ Rechazar Orden #" + tramiteId, "rechazar_orden_tramite_" + tramiteId)
                        ));
                        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                        return botones;
                    }
                }

                Long tramiteId = sesion.getTramiteEnRevision();

                boolean tieneOrdenTransferencia = false;
                Optional<TramiteDetalleDTO> detalleChequeo = tramiteService.obtenerDetalle(tramiteId);

                if (detalleChequeo.isPresent()) {
                    String estadoActual = detalleChequeo.get().getEstado();
                    if (estadoActual.equals("Transferido") || estadoActual.equals("Orden generada") || estadoActual.equals("Orden aprobada") || estadoActual.equals("Liquidación cerrada") || estadoActual.equals("Liquidación en proceso") || estadoActual.equals("Liquidación revisada") || estadoActual.equals("Liquidación aprobada") || estadoActual.equals("Reembolso autorizado") || estadoActual.equals("Orden de reembolso generada") || estadoActual.equals("Orden de reembolso aprobada")) {
                        tieneOrdenTransferencia = true;
                    }
                }

                if (tieneOrdenTransferencia) {
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                } else {
                    botones.add(ResponseBot.fila(
                            ResponseBot.boton("✅ Aprobar Trámite #" + tramiteId, "aprobar_tramite_" + tramiteId),
                            ResponseBot.boton("❌ Rechazar Trámite #" + tramiteId, "rechazar_tramite_" + tramiteId)
                    ));
                    botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
                }
                return botones;
            }
        }

        return botones;
    }

    // ============================================================
    // CONFIRMACIÓN DE CANCELACIÓN
    // ============================================================
    private String procesarConfirmacionCancelacion(String mensajeLower, SesionUsuario sesion) {
        if (esComando(mensajeLower, "1", "si", "sí", "s", "yes", "confirmar", "confirmo")) {
            sesion.setEsperandoConfirmacionCancelacion(false);
            limpiarOperacion(sesion);
            return "✅ Operación cancelada.\n\n" + menuPrincipal(sesion);
        }
        if (esComando(mensajeLower, "2", "no", "n", "continuar")) {
            sesion.setEsperandoConfirmacionCancelacion(false);
            return "👍 Perfecto, continúo con tu operación.";
        }
        return "⚠️ No reconocí tu respuesta.\n\n👉 Usa los botones para responder.";
    }

    // ============================================================
    // CONFIRMACIÓN DE CERRAR SESIÓN
    // ============================================================
    private String procesarConfirmacionCerrarSesion(String mensajeLower, SesionUsuario sesion, Long chatId) {
        if (esComando(mensajeLower, "cerrar_sesion_si", "1", "si", "sí", "s", "yes", "confirmar", "confirmo")) {
            sesiones.remove(chatId);
            return "👋 Sesión cerrada correctamente. Hasta luego.\n\nPara volver a ingresar, escribe *hola*.";
        }
        if (esComando(mensajeLower, "cerrar_sesion_no", "2", "no", "n", "continuar")) {
            sesion.setEsperandoConfirmacionCerrarSesion(false);
            return "👍 Perfecto, continúo con tu sesión.\n\n" + menuPrincipal(sesion);
        }
        return "⚠️ No reconocí tu respuesta.\n\n👉 Usa los botones para responder.";
    }

    // ============================================================
    // AYUDA CONTEXTUAL
    // ============================================================
    private String menuAyudaContextual(SesionUsuario sesion) {
        String accion = sesion.getAccionPendiente();
        String rol = sesion.getRolNombre();

        if (rol != null && rol.equalsIgnoreCase("Funcionario")) {
            if (sesion.isEsperandoComprobanteDevolucion()) {
                return "ℹ️ *Ayuda — Comprobante de devolución*\n\n📌 Estás en el caso SOBRANTE.\n\n👉 Sube el comprobante de depósito en PDF para continuar.";
            }
            if ("SOLICITAR".equals(accion) && sesion.getFormularioSolicitud() == null) {
                return "ℹ️ *Ayuda — Esperando formulario*\n\n📌 En este momento necesitas enviar el formulario de solicitud de anticipo.\n\n👉 Adjúntalo en formato PDF desde esta conversación.";
            }
            if ("SOLICITAR".equals(accion) && sesion.getFormularioSolicitud() != null) {
                return "ℹ️ *Ayuda — Confirmando solicitud*\n\n📌 Ya extraje los datos del formulario.\n\n👉 Usa los botones para confirmar o corregir.";
            }
            if ("LIQUIDAR".equals(accion) && sesion.getFormularioLiquidacion() == null) {
                return "ℹ️ *Ayuda — Esperando formulario*\n\n📌 En este momento necesitas enviar el formulario de liquidación.\n\n👉 Adjúntalo en formato PDF.";
            }
            if ("LIQUIDAR".equals(accion) && sesion.isSubiendoFacturas()) {
                return "ℹ️ *Ayuda — Subiendo facturas*\n\n📌 Estás subiendo las facturas una por una.\n\n👉 Sube cada factura en PDF.\n✅ Cuando termines, escribe *listo*.\n\n📊 Llevas *" + sesion.getFacturasExtraidas().size() + "* facturas registradas.";
            }
            return "ℹ️ *Ayuda — Funcionario*\n\nPuedo ayudarte con:\n📝 Solicitar un anticipo\n💰 Liquidar un anticipo\n📋 Consultar el estado de tus trámites";
        }

        if (sesion.isEsperandoMotivoRechazo()) {
            return "ℹ️ *Ayuda — Motivo de rechazo*\n\n📌 Estás escribiendo el motivo del rechazo.\n\n👉 Escribe el motivo y se enviará como observación.";
        }

        if (sesion.getTramiteEnRevision() != null) {
            return "ℹ️ *Ayuda — Revisando trámite*\n\n📌 Estás revisando el trámite *" + sesion.getTramiteEnRevision() + "*.\n\n👉 Usa los botones para aprobar o rechazar.";
        }

        return "ℹ️ *Ayuda — " + rol + "*\n\n👉 Usa los botones del menú para navegar.";
    }

    // ============================================================
    // DOCUMENTOS
    // ============================================================
    public String procesarDocumento(MultipartFile archivo, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);

        if (sesion == null || !sesion.isAutenticado()) {
            return "⚠️ Debes iniciar sesión primero.\n\n👉 Escribe *hola* para comenzar.";
        }
        if (sesion.isEsperandoConfirmacionCancelacion()) {
            return "⚠️ Tienes una pregunta pendiente.\n\n👉 Responde con los botones antes de enviar documentos.";
        }
        if (sesion.isProcesandoDocumento()) {
            return "⏳ *Estoy procesando tu documento.*\n\n📌 Esto puede tardar 1-2 minutos (el OCR es lento).\n\n⏰ *Espera un momento* y envíalo de nuevo si no ves respuesta.";
        }

        sesion.setProcesandoDocumento(true);
        try {
            if (sesion.getRolNombre().equalsIgnoreCase("Funcionario") || sesion.getRolNombre().equalsIgnoreCase("Colectora")) {
                return funcionarioChatService.procesarDocumento(archivo, sesion);
            }
            return "ℹ️ Tu rol no permite subir documentos.";
        } finally {
            sesion.setProcesandoDocumento(false);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private boolean esComando(String mensajeLower, String... opciones) {
        for (String op : opciones) {
            if (mensajeLower.equals(op)) return true;
        }
        return false;
    }

    private void limpiarOperacion(SesionUsuario sesion) {
        sesion.setAccionPendiente(null);
        sesion.setFormularioSolicitud(null);
        sesion.setFormularioLiquidacion(null);
        sesion.setSubiendoFacturas(false);
        sesion.getFacturasExtraidas().clear();
        sesion.setTramiteEnRevision(null);
        sesion.setEsperandoMotivoRechazo(false);
        sesion.setEsperandoConfirmacionCancelacion(false);
        sesion.setEsperandoComprobanteDevolucion(false);
        sesion.setMontoDevolucionPendiente(null);
    }

    private String menuPrincipal(SesionUsuario sesion) {
        return "¿Qué necesitas hacer hoy?";
    }

    // ============================================================
    // DELEGACIÓN POR ROL
    // ============================================================
    private String delegarPorRol(String mensaje, String mensajeLower, SesionUsuario sesion, Long chatId) {
        String rol = sesion.getRolNombre();

        if (mensajeLower.matches("ver\\s+detalles\\s+\\d+")) {
            Long tramiteId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return verDetallesTramite(tramiteId);
        }

        if (mensajeLower.equals("descargar formulario") || mensajeLower.equals("descargar facturas")) {
            return procesarMensaje(mensaje, chatId);
        }

        if (mensajeLower.equals("ver_solicitudes_pendientes")
                || mensajeLower.equals("ver_ordenes_anticipo")
                || mensajeLower.equals("ver_liquidaciones_pendientes")
                || mensajeLower.equals("registrar_comprobante")
                || mensajeLower.equals("generar_orden_anticipo")
                || mensajeLower.equals("ver_reembolsos")
                || mensajeLower.equals("ver_ordenes_reembolso")
                || mensajeLower.equals("ver_devoluciones")
                || mensajeLower.equals("revisar_liquidaciones_asistente")
                || mensajeLower.startsWith("revisar_")
                || mensajeLower.startsWith("pag_")
                || mensajeLower.startsWith("aprobar_tramite_")
                || mensajeLower.startsWith("rechazar_tramite_")
                || mensajeLower.startsWith("aprobar_orden_tramite_")
                || mensajeLower.startsWith("rechazar_orden_tramite_")
                || mensajeLower.startsWith("registrar_comprobante_tramite_")
                || mensajeLower.startsWith("validar_devolucion_")
                || mensajeLower.startsWith("rechazar_devolucion_")
                || mensajeLower.startsWith("enviar_contadora_")
                || mensajeLower.startsWith("devolver_funcionario_")
                || mensajeLower.startsWith("generar_orden_tramite_")
                || mensajeLower.startsWith("autorizar_reembolso_tramite_")
                || mensajeLower.startsWith("autorizar_reembolso_director_")
                || mensajeLower.startsWith("solicitar_pdf_orden_reembolso_")
                || mensajeLower.startsWith("aprobar_orden_reembolso_")
                || mensajeLower.startsWith("registrar_comprobante_reembolso_")
                || mensajeLower.startsWith("informar_colectora_")) {
            return "";
        }

        String accion = sesion.getAccionPendiente();
        if (accion != null && !accion.isEmpty()) {
            if (accion.equals("SOLICITAR") || accion.equals("LIQUIDAR") || accion.equals("FACTURAS") || accion.equals("DEVOLUCION")) {
                boolean esConfirmacion = mensajeLower.equals("si") || mensajeLower.equals("sí")
                        || mensajeLower.equals("no") || mensajeLower.equals("confirmo")
                        || mensajeLower.equals("listo") || mensajeLower.equals("1")
                        || mensajeLower.equals("2") || mensajeLower.equals("3")
                        || mensajeLower.equals("4") || mensajeLower.equals("5")
                        || mensajeLower.equals("subir_comprobante")
                        || mensajeLower.contains("ayuda")
                        || mensajeLower.contains("opciones") || mensajeLower.contains("menu")
                        || mensajeLower.contains("menú") || mensajeLower.contains("cancelar");

                if (esConfirmacion) {
                } else {
                    boolean esTexto = !mensajeLower.contains(".pdf") && !mensajeLower.startsWith("http");
                    if (esTexto) {
                        switch (accion) {
                            case "SOLICITAR": return "⚠️ El archivo enviado no es un PDF.\n\n📌 En este paso necesito el formulario de solicitud de anticipo.\n\n👉 Adjúntalo nuevamente en formato PDF.";
                            case "LIQUIDAR": return "⚠️ El archivo enviado no es un PDF.\n\n📌 En este paso necesito el formulario de liquidación.\n\n👉 Adjúntalo nuevamente en formato PDF.";
                            case "DEVOLUCION": return "⚠️ El archivo enviado no es un PDF.\n\n📌 En este paso necesito el comprobante de devolución.\n\n👉 Adjúntalo nuevamente en formato PDF.";
                        }
                    }
                }
            }
        }

        if (rol.equalsIgnoreCase("Funcionario")) {
            String respuesta = funcionarioChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Jefe Inmediato")) {
            ConfiguracionAprobador config = new ConfiguracionAprobador(
                    "Pendiente", "Aprobado por Jefe",
                    usuarioService.obtenerCorreoPorRol("Director Financiero"),
                    "Jefe", null, "Director Financiero");
            String respuesta = aprobadorChatService.procesar(mensaje, mensajeLower, sesion, config);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Director Financiero")) {
            ConfiguracionAprobador config = new ConfiguracionAprobador(
                    "Aprobado por Jefe", "Aprobado por Director",
                    usuarioService.obtenerCorreoPorRol("Contadora"),
                    "Director", null, "Contadora");
            String respuesta = aprobadorChatService.procesar(mensaje, mensajeLower, sesion, config);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Contadora")) {
            String respuesta = contadoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Colectora")) {
            String respuesta = colectoraChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Asistente Contable")) {
            String respuesta = asistenteContableChatService.procesar(mensaje, mensajeLower, sesion);
            if (respuesta != null) return respuesta;
        }

        if (rol.equalsIgnoreCase("Administrador")) {
            if (mensajeLower.contains("usuario")) return "ℹ️ Mostrando gestión de usuarios...";
            if (mensajeLower.contains("historial")) return "ℹ️ Mostrando historial general...";
        }

        return mensajeFueraDeAlcance(sesion);
    }

    // ============================================================
    // LOGIN
    // ============================================================
    private ResponseBot manejarLogin(String mensaje, String mensajeLower, SesionUsuario sesion, Long chatId) {
        if (!sesion.isEsperandoContrasena()) {
            String correo = mensaje.trim();

            if (!correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return new ResponseBot("👋 Hola. Soy ITI, el asistente virtual del Instituto Tecnológico Internacional.\n\n👉 Para continuar, ingresa tu correo electrónico.");
            }

            if (!usuarioService.existeCorreo(correo)) {
                return new ResponseBot("⚠️ El correo no está registrado en el sistema.\n\n📌 Verifica que esté bien escrito.\n\n👉 Intenta nuevamente con tu correo institucional.");
            }

            sesion.setCorreo(correo);
            sesion.setEsperandoContrasena(true);
            return new ResponseBot("✅ Correo verificado.\n\n👉 Ahora ingresa tu contraseña.");
        }

        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setCorreo(sesion.getCorreo());
        loginDTO.setContrasena(mensaje.trim());

        LoginResponseDTO response = usuarioService.login(loginDTO);

        if (response.isBloqueado()) {
            String correoBloqueado = sesion.getCorreo();
            sesion.setEsperandoContrasena(false);
            sesion.setCorreo(null);
            return new ResponseBot("🔒 *Cuenta bloqueada*\n\n📌 La cuenta *" + correoBloqueado + "* está bloqueada por seguridad.\n\n📞 Contacta al departamento de sistemas del ITI:\n• Teléfono: (02) 123-4567\n• Correo: soporte@iti.edu.ec\n\nCuando soporte desbloquee tu cuenta, escribe *hola* para volver a intentar.");
        }

        if (response.isAutenticado()) {
            sesion.setUsuarioId(response.getUsuarioId());
            sesion.setNombre(response.getNombre());
            sesion.setRolNombre(response.getRolNombre());
            sesion.setAutenticado(true);
            sesion.setEsperandoContrasena(false);

            if (response.isRequiereCambioContrasena()) {
                sesion.setEsperandoNuevaContrasena(true);
                return new ResponseBot("🔐 *¡Bienvenido por primera vez!*\n\n📌 Por seguridad, debes cambiar tu contraseña antes de continuar.\n\n👉 Escribe tu *nueva contraseña* (mínimo 8 caracteres).\n\n⚠️ Si deseas cancelar, escribe *cancelar*.");
            }

            try {
                if (chatId != null) {
                    usuarioService.actualizarTelegramChatId(response.getUsuarioId(), chatId.toString());
                }
            } catch (Exception e) {
                System.err.println("No se pudo guardar el chatId de Telegram: " + e.getMessage());
            }

            String bienvenida = "👋 Hola *" + response.getNombre() + "*. Soy ITI, tu asistente virtual.\n\n📌 *Rol:* " + response.getRolNombre() + "\n\n";

            if (sesion.getTramiteRedirigido() != null) {
                Long tramiteId = sesion.getTramiteRedirigido();
                sesion.setTramiteRedirigido(null);
                sesion.setTramiteEnRevision(tramiteId);
                String detalle = verDetallesTramite(tramiteId);
                List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
                return new ResponseBot(bienvenida + "\n\n" + detalle, botones);
            }

            if (response.getRolNombre().equalsIgnoreCase("Funcionario")) {
                String avisoDevolucion = verificarDevolucionPendiente(response.getUsuarioId(), sesion);
                if (avisoDevolucion != null) {
                    return new ResponseBot(bienvenida + avisoDevolucion);
                }
            }

            return new ResponseBot(bienvenida + menuPrincipal(sesion));
        }

        return new ResponseBot("⚠️ Contraseña incorrecta.\n\n📌 El correo *" + sesion.getCorreo() + "* es válido, pero la contraseña no coincide.\n\n🔢 Intentos restantes: *" + response.getIntentosRestantes() + "*\n\n👉 Ingresa tu contraseña nuevamente.");
    }

    // ============================================================
    // UTILIDADES
    // ============================================================
    private boolean expiroSesion(SesionUsuario sesion) {
        return Duration.between(sesion.getUltimaActividad(), LocalDateTime.now()).toMinutes() > TIMEOUT_MINUTOS;
    }

    // ============================================================
    // MENSAJE FUERA DE ALCANCE
    // ============================================================
    private String mensajeFueraDeAlcance(SesionUsuario sesion) {
        String rol = sesion.getRolNombre();

        if (rol == null) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.";
        }

        if (rol.equalsIgnoreCase("Funcionario")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\nPuedo ayudarte con:\n📝 Solicitar un anticipo\n💰 Liquidar un anticipo\n📋 Consultar el estado de tus trámites\n\n👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Jefe Inmediato") || rol.equalsIgnoreCase("Director Financiero")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\nPuedo ayudarte con:\n📋 Revisar trámites pendientes\n✅ Aprobar o rechazar trámites\n\n👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Contadora")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\nPuedo ayudarte con:\n📋 Revisar solicitudes y liquidaciones\n✅ Aprobar o rechazar trámites\n🔍 Validar devoluciones\n\n👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Colectora")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\nPuedo ayudarte con:\n📋 Ver transferencias pendientes\n💸 Registrar transferencias\n\n👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Asistente Contable")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\nPuedo ayudarte con:\n📋 Revisar liquidaciones pendientes\n📤 Enviar liquidaciones a la Contadora\n\n👉 Usa los botones para continuar.";
        }

        if (rol.equalsIgnoreCase("Administrador")) {
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n👉 Usa los botones para continuar.";
        }

        return "🤔 Esa pregunta está fuera de mi alcance.\n\n📌 Soy un asistente especializado en la gestión de anticipos del ITI.";
    }

    // ============================================================
    // VERIFICAR DEVOLUCIÓN PENDIENTE
    // ============================================================
    private String verificarDevolucionPendiente(Long usuarioId, SesionUsuario sesion) {
        try {
            Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPendientePorUsuario(usuarioId);
            if (devolucionOpt.isPresent()) {
                DevolucionResponseDTO devolucion = devolucionOpt.get();
                sesion.setAccionPendiente("DEVOLUCION");
                return "⚠️ *Tienes una devolución pendiente*\n\n💰 Monto a devolver: $" + devolucion.getMonto() + "\n🆔 Trámite: #" + devolucion.getTramiteId() + "\n\n📎 Sube el comprobante de transferencia en PDF.\n\n👉 Adjúntalo en este chat.";
            }
        } catch (Exception e) {
            System.err.println("Error al verificar devolución: " + e.getMessage());
        }
        return null;
    }

    // ============================================================
    // VER DETALLES DE UN TRÁMITE
    // ============================================================
    private String verDetallesTramite(Long tramiteId) {
        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);

        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite #" + tramiteId + ".";
        }

        TramiteDetalleDTO d = detalleOpt.get();

        StringBuilder sb = new StringBuilder();

        String tituloBloque;
        if (d.getTotalGastado() != null) {
            tituloBloque = "💰 *Datos de la liquidación:*";
        } else {
            tituloBloque = "📝 *Datos de la solicitud:*";
        }

        sb.append("📋 *Trámite #").append(d.getTramiteId()).append("*\n\n");
        sb.append(tituloBloque).append("\n");
        sb.append("👤 Solicitante: ").append(d.getNombreUsuario()).append("\n");
        sb.append("📝 Tipo: ").append(d.getNombreTipoTramite()).append("\n");
        sb.append("📅 Fecha creación: ").append(d.getFechaCreacion()).append("\n");
        sb.append("📊 Estado: *").append(d.getEstado()).append("*\n");

        if (d.getTotalGastado() == null) {
            if (d.getPresupuesto() != null) {
                sb.append("💰 Presupuesto: $").append(d.getPresupuesto()).append("\n");
            } else {
                solicitudService.buscarPorTramiteId(tramiteId).ifPresent(sol -> {
                    sb.append("💰 Presupuesto: $").append(sol.getPresupuesto()).append("\n");
                });
            }
            if (d.getProposito() != null) sb.append("🎯 Propósito: ").append(d.getProposito()).append("\n");
            if (d.getUnidad() != null) sb.append("🏢 Unidad: ").append(d.getUnidad()).append("\n");
            if (d.getCampus() != null) sb.append("📍 Campus: ").append(d.getCampus()).append("\n");
            if (d.getLugar() != null) sb.append("📌 Lugar: ").append(d.getLugar()).append("\n");
            if (d.getFechaRealizacion() != null) sb.append("📅 Fecha realización: ").append(d.getFechaRealizacion()).append("\n");

            sb.append("\n📄 *Documentos adjuntos:*\n");
            sb.append("• Formulario: subido ✅\n");
        }

        if (d.getTotalGastado() != null) {
            sb.append("💵 Total gastado: $").append(d.getTotalGastado()).append("\n");
            if (d.getSaldo() != null) sb.append("📈 Saldo: $").append(d.getSaldo()).append("\n");
            if (d.getProposito() != null) sb.append("🎯 Propósito: ").append(d.getProposito()).append("\n");
            if (d.getFechaPresentacion() != null) sb.append("📅 Fecha presentación: ").append(d.getFechaPresentacion()).append("\n");

            if (d.getSaldo() != null) {
                if (d.getSaldo().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    sb.append("\n📌 *Tipo de caso:* SOBRANTE\n");
                    sb.append("👉 El funcionario debe devolver $").append(d.getSaldo()).append(" al ITI.\n");
                } else if (d.getSaldo().compareTo(java.math.BigDecimal.ZERO) < 0) {
                    sb.append("\n📌 *Tipo de caso:* FALTANTE (REEMBOLSO)\n");
                    sb.append("👉 El ITI debe reembolsar $").append(d.getSaldo().abs()).append(" al funcionario.\n");
                } else {
                    sb.append("\n📌 *Tipo de caso:* EXACTO\n");
                    sb.append("👉 No hay devoluciones ni reembolsos pendientes.\n");
                }
            }

            sb.append("\n📄 *Documentos adjuntos:*\n");
            sb.append("• Formulario: subido ✅\n");
            sb.append("• Facturas: subidas ✅\n");
            if (d.getSaldo() != null && d.getSaldo().compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append("• Comprobante de transferencia: subido ✅\n");
            }
        }

        sb.append("\n👉 Usa los botones del menú para continuar.");

        return sb.toString();
    }

    // ============================================================
    // MOSTRAR LISTA PAGINADA
    // ============================================================
    private ResponseBot mostrarListaPaginada(SesionUsuario sesion, String estado, String nombreLista, int pagina, String titulo) {
        final int TAMAÑO_PAGINA = 5;

        if ("DEVOLUCIONES_CONTADORA".equals(nombreLista)) {
            return mostrarListaDevolucionesPaginada(sesion, pagina);
        }

        org.springframework.data.domain.Page<ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO> page =
                tramiteService.listarPaginado(estado, pagina, TAMAÑO_PAGINA);

        if ("LIQUIDACIONES_ASISTENTE".equals(nombreLista) || "LIQUIDACIONES_CONTADORA".equals(nombreLista)) {
            List<ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO> filtrados = page.getContent().stream()
                    .filter(t -> {
                        Optional<TramiteDetalleDTO> det = tramiteService.obtenerDetalle(t.getTramiteId());
                        return det.isPresent() && det.get().getTotalGastado() != null;
                    })
                    .toList();
            page = new org.springframework.data.domain.PageImpl<>(filtrados, page.getPageable(), filtrados.size());
        }

        if (page.isEmpty()) {
            sesion.setPaginaActualLista(0);
            sesion.setListaEnProgreso(null);
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("📋 No hay " + titulo.toLowerCase() + " pendientes.\n\n👉 Usa los botones para continuar.", botones);
        }

        sesion.setPaginaActualLista(pagina);
        sesion.setListaEnProgreso(nombreLista);
        sesion.setEstadoFiltroLista(estado);

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *").append(titulo).append(":* (").append(page.getTotalElements()).append(")\n\n");

        for (ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO t : page.getContent()) {
            sb.append("🆔 ").append(t.getTramiteId()).append(" — ").append(t.getNombreUsuario());
            if (t.getProposito() != null) {
                String prop = t.getProposito();
                if (prop.length() > 30) prop = prop.substring(0, 30) + "...";
                sb.append(" · ").append(prop);
            }
            sb.append("\n");
        }

        sb.append("\n📄 Página *").append(pagina + 1).append(" de ").append(page.getTotalPages()).append("*");

        List<List<ResponseBot.Boton>> botones = new ArrayList<>();
        for (ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO t : page.getContent()) {
            botones.add(ResponseBot.fila(ResponseBot.boton("🔍 Ver trámite #" + t.getTramiteId(), "revisar_" + t.getTramiteId())));
        }

        List<ResponseBot.Boton> filaPaginacion = new ArrayList<>();
        if (pagina > 0) filaPaginacion.add(ResponseBot.boton("⬅️ Anterior", "pag_" + nombreLista + "_" + (pagina - 1)));
        if (pagina < page.getTotalPages() - 1) filaPaginacion.add(ResponseBot.boton("Siguiente ➡️", "pag_" + nombreLista + "_" + (pagina + 1)));
        if (!filaPaginacion.isEmpty()) botones.add(filaPaginacion);

        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
        return new ResponseBot(sb.toString(), botones);
    }

    // ============================================================
    // MOSTRAR LISTA DE DEVOLUCIONES PAGINADA
    // ============================================================
    private ResponseBot mostrarListaDevolucionesPaginada(SesionUsuario sesion, int pagina) {
        final int TAMAÑO_PAGINA = 5;
        List<DevolucionResponseDTO> todas = devolucionService.listarPorEstado("Comprobante subido");

        if (todas.isEmpty()) {
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("📋 No hay devoluciones pendientes de validar.\n\n👉 Usa los botones para continuar.", botones);
        }

        int totalPaginas = (int) Math.ceil((double) todas.size() / TAMAÑO_PAGINA);
        int inicio = pagina * TAMAÑO_PAGINA;
        int fin = Math.min(inicio + TAMAÑO_PAGINA, todas.size());
        List<DevolucionResponseDTO> paginaActual = todas.subList(inicio, fin);

        StringBuilder sb = new StringBuilder();
        sb.append("🔍 *Devoluciones pendientes de validar:* (").append(todas.size()).append(")\n\n");

        for (DevolucionResponseDTO d : paginaActual) {
            sb.append("🆔 Devolución #").append(d.getDevolucionId()).append(" — Trámite #").append(d.getTramiteId()).append(" — $").append(d.getMonto()).append("\n");
        }

        sb.append("\n📄 Página *").append(pagina + 1).append(" de ").append(totalPaginas).append("*");

        List<List<ResponseBot.Boton>> botones = new ArrayList<>();
        for (DevolucionResponseDTO d : paginaActual) {
            botones.add(ResponseBot.fila(
                    ResponseBot.boton("✅ Validar Devolución #" + d.getDevolucionId(), "validar_devolucion_" + d.getDevolucionId()),
                    ResponseBot.boton("❌ Rechazar Devolución #" + d.getDevolucionId(), "rechazar_devolucion_" + d.getDevolucionId())
            ));
        }

        List<ResponseBot.Boton> filaPaginacion = new ArrayList<>();
        if (pagina > 0) filaPaginacion.add(ResponseBot.boton("⬅️ Anterior", "pag_DEVOLUCIONES_CONTADORA_" + (pagina - 1)));
        if (pagina < totalPaginas - 1) filaPaginacion.add(ResponseBot.boton("Siguiente ➡️", "pag_DEVOLUCIONES_CONTADORA_" + (pagina + 1)));
        if (!filaPaginacion.isEmpty()) botones.add(filaPaginacion);

        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
        return new ResponseBot(sb.toString(), botones);
    }

    // ============================================================
    // APROBAR TRÁMITE CON NOTIFICACIÓN ESPECÍFICA
    // ============================================================
    private ResponseBot aprobarTramiteConNotificacion(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        String rol = sesion.getRolNombre();
        String nuevoEstado;
        String nombreSiguienteRol;
        String tituloSiguiente;
        String instruccionSiguiente;

        if (rol.equalsIgnoreCase("Jefe Inmediato")) {
            nuevoEstado = "Aprobado por Jefe";
            nombreSiguienteRol = "Director Financiero";
            tituloSiguiente = "Solicitud de anticipo pendiente de aprobación";
            instruccionSiguiente = "Toca el botón *[📄 Revisar Trámite]* que acabas de recibir para abrirlo directamente.";
        } else if (rol.equalsIgnoreCase("Director Financiero")) {
            nuevoEstado = "Aprobado por Director";
            nombreSiguienteRol = "Contadora";
            tituloSiguiente = "Solicitud de anticipo pendiente de revisión";
            instruccionSiguiente = "Toca el botón *[📄 Revisar Trámite]* que acabas de recibir para abrirlo directamente.";
        } else if (rol.equalsIgnoreCase("Contadora")) {
            nuevoEstado = "Aprobado por Contadora";
            nombreSiguienteRol = "Colectora";
            tituloSiguiente = "Solicitud aprobada - Generar orden de transferencia";
            instruccionSiguiente = "Toca el botón *[📄 Revisar Trámite]* para generar la orden de transferencia.";
        } else {
            return new ResponseBot("⚠️ Tu rol no puede aprobar este trámite.");
        }

        tramiteService.actualizarEstado(tramiteId, nuevoEstado);

        String correoSiguiente = usuarioService.obtenerCorreoPorRol(nombreSiguienteRol);

        if (correoSiguiente != null) {
            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
            String nombreSolicitante = detalleOpt.map(d -> d.getNombreUsuario()).orElse("N/A");
            String presupuesto = detalleOpt.map(d -> {
                if (d.getPresupuesto() != null) return d.getPresupuesto().toString();
                return solicitudService.buscarPorTramiteId(d.getTramiteId()).map(s -> s.getPresupuesto().toString()).orElse("N/A");
            }).orElse("N/A");

            String mensajeNotificacion = "🔔 *" + tituloSiguiente + "*\n\n" +
                    "🆔 Trámite: #" + tramiteId + "\n" +
                    "👤 Solicitante: " + nombreSolicitante + "\n" +
                    "💰 Presupuesto: $" + presupuesto + "\n" +
                    "📊 Estado: " + nuevoEstado + "\n\n" +
                    "👉 " + instruccionSiguiente;

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoSiguiente);
            notifDTO.setTipo(tituloSiguiente);
            notifDTO.setMensajePersonalizado(mensajeNotificacion);
            notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);

            notificacionService.guardar(notifDTO);
        } else {
            System.err.println("⚠️ ALERTA: No se encontró ningún usuario con el rol: " + nombreSiguienteRol);
        }

        sesion.setTramiteEnRevision(null);

        String mensajeConfirmacion = "✅ *Trámite #" + tramiteId + " aprobado correctamente.*\n\n" +
                "📬 Se ha notificado al *" + nombreSiguienteRol + "* " +
                "para que " + obtenerAccionSiguiente(nombreSiguienteRol) + ".";

        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot(mensajeConfirmacion, botones);
    }

    // ============================================================
    // INICIAR RECHAZO CON MOTIVO
    // ============================================================
    private ResponseBot iniciarRechazoConMotivo(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        sesion.setTramiteEnRevision(tramiteId);
        sesion.setEsperandoMotivoRechazo(true);

        return new ResponseBot("✍️ ¿Cuál es el motivo del rechazo del trámite #" + tramiteId + "?\n\n👉 Escribe el motivo y se enviará al funcionario.", null);
    }

    // ============================================================
    // PROCESAR MOTIVO DE RECHAZO
    // ============================================================
    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();
        if (tramiteId == null) {
            return "⚠️ No encontré el trámite en revisión.";
        }

        if (sesion.getRolNombre() != null && sesion.getRolNombre().equalsIgnoreCase("Contadora")) {
            Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPorId(tramiteId);
            if (devolucionOpt.isPresent()) {
                devolucionService.actualizarEstado(tramiteId, "Rechazado");
                String correoFuncionario = tramiteService.obtenerDetalle(devolucionOpt.get().getTramiteId())
                        .map(d -> d.getCorreoUsuario()).orElse(null);
                if (correoFuncionario != null) {
                    NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                    notifDTO.setTramiteId(devolucionOpt.get().getTramiteId());
                    notifDTO.setDestinatario(correoFuncionario);
                    notifDTO.setTipo("Devolución rechazada");
                    notifDTO.setMensajePersonalizado("❌ *Devolución rechazada*\n\n🆔 Devolución: #" + tramiteId + "\n📌 Motivo: " + motivo);
                    notificacionService.guardar(notifDTO);
                }
                sesion.setTramiteEnRevision(null);
                sesion.setMotivoRechazo(null);
                return "❌ *Devolución #" + tramiteId + " rechazada.*\n\n📌 Motivo: " + motivo + "\n\n📬 Se ha notificado al *Funcionario*.";
            }
        }

        if (sesion.getRolNombre() != null && sesion.getRolNombre().equalsIgnoreCase("Asistente Contable")) {
            tramiteService.actualizarEstado(tramiteId, "Devuelto al funcionario");
            String correoFuncionario = tramiteService.obtenerDetalle(tramiteId).map(d -> d.getCorreoUsuario()).orElse(null);
            if (correoFuncionario != null) {
                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoFuncionario);
                notifDTO.setTipo("Liquidación devuelta para corrección");
                notifDTO.setMensajePersonalizado("↩️ *Tu liquidación ha sido devuelta*\n\n🆔 Trámite: #" + tramiteId + "\n📌 Observación: " + motivo + "\n\n👉 Ingresa al bot para corregirla.");
                notificacionService.guardar(notifDTO);
            }
            sesion.setTramiteEnRevision(null);
            sesion.setMotivoRechazo(null);
            return "↩️ *Liquidación #" + tramiteId + " devuelta al Funcionario.*\n\n📌 Observación: " + motivo + "\n\n📬 Se ha notificado al *Funcionario*.";
        }

        tramiteService.actualizarEstado(tramiteId, "Rechazado");
        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId).map(d -> d.getCorreoUsuario()).orElse(null);

        if (correoFuncionario != null) {
            String mensajeNotificacion = "❌ *Tu solicitud ha sido rechazada*\n\n🆔 Trámite: #" + tramiteId + "\n📌 Motivo: " + motivo + "\n\n👉 Ingresa al bot para más detalles.";
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoFuncionario);
            notifDTO.setTipo("Trámite rechazado");
            notifDTO.setMensajePersonalizado(mensajeNotificacion);
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ *Trámite #" + tramiteId + " rechazado.*\n\n📌 Motivo: " + motivo + "\n\n📬 Se ha notificado al *Funcionario*.";
    }

    // ============================================================
    // ASISTENTE CONTABLE: ENVIAR A CONTADORA
    // ============================================================
    private ResponseBot enviarAContadora(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        tramiteService.actualizarEstado(tramiteId, "Liquidación revisada");

        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
        if (correoContadora != null) {
            String mensajeNotificacion = "💸 *Nueva liquidación pendiente de aprobación*\n\n🆔 Trámite: #" + tramiteId + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* que acabas de recibir para abrirlo directamente.";

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoContadora);
            notifDTO.setTipo("Liquidación pendiente de aprobación");
            notifDTO.setMensajePersonalizado(mensajeNotificacion);
            notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot("✅ *Liquidación #" + tramiteId + " enviada a la Contadora.*\n\n📬 Se ha notificado a la *Contadora* para su revisión.", botones);
    }

    // ============================================================
    // ASISTENTE CONTABLE: DEVOLVER AL FUNCIONARIO
    // ============================================================
    private ResponseBot iniciarDevolucionFuncionario(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        sesion.setTramiteEnRevision(tramiteId);
        sesion.setEsperandoMotivoRechazo(true);
        return new ResponseBot("✍️ ¿Cuál es la observación para devolver la liquidación del trámite #" + tramiteId + " al Funcionario?\n\n👉 Escribe el motivo y se enviará al Funcionario.");
    }

    // ============================================================
    // COLECTORA: GENERAR ORDEN DE TRANSFERENCIA
    // ============================================================
    private ResponseBot generarOrdenColectora(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        if (detalleOpt.isEmpty()) {
            return new ResponseBot("⚠️ No encontré el trámite #" + tramiteId + ".");
        }

        TramiteDetalleDTO detalle = detalleOpt.get();

        String valor = "0.00";
        if (detalle.getPresupuesto() != null) {
            valor = detalle.getPresupuesto().toString();
        } else {
            Optional<SolicitudResponseDTO> solOpt = solicitudService.buscarPorTramiteId(tramiteId);
            if (solOpt.isPresent() && solOpt.get().getPresupuesto() != null) {
                valor = solOpt.get().getPresupuesto().toString();
            }
        }

        tramiteService.actualizarEstado(tramiteId, "Orden generada");

        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");
        if (correoDirector != null) {
            String mensajeNotificacion = "🔔 *Nueva orden de transferencia pendiente de aprobación*\n\n🆔 Trámite: #" + tramiteId + "\n👤 Solicitante: " + detalle.getNombreUsuario() + "\n💰 Valor: $" + valor + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* que acabas de recibir para aprobar la orden.";

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoDirector);
            notifDTO.setTipo("Orden de transferencia pendiente");
            notifDTO.setMensajePersonalizado(mensajeNotificacion);
            notifDTO.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot("✅ *Orden de transferencia generada correctamente.*\n\n🆔 Trámite: #" + tramiteId + "\n💰 Valor: $" + valor + "\n📊 Estado: *Orden generada*\n\n📬 Se ha notificado al *Director Financiero* para que apruebe la orden.", botones);
    }

    // ============================================================
    // DIRECTOR: APROBAR ORDEN DE TRANSFERENCIA
    // ============================================================
    private ResponseBot aprobarOrdenConNotificacion(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        // 1. Cambiar estado del trámite
        tramiteService.actualizarEstado(tramiteId, "Orden aprobada");

        // ✅ Buscar la orden PENDIENTE ("Generada") del trámite
        Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService
                .listarPorEstado("Generada")
                .stream()
                .filter(o -> tramiteId.equals(o.getTramiteId()))
                .findFirst();

        if (ordenOpt.isPresent()) {
            OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
            ordenDTO.setTramiteId(tramiteId);
            ordenDTO.setColectoraId(ordenOpt.get().getColectoraId());
            ordenDTO.setDirectorId(sesion.getUsuarioId());
            ordenDTO.setNumeroCuenta(ordenOpt.get().getNumeroCuenta());
            ordenDTO.setValor(ordenOpt.get().getValor());
            ordenDTO.setConcepto(ordenOpt.get().getConcepto());
            ordenDTO.setFechaGeneracion(ordenOpt.get().getFechaGeneracion());
            ordenDTO.setFechaAprobacion(LocalDate.now());
            ordenDTO.setEstado("Aprobada");

            ordenTransferenciaService.actualizar(ordenOpt.get().getOrdenId(), ordenDTO);
        } else {
            System.err.println("⚠️ No se encontró orden 'Generada' para el trámite #" + tramiteId);
        }

        // 3. Notificar a Colectora y Contadora
        String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        String nombreSolicitante = detalleOpt.map(d -> d.getNombreUsuario()).orElse("N/A");

        String valor = "N/A";
        if (detalleOpt.isPresent() && detalleOpt.get().getPresupuesto() != null) {
            valor = detalleOpt.get().getPresupuesto().toString();
        } else {
            Optional<SolicitudResponseDTO> solOpt = solicitudService.buscarPorTramiteId(tramiteId);
            if (solOpt.isPresent() && solOpt.get().getPresupuesto() != null) {
                valor = solOpt.get().getPresupuesto().toString();
            }
        }

        if (correoColectora != null) {
            NotificacionRequestDTO notifColectora = new NotificacionRequestDTO();
            notifColectora.setTramiteId(tramiteId);
            notifColectora.setDestinatario(correoColectora);
            notifColectora.setTipo("Orden de transferencia aprobada");
            notifColectora.setMensajePersonalizado("✅ *Orden de transferencia aprobada*\n\n" +
                    "🆔 Trámite: #" + tramiteId + "\n" +
                    "👤 Solicitante: " + nombreSolicitante + "\n" +
                    "💰 Valor: $" + valor + "\n\n" +
                    "👉 Toca el botón *[📄 Revisar Trámite]* para ver los detalles.");
            notifColectora.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
            notificacionService.guardar(notifColectora);
        }

        if (correoContadora != null) {
            NotificacionRequestDTO notifContadora = new NotificacionRequestDTO();
            notifContadora.setTramiteId(tramiteId);
            notifContadora.setDestinatario(correoContadora);
            notifContadora.setTipo("Orden aprobada - Registrar comprobante");
            notifContadora.setMensajePersonalizado("📄 *Orden de transferencia aprobada - Registrar comprobante*\n\n" +
                    "🆔 Trámite: #" + tramiteId + "\n" +
                    "👤 Solicitante: " + nombreSolicitante + "\n" +
                    "💰 Valor: $" + valor + "\n\n" +
                    "👉 Toca el botón *[📄 Revisar Trámite]* para registrar la transferencia.");
            notifContadora.setUrlBotonTelegram("https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId);
            notificacionService.guardar(notifContadora);
        }

        sesion.setTramiteEnRevision(null);
        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot("✅ *Orden de transferencia #" + tramiteId + " aprobada correctamente.*\n\n" +
                "📬 Se ha notificado a la *Colectora* y a la *Contadora*.\n\n" +
                "📌 La *Contadora* debe registrar el comprobante.", botones);
    }

    // ============================================================
    // CONTADORA: REGISTRAR COMPROBANTE (ANTICIPO)
    // ============================================================
    private ResponseBot registrarComprobanteContadora(Long tramiteId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Sesión expirada.");
        }

        tramiteService.actualizarEstado(tramiteId, "Transferido");

        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId).map(d -> d.getCorreoUsuario()).orElse(null);
        String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);
        String nombreSolicitante = detalleOpt.map(d -> d.getNombreUsuario()).orElse("N/A");

        String valor = "N/A";
        if (detalleOpt.isPresent()) {
            TramiteDetalleDTO detalle = detalleOpt.get();
            if (detalle.getPresupuesto() != null) {
                valor = detalle.getPresupuesto().toString();
            } else {
                Optional<SolicitudResponseDTO> solOpt = solicitudService.buscarPorTramiteId(tramiteId);
                if (solOpt.isPresent() && solOpt.get().getPresupuesto() != null) {
                    valor = solOpt.get().getPresupuesto().toString();
                } else {
                    Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService.buscarPorTramite(tramiteId);
                    if (ordenOpt.isPresent() && ordenOpt.get().getValor() != null) {
                        valor = ordenOpt.get().getValor().toString();
                    }
                }
            }
        }

        String mensaje = "💰 *Anticipo acreditado*\n\n🆔 Trámite: #" + tramiteId + "\n👤 Solicitante: " + nombreSolicitante + "\n💰 Valor: $" + valor + "\n📊 Estado: Transferido\n\n✅ El anticipo ha sido acreditado exitosamente.";

        if (correoFuncionario != null) {
            NotificacionRequestDTO notifFuncionario = new NotificacionRequestDTO();
            notifFuncionario.setTramiteId(tramiteId);
            notifFuncionario.setDestinatario(correoFuncionario);
            notifFuncionario.setTipo("Anticipo acreditado");
            notifFuncionario.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifFuncionario);
        }

        if (correoDirector != null) {
            NotificacionRequestDTO notifDirector = new NotificacionRequestDTO();
            notifDirector.setTramiteId(tramiteId);
            notifDirector.setDestinatario(correoDirector);
            notifDirector.setTipo("Anticipo acreditado");
            notifDirector.setMensajePersonalizado(mensaje);
            notificacionService.guardar(notifDirector);
        }

        sesion.setTramiteEnRevision(null);
        List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
        return new ResponseBot("✅ *Comprobante registrado correctamente.*\n\n🆔 Trámite: #" + tramiteId + "\n💰 Valor: $" + valor + "\n📊 Estado: *Transferido*\n\n📬 Se ha notificado al *Funcionario* y al *Director Financiero*.\n\n🎉 El anticipo ha sido acreditado exitosamente.", botones);
    }

    // ============================================================
    // APROBACIÓN DIRECTA CON PDF (legacy)
    // ============================================================
    private ResponseBot aprobarOrdenConPdf(Long ordenId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Debes iniciar sesión primero.");
        }
        return new ResponseBot("✅ *Orden #" + ordenId + " aprobada exitosamente.*\n\n📄 Aquí tienes tu comprobante en PDF.", null, "Comprobante_Orden_" + ordenId + ".pdf");
    }

    private ResponseBot rechazarOrden(Long ordenId, Long chatId) {
        SesionUsuario sesion = sesiones.get(chatId);
        if (sesion == null || !sesion.isAutenticado()) {
            return new ResponseBot("⚠️ Debes iniciar sesión primero.");
        }
        return new ResponseBot("❌ *Orden #" + ordenId + " rechazada.*\n\n📌 Se ha notificado al solicitante.");
    }

    private String obtenerTituloLista(String nombreLista) {
        switch (nombreLista) {
            case "SOLICITUDES_JEFE": return "Solicitudes pendientes";
            case "SOLICITUDES_DIRECTOR": return "Solicitudes de anticipo pendientes";
            case "ORDENES_DIRECTOR": return "Órdenes de anticipo pendientes";
            case "REEMBOLSOS_DIRECTOR": return "Reembolsos pendientes";
            case "ORDENES_REEMBOLSO_DIRECTOR": return "Órdenes de reembolso pendientes";
            case "SOLICITUDES_CONTADORA": return "Solicitudes pendientes";
            case "LIQUIDACIONES_CONTADORA": return "Liquidaciones pendientes";
            case "DEVOLUCIONES_CONTADORA": return "Devoluciones por validar";
            case "LIQUIDACIONES_ASISTENTE": return "Liquidaciones por revisar";
            case "ANTICIPOS_COLECTORA": return "Anticipos por generar orden";
            case "COMPROBANTES_CONTADORA": return "Comprobantes por registrar";
            default: return "Pendientes";
        }
    }

    private String obtenerAccionSiguiente(String rol) {
        switch (rol) {
            case "Director Financiero": return "revise y apruebe la solicitud";
            case "Contadora": return "revise y apruebe la solicitud";
            case "Colectora": return "genere la orden de transferencia";
            case "Asistente Contable": return "revise la liquidación";
            default: return "continúe con el proceso";
        }
    }

    public Map<Long, SesionUsuario> obtenerSesiones() {
        return sesiones;
    }

    private ResponseBot mostrarListaTramitesPorLiquidar(SesionUsuario sesion) {
        List<TramiteResponseDTO> tramites = tramiteService.listarPorUsuarioYEstado(sesion.getUsuarioId(), "Transferido");

        if (tramites.isEmpty()) {
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("⚠️ *No tienes anticipos pendientes de liquidar.*\n\n📌 Solo puedes liquidar un anticipo cuando ya haya sido transferido a tu cuenta.\n\n👉 Usa la opción *3* para consultar el estado de tus solicitudes.", botones);
        }

        StringBuilder sb = new StringBuilder("📋 *Tus anticipos pendientes de liquidar:*\n\n");
        List<List<ResponseBot.Boton>> botones = new ArrayList<>();

        for (TramiteResponseDTO t : tramites) {
            sb.append("🆔 Trámite #").append(t.getTramiteId()).append(" — ").append(t.getProposito()).append("\n");
            botones.add(ResponseBot.fila(ResponseBot.boton("🔍 Ver trámite #" + t.getTramiteId(), "revisar_" + t.getTramiteId())));
        }

        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
        return new ResponseBot(sb.toString(), botones);
    }

    private ResponseBot mostrarListaTramitesLiquidacion(SesionUsuario sesion, String estado, int pagina) {
        final int TAMAÑO_PAGINA = 5;

        List<TramiteResponseDTO> todas = tramiteService.listarPorEstadoYTipo(estado);

        if (todas.isEmpty()) {
            List<List<ResponseBot.Boton>> botones = determinarBotones(sesion);
            return new ResponseBot("📋 No hay liquidaciones pendientes de revisar.\n\n👉 Usa los botones para continuar.", botones);
        }

        int totalPaginas = (int) Math.ceil((double) todas.size() / TAMAÑO_PAGINA);
        int inicio = pagina * TAMAÑO_PAGINA;
        int fin = Math.min(inicio + TAMAÑO_PAGINA, todas.size());
        List<TramiteResponseDTO> paginaActual = todas.subList(inicio, fin);

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Liquidaciones por revisar:* (").append(todas.size()).append(")\n\n");

        List<List<ResponseBot.Boton>> botones = new ArrayList<>();

        for (TramiteResponseDTO t : paginaActual) {
            sb.append("🆔 Trámite #").append(t.getTramiteId()).append(" — ").append(t.getNombreUsuario()).append("\n");
            botones.add(ResponseBot.fila(ResponseBot.boton("🔍 Ver trámite #" + t.getTramiteId(), "revisar_" + t.getTramiteId())));
        }

        sb.append("\n📄 Página *").append(pagina + 1).append(" de ").append(totalPaginas).append("*");

        List<ResponseBot.Boton> filaPaginacion = new ArrayList<>();
        if (pagina > 0) filaPaginacion.add(ResponseBot.boton("⬅️ Anterior", "pag_LIQUIDACIONES_ASISTENTE_" + (pagina - 1)));
        if (pagina < totalPaginas - 1) filaPaginacion.add(ResponseBot.boton("Siguiente ➡️", "pag_LIQUIDACIONES_ASISTENTE_" + (pagina + 1)));
        if (!filaPaginacion.isEmpty()) botones.add(filaPaginacion);

        botones.add(ResponseBot.fila(ResponseBot.boton("🏠 Menú principal", "menu")));
        return new ResponseBot(sb.toString(), botones);
    }
}