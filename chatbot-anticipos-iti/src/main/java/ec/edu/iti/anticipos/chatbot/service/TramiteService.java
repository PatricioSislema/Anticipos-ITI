package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.TramiteRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteResponseDTO;
import ec.edu.iti.anticipos.chatbot.entity.TipoTramite;
import ec.edu.iti.anticipos.chatbot.entity.Tramite;
import ec.edu.iti.anticipos.chatbot.entity.Usuario;
import ec.edu.iti.anticipos.chatbot.repository.TipoTramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.TramiteRepository;
import ec.edu.iti.anticipos.chatbot.repository.UsuarioRepository;
import ec.edu.iti.anticipos.chatbot.repository.OrdenTransferenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ec.edu.iti.anticipos.chatbot.dto.response.TramiteDetalleDTO;
import ec.edu.iti.anticipos.chatbot.repository.SolicitudRepository;
import ec.edu.iti.anticipos.chatbot.repository.LiquidacionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoTramiteRepository tipoTramiteRepository;
    private final SolicitudRepository solicitudRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final OrdenTransferenciaRepository ordenTransferenciaRepository;

    @Transactional(readOnly = true)
    public List<TramiteResponseDTO> listarTodos() {
        List<Tramite> todos = tramiteRepository.findAll();

        // ✅ FIX PROFESIONAL: Obtener los IDs de los trámites que YA TIENEN una liquidación
        Set<Long> idsConLiquidacion = todos.stream()
                .filter(t -> t.getTramiteOriginal() != null)
                .map(t -> t.getTramiteOriginal().getTramiteId())
                .collect(Collectors.toSet());

        return todos.stream()
                .filter(tramite -> {
                    // Si es un trámite original (no tiene padre) y ya tiene una liquidación, LO OCULTAMOS
                    boolean esOriginal = tramite.getTramiteOriginal() == null;
                    boolean tieneLiquidacion = idsConLiquidacion.contains(tramite.getTramiteId());

                    if (esOriginal && tieneLiquidacion) {
                        return false; // No lo enviamos al frontend
                    }
                    return true;
                })
                .map(tramite -> {
                    if (tramite.getUsuario() != null) {
                        tramite.getUsuario().getTelefono();
                    }
                    return convertirAResponse(tramite);
                })
                .toList();
    }

    public Optional<TramiteResponseDTO> buscarPorId(Long id) {
        return tramiteRepository.findById(id).map(this::convertirAResponse);
    }

    public TramiteResponseDTO guardar(TramiteRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        TipoTramite tipoTramite = tipoTramiteRepository.findById(dto.getTipoTramiteId())
                .orElseThrow(() -> new RuntimeException("Tipo de trámite no encontrado"));

        Tramite tramite = new Tramite();
        tramite.setUsuario(usuario);
        tramite.setTipoTramite(tipoTramite);
        tramite.setEstado(dto.getEstado() != null ? dto.getEstado() : "Pendiente");
        tramite.setFechaCreacion(LocalDate.now());

        if (dto.getTramiteOriginalId() != null) {
            Tramite original = tramiteRepository.findById(dto.getTramiteOriginalId())
                    .orElseThrow(() -> new RuntimeException("Trámite original no encontrado"));
            tramite.setTramiteOriginal(original);
        }

        Tramite guardado = tramiteRepository.save(tramite);
        return convertirAResponse(guardado);
    }

    public TramiteResponseDTO actualizarEstado(Long id, String nuevoEstado) {
        Tramite tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        tramite.setEstado(nuevoEstado);
        Tramite actualizado = tramiteRepository.save(tramite);
        return convertirAResponse(actualizado);
    }

    public void eliminar(Long id) {
        tramiteRepository.deleteById(id);
    }

    private TramiteResponseDTO convertirAResponse(Tramite tramite) {
        TramiteResponseDTO dto = new TramiteResponseDTO();
        dto.setTramiteId(tramite.getTramiteId());
        dto.setEstado(tramite.getEstado());
        dto.setFechaCreacion(tramite.getFechaCreacion());
        dto.setNombreUsuario(tramite.getUsuario().getNombre());
        dto.setNombreTipoTramite(tramite.getTipoTramite().getNombre());
        dto.setCorreoUsuario(tramite.getUsuario().getCorreo());
        dto.setTelefono(tramite.getUsuario().getTelefono());

        // ✅ IMPORTANTE: Mandamos el ID del trámite original al frontend (si es que lo tiene)
        if (tramite.getTramiteOriginal() != null) {
            dto.setTramiteOriginalId(tramite.getTramiteOriginal().getTramiteId());
        }

        Optional<ec.edu.iti.anticipos.chatbot.entity.Solicitud> solicitudOpt = solicitudRepository.findByTramiteTramiteId(tramite.getTramiteId());
        if (solicitudOpt.isPresent()) {
            dto.setPresupuesto(solicitudOpt.get().getPresupuesto());
            dto.setProposito(solicitudOpt.get().getProposito());
        } else {
            liquidacionRepository.findByTramiteTramiteId(tramite.getTramiteId()).ifPresentOrElse(
                    liquidacion -> {
                        dto.setPresupuesto(liquidacion.getTotalGastado());
                        dto.setProposito("Liquidación de Anticipo");
                    },
                    () -> dto.setProposito("Liquidación de Anticipo")
            );
        }

        String estado = tramite.getEstado();
        boolean estadoValidoParaFecha =
                "Transferido".equals(estado) ||
                        "Liquidación en proceso".equals(estado) ||
                        "Liquidación revisada".equals(estado) ||
                        "Liquidación aprobada".equals(estado);

        if (estadoValidoParaFecha) {
            try {
                Optional<ec.edu.iti.anticipos.chatbot.entity.Liquidacion> liqOpt = liquidacionRepository.findByTramiteTramiteId(tramite.getTramiteId());
                Long idBusqueda = liqOpt.isPresent() && liqOpt.get().getTramiteOriginal() != null
                        ? liqOpt.get().getTramiteOriginal().getTramiteId()
                        : tramite.getTramiteId();

                List<ec.edu.iti.anticipos.chatbot.entity.OrdenTransferencia> ordenes = ordenTransferenciaRepository.findByTramiteTramiteId(idBusqueda);
                if (!ordenes.isEmpty()) {
                    dto.setFechaTransferencia(ordenes.get(0).getFechaGeneracion());
                } else {
                    dto.setFechaTransferencia(tramite.getFechaCreacion());
                }
            } catch (Exception e) {
                dto.setFechaTransferencia(tramite.getFechaCreacion());
            }
        } else {
            dto.setFechaTransferencia(null);
        }

        boolean tieneLiquidacionCerrada = false;
        List<Tramite> liquidacionesAsociadas = tramiteRepository.findAll().stream()
                .filter(t -> t.getTramiteOriginal() != null && t.getTramiteOriginal().getTramiteId().equals(tramite.getTramiteId()))
                .toList();

        for (Tramite liq : liquidacionesAsociadas) {
            if ("Liquidación cerrada".equals(liq.getEstado())) {
                tieneLiquidacionCerrada = true;
                break;
            }
        }

        if (tieneLiquidacionCerrada) {
            dto.setEstado("Liquidación cerrada");
            dto.setFechaTransferencia(null);
        }

        return dto;
    }

    public List<TramiteResponseDTO> listarPorEstado(String estado) {
        return tramiteRepository.findByEstado(estado).stream().map(this::convertirAResponse).toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TramiteResponseDTO> listarPaginado(String estado, int pagina, int tamaño) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(pagina, tamaño);
        org.springframework.data.domain.Page<Tramite> page = tramiteRepository.filtrar(estado, null, pageable);
        return page.map(this::convertirAResponse);
    }

    public List<TramiteResponseDTO> listarPorUsuario(Long usuarioId) {
        return tramiteRepository.findByUsuarioUsuarioId(usuarioId).stream().map(this::convertirAResponse).toList();
    }

    public Optional<TramiteDetalleDTO> obtenerDetalle(Long tramiteId) {
        Optional<Tramite> tramiteOpt = tramiteRepository.findById(tramiteId);
        if (tramiteOpt.isEmpty()) {
            return Optional.empty();
        }

        Tramite tramite = tramiteOpt.get();
        TramiteDetalleDTO dto = new TramiteDetalleDTO();
        dto.setTramiteId(tramite.getTramiteId());
        dto.setEstado(tramite.getEstado());
        dto.setFechaCreacion(tramite.getFechaCreacion());
        dto.setNombreTipoTramite(tramite.getTipoTramite().getNombre());

        if (tramite.getUsuario() != null) {
            dto.setUsuarioId(tramite.getUsuario().getUsuarioId());
            dto.setNombreUsuario(tramite.getUsuario().getNombre());
            dto.setCorreoUsuario(tramite.getUsuario().getCorreo());
            dto.setCedulaUsuario(tramite.getUsuario().getCedula());
        }

        solicitudRepository.findByTramiteTramiteId(tramiteId).ifPresent(solicitud -> {
            dto.setUnidad(solicitud.getUnidad());
            dto.setCampus(solicitud.getCampus());
            dto.setProposito(solicitud.getProposito());
            dto.setLugar(solicitud.getLugar());
            dto.setFechaRealizacion(solicitud.getFechaRealizacion());
            dto.setPresupuesto(solicitud.getPresupuesto());
        });

        liquidacionRepository.findByTramiteTramiteId(tramiteId).ifPresent(liquidacion -> {
            dto.setTotalGastado(liquidacion.getTotalGastado());
            dto.setSaldo(liquidacion.getSaldo());
            dto.setFechaPresentacion(liquidacion.getFechaPresentacion());

            Long idOriginal = null;
            if (liquidacion.getTramiteOriginal() != null) {
                idOriginal = liquidacion.getTramiteOriginal().getTramiteId();
            } else if (tramite.getTramiteOriginal() != null) {
                idOriginal = tramite.getTramiteOriginal().getTramiteId();
            }

            // Si encontramos el ID, se busca la solicitud original
            if (idOriginal != null) {
                solicitudRepository.findByTramiteTramiteId(idOriginal).ifPresent(solicitudOriginal -> {
                    dto.setProposito(solicitudOriginal.getProposito());
                    dto.setUnidad(solicitudOriginal.getUnidad());
                    dto.setCampus(solicitudOriginal.getCampus());
                    dto.setLugar(solicitudOriginal.getLugar());
                    dto.setFechaRealizacion(solicitudOriginal.getFechaRealizacion());
                    dto.setPresupuesto(solicitudOriginal.getPresupuesto());
                });
            }
        });

        return Optional.of(dto);
    }

    public List<TramiteResponseDTO> listarPorUsuarioYEstado(Long usuarioId, String estado) {
        return tramiteRepository.findByUsuarioUsuarioIdAndEstado(usuarioId, estado)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public List<TramiteResponseDTO> listarPorEstadoYTipo(String estado) {
        return tramiteRepository.findByEstadoYTipo(estado)
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }
}