package ec.edu.iti.anticipos.chatbot.service.chat;

import ec.edu.iti.anticipos.chatbot.dto.ComprobanteExtraidoDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.FacturaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.HistorialEstadoRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.LiquidacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.NotificacionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.SolicitudRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.request.OrdenTransferenciaRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.*;
import ec.edu.iti.anticipos.chatbot.service.DocumentIntelligenceService;
import ec.edu.iti.anticipos.chatbot.service.FacturaService;
import ec.edu.iti.anticipos.chatbot.service.HistorialEstadoService;
import ec.edu.iti.anticipos.chatbot.service.LiquidacionService;
import ec.edu.iti.anticipos.chatbot.service.NotificacionService;
import ec.edu.iti.anticipos.chatbot.service.SolicitudService;
import ec.edu.iti.anticipos.chatbot.service.TramiteService;
import ec.edu.iti.anticipos.chatbot.service.UsuarioService;
import ec.edu.iti.anticipos.chatbot.service.parser.ComprobanteParser;
import ec.edu.iti.anticipos.chatbot.sesion.SesionUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ec.edu.iti.anticipos.chatbot.dto.request.DevolucionRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.DevolucionResponseDTO;
import ec.edu.iti.anticipos.chatbot.service.DevolucionService;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.service.OrdenTransferenciaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuncionarioChatService {

    private final TramiteService tramiteService;
    private final TramiteRepository tramiteRepository;
    private final SolicitudService solicitudService;
    private final HistorialEstadoService historialEstadoService;
    private final NotificacionService notificacionService;
    private final DocumentIntelligenceService documentIntelligenceService;
    private final LiquidacionService liquidacionService;
    private final FacturaService facturaService;
    private final DevolucionService devolucionService;
    private final UsuarioService usuarioService;
    private final ComprobanteParser comprobanteParser;
    private final OrdenTransferenciaService ordenTransferenciaService;
    private Long tramiteOriginalId;

    // ============================================================
    // HELPER: URL del botón "Revisar Trámite" (deep link de Telegram)
    // ============================================================
    private String urlRevisarTramite(Long tramiteId) {
        return "https://t.me/anticipos_iti_bot?start=tramite_" + tramiteId;
    }

    // ==================== MENSAJES ====================
    public String procesar(String mensaje, String mensajeLower, SesionUsuario sesion) {

        System.out.println("=== FUNCIONARIO PROCESAR ===");
        System.out.println("Mensaje: [" + mensaje + "]");
        System.out.println("AccionPendiente: [" + sesion.getAccionPendiente() + "]");

        if (mensajeLower.equals("cancelar") || mensajeLower.equals("no")) {
            if ("GENERAR_ORDEN".equals(sesion.getAccionPendiente())
                    || "GENERAR_ORDEN_REEMBOLSO".equals(sesion.getAccionPendiente())) {
                sesion.setAccionPendiente(null);
                sesion.setTramiteEnRevision(null);
                return "✅ Operación cancelada.\n\n¿Qué necesitas hacer hoy?";
            }
        }

        if (sesion.getFormularioSolicitud() != null) {
            if (mensajeLower.equals("si") || mensajeLower.equals("sí") || mensajeLower.equals("1") || mensajeLower.equals("confirmo")) {
                return confirmarSolicitud(sesion);
            }
            if (mensajeLower.equals("no") || mensajeLower.equals("2")) {
                sesion.setFormularioSolicitud(null);
                sesion.setAccionPendiente(null);
                return "✅ Solicitud descartada.\n\n¿Qué necesitas hacer hoy?";
            }
            return null;
        }

        if (sesion.getFormularioLiquidacion() != null && !sesion.isSubiendoFacturas()) {
            if (mensajeLower.equals("si") || mensajeLower.equals("sí") || mensajeLower.equals("1") || mensajeLower.equals("confirmo")) {
                return confirmarLiquidacion(sesion);
            }
            if (mensajeLower.equals("no") || mensajeLower.equals("2")) {
                sesion.setSubiendoFacturas(true);
                sesion.setAccionPendiente("LIQUIDAR");
                return "↩️ *Modo corrección activado.*\n\n" +
                        "Puedes seguir subiendo más facturas o usar el botón *Corregir* para borrar la última si te equivocaste.\n\n" +
                        "📎 Sube otra factura cuando estés listo.";
            }
            return null;
        }

        if (sesion.isSubiendoFacturas() && sesion.getFormularioLiquidacion() != null) {
            if (mensajeLower.equals("si") || mensajeLower.equals("sí") || mensajeLower.equals("1") || mensajeLower.equals("confirmo")) {
                return confirmarLiquidacion(sesion);
            }
            if (mensajeLower.equals("no") || mensajeLower.equals("2")) {
                sesion.setSubiendoFacturas(true);
                sesion.setAccionPendiente("LIQUIDAR");
                return "↩️ *Modo corrección activado.*\n\n" +
                        "Puedes seguir subiendo más facturas o usar el botón *Corregir* para borrar la última si te equivocaste.\n\n" +
                        "📎 Sube otra factura cuando estés listo.";
            }
            if (mensajeLower.contains("listo")) {
                if (sesion.getFacturasExtraidas() == null || sesion.getFacturasExtraidas().isEmpty()) {
                    return "⚠️ *No has subido ninguna factura.*\n\n" +
                            "📌 Para continuar necesitas subir al menos 1 factura en PDF.\n\n" +
                            "👉 Adjúntala ahora.";
                }
                return calcularYMostrarSaldo(sesion);
            }
            return null;
        }

        if (mensajeLower.contains("solicitar") || mensajeLower.contains("pedir")
                || mensajeLower.contains("anticipo") || mensajeLower.contains("fondos")
                || mensajeLower.contains("plata") || mensajeLower.contains("dinero")
                || mensajeLower.contains("adelanto") || mensajeLower.contains("nueva solicitud")
                || mensajeLower.equals("1")) {

            if (tieneLiquidacionesPendientes(sesion.getUsuarioId())) {
                return "⚠️ *No puedes solicitar un nuevo anticipo*\n\n" +
                        "📌 Tienes una liquidación pendiente de completar.\n\n" +
                        "👉 Debes terminar el proceso de liquidación actual antes de solicitar un nuevo anticipo.\n\n" +
                        "📋 Escribe *consultar* para ver tus trámites pendientes.";
            }

            sesion.setAccionPendiente("SOLICITAR");
            sesion.setSubiendoFacturas(false);
            return "📎 Perfecto. Ahora adjunta el formulario de solicitud de anticipo en formato PDF, ya llenado y firmado.";
        }

        if (mensajeLower.contains("liquidar") || mensajeLower.contains("rendir")
                || mensajeLower.contains("justificar") || mensajeLower.contains("cuadrar")
                || mensajeLower.equals("2")) {
            sesion.setAccionPendiente("LIQUIDAR");
            sesion.setSubiendoFacturas(false);
            return "📎 Perfecto. Ahora adjunta el formulario de liquidación en formato PDF.";
        }

        if (mensajeLower.contains("estado") || mensajeLower.contains("seguimiento")
                || mensajeLower.contains("consultar") || mensajeLower.contains("tramite")
                || mensajeLower.contains("trámite") || mensajeLower.equals("3")) {
            return consultarEstado(sesion);
        }

        if (mensajeLower.contains("ayuda") || mensajeLower.contains("opciones")
                || mensajeLower.contains("puedo hacer") || mensajeLower.equals("4")) {
            return "ℹ️ Puedo ayudarte con:\n\n" +
                    "1. Solicitar un anticipo\n" +
                    "2. Liquidar un anticipo\n" +
                    "3. Consultar el estado de tus trámites\n" +
                    "4. Resolver dudas generales";
        }

        if (mensajeLower.contains("listo") && sesion.isSubiendoFacturas()) {
            if (sesion.getFacturasExtraidas() == null || sesion.getFacturasExtraidas().isEmpty()) {
                return "⚠️ *No has subido ninguna factura.*\n\n" +
                        "📌 Para continuar necesitas subir al menos 1 factura en PDF.\n\n" +
                        "👉 Adjúntala ahora.";
            }
            return calcularYMostrarSaldo(sesion);
        }

        if (mensajeLower.contains("devolucion") || mensajeLower.contains("devolución")) {
            sesion.setAccionPendiente("DEVOLUCION");
            return "📎 Perfecto. Adjunta el comprobante de depósito de la devolución en formato PDF.";
        }

        return null;
    }

    // ==================== DOCUMENTOS ====================
    public String procesarDocumento(MultipartFile archivo, SesionUsuario sesion) {
        try {

            String nombreArchivo = archivo.getOriginalFilename();
            String tipoContenido = archivo.getContentType();

            // ✅ Colectora sube PDF de la orden de ANTICIPO
            if ("GENERAR_ORDEN".equals(sesion.getAccionPendiente())) {
                if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
                    return "⚠️ El respaldo de la orden debe ser un PDF.";
                }

                tramiteService.actualizarEstado(sesion.getTramiteEnRevision(), "Orden generada");

                Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(sesion.getTramiteEnRevision());
                String nombreSolicitante = detalleOpt.map(d -> d.getNombreUsuario()).orElse("N/A");

                String valor = "0.00";
                if (detalleOpt.isPresent() && detalleOpt.get().getPresupuesto() != null) {
                    valor = detalleOpt.get().getPresupuesto().toString();
                } else {
                    Optional<SolicitudResponseDTO> solOpt = solicitudService.buscarPorTramiteId(sesion.getTramiteEnRevision());
                    if (solOpt.isPresent() && solOpt.get().getPresupuesto() != null) {
                        valor = solOpt.get().getPresupuesto().toString();
                    }
                }

                OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
                ordenDTO.setTramiteId(sesion.getTramiteEnRevision());
                ordenDTO.setColectoraId(sesion.getUsuarioId());
                ordenDTO.setNumeroCuenta("1234567890");
                ordenDTO.setValor(new BigDecimal(valor));
                ordenDTO.setConcepto("Anticipo para: " + (detalleOpt.isPresent() && detalleOpt.get().getProposito() != null ? detalleOpt.get().getProposito() : "Anticipo"));
                ordenDTO.setFechaGeneracion(LocalDate.now());
                ordenDTO.setEstado("Generada");
                ordenTransferenciaService.guardar(ordenDTO);

                String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");
                if (correoDirector != null) {
                    String mensajeNotificacion = "🔔 *Nueva orden de transferencia pendiente de aprobación*\n\n" +
                            "🆔 Trámite: #" + sesion.getTramiteEnRevision() + "\n" +
                            "👤 Solicitante: " + nombreSolicitante + "\n" +
                            "💰 Valor: $" + valor + "\n" +
                            "📎 Respaldo: " + nombreArchivo + "\n\n" +
                            "👉 Toca el botón *[📄 Revisar Trámite]* para aprobar la orden.";

                    NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                    notifDTO.setTramiteId(sesion.getTramiteEnRevision());
                    notifDTO.setDestinatario(correoDirector);
                    notifDTO.setTipo("Orden de transferencia pendiente");
                    notifDTO.setMensajePersonalizado(mensajeNotificacion);
                    notifDTO.setUrlBotonTelegram(urlRevisarTramite(sesion.getTramiteEnRevision()));
                    notificacionService.guardar(notifDTO);
                }

                sesion.setAccionPendiente(null);
                sesion.setTramiteEnRevision(null);

                return "✅ *Orden de transferencia generada correctamente.*\n\n" +
                        "🆔 Trámite: #" + detalleOpt.map(TramiteDetalleDTO::getTramiteId).orElse(0L) + "\n" +
                        "💰 Valor: $" + valor + "\n" +
                        "📊 Estado: *Orden generada*\n\n" +
                        "📬 Se ha notificado al *Director Financiero* para que apruebe la orden.";
            }

            // ✅ Colectora sube PDF para orden de REEMBOLSO
            if ("GENERAR_ORDEN_REEMBOLSO".equals(sesion.getAccionPendiente())) {
                if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
                    return "⚠️ El respaldo debe ser un PDF.";
                }

                tramiteService.actualizarEstado(sesion.getTramiteEnRevision(), "Orden de reembolso generada");

                Optional<TramiteDetalleDTO> detalleOpt = tramiteService.obtenerDetalle(sesion.getTramiteEnRevision());
                String nombreSolicitante = detalleOpt.map(d -> d.getNombreUsuario()).orElse("N/A");
                String valor = "0.00";
                if (detalleOpt.isPresent() && detalleOpt.get().getSaldo() != null) {
                    valor = detalleOpt.get().getSaldo().abs().toString();
                }

                OrdenTransferenciaRequestDTO ordenDTO = new OrdenTransferenciaRequestDTO();
                ordenDTO.setTramiteId(sesion.getTramiteEnRevision());
                ordenDTO.setColectoraId(sesion.getUsuarioId());
                ordenDTO.setNumeroCuenta("1234567890");
                ordenDTO.setValor(new BigDecimal(valor));
                ordenDTO.setConcepto("Reembolso al funcionario - Trámite #" + sesion.getTramiteEnRevision());
                ordenDTO.setFechaGeneracion(LocalDate.now());
                ordenDTO.setEstado("Generada");
                ordenTransferenciaService.guardar(ordenDTO);

                String correoDirector = usuarioService.obtenerCorreoPorRol("Director Financiero");
                if (correoDirector != null) {
                    NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
                    notifDTO.setTramiteId(sesion.getTramiteEnRevision());
                    notifDTO.setDestinatario(correoDirector);
                    notifDTO.setTipo("Orden de reembolso pendiente");
                    notifDTO.setMensajePersonalizado("🔔 *Nueva orden de reembolso pendiente de aprobación*\n\n" +
                            "🆔 Trámite: #" + sesion.getTramiteEnRevision() + "\n" +
                            "👤 Solicitante: " + nombreSolicitante + "\n" +
                            "💰 Valor: $" + valor + "\n" +
                            "📎 Respaldo: " + nombreArchivo + "\n\n" +
                            "👉 Toca el botón *[📄 Revisar Trámite]* para aprobar la orden.");
                    notifDTO.setUrlBotonTelegram(urlRevisarTramite(sesion.getTramiteEnRevision()));
                    notificacionService.guardar(notifDTO);
                }

                sesion.setAccionPendiente(null);
                sesion.setTramiteEnRevision(null);

                return "✅ *Orden de reembolso generada correctamente.*\n\n" +
                        "🆔 Trámite: #" + detalleOpt.map(TramiteDetalleDTO::getTramiteId).orElse(0L) + "\n" +
                        "💰 Valor: $" + valor + "\n" +
                        "📊 Estado: *Orden de reembolso generada*\n\n" +
                        "📬 Se ha notificado al *Director Financiero* para que apruebe la orden.";
            }

            if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
                return "⚠️ *Formato incorrecto.*\n\n" +
                        "📌 Por favor, sube un archivo en formato PDF (.pdf).";
            }

            if (tipoContenido == null || !tipoContenido.contains("pdf")) {
                return "⚠️ *El archivo no parece ser un PDF válido.*\n\n" +
                        "📌 Por favor, sube un archivo en formato PDF (.pdf).";
            }

            byte[] bytes = archivo.getBytes();

            if (sesion.isEsperandoComprobanteDevolucion()) {
                return procesarComprobanteDevolucion(archivo, sesion);
            }

            if ("SOLICITAR".equals(sesion.getAccionPendiente())) {
                FormularioSolicitudDTO form = documentIntelligenceService.extraerFormularioSolicitud(bytes);
                if (!form.isExtraccionExitosa()) {
                    return "⚠️ No pude leer el formulario.\n\n" +
                            "📌 El archivo puede estar dañado o protegido.\n\n" +
                            "👉 Verifica que sea el PDF correcto y vuelve a enviarlo.";
                }

                if (form.getSolicitante() == null && form.getPresupuesto() == null) {
                    return "⚠️ El PDF no corresponde al formulario de solicitud de anticipo.\n\n" +
                            "📌 En este paso necesito el formato *FORMULARIO DE ANTICIPO DE FONDOS*.\n\n" +
                            "👉 Verifica el documento y vuelve a adjuntarlo.";
                }

                String errorSolicitante = validarSolicitante(form.getSolicitante(), form.getCi(), sesion);
                if (errorSolicitante != null) {
                    return errorSolicitante;
                }

                String errorBasico = validarCamposBasicosSolicitud(form);
                if (errorBasico != null) {
                    return errorBasico;
                }

                sesion.setFormularioSolicitud(form);

                StringBuilder resumen = new StringBuilder();
                resumen.append("✅ He procesado el formulario.\n\n");
                resumen.append("Estos son los datos encontrados:\n\n");
                resumen.append("👤 Solicitante: ").append(form.getSolicitante()).append("\n");
                resumen.append("🪪 Cédula: ").append(form.getCi() != null ? form.getCi() : "No detectada").append("\n");
                resumen.append("🏢 Unidad: ").append(form.getUnidad()).append("\n");
                resumen.append("📍 Campus: ").append(form.getCampus() != null ? form.getCampus() : "No detectado").append("\n");
                resumen.append("📅 Fecha: ").append(form.getFecha() != null ? form.getFecha() : "No detectada").append("\n");
                resumen.append("🎯 Propósito: ").append(form.getProposito()).append("\n");
                resumen.append("📍 Lugar: ").append(form.getLugar() != null ? form.getLugar() : "No detectado").append("\n");
                resumen.append("💰 Presupuesto: $").append(form.getPresupuesto()).append("\n");
                resumen.append("\n¿Los datos son correctos?");

                return resumen.toString();
            }

            if ("LIQUIDAR".equals(sesion.getAccionPendiente()) && !sesion.isSubiendoFacturas()) {
                FormularioLiquidacionDTO form = documentIntelligenceService.extraerFormularioLiquidacion(bytes);
                if (!form.isExtraccionExitosa()) {
                    return "⚠️ No pude leer el formulario de liquidación.\n\n" +
                            "📌 El archivo puede estar dañado o protegido.\n\n" +
                            "👉 Verifica el archivo y vuelve a enviarlo.";
                }

                if (form.getSolicitante() == null && form.getTotalGastos() == null) {
                    return "⚠️ El PDF no corresponde al formulario de liquidación.\n\n" +
                            "📌 En este paso necesito el formato *FORMATO DE LIQUIDACIÓN DE FONDOS*.\n\n" +
                            "👉 Verifica el documento y vuelve a adjuntarlo.";
                }

                String errorSolicitante = validarSolicitante(form.getSolicitante(), form.getCi(), sesion);
                if (errorSolicitante != null) {
                    return errorSolicitante;
                }

                String errorValidacion = validarFormularioLiquidacion(form, sesion);
                if (errorValidacion != null) {
                    return errorValidacion;
                }

                Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarPorTramiteId(sesion.getTramiteEnRevision());
                if (solicitudOpt.isPresent()) {
                    form.setAnticipo(solicitudOpt.get().getPresupuesto());
                }

                sesion.setFormularioLiquidacion(form);
                sesion.setSubiendoFacturas(true);

                return "✅ Formulario de liquidación registrado.\n\n" +
                        "👤 Solicitante: " + form.getSolicitante() + "\n" +
                        "💰 Anticipo: $" + (form.getAnticipo() != null ? form.getAnticipo() : "0.00") + "\n" +
                        "📊 Total gastos: $" + form.getTotalGastos() + "\n\n" +
                        "📎 *Sube tus facturas para liquidar el anticipo.*\n\n" +
                        "⚠️ Solo se aceptan archivos en formato PDF.\n" +
                        "✍️ Cuando termines, escribe *listo* o usa el botón para calcular el saldo.";
            }

            if ("LIQUIDAR".equals(sesion.getAccionPendiente()) && sesion.isSubiendoFacturas()) {
                FacturaExtraidaDTO factura = documentIntelligenceService.extraerDatos(bytes);

                if (!factura.isExtraccionExitosa()) {
                    return "⚠️ No pude leer la factura.\n\n" +
                            "📌 El archivo puede estar dañado o protegido.\n\n" +
                            "👉 Verifica que sea una factura válida y vuelve a enviarla.";
                }

                if (factura.getRuc() == null && factura.getMonto() == null) {
                    return "⚠️ El PDF no corresponde a una factura válida.\n\n" +
                            "📌 En este paso necesito una factura con RUC y monto.\n\n" +
                            "👉 Verifica el archivo y vuelve a enviarlo.";
                }

                String errorFactura = validarFactura(factura);
                if (errorFactura != null) {
                    return errorFactura;
                }

                FormularioLiquidacionDTO.LineaFactura linea = new FormularioLiquidacionDTO.LineaFactura();
                linea.setNumeroFactura(factura.getNumeroFactura());
                linea.setProveedor(factura.getProveedor());
                linea.setValor(factura.getMonto());
                linea.setFecha(factura.getFecha());

                sesion.getFacturasExtraidas().add(linea);

                return "✅ *Factura registrada:*\n\n" +
                        "🏢 Proveedor: " + factura.getProveedor() + "\n" +
                        "🔢 RUC Proveedor: " + (factura.getRuc() != null ? factura.getRuc() : "No detectado") + "\n" +
                        "📄 N° Factura: " + (factura.getNumeroFactura() != null ? factura.getNumeroFactura() : "No detectado") + "\n" +
                        "📅 Fecha: " + (factura.getFecha() != null ? factura.getFecha() : "No detectada") + "\n" +
                        "💰 Monto: $" + factura.getMonto() + "\n" +
                        "📊 Total facturas acumuladas: " + sesion.getFacturasExtraidas().size() + "\n\n" +
                        "📎 Sube otra factura o toca *Terminar y calcular saldo*.";
            }

            if ("DEVOLUCION".equals(sesion.getAccionPendiente())) {
                Optional<DevolucionResponseDTO> devolucionOpt = devolucionService.buscarPendientePorUsuario(sesion.getUsuarioId());

                if (devolucionOpt.isEmpty()) {
                    return "⚠️ No tienes devoluciones pendientes.\n\n" +
                            "📌 No hay ninguna liquidación aprobada con saldo por devolver.\n\n" +
                            "👉 Escribe *menu* para volver al menú principal.";
                }

                DevolucionResponseDTO devolucion = devolucionOpt.get();

                DevolucionRequestDTO devolucionDTO = new DevolucionRequestDTO();
                devolucionDTO.setLiquidacionId(devolucion.getLiquidacionId());
                devolucionDTO.setMonto(devolucion.getMonto());
                devolucionDTO.setFechaDevolucion(LocalDate.now());
                devolucionDTO.setComprobantePdf(archivo.getOriginalFilename());

                devolucionService.actualizar(devolucion.getDevolucionId(), devolucionDTO);

                tramiteService.actualizarEstado(devolucion.getTramiteId(), "Liquidación cerrada");

                HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
                historialDTO.setTramiteId(devolucion.getTramiteId());
                historialDTO.setEstadoAnterior("Liquidación aprobada");
                historialDTO.setEstadoNuevo("Liquidación cerrada");
                historialDTO.setUsuarioId(sesion.getUsuarioId());
                historialDTO.setObservacion("Devolución registrada por el funcionario");
                historialEstadoService.guardar(historialDTO);

                sesion.setAccionPendiente(null);
                return "✅ Devolución registrada correctamente.\n\n" +
                        "🎉 La liquidación ha sido cerrada.\n\n" +
                        "Gracias por usar el sistema.";
            }

            return "⚠️ Acción no reconocida.\n\n" +
                    "📌 No sé qué hacer con este documento.\n\n" +
                    "👉 Escribe *menu* para volver al menú principal.";

        } catch (Exception e) {
            return "⚠️ Ocurrió un error al procesar el documento.\n\n" +
                    "📌 Detalle técnico: " + e.getMessage() + "\n\n" +
                    "👉 Intenta nuevamente o escribe *menu* para volver.";
        }
    }

    // ==================== PROCESAR COMPROBANTE DE DEVOLUCIÓN ====================
    private String procesarComprobanteDevolucion(MultipartFile archivo, SesionUsuario sesion) {
        String nombreArchivo = archivo.getOriginalFilename();

        try {
            if (nombreArchivo == null || !nombreArchivo.toLowerCase().endsWith(".pdf")) {
                return "⚠️ El comprobante debe ser un PDF.\n\n" +
                        "📌 En este paso necesito el comprobante de depósito.\n\n" +
                        "👉 Adjúntalo nuevamente con la extensión .pdf";
            }

            String tipoContenido = archivo.getContentType();
            if (tipoContenido == null || !tipoContenido.contains("pdf")) {
                return "⚠️ El archivo no parece ser un PDF válido.\n\n" +
                        "📌 En este paso necesito el comprobante de depósito.\n\n" +
                        "👉 Verifica el archivo y vuelve a enviarlo.";
            }

            System.out.println(">>> ✅ Comprobante recibido: " + nombreArchivo);
            System.out.println(">>> Monto pendiente: $" + sesion.getMontoDevolucionPendiente());

            byte[] bytes = archivo.getBytes();
            String texto = documentIntelligenceService.extraerTextoPlano(bytes);

            BigDecimal saldoPendiente = sesion.getMontoDevolucionPendiente();
            ComprobanteExtraidoDTO extraido = comprobanteParser.parsear(texto, saldoPendiente);

            if (!extraido.isEsDelIti()) {
                return "❌ *El comprobante no corresponde al ITI.*\n\n" +
                        "📌 No se detectó el RUC 1792146836001 ni el nombre del Instituto.\n\n" +
                        "👉 Sube el comprobante digital correcto (PDF de la banca en línea).";
            }

            if (extraido.isMontoEncontrado() && !extraido.isMontoCoincide()) {
                return String.format(
                        "⚠️ *El monto del comprobante no coincide.*\n\n" +
                                "📌 Comprobante: $%.2f\n" +
                                "📌 Saldo pendiente: $%.2f\n\n" +
                                "👉 Verifica que hayas subido el comprobante correcto.",
                        extraido.getMontoDepositado(), saldoPendiente);
            }

            return crearLiquidacionYCerrar(sesion, nombreArchivo, extraido);

        } catch (Exception e) {
            System.err.println("Error al procesar comprobante con Azure DI: " + e.getMessage());
            System.out.println(">>> ⚠️ Azure DI no pudo leer el comprobante. Se acepta para validación manual.");
            return crearLiquidacionYCerrar(sesion, nombreArchivo, null);
        }
    }

    // ==================== VALIDACIONES ====================
    private String validarSolicitante(String solicitanteForm, String ciForm, SesionUsuario sesion) {
        if (solicitanteForm == null || solicitanteForm.trim().isEmpty()) {
            return "⚠️ *No se pudo leer el nombre del solicitante en el formulario.*\n\n" +
                    "📌 El PDF puede estar ilegible o tener un formato incorrecto.\n\n" +
                    "👉 Verifica el documento y vuelve a enviarlo.";
        }

        if (solicitanteForm.trim().length() < 5) {
            return "⚠️ *El nombre del solicitante es demasiado corto.*\n\n" +
                    "📌 El formulario dice: *" + solicitanteForm + "*\n\n" +
                    "👉 Verifica que sea el formato correcto.";
        }

        String solicitanteNorm = normalizar(solicitanteForm);
        String nombreSesionNorm = normalizar(sesion.getNombre());

        boolean esMismoUsuario = solicitanteNorm.equals(nombreSesionNorm) || solicitanteNorm.contains(nombreSesionNorm);

        if (!esMismoUsuario) {
            String[] palabrasSesion = nombreSesionNorm.split(" ");
            int coincidencias = 0;
            for (String palabra : palabrasSesion) {
                if (palabra.length() > 2 && solicitanteNorm.contains(palabra)) {
                    coincidencias++;
                }
            }
            if (coincidencias >= 2) {
                esMismoUsuario = true;
            }
        }

        if (!esMismoUsuario) {
            return "⚠️ *El formulario no está a tu nombre.*\n\n" +
                    "📌 El formulario dice: *" + solicitanteForm + "*\n" +
                    "📌 Tu usuario es: *" + sesion.getNombre() + "*\n\n" +
                    "👉 Por seguridad, solo puedes subir formularios a tu nombre.\n" +
                    "👉 Solicita al funcionario correcto que suba el formulario.";
        }

        System.out.println(">>> ✅ Validación de solicitante OK: " + solicitanteForm);
        return null;
    }

    private String validarCamposBasicosSolicitud(FormularioSolicitudDTO form) {
        if (form.getProposito() == null || form.getProposito().trim().length() < 5) {
            return "⚠️ *El propósito del anticipo es demasiado corto o está vacío.*\n\n" +
                    "📌 El formulario debe incluir un propósito claro de al menos 5 caracteres.\n\n" +
                    "👉 Verifica el documento y vuelve a enviarlo.";
        }
        if (form.getPresupuesto() == null || form.getPresupuesto().compareTo(BigDecimal.ZERO) <= 0) {
            return "⚠️ *El presupuesto del anticipo debe ser mayor a cero.*\n\n" +
                    "📌 El formulario debe incluir un monto válido.\n\n" +
                    "👉 Verifica el documento y vuelve a enviarlo.";
        }
        return null;
    }

    private String validarFactura(FacturaExtraidaDTO factura) {
        if (factura.getMonto() == null || factura.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return "⚠️ *El monto de la factura debe ser mayor a cero.*\n\n" +
                    "📌 Verifica que sea una factura válida.\n\n" +
                    "👉 Vuelve a subir el archivo.";
        }
        return null;
    }

    private String validarFormularioLiquidacion(FormularioLiquidacionDTO form, SesionUsuario sesion) {
        Long tramiteId = sesion.getTramiteEnRevision();
        if (tramiteId == null) {
            return "⚠️ No has seleccionado ningún trámite para liquidar.";
        }

        Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarPorTramiteId(tramiteId);
        if (solicitudOpt.isEmpty()) {
            return "⚠️ No encontré la solicitud de anticipo para el trámite #" + tramiteId;
        }

        return null;
    }

    private String obtenerCedulaUsuario(Long usuarioId) {
        try {
            return usuarioService.buscarPorId(usuarioId)
                    .map(dto -> dto.getCedula())
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("Error al obtener cédula del usuario " + usuarioId + ": " + e.getMessage());
            return null;
        }
    }

    // ==================== CÁLCULO DE SALDO ====================
    private String calcularYMostrarSaldo(SesionUsuario sesion) {
        FormularioLiquidacionDTO form = sesion.getFormularioLiquidacion();

        Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarPorTramiteId(sesion.getTramiteEnRevision());
        BigDecimal anticipo = solicitudOpt.map(SolicitudResponseDTO::getPresupuesto)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalFacturas = BigDecimal.ZERO;
        for (FormularioLiquidacionDTO.LineaFactura f : sesion.getFacturasExtraidas()) {
            if (f.getValor() != null) {
                totalFacturas = totalFacturas.add(f.getValor());
            }
        }

        BigDecimal saldo = anticipo.subtract(totalFacturas);

        if (form.getProposito() != null && form.getProposito().toLowerCase().contains("cumpleaños")) {
            if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                saldo = BigDecimal.ZERO;
            }
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("📊 *Resumen de la liquidación*\n\n");
        resumen.append("💰 Anticipo: $").append(anticipo).append("\n");
        resumen.append("📉 Total gastado: $").append(totalFacturas).append("\n");
        resumen.append("📈 Saldo: $").append(saldo).append("\n\n");

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            resumen.append("⚠️ *Caso SOBRANTE*\n");
            resumen.append("Debes devolver $").append(saldo).append(" al ITI.\n");
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            resumen.append("ℹ️ *Caso FALTANTE (REEMBOLSO)*\n");
            resumen.append("El ITI te debe devolver $").append(saldo.abs()).append(".\n");
        } else {
            resumen.append("✅ *Caso EXACTO*\n");
            resumen.append("No hay devolución ni reembolso pendiente.\n");
        }

        resumen.append("\n¿Confirmas la liquidación?");
        return resumen.toString();
    }

    // ==================== CONFIRMACIONES ====================
    private String confirmarSolicitud(SesionUsuario sesion) {
        FormularioSolicitudDTO form = sesion.getFormularioSolicitud();

        TramiteRequestDTO tramiteDTO = new TramiteRequestDTO();
        tramiteDTO.setUsuarioId(sesion.getUsuarioId());
        tramiteDTO.setTipoTramiteId(1L);
        tramiteDTO.setEstado("Pendiente");
        TramiteResponseDTO tramite = tramiteService.guardar(tramiteDTO);

        SolicitudRequestDTO solicitudDTO = new SolicitudRequestDTO();
        solicitudDTO.setTramiteId(tramite.getTramiteId());
        solicitudDTO.setUnidad(form.getUnidad());
        solicitudDTO.setCampus(form.getCampus());
        solicitudDTO.setProposito(form.getProposito());
        solicitudDTO.setLugar(form.getLugar());
        solicitudDTO.setFechaRealizacion(form.getFechaRealizacion());
        solicitudDTO.setPresupuesto(form.getPresupuesto());
        solicitudService.guardar(solicitudDTO);

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramite.getTramiteId());
        historialDTO.setEstadoAnterior(null);
        historialDTO.setEstadoNuevo("Pendiente");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Solicitud creada por el funcionario");
        historialEstadoService.guardar(historialDTO);

        String correoJefe = usuarioService.obtenerCorreoJefeInmediato(sesion.getUsuarioId());
        if (correoJefe == null) {
            System.err.println("⚠️ El funcionario " + sesion.getNombre() + " no tiene jefe inmediato asignado.");
        } else {
            String mensajePersonalizado =
                    "📬 *Nueva solicitud pendiente*\n\n" +
                            "🆔 Trámite: #" + tramite.getTramiteId() + "\n" +
                            "👤 Solicitante: " + sesion.getNombre() + "\n" +
                            "🎯 Propósito: " + form.getProposito() + "\n" +
                            "💰 Presupuesto: $" + form.getPresupuesto() + "\n\n" +
                            "👉 Toca el botón *[📄 Revisar Trámite]* para abrirlo directamente.";

            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramite.getTramiteId());
            notifDTO.setDestinatario(correoJefe);
            notifDTO.setTipo("Nueva solicitud pendiente");
            notifDTO.setMensajePersonalizado(mensajePersonalizado);
            // ✅ FIX: agregar el botón "Revisar Trámite" al Jefe
            notifDTO.setUrlBotonTelegram(urlRevisarTramite(tramite.getTramiteId()));
            notificacionService.guardar(notifDTO);
        }

        sesion.setAccionPendiente(null);
        sesion.setFormularioSolicitud(null);

        return "✅ Solicitud registrada correctamente.\n\n" +
                "🆔 Número: *ANT-" + String.format("%05d", tramite.getTramiteId()) + "*\n\n" +
                "📬 Se ha notificado al Jefe Inmediato para su revisión.";
    }

    private String confirmarLiquidacion(SesionUsuario sesion) {
        FormularioLiquidacionDTO form = sesion.getFormularioLiquidacion();

        Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarAnticipoTransferido(sesion.getUsuarioId());
        BigDecimal anticipo = solicitudOpt.map(SolicitudResponseDTO::getPresupuesto)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalFacturas = BigDecimal.ZERO;
        for (FormularioLiquidacionDTO.LineaFactura f : sesion.getFacturasExtraidas()) {
            if (f.getValor() != null) {
                totalFacturas = totalFacturas.add(f.getValor());
            }
        }

        BigDecimal saldo = anticipo.subtract(totalFacturas);

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            sesion.setEsperandoComprobanteDevolucion(true);
            sesion.setMontoDevolucionPendiente(saldo);
            sesion.setSubiendoFacturas(false);

            StringBuilder resumen = new StringBuilder();
            resumen.append("📊 *Resumen de la liquidación*\n\n");
            resumen.append("💰 Anticipo: $").append(anticipo).append("\n");
            resumen.append("📉 Total gastado: $").append(totalFacturas).append("\n");
            resumen.append("📈 Saldo a devolver: $").append(saldo).append("\n\n");
            resumen.append("⚠️ *Debes devolver $").append(saldo).append(" al ITI.*\n\n");
            resumen.append("📎 Para continuar, por favor sube el comprobante de depósito en formato PDF.\n\n");
            resumen.append("⚠️ Solo se aceptan comprobantes *digitales* (PDF descargado de la banca en línea).\n");
            resumen.append("❌ No se aceptan fotos de tickets físicos, escaneos ni capturas de pantalla.");

            return resumen.toString();
        }

        return crearLiquidacionYCerrar(sesion, null, null);
    }

    private String crearLiquidacionYCerrar(SesionUsuario sesion,
                                           String nombreComprobanteDevolucion,
                                           ComprobanteExtraidoDTO extraido) {
        FormularioLiquidacionDTO form = sesion.getFormularioLiquidacion();

        Optional<SolicitudResponseDTO> solicitudOpt = solicitudService.buscarPorTramiteId(sesion.getTramiteEnRevision());
        BigDecimal anticipo = solicitudOpt.map(SolicitudResponseDTO::getPresupuesto)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalFacturas = BigDecimal.ZERO;
        for (FormularioLiquidacionDTO.LineaFactura f : sesion.getFacturasExtraidas()) {
            if (f.getValor() != null) {
                totalFacturas = totalFacturas.add(f.getValor());
            }
        }

        BigDecimal saldo = anticipo.subtract(totalFacturas);

        TramiteRequestDTO tramiteDTO = new TramiteRequestDTO();
        tramiteDTO.setUsuarioId(sesion.getUsuarioId());
        tramiteDTO.setTipoTramiteId(2L);
        tramiteDTO.setEstado("Liquidación en proceso");
        tramiteDTO.setTramiteOriginalId(sesion.getTramiteEnRevision());

        TramiteResponseDTO tramite = tramiteService.guardar(tramiteDTO);

        if (sesion.getTramiteEnRevision() != null) {
            tramiteService.actualizarEstado(sesion.getTramiteEnRevision(), "Liquidación en proceso");
        }

        LiquidacionRequestDTO liquidacionDTO = new LiquidacionRequestDTO();
        liquidacionDTO.setTramiteId(tramite.getTramiteId());
        liquidacionDTO.setTotalGastado(totalFacturas);
        liquidacionDTO.setSaldo(saldo);
        liquidacionDTO.setFechaPresentacion(LocalDate.now());
        LiquidacionResponseDTO liquidacion = liquidacionService.guardar(liquidacionDTO);

        for (FormularioLiquidacionDTO.LineaFactura linea : sesion.getFacturasExtraidas()) {
            FacturaRequestDTO facturaDTO = new FacturaRequestDTO();
            facturaDTO.setLiquidacionId(liquidacion.getLiquidacionId());
            facturaDTO.setNumeroFactura(linea.getNumeroFactura());
            facturaDTO.setProveedor(linea.getProveedor());
            facturaDTO.setFecha(linea.getFecha());
            facturaDTO.setMonto(linea.getValor());
            facturaService.guardar(facturaDTO);
        }

        if (nombreComprobanteDevolucion != null && saldo.compareTo(BigDecimal.ZERO) > 0) {
            DevolucionRequestDTO devolucionDTO = new DevolucionRequestDTO();
            devolucionDTO.setLiquidacionId(liquidacion.getLiquidacionId());
            devolucionDTO.setMonto(saldo);
            devolucionDTO.setFechaDevolucion(LocalDate.now());
            devolucionDTO.setComprobantePdf(nombreComprobanteDevolucion);

            if (extraido != null) {
                devolucionDTO.setBancoOrigen(extraido.getBancoOrigen());
                devolucionDTO.setCuentaOrigen(extraido.getCuentaOrigen());
                devolucionDTO.setMontoDepositado(extraido.getMontoDepositado());
                devolucionDTO.setFechaDeposito(extraido.getFechaDeposito());
                devolucionDTO.setEstado("Comprobante subido");
            } else {
                devolucionDTO.setEstado("Comprobante subido - Validación manual");
            }

            devolucionService.guardar(devolucionDTO);
            System.out.println(">>> ✅ Devolución creada con comprobante: " + nombreComprobanteDevolucion);
        }

        HistorialEstadoRequestDTO historialDTO = new HistorialEstadoRequestDTO();
        historialDTO.setTramiteId(tramite.getTramiteId());
        historialDTO.setEstadoAnterior(null);
        historialDTO.setEstadoNuevo("Liquidación en proceso");
        historialDTO.setUsuarioId(sesion.getUsuarioId());
        historialDTO.setObservacion("Liquidación creada por el funcionario");
        historialEstadoService.guardar(historialDTO);

        String correoAsistente = usuarioService.obtenerCorreoPorRol("Asistente Contable");
        if (correoAsistente == null) {
            System.err.println("⚠️ No se encontró un Asistente Contable en la BD.");
        } else {
            NotificacionRequestDTO notifDTO = new NotificacionRequestDTO();
            notifDTO.setTramiteId(tramite.getTramiteId());
            notifDTO.setDestinatario(correoAsistente);
            notifDTO.setTipo("Nueva liquidación pendiente");
            // ✅ FIX: botón "Revisar Trámite" al Asistente Contable
            notifDTO.setUrlBotonTelegram(urlRevisarTramite(tramite.getTramiteId()));
            notificacionService.guardar(notifDTO);
        }

        sesion.setFormularioLiquidacion(null);
        sesion.setSubiendoFacturas(false);
        sesion.getFacturasExtraidas().clear();
        sesion.setAccionPendiente(null);
        sesion.setTramiteEnRevision(null);
        sesion.setEsperandoComprobanteDevolucion(false);
        sesion.setMontoDevolucionPendiente(null);

        StringBuilder respuesta = new StringBuilder();
        respuesta.append("✅ Liquidación registrada correctamente.\n\n");
        respuesta.append("🆔 Trámite: *").append(tramite.getTramiteId()).append("*\n");
        respuesta.append("💰 Total gastado: $").append(totalFacturas).append("\n");
        respuesta.append("📈 Saldo: $").append(saldo).append("\n\n");

        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            if (extraido != null) {
                respuesta.append("⚠️ Devolución registrada. La Contadora la validará.\n\n");
                if (extraido.getObservacion() != null && !extraido.getObservacion().isEmpty()) {
                    respuesta.append("📋 Observaciones: ").append(extraido.getObservacion()).append("\n\n");
                }
            } else {
                respuesta.append("⚠️ Devolución registrada (validación manual).\n\n");
                respuesta.append("📌 El sistema no pudo leer automáticamente el comprobante.\n");
                respuesta.append("📌 La *Contadora* lo validará manualmente.\n\n");
            }
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            respuesta.append("ℹ️ El ITI te deberá devolver $").append(saldo.abs()).append(".\n");
            respuesta.append("(Se te notificará cuando la liquidación sea aprobada).\n\n");
        } else {
            respuesta.append("✅ No hay devolución pendiente.\n\n");
        }

        respuesta.append("📬 Se ha notificado al Asistente Contable para su revisión.\n\n");
        respuesta.append("👉 Cuando sea aprobada por la Contadora, recibirás una notificación.");

        return respuesta.toString();
    }

    // ==================== CONSULTA ====================
    private String consultarEstado(SesionUsuario sesion) {
        List<TramiteResponseDTO> tramites = tramiteService.listarPorUsuario(sesion.getUsuarioId());

        if (tramites.isEmpty()) {
            return "📋 No tienes trámites registrados aún.\n\n" +
                    "👉 Usa la opción *1* para solicitar un anticipo.";
        }

        // Mostramos los 5 más recientes para el resumen
        List<TramiteResponseDTO> recientes = tramites.stream()
                .sorted((a, b) -> b.getTramiteId().compareTo(a.getTramiteId()))
                .limit(5)
                .collect(Collectors.toList());

        StringBuilder lista = new StringBuilder("📋 *Tus trámites recientes:*\n\n");
        for (TramiteResponseDTO t : recientes) {
            lista.append("🆔 ").append(t.getTramiteId())
                    .append(" — ").append(t.getEstado()).append("\n");
        }
        lista.append("\n👉 Toca el botón de abajo para ver el resumen y el estado de cada trámite.");

        return lista.toString();
    }

    // ==================== UTILIDADES ====================
    private String normalizar(String texto) {
        if (texto == null) return "";
        return java.text.Normalizer
                .normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean tieneLiquidacionesPendientes(Long usuarioId) {
        try {
            // ✅ Buscar solo trámites de tipo "Liquidación de Anticipo"
            List<String> estadosPendientes = List.of(
                    "Liquidación en proceso",
                    "Liquidación revisada",
                    "Liquidación aprobada",
                    "Devuelto al funcionario"
            );

            // Usar el repositorio para buscar por usuario y estados, pero filtrando por tipo
            List<Tramite> pendientes = tramiteRepository.buscarPorUsuarioYEstados(usuarioId, estadosPendientes);

            // ✅ Filtrar solo los que sean de tipo "Liquidación de Anticipo"
            List<Tramite> liquidacionesPendientes = pendientes.stream()
                    .filter(t -> t.getTipoTramite() != null
                            && t.getTipoTramite().getNombre().contains("Liquidación"))
                    .toList();

            System.out.println(">>> Liquidaciones pendientes reales para usuario " + usuarioId + ": " + liquidacionesPendientes.size());
            return !liquidacionesPendientes.isEmpty();
        } catch (Exception e) {
            System.err.println("Error al verificar liquidaciones pendientes: " + e.getMessage());
            return false;
        }
    }
}