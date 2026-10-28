import React, { useState, useEffect } from 'react';
import { Search, Eye, CheckCircle, XCircle, Clock, ChevronLeft, ChevronRight, X, Download, AlertTriangle } from 'lucide-react';
import api from '../services/api';

interface Tramite {
  tramiteId: number;
  estado: string;
  fechaCreacion: string;
  nombreUsuario: string;
  nombreTipoTramite: string;
}

interface TramiteDetalle {
  tramiteId: number;
  estado: string;
  fechaCreacion: string;
  nombreTipoTramite: string;
  nombreUsuario: string;
  correoUsuario: string;
  cedulaUsuario: string;
  unidad?: string;
  campus?: string;
  proposito?: string;
  presupuesto?: number;
  totalGastado?: number;
  saldo?: number;
}

export const Tramites = () => {
  const [tramites, setTramites] = useState<Tramite[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('Todos');
  const [paginaActual, setPaginaActual] = useState(1);
  const elementosPorPagina = 5;

  // Estados para los Modales
  const [modalAbierto, setModalAbierto] = useState(false);
  const [detalle, setDetalle] = useState<TramiteDetalle | null>(null);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);

  // NUEVO: Estado para el modal de PDFs
  const [modalPdfAbierto, setModalPdfAbierto] = useState(false);

  useEffect(() => {
    cargarTramites();
  }, []);

  const cargarTramites = async () => {
    try {
      setCargando(true);
      const res = await api.get('/tramites/todos');
      setTramites(res.data);
    } catch (error) {
      console.error("Error al cargar trámites:", error);
    } finally {
      setCargando(false);
    }
  };

  const abrirDetalle = async (id: number) => {
    setModalAbierto(true);
    setCargandoDetalle(true);
    setDetalle(null);
    try {
      const res = await api.get(`/tramites/${id}/detalle`);
      setDetalle(res.data);
    } catch (error) {
      console.error("Error al cargar detalle:", error);
      setModalAbierto(false);
    } finally {
      setCargandoDetalle(false);
    }
  };

  const tramitesFiltrados = tramites.filter((t) => {
    const coincideTexto = t.nombreUsuario.toLowerCase().includes(busqueda.toLowerCase()) ||
      t.tramiteId.toString().includes(busqueda);
    const coincideEstado = filtroEstado === 'Todos' || t.estado === filtroEstado;
    return coincideTexto && coincideEstado;
  });

  const indiceUltimo = paginaActual * elementosPorPagina;
  const indicePrimero = indiceUltimo - elementosPorPagina;
  const tramitesPaginados = tramitesFiltrados.slice(indicePrimero, indiceUltimo);
  const totalPaginas = Math.ceil(tramitesFiltrados.length / elementosPorPagina) || 1;

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f4f6f9', fontFamily: 'sans-serif' }}>
      <main style={{ padding: '32px 24px', maxWidth: '1200px', margin: '0 auto' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 'bold', color: '#1f2937', margin: '0 0 24px 0' }}>Gestión de Trámites</h1>

        {/* FILTROS */}
        <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', backgroundColor: 'white', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <span style={{ position: 'absolute', top: '10px', left: '12px', color: '#9ca3af' }}><Search size={18} /></span>
            <input type="text" placeholder="Buscar por ID o solicitante..." value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setPaginaActual(1); }} style={{ width: '100%', padding: '10px 12px 10px 38px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none', boxSizing: 'border-box' }} />
          </div>
          <select value={filtroEstado} onChange={(e) => { setFiltroEstado(e.target.value); setPaginaActual(1); }} style={{ padding: '10px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none' }}>
            <option value="Todos">Todos los Estados</option>
            <option value="Pendiente">Pendiente</option>
            <option value="Aprobado por Jefe">Aprobado por Jefe</option>
            <option value="Aprobado por Director">Aprobado por Director</option>
            <option value="Transferido">Transferido</option>
            <option value="Rechazado">Rechazado</option>
            <option value="Liquidación en proceso">Liquidación en proceso</option>
            <option value="Orden aprobada">Orden aprobada</option>
          </select>
        </div>

        {/* TABLA */}
        <div style={{ backgroundColor: 'white', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
              <tr>
                <th style={{ padding: '14px 16px' }}>Código</th>
                <th style={{ padding: '14px 16px' }}>Solicitante</th>
                <th style={{ padding: '14px 16px' }}>Tipo</th>
                <th style={{ padding: '14px 16px' }}>Fecha</th>
                <th style={{ padding: '14px 16px' }}>Estado</th>
                <th style={{ padding: '14px 16px', textAlign: 'center' }}>Ver Detalle</th>
                <th style={{ padding: '14px 16px', textAlign: 'center' }}>Documentos</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={7} style={{ padding: '32px', textAlign: 'center' }}>Cargando trámites...</td></tr>
              ) : tramitesPaginados.length > 0 ? (
                tramitesPaginados.map((t) => (
                  <tr key={t.tramiteId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: '#8C1D40' }}>TRM-{t.tramiteId}</td>
                    <td style={{ padding: '14px 16px' }}>{t.nombreUsuario}</td>
                    <td style={{ padding: '14px 16px' }}>{t.nombreTipoTramite}</td>
                    <td style={{ padding: '14px 16px' }}>{t.fechaCreacion}</td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 500, backgroundColor: t.estado.includes('Aprobado') || t.estado === 'Transferido' ? '#d1fae5' : t.estado === 'Rechazado' ? '#fee2e2' : '#fef3c7', color: t.estado.includes('Aprobado') || t.estado === 'Transferido' ? '#065f46' : t.estado === 'Rechazado' ? '#991b1b' : '#92400e' }}>
                        {t.estado}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                      <button onClick={() => abrirDetalle(t.tramiteId)} style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px', fontWeight: 500 }}>
                        <Eye size={14} /> Ver
                      </button>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                      <button
                        onClick={() => setModalPdfAbierto(true)}
                        title="Descargar Documentos"
                        style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #bfdbfe', backgroundColor: '#eff6ff', color: '#1e40af', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px', fontWeight: 500, fontSize: '13px' }}
                      >
                        <Download size={14} /> PDF
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan={7} style={{ padding: '32px', textAlign: 'center' }}>No hay trámites registrados.</td></tr>
              )}
            </tbody>
          </table>

          {/* PAGINACIÓN */}
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '16px 24px', backgroundColor: '#f9fafb', borderTop: '1px solid #e5e7eb' }}>
            <span style={{ fontSize: '13px', color: '#6b7280' }}>Mostrando {tramitesPaginados.length > 0 ? indicePrimero + 1 : 0} a {Math.min(indiceUltimo, tramitesFiltrados.length)} de {tramitesFiltrados.length} registros</span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button onClick={() => setPaginaActual(p => Math.max(p - 1, 1))} disabled={paginaActual === 1} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: 'white', cursor: paginaActual === 1 ? 'not-allowed' : 'pointer', opacity: paginaActual === 1 ? 0.5 : 1 }}><ChevronLeft size={16} /></button>
              <button onClick={() => setPaginaActual(p => Math.min(p + 1, totalPaginas))} disabled={paginaActual === totalPaginas} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: 'white', cursor: paginaActual === totalPaginas ? 'not-allowed' : 'pointer', opacity: paginaActual === totalPaginas ? 0.5 : 1 }}><ChevronRight size={16} /></button>
            </div>
          </div>
        </div>
      </main>

      {/* MODAL DE DETALLE (El ojito) */}
      {modalAbierto && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', maxWidth: '600px', width: '100%', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>{detalle ? `Detalle TRM-${detalle.tramiteId}` : 'Cargando...'}</h3>
              <button onClick={() => setModalAbierto(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <div style={{ padding: '24px' }}>
              {cargandoDetalle ? <p>Cargando información...</p> : detalle ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '14px', color: '#374151' }}>
                  <div style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
                    <p style={{ margin: '0 0 8px 0' }}><strong>Solicitante:</strong> {detalle.nombreUsuario}</p>
                    <p style={{ margin: '0 0 8px 0' }}><strong>Tipo:</strong> {detalle.nombreTipoTramite}</p>
                    <p style={{ margin: 0 }}><strong>Estado:</strong> {detalle.estado}</p>
                  </div>
                  {detalle.presupuesto && (
                    <div style={{ backgroundColor: '#eff6ff', padding: '16px', borderRadius: '8px', border: '1px solid #bfdbfe' }}>
                      <p style={{ margin: '0 0 8px 0' }}><strong>Presupuesto:</strong> ${detalle.presupuesto}</p>
                      {detalle.proposito && <p style={{ margin: 0 }}><strong>Propósito:</strong> {detalle.proposito}</p>}
                    </div>
                  )}
                </div>
              ) : <p>No se pudo cargar.</p>}
            </div>
            <div style={{ padding: '16px 24px', borderTop: '1px solid #e5e7eb', display: 'flex', justifyContent: 'flex-end', backgroundColor: '#f9fafb' }}>
              <button onClick={() => setModalAbierto(false)} style={{ padding: '8px 16px', borderRadius: '6px', border: 'none', backgroundColor: '#e5e7eb', cursor: 'pointer', fontWeight: 500 }}>Cerrar</button>
            </div>
          </div>
        </div>
      )}

      {/* NUEVO MODAL DE PDF (Aviso bonito de limitación) */}
      {modalPdfAbierto && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
          <div style={{ backgroundColor: 'white', borderRadius: '12px', maxWidth: '450px', width: '100%', overflow: 'hidden', boxShadow: '0 10px 25px rgba(0,0,0,0.2)' }}>

            <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>Documentos del Trámite</h3>
              <button onClick={() => setModalPdfAbierto(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
            </div>

            <div style={{ padding: '24px', fontSize: '14px', color: '#374151', lineHeight: '1.6' }}>
              <p style={{ marginTop: 0 }}><strong>Documentos asociados:</strong></p>
              <ul style={{ paddingLeft: '20px', marginBottom: '20px', color: '#4b5563' }}>
                <li>Formulario de Solicitud / Liquidación</li>
                <li>Comprobantes y Facturas (si aplica)</li>
              </ul>

              <div style={{ backgroundColor: '#fef3c7', borderLeft: '4px solid #d97706', padding: '12px 16px', borderRadius: '0 8px 8px 0', color: '#92400e' }}>
                <strong style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><AlertTriangle size={16} /> Archivo no disponible para descarga</strong>
                <p style={{ margin: '8px 0 0 0', fontSize: '13px' }}>
                  Para evitar costos elevados de infraestructura en este prototipo, los PDFs no se almacenan en la base de datos.
                  <br /><br />
                  <strong>Mejora futura:</strong> En producción se implementará <strong>Azure Blob Storage</strong> para permitir la descarga desde el Chatbot y la Web.
                </p>
              </div>
            </div>

            <div style={{ padding: '16px 24px', backgroundColor: '#f9fafb', borderTop: '1px solid #e5e7eb', display: 'flex', justifyContent: 'flex-end' }}>
              <button onClick={() => setModalPdfAbierto(false)} style={{ padding: '8px 16px', backgroundColor: '#8C1D40', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600 }}>
                Entendido
              </button>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};