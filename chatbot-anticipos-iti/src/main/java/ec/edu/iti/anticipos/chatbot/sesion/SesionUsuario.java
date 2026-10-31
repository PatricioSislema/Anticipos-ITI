package ec.edu.iti.anticipos.chatbot.sesion;

import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.FormularioSolicitudDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Representa la sesión activa de un usuario en el chatbot.
 * Almacena el estado de autenticación, el rol, la última actividad
 * y los formularios extraídos durante el flujo conversacional.
 */
@Data
public class SesionUsuario {

    // Datos del usuario autenticado
    private Long usuarioId;
    private String nombre;
    private String correo;
    private String rolNombre;

    // Estado de autenticación
    private boolean autenticado;
    private boolean esperandoContrasena;

    // Acción que el usuario está ejecutando: "SOLICITAR" o "LIQUIDAR"
    private String accionPendiente;

    // Formularios extraídos
    private FormularioSolicitudDTO formularioSolicitud;
    private FormularioLiquidacionDTO formularioLiquidacion;

    // Trámite que el usuario está revisando actualmente (para aprobar/rechazar)
    private Long tramiteEnRevision;

    // Motivo del rechazo (cuando el usuario rechaza un trámite)
    private String motivoRechazo;

    // Indica si el usuario está esperando escribir el motivo del rechazo
    private boolean esperandoMotivoRechazo;

    // Indica si el funcionario está subiendo facturas (después del formulario de liquidación)
    private boolean subiendoFacturas;

    // Facturas extraídas durante la liquidación
    private List<FormularioLiquidacionDTO.LineaFactura> facturasExtraidas = new ArrayList<>();

    // Última vez que el usuario interactuó (para controlar el timeout)
    private LocalDateTime ultimaActividad;

    // ===== NUEVO (FASE 2) =====
    // Indica si el usuario está confirmando la cancelación de una operación
    private boolean esperandoConfirmacionCancelacion;
    // Indica si el usuario está confirmando el cierre de sesión
    private boolean esperandoConfirmacionCerrarSesion;

    // Indica si el bot está procesando un documento del usuario
    private boolean procesandoDocumento;

    // Indica si el funcionario está subiendo el comprobante de devolución (SOBRANTE)
    private boolean esperandoComprobanteDevolucion;

    private boolean esperandoNuevaContrasena = false;

    // Monto que el funcionario debe devolver al ITI (SOBRANTE)
    private BigDecimal montoDevolucionPendiente;

    // ID de la liquidación creada pero pendiente de comprobante (SOBRANTE)
    private Long liquidacionIdEnProceso;

    private String contextoActual;

    // Contador de intentos fallidos de contraseña
    private int intentosFallidos = 0;

    //  CAMPOS PARA PAGINACIÓN
    private int paginaActualLista = 0;      // Página en la que está el usuario
    private String listaEnProgreso = null;  // Ej: "SOLICITUDES_JEFE", "ORDENES_DIRECTOR"
    private String estadoFiltroLista = null; // Ej: "Pendiente", "Generada"

    // Variable para guardar el trámite al que el usuario quiere ir tras loguearse
    private Long tramiteRedirigido;

    // Contador de mensajes para recordar la finalidad
    private int contadorMensajes = 0;

    // Bandera para saber si ya se mostró el recordatorio recientemente
    private boolean recordatorioMostrado = false;

    public SesionUsuario() {
        this.autenticado = false;
        this.esperandoContrasena = false;
        this.ultimaActividad = LocalDateTime.now();
    }

    // ✅ NUEVO: Incrementar contador de mensajes
    public void incrementarContador() {
        this.contadorMensajes++;
    }

    // Resetear contador
    public void resetearContador() {
        this.contadorMensajes = 0;
        this.recordatorioMostrado = false;
    }

    // Verificar si debe mostrar el recordatorio (cada 5 mensajes)
    public boolean debeMostrarRecordatorio() {
        return this.contadorMensajes > 0 && this.contadorMensajes % 5 == 0 && !this.recordatorioMostrado;
    }
}