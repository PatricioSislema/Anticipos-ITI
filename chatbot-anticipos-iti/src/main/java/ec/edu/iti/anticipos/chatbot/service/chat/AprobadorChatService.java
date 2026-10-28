package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.OrdenTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.OrdenTransferenciaResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.ReembolsoResponseDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.*;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ec.edu.iti.anticipos.chatbot.util.MensajeHelper;
import ec.edu.iti.anticipos.chatbot.dto.request.ReembolsoRequestDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AprobadorChatService {

    private final TramiteService tramiteService;
    private final NotificacionService notificacionService;
    private final HistorialEstadoService historialEstadoService;
    private final OrdenTransferenciaService ordenTransferenciaService;
    private final ReembolsoService reembolsoService;
    private final UsuarioService usuarioService;

    private static final int MAX_PROPOSITO = 30;

    // ============================================================
    // HELPER: URL del botón "Revisar Trámite"
    // ============================================================
    private String urlRevisarTramite(Long tramiteId) {
        return "https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId;
    }

    // ============================================================
    // PROCESAR MENSAJE
    // ============================================================
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion,
                           ConfiguracionAprobador config) {

        if (mensajeLower.startsWith("ver_")
                || mensajeLower.startsWith("revisar_")
                || mensajeLower.startsWith("pag_")
                || mensajeLower.startsWith("aprobar_")
                || mensajeLower.startsWith("rechazar_")) {
            return null;
        }

        if (mensajeLower.matches("autorizar\\s+reembolso\\s+\\d+")) {
            Long reembolsoId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return autorizarReembolso(reembolsoId, sesion);
        }

        if (sesion.getRolNombre() != null
                && sesion.getRolNombre().equalsIgnoreCase("Director Financiero")
                && (mensajeLower.equals("reembolsos") || mensajeLower.equals("reembolso"))) {
            return listarReembolsosPendientes();
        }

        if (mensajeLower.matches("aprobar\\s+orden\\s+\\d+")) {
            Long ordenId = Long.parseLong(mensajeLower.replaceAll("[^0-9]", ""));
            return aprobarOrden(ordenId, sesion);
        }

        if (sesion.getRolNombre() != null
                && sesion.getRolNombre().equalsIgnoreCase("Director Financiero")
                && (mensajeLower.equals("ordenes") || mensajeLower.contains("orden"))) {
            return listarOrdenesPendientes();
        }

        if (mensajeLower.matches("\\d+")
                && sesion.getTramiteEnRevision() == null
                && sesion.getContextoActual() != null) {

            Long numero = Long.parseLong(mensajeLower);

            if ("ORDENES".equals(sesion.getContextoActual())) {
                System.out.println(">>> Contexto ORDENES: mostrando detalles de la orden " + numero);
                return mostrarDetalleOrden(numero, sesion);
            }

            if ("REEMBOLSOS".equals(sesion.getContextoActual())) {
                System.out.println(">>> Contexto REEMBOLSOS: mostrando detalles del reembolso " + numero);
                return mostrarDetalleReembolso(numero, sesion);
            }
        }

        if ("ORDENES".equals(sesion.getContextoActual())
                && sesion.getTramiteEnRevision() != null
                && (mensajeLower.equals("aprobar") || mensajeLower.equals("1"))) {
            Long ordenId = sesion.getTramiteEnRevision();
            return aprobarOrden(ordenId, sesion);
        }

        if ("ORDENES".equals(sesion.getContextoActual())
                && sesion.getTramiteEnRevision() != null
                && (mensajeLower.equals("rechazar") || mensajeLower.equals("2"))) {
            Long ordenId = sesion.getTramiteEnRevision();
            return iniciarRechazoOrden(ordenId, sesion);
        }

        if ("REEMBOLSOS".equals(sesion.getContextoActual())
                && sesion.getTramiteEnRevision() != null
                && (mensajeLower.equals("autorizar") || mensajeLower.equals("1"))) {
            Long reembolsoId = sesion.getTramiteEnRevision();
            return autorizarReembolso(reembolsoId, sesion);
        }

        if ("REEMBOLSOS".equals(sesion.getContextoActual())
                && sesion.getTramiteEnRevision() != null
                && (mensajeLower.equals("rechazar") || mensajeLower.equals("2"))) {
            Long reembolsoId = sesion.getTramiteEnRevision();
            return iniciarRechazoReembolso(reembolsoId, sesion);
        }

        if (sesion.isEsperandoMotivoRechazo()) {
            return procesarMotivoRechazo(mensaje, sesion, config);
        }

        if (mensajeLower.matches("revisar\\s+\\d+")) {
            return seleccionarTramite(mensaje, sesion);
        }

        if (mensajeLower.matches("\\d+")) {
            return seleccionarTramitePorId(Long.parseLong(mensajeLower), sesion);
        }

        if (mensajeLower.contains("pendiente") || mensajeLower.contains("revisar")) {
            return listarPendientes(sesion, config);
        }

        if (mensajeLower.contains("aprobar")) {
            return aprobar(sesion, config);
        }

        if (mensajeLower.contains("rechazar")) {
            return iniciarRechazo(sesion);
        }

        if (sesion.getTramiteEnRevision() == null
                && !mensajeLower.isEmpty()
                && !mensajeLower.matches("\\d+")
                && !mensajeLower.contains("menu")
                && !mensajeLower.contains("menú")
                && !mensajeLower.contains("ayuda")
                && !mensajeLower.contains("cancelar")
                && !mensajeLower.contains("salir")
                && !mensajeLower.contains("pendiente")
                && !mensajeLower.contains("reembolso")
                && !mensajeLower.contains("orden")
                && !mensajeLower.contains("revisar")
                && !mensajeLower.contains("aprobar")
                && !mensajeLower.contains("rechazar")) {
            return "⚠️ Por favor, ingresa un número válido según la acción que estés realizando:\n\n" +
                    "• Para revisar un trámite: *el número del trámite*\n" +
                    "• Para autorizar un reembolso: *el número del reembolso*\n" +
                    "• Para aprobar una orden: *el número de la orden*";
        }

        return null;
    }

    // ============================================================
    // LISTAR TRÁMITES PENDIENTES
    // ============================================================
    private String listarPendientes(SesionUsuario sesion, ConfiguracionAprobador config) {
        List<TramiteResponseDTO> pendientes = tramiteService.listarPorEstado(config.getEstadoBuscado());

        if (pendientes.isEmpty()) {
            return "📋 No hay trámites pendientes de revisión.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Solicitudes de anticipo pendientes:* (")
                .append(pendientes.size()).append(")\n\n");

        for (TramiteResponseDTO t : pendientes) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getNombreUsuario());

            Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(t.getTramiteId());
            if (detalleOpt.isPresent() && detalleOpt.get().getProposito() != null) {
                String proposito = detalleOpt.get().getProposito();
                if (proposito.length() > MAX_PROPOSITO) {
                    proposito = proposito.substring(0, MAX_PROPOSITO) + "...";
                }
                lista.append(" · ").append(proposito);
            }

            lista.append("\n");
        }

        lista.append("\n👉 Escribe el *número del trámite* que deseas revisar.");
        lista.append("\nEjemplo: *15*.");
        return lista.toString();
    }

    // ============================================================
    // SELECCIONAR TRÁMITE
    // ============================================================
    private String seleccionarTramite(String mensaje, SesionUsuario sesion) {
        try {
            Long tramiteId = Long.parseLong(mensaje.replaceAll("[^0-9]", ""));
            return seleccionarTramitePorId(tramiteId, sesion);
        } catch (NumberFormatException e) {
            return "⚠️ Formato incorrecto.\n\n" +
                    "📌 Debes escribir *revisar* seguido del número.\n\n" +
                    "👉 Ejemplo: revisar 15";
        }
    }

    private String seleccionarTramitePorId(Long tramiteId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(tramiteId);

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(tramiteId);

        if (detalleOpt.isEmpty()) {
            return "⚠️ No encontré el trámite *" + tramiteId + "*.\n\n" +
                    "📌 Verifica que el ID sea correcto.";
        }

        TramiteDetalleDTO detalle = detalleOpt.get();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Trámite #").append(detalle.getTramiteId()).append("*\n\n");
        sb.append("👤 Solicitante: ").append(detalle.getNombreUsuario()).append("\n");
        sb.append("📝 Tipo: ").append(detalle.getNombreTipoTramite()).append("\n");
        sb.append("📅 Fecha: ").append(detalle.getFechaCreacion()).append("\n");
        sb.append("📊 Estado: ").append(detalle.getEstado()).append("\n");

        if (detalle.getPresupuesto() != null) {
            sb.append("\n💰 Presupuesto: $").append(detalle.getPresupuesto()).append("\n");
            if (detalle.getUnidad() != null) {
                sb.append("🏢 Unidad: ").append(detalle.getUnidad()).append("\n");
            }
            if (detalle.getCampus() != null) {
                sb.append("📍 Campus: ").append(detalle.getCampus()).append("\n");
            }
            if (detalle.getProposito() != null) {
                sb.append("🎯 Propósito: ").append(detalle.getProposito()).append("\n");
            }
            if (detalle.getLugar() != null) {
                sb.append("📌 Lugar: ").append(detalle.getLugar()).append("\n");
            }
            if (detalle.getFechaRealizacion() != null) {
                sb.append("📅 Fecha realización: ").append(detalle.getFechaRealizacion()).append("\n");
            }
        }

        if (detalle.getTotalGastado() != null) {
            sb.append("\n💰 Total gastado: $").append(detalle.getTotalGastado()).append("\n");
            if (detalle.getSaldo() != null) {
                sb.append("📈 Saldo: $").append(detalle.getSaldo()).append("\n");
            }
        }

        sb.append("\n¿Qué deseas hacer con este trámite?");
        sb.append("\n👉 Usa los botones de abajo para continuar.");

        return sb.toString();
    }

    // ============================================================
    // APROBAR TRÁMITE
    // ============================================================
    private String aprobar(SesionUsuario sesion, ConfiguracionAprobador config) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.\n\n" +
                    "📌 Primero debes elegir uno de la lista.\n\n" +
                    "👉 Escribe *revisar ID* o solo el número para seleccionarlo.";
        }

        Long tramiteId = sesion.getTramiteEnRevision();
        String estadoAnterior = config.getEstadoBuscado();
        String nuevoEstado = config.getNuevoEstado();

        tramiteService.actualizarEstado(tramiteId, nuevoEstado);

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo(nuevoEstado);
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Aprobado por " + config.getRolNombre());
        historialEstadoService.guardar(historialDTO);

        String correoSiguiente = usuarioService.obtenerCorreoPorRol(config.getNombreSiguienteRol());

        if (correoSiguiente == null) {
            System.err.println("⚠️ No se encontró un usuario con el rol: " + config.getNombreSiguienteRol());
        } else {
            String nombreSolicitante = tramiteService.obtenerDetalle(tramiteId)
                    .map(TramiteDetalleDTO::getNombreUsuario)
                    .orElse("N/A");

            String mensaje = MensajeHelper.tramiteAprobado(tramiteId, nombreSolicitante, nuevoEstado);

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoSiguiente);
            notifDTO.setTipo("Trámite aprobado por " + config.getRolNombre());
            notifDTO.setMensajePersonalizado(mensaje + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para abrirlo directamente.");
            // ✅ FIX: botón "Revisar Trámite"
            notifDTO.setUrlBotonTelegram(urlRevisarTramite(tramiteId));
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);

        return "✅ Trámite *" + tramiteId + "* aprobado correctamente.\n\n" +
                "📬 Se ha notificado al *" + config.getNombreSiguienteRol() + "*.";
    }

    // ============================================================
    // RECHAZAR TRÁMITE
    // ============================================================
    private String iniciarRechazo(SesionUsuario sesion) {
        if (sesion.getTramiteEnRevision() == null) {
            return "⚠️ No has seleccionado ningún trámite.\n\n" +
                    "📌 Primero debes elegir uno de la lista.\n\n" +
                    "👉 Escribe *revisar ID* o solo el número para seleccionarlo.";
        }
        sesion.setEsperandoMotivoRechazo(true);
        return "✍️ ¿Cuál es el motivo del rechazo?\n\n" +
                "👉 Escribe el motivo y se enviará al funcionario.";
    }

    private String procesarMotivoRechazo(String motivo, SesionUsuario sesion,
                                         ConfiguracionAprobador config) {
        sesion.setMotivoRechazo(motivo);
        sesion.setEsperandoMotivoRechazo(false);

        Long tramiteId = sesion.getTramiteEnRevision();

        if ("ORDENES".equals(sesion.getContextoActual())) {
            return rechazarOrden(tramiteId, motivo, sesion);
        }

        if ("REEMBOLSOS".equals(sesion.getContextoActual())) {
            return rechazarReembolso(tramiteId, motivo, sesion);
        }

        Optional<OrdenTransferenciaResponseDTO> ordenExistente = ordenTransferenciaService.buscarPorTramite(tramiteId);

        if (ordenExistente.isPresent()) {
            String estadoOrden = ordenExistente.get().getEstado();
            if ("Generada".equals(estadoOrden) || "Aprobada".equals(estadoOrden)) {
                sesion.setTramiteEnRevision(null);
                sesion.setMotivoRechazo(null);
                sesion.setEsperandoMotivoRechazo(false);

                return "🚨 *Acción denegada por seguridad*\n\n" +
                        "📌 El trámite *#" + tramiteId + "* ya tiene una orden de transferencia en estado *" + estadoOrden + "*.\n\n" +
                        "⚠️ *No se puede rechazar un trámite cuando el dinero ya está en proceso o fue transferido.*\n\n" +
                        "👉 Si hubo un error en la aprobación, contacta al administrador del sistema.";
            }
        }

        String estadoAnterior = config.getEstadoBuscado();

        tramiteService.actualizarEstado(tramiteId, "Rechazado");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramiteId);
        historialDTO.setEstadoAnterior(estadoAnterior);
        historialDTO.setEstadoNuevo("Rechazado");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Rechazado por " + config.getRolNombre() + ": " + motivo);
        historialEstadoService.guardar(historialDTO);

        String correoFuncionario = tramiteService.obtenerDetalle(tramiteId)
                .map(TramiteDetalleDTO::getCorreoUsuario)
                .orElse(null);

        if (correoFuncionario != null) {
            String mensaje = MensajeHelper.tramiteRechazado(tramiteId, config.getRolNombre(), motivo);

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramiteId);
            notifDTO.setDestinatario(correoFuncionario);
            notifDTO.setTipo("Trámite rechazado por " + config.getRolNombre());
            notifDTO.setMensajePersonalizado(mensaje + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para ver los detalles.");
            // ✅ FIX: botón "Revisar Trámite"
            notifDTO.setUrlBotonTelegram(urlRevisarTramite(tramiteId));
            notificacionService.guardar(notifDTO);
        }

        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ Trámite *" + tramiteId + "* rechazado.\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado al *Funcionario*.";
    }

    // ============================================================
    // ÓRDENES DE TRANSFERENCIA
    // ============================================================
    private String listarOrdenesPendientes() {
        List<OrdenTransferenciaResponseDTO> ordenes = ordenTransferenciaService.listarPorEstado("Generada");

        if (ordenes.isEmpty()) {
            return "📋 No hay órdenes de transferencia pendientes de aprobación.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Órdenes de transferencia pendientes:* (" + ordenes.size() + ")\n\n");
        for (OrdenTransferenciaResponseDTO o : ordenes) {
            lista.append("🆔 Orden #").append(o.getOrdenId())
                    .append(" — Trámite #").append(o.getTramiteId())
                    .append(" — $").append(o.getValor())
                    .append("\n");
        }
        lista.append("\n👉 Escribe el *número de la orden* que deseas aprobar.");
        lista.append("\nEjemplo: *3*.");
        return lista.toString();
    }

    private String aprobarOrden(Long ordenId, SesionUsuario sesion) {
        Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService.buscarPorId(ordenId);
        if (ordenOpt.isEmpty()) {
            return "⚠️ Orden no encontrada.\n\n" +
                    "📌 Por favor, ingresa el número de la orden que deseas aprobar.\n\n" +
                    "👉 Ejemplo: *3*";
        }

        OrdenTransferenciaResponseDTO orden = ordenOpt.get();
        if (!"Generada".equals(orden.getEstado())) {
            return "⚠️ La orden no está pendiente de aprobación.";
        }

        OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
        ordenDTO.setTramiteId(orden.getTramiteId());
        ordenDTO.setColectoraId(orden.getColectoraId());
        ordenDTO.setDirectorId(sesion.getUsuarioId());
        ordenDTO.setNumeroCuenta(orden.getNumeroCuenta());
        ordenDTO.setValor(orden.getValor());
        ordenDTO.setConcepto(orden.getConcepto());
        ordenDTO.setFechaGeneracion(orden.getFechaGeneracion());
        ordenDTO.setFechaAprobacion(LocalDate.now());
        ordenDTO.setEstado("Aprobada");

        ordenTransferenciaService.actualizar(ordenId, ordenDTO);

        tramiteService.actualizarEstado(orden.getTramiteId(), "Orden aprobada");

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(orden.getTramiteId());
        historialDTO.setEstadoAnterior("Orden generada");
        historialDTO.setEstadoNuevo("Orden aprobada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Orden de transferencia #" + ordenId + " aprobada por el Director Financiero");
        historialEstadoService.guardar(historialDTO);

        String mensaje = MensajeHelper.ordenAprobada(ordenId, orden.getTramiteId(), orden.getValor().toString());

        String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
        if (correoColectora != null) {
            NotificacionRequestDTO notifColectora = new NotificacionRequestDTO();
            notifColectora.setTramiteId(orden.getTramiteId());
            notifColectora.setDestinatario(correoColectora);
            notifColectora.setTipo("Orden de transferencia aprobada");
            notifColectora.setMensajePersonalizado(mensaje + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para ver los detalles.");
            // ✅ FIX: botón "Revisar Trámite"
            notifColectora.setUrlBotonTelegram(urlRevisarTramite(orden.getTramiteId()));
            notificacionService.guardar(notifColectora);
        }

        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
        if (correoContadora != null) {
            NotificacionRequestDTO notifContadora = new NotificacionRequestDTO();
            notifContadora.setTramiteId(orden.getTramiteId());
            notifContadora.setDestinatario(correoContadora);
            notifContadora.setTipo("Orden aprobada - Registrar comprobante");
            notifContadora.setMensajePersonalizado(mensaje + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* para registrar la transferencia.");
            // ✅ FIX: botón "Revisar Trámite"
            notifContadora.setUrlBotonTelegram(urlRevisarTramite(orden.getTramiteId()));
            notificacionService.guardar(notifContadora);
        }

        sesion.setContextoActual(null);
        sesion.setTramiteEnRevision(null);

        return "✅ Orden #" + ordenId + " aprobada correctamente.\n\n" +
                "📬 Se ha notificado a la *Colectora* y a la *Contadora*.\n\n" +
                "📌 La *Contadora* debe registrar el comprobante con:\n" +
                "*registrar comprobante " + orden.getTramiteId() + "*";
    }

    // ============================================================
    // REEMBOLSOS (solo Director Financiero)
    // ============================================================
    private String listarReembolsosPendientes() {
        List<ReembolsoResponseDTO> reembolsos = reembolsoService.listarPorEstado("Pendiente");

        if (reembolsos.isEmpty()) {
            return "📋 No hay reembolsos pendientes de autorización.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";
        }

        StringBuilder lista = new StringBuilder("📋 *Reembolsos pendientes de autorización:* (" + reembolsos.size() + ")\n\n");
        for (ReembolsoResponseDTO r : reembolsos) {
            lista.append("🆔 Reembolso #").append(r.getReembolsoId())
                    .append(" — Trámite #").append(r.getTramiteId())
                    .append(" — $").append(r.getMontoSolicitado())
                    .append("\n");
        }
        lista.append("\n👉 Escribe el *número del reembolso* que deseas autorizar.");
        lista.append("\nEjemplo: *1*.");
        return lista.toString();
    }

    private String autorizarReembolso(Long reembolsoId, SesionUsuario sesion) {
        try {
            Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorId(reembolsoId);

            if (reembolsoOpt.isEmpty()) {
                System.out.println("No existe reembolso con ID " + reembolsoId + ". Buscando por trámite...");
                reembolsoOpt = reembolsoService.buscarPorTramite(reembolsoId);
            }

            if (reembolsoOpt.isEmpty()) {
                return "⚠️ *Reembolso no encontrado.*\n\n" +
                        "📌 Por favor, ingresa el número del reembolso que deseas autorizar.\n\n" +
                        "👉 Ejemplo: *1*";
            }

            ReembolsoResponseDTO reembolso = reembolsoOpt.get();

            if (!"Pendiente".equals(reembolso.getEstado())) {
                return "⚠️ El reembolso #" + reembolso.getReembolsoId() +
                        " no está pendiente de autorización.\n\n" +
                        "📊 Estado actual: *" + reembolso.getEstado() + "*";
            }

            reembolso = reembolsoService.autorizar(
                    reembolso.getReembolsoId(), sesion.getUsuarioId(),
                    "Autorizado por el Director Financiero");

            Long tramiteId = reembolso.getTramiteId();

            String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
            if (correoContadora != null) {
                String mensaje = MensajeHelper.reembolsoAutorizado(
                        reembolso.getReembolsoId(), tramiteId,
                        reembolso.getMontoSolicitado().toString());

                NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                notifDTO.setTramiteId(tramiteId);
                notifDTO.setDestinatario(correoContadora);
                notifDTO.setTipo("Reembolso autorizado - Informa a Colectora");
                notifDTO.setMensajePersonalizado(mensaje + "\n\n👉 Toca el botón *[📄 Revisar Trámite]* e informa a la Colectora.");
                // ✅ FIX: botón "Revisar Trámite"
                notifDTO.setUrlBotonTelegram(urlRevisarTramite(tramiteId));
                notificacionService.guardar(notifDTO);
            }

            HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
            historialDTO.setTramiteId(tramiteId);
            historialDTO.setEstadoAnterior("Liquidación aprobada");
            historialDTO.setEstadoNuevo("Reembolso autorizado");
            historialDTO.setUsuarioId(sesion.getUsuarioId());
            historialDTO.setObservacion("Reembolso #" + reembolso.getReembolsoId() +
                    " autorizado por el Director Financiero");
            historialEstadoService.guardar(historialDTO);
            sesion.setContextoActual(null);

            return "✅ *Reembolso #" + reembolso.getReembolsoId() + " autorizado.*\n\n" +
                    "💰 Monto: $" + reembolso.getMontoSolicitado() + "\n" +
                    "🆔 Trámite: #" + tramiteId + "\n\n" +
                    "📬 Se ha notificado a la *Contadora* para que informe a la Colectora.";

        } catch (Exception e) {
            return "⚠️ No se pudo autorizar el reembolso.\n\n" +
                    "📌 Detalle: " + e.getMessage();
        }
    }

    // ============================================================
    // MOSTRAR DETALLE DE UNA ORDEN
    // ============================================================
    private String mostrarDetalleOrden(Long ordenId, SesionUsuario sesion) {
        Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService.buscarPorId(ordenId);

        if (ordenOpt.isEmpty()) {
            return "⚠️ *Orden no encontrada.*\n\n" +
                    "📌 Por favor, ingresa el número de la orden que deseas aprobar.\n\n" +
                    "👉 Ejemplo: *3*";
        }

        OrdenTransferenciaResponseDTO orden = ordenOpt.get();

        if (!"Generada".equals(orden.getEstado())) {
            return "⚠️ La orden #" + ordenId + " no está pendiente de aprobación.\n\n" +
                    "📊 Estado actual: *" + orden.getEstado() + "*";
        }

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(orden.getTramiteId());

        StringBuilder sb = new StringBuilder();
        sb.append("📄 *Orden de transferencia #").append(orden.getOrdenId()).append("*\n\n");
        sb.append("🆔 Trámite: #").append(orden.getTramiteId()).append("\n");
        sb.append("💰 Valor: $").append(orden.getValor()).append("\n");
        sb.append("📊 Estado: *").append(orden.getEstado()).append("*\n");
        sb.append("📝 Concepto: ").append(orden.getConcepto()).append("\n");

        if (detalleOpt.isPresent()) {
            TramiteDetalleDTO d = detalleOpt.get();
            sb.append("\n👤 *Solicitante:* ").append(d.getNombreUsuario()).append("\n");
            if (d.getProposito() != null) {
                sb.append("🎯 *Propósito:* ").append(d.getProposito()).append("\n");
            }
            if (d.getUnidad() != null) {
                sb.append("🏢 Unidad: ").append(d.getUnidad()).append("\n");
            }
            if (d.getCampus() != null) {
                sb.append("📍 Campus: ").append(d.getCampus()).append("\n");
            }
            if (d.getLugar() != null) {
                sb.append("📌 Lugar: ").append(d.getLugar()).append("\n");
            }
            if (d.getFechaRealizacion() != null) {
                sb.append("📅 Fecha realización: ").append(d.getFechaRealizacion()).append("\n");
            }
            if (d.getPresupuesto() != null) {
                sb.append("💰 Presupuesto original: $").append(d.getPresupuesto()).append("\n");
            }
        }

        sb.append("\n¿Qué deseas hacer con esta orden?\n\n");
        sb.append("👉 Escribe *aprobar* o *1* para aprobarla.\n");
        sb.append("👉 Escribe *rechazar* o *2* para rechazarla.");

        sesion.setTramiteEnRevision(ordenId);

        return sb.toString();
    }

    // ============================================================
    // RECHAZAR ORDEN
    // ============================================================
    private String iniciarRechazoOrden(Long ordenId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(ordenId);
        sesion.setEsperandoMotivoRechazo(true);

        return "✍️ ¿Cuál es el motivo del rechazo de la orden #" + ordenId + "?\n\n" +
                "👉 Escribe el motivo y se enviará a la Colectora.";
    }

    private String rechazarOrden(Long ordenId, String motivo, SesionUsuario sesion) {
        Optional<OrdenTransferenciaResponseDTO> ordenOpt = ordenTransferenciaService.buscarPorId(ordenId);

        if (ordenOpt.isEmpty()) {
            return "⚠️ No encontré la orden #" + ordenId + ".";
        }

        OrdenTransferenciaResponseDTO orden = ordenOpt.get();

        OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
        ordenDTO.setTramiteId(orden.getTramiteId());
        ordenDTO.setColectoraId(orden.getColectoraId());
        ordenDTO.setDirectorId(sesion.getUsuarioId());
        ordenDTO.setNumeroCuenta(orden.getNumeroCuenta());
        ordenDTO.setValor(orden.getValor());
        ordenDTO.setConcepto(orden.getConcepto());
        ordenDTO.setFechaGeneracion(orden.getFechaGeneracion());
        ordenDTO.setEstado("Rechazada");

        ordenTransferenciaService.actualizar(ordenId, ordenDTO);

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(orden.getTramiteId());
        historialDTO.setEstadoAnterior("Orden generada");
        historialDTO.setEstadoNuevo("Orden rechazada");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Orden #" + ordenId + " rechazada por el Director: " + motivo);
        historialEstadoService.guardar(historialDTO);

        String correoColectora = usuarioService.obtenerCorreoPorRol("Colectora");
        if (correoColectora != null) {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(orden.getTramiteId());
            notifDTO.setDestinatario(correoColectora);
            notifDTO.setTipo("Orden de transferencia rechazada");
            notifDTO.setMensajePersonalizado(
                    "❌ *Orden de transferencia rechazada*\n\n" +
                            "🆔 Orden: #" + ordenId + "\n" +
                            "📋 Trámite: #" + orden.getTramiteId() + "\n" +
                            "💰 Valor: $" + orden.getValor() + "\n\n" +
                            "📌 Motivo: " + motivo + "\n\n" +
                            "👉 Revisa y vuelve a generar la orden si es necesario.");
            // ✅ FIX: botón "Revisar Trámite"
            notifDTO.setUrlBotonTelegram(urlRevisarTramite(orden.getTramiteId()));
            notificacionService.guardar(notifDTO);
        }

        sesion.setContextoActual(null);
        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ *Orden #" + ordenId + " rechazada.*\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado a la *Colectora*.";
    }

    // ============================================================
    // MOSTRAR DETALLE DE UN REEMBOLSO
    // ============================================================
    private String mostrarDetalleReembolso(Long reembolsoId, SesionUsuario sesion) {
        Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorId(reembolsoId);

        if (reembolsoOpt.isEmpty()) {
            reembolsoOpt = reembolsoService.buscarPorTramite(reembolsoId);
        }

        if (reembolsoOpt.isEmpty()) {
            return "⚠️ *Reembolso no encontrado.*\n\n" +
                    "📌 Por favor, ingresa el número del reembolso.\n\n" +
                    "👉 Ejemplo: *1*";
        }

        ReembolsoResponseDTO reembolso = reembolsoOpt.get();

        if (!"Pendiente".equals(reembolso.getEstado())) {
            return "⚠️ El reembolso #" + reembolso.getReembolsoId() +
                    " no está pendiente de autorización.\n\n" +
                    "📊 Estado actual: *" + reembolso.getEstado() + "*";
        }

        Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(reembolso.getTramiteId());

        StringBuilder sb = new StringBuilder();
        sb.append("💰 *Reembolso #").append(reembolso.getReembolsoId()).append("*\n\n");
        sb.append("🆔 Trámite: #").append(reembolso.getTramiteId()).append("\n");
        sb.append("💵 Monto a reembolsar: $").append(reembolso.getMontoSolicitado()).append("\n");
        sb.append("📊 Estado: *").append(reembolso.getEstado()).append("*\n");

        if (detalleOpt.isPresent()) {
            TramiteDetalleDTO d = detalleOpt.get();
            sb.append("\n👤 *Solicitante:* ").append(d.getNombreUsuario()).append("\n");
            if (d.getProposito() != null) {
                sb.append("🎯 *Propósito:* ").append(d.getProposito()).append("\n");
            }
            if (d.getUnidad() != null) {
                sb.append("🏢 Unidad: ").append(d.getUnidad()).append("\n");
            }
            if (d.getPresupuesto() != null) {
                sb.append("💰 Anticipo original: $").append(d.getPresupuesto()).append("\n");
            }
            if (d.getTotalGastado() != null) {
                sb.append("💸 Total gastado: $").append(d.getTotalGastado()).append("\n");
            }
            if (d.getSaldo() != null) {
                sb.append("📈 Saldo: $").append(d.getSaldo()).append("\n");
            }
        }

        sb.append("\n¿Qué deseas hacer con este reembolso?\n\n");
        sb.append("👉 Escribe *autorizar* o *1* para autorizarlo.\n");
        sb.append("👉 Escribe *rechazar* o *2* para rechazarlo.");

        sesion.setTramiteEnRevision(reembolso.getReembolsoId());

        return sb.toString();
    }

    // ============================================================
    // RECHAZAR REEMBOLSO
    // ============================================================
    private String iniciarRechazoReembolso(Long reembolsoId, SesionUsuario sesion) {
        sesion.setTramiteEnRevision(reembolsoId);
        sesion.setEsperandoMotivoRechazo(true);

        return "✍️ ¿Cuál es el motivo del rechazo del reembolso #" + reembolsoId + "?\n\n" +
                "👉 Escribe el motivo y se enviará a la Contadora.";
    }

    private String rechazarReembolso(Long reembolsoId, String motivo, SesionUsuario sesion) {
        Optional<ReembolsoResponseDTO> reembolsoOpt = reembolsoService.buscarPorId(reembolsoId);

        if (reembolsoOpt.isEmpty()) {
            return "⚠️ No encontré el reembolso #" + reembolsoId + ".";
        }

        ReembolsoResponseDTO reembolso = reembolsoOpt.get();

        ReembolsoRequestDTO reembolsoDTO = new ReembolsoRequestDTO();
        reembolsoDTO.setLiquidacionId(reembolso.getLiquidacionId());
        reembolsoDTO.setMontoSolicitado(reembolso.getMontoSolicitado());
        reembolsoDTO.setEstado("Rechazado");
        reembolsoDTO.setObservacion("Rechazado por el Director: " + motivo);

        reembolsoService.actualizar(reembolsoId, reembolsoDTO);

        String correoContadora = usuarioService.obtenerCorreoPorRol("Contadora");
        if (correoContadora != null) {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(reembolso.getTramiteId());
            notifDTO.setDestinatario(correoContadora);
            notifDTO.setTipo("Reembolso rechazado");
            notifDTO.setMensajePersonalizado(
                    "❌ *Reembolso rechazado*\n\n" +
                            "🆔 Reembolso: #" + reembolsoId + "\n" +
                            "📋 Trámite: #" + reembolso.getTramiteId() + "\n" +
                            "💰 Monto: $" + reembolso.getMontoSolicitado() + "\n\n" +
                            "📌 Motivo: " + motivo);
            // ✅ FIX: botón "Revisar Trámite"
            notifDTO.setUrlBotonTelegram(urlRevisarTramite(reembolso.getTramiteId()));
            notificacionService.guardar(notifDTO);
        }

        sesion.setContextoActual(null);
        sesion.setTramiteEnRevision(null);
        sesion.setMotivoRechazo(null);

        return "❌ *Reembolso #" + reembolsoId + " rechazado.*\n\n" +
                "📌 Motivo: " + motivo + "\n\n" +
                "📬 Se ha notificado a la *Contadora*.";
    }
}