import React, { useState, useEffect } from 'react';
import { Plus, Trash2, X, CheckCircle, Edit } from 'lucide-react';
import api from '../services/api';
import { ModalConfirmacion } from '../components/ModalConfirmacion';

interface TipoTramite {
  tipoTramiteId: number;
  nombre: string;
}

export const TiposTramite = () => {
  const [tipos, setTipos] = useState<TipoTramite[]>([]);
  const [cargando, setCargando] = useState(true);

  const [alertaExito, setAlertaExito] = useState('');
  const [alertaError, setAlertaError] = useState('');

  const [modalFormAbierto, setModalFormAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [tipoActualId, setTipoActualId] = useState<number | null>(null);

  // Modal de Confirmación
  const [accionPendiente, setAccionPendiente] = useState<{ tipo: 'crear' | 'editar' | 'eliminar', id?: number, nombre: string } | null>(null);

  const [formNombre, setFormNombre] = useState('');

  useEffect(() => {
    cargarTipos();
  }, []);

  const cargarTipos = async () => {
    try {
      setCargando(true);
      const res = await api.get('/tipos-tramite');
      setTipos(res.data);
    } catch (error) {
      console.error("Error al cargar tipos:", error);
      mostrarError("No se pudieron cargar los tipos de trámite.");
    } finally {
      setCargando(false);
    }
  };

  const abrirModalCrear = () => {
    setModoEdicion(false);
    setFormNombre('');
    setModalFormAbierto(true);
  };

  const abrirModalEditar = (tipo: TipoTramite) => {
    setModoEdicion(true);
    setTipoActualId(tipo.tipoTramiteId);
    setFormNombre(tipo.nombre);
    setModalFormAbierto(true);
  };

  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();
    setAccionPendiente({
      tipo: modoEdicion ? 'editar' : 'crear',
      nombre: formNombre
    });
  };

  const ejecutarGuardado = async () => {
    try {
      if (modoEdicion && tipoActualId !== null) {
        await api.put(`/tipos-tramite/${tipoActualId}`, { nombre: formNombre });
        mostrarAlerta('Tipo de trámite actualizado correctamente');
      } else {
        await api.post('/tipos-tramite', { nombre: formNombre });
        mostrarAlerta('Tipo de trámite creado correctamente');
      }
      setModalFormAbierto(false);
      cargarTipos();
    } catch (error: any) {
      mostrarError("Error: " + (error.response?.data?.message || error.response?.data || error.message));
    } finally {
      setAccionPendiente(null);
    }
  };

  const handleEliminarClick = (id: number, nombre: string) => {
    setAccionPendiente({ tipo: 'eliminar', id, nombre });
  };

  const ejecutarEliminar = async () => {
    if (!accionPendiente || !accionPendiente.id) return;
    try {
      await api.delete(`/tipos-tramite/${accionPendiente.id}`);
      mostrarAlerta('Tipo de trámite eliminado correctamente');
      cargarTipos();
    } catch (error: any) {
      const mensajeError = error.response?.data?.message
        || error.response?.data
        || "No se puede eliminar. Puede que esté en uso.";
      mostrarError(mensajeError);
    } finally {
      setAccionPendiente(null);
    }
  };

  const mostrarAlerta = (msg: string) => {
    setAlertaExito(msg);
    setTimeout(() => setAlertaExito(''), 3000);
  };

  const mostrarError = (msg: string) => {
    setAlertaError(msg);
    setTimeout(() => setAlertaError(''), 4000);
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f4f6f9', fontFamily: 'sans-serif', padding: '32px' }}>

      {/* ALERTAS FLOTANTES */}
      {alertaExito && (
        <div style={{ position: 'fixed', top: '20px', right: '20px', backgroundColor: '#d1fae5', color: '#065f46', padding: '16px 24px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', zIndex: 10000, fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle size={20} /> {alertaExito}
        </div>
      )}
      {alertaError && (
        <div style={{ position: 'fixed', top: '20px', right: '20px', backgroundColor: '#fee2e2', color: '#991b1b', padding: '16px 24px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', zIndex: 10000, fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <X size={20} /> {alertaError}
        </div>
      )}

      <div style={{ maxWidth: '1000px', margin: '0 auto' }}>

        {/* ENCABEZADO Y BOTÓN CREAR */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h1 style={{ fontSize: '26px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>Tipos de Trámite</h1>
            <p style={{ color: '#6b7280', fontSize: '14px', marginTop: '4px' }}>Configure los tipos de trámites disponibles.</p>
          </div>
          <button onClick={abrirModalCrear} style={{ backgroundColor: '#8C1D40', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '6px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
            <Plus size={18} /> Nuevo Tipo
          </button>
        </div>

        {/* TABLA DE TIPOS */}
        <div style={{ backgroundColor: 'white', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                <th style={{ padding: '14px 16px', width: '80px' }}>ID</th>
                <th style={{ padding: '14px 16px' }}>Nombre del Trámite</th>
                <th style={{ padding: '14px 16px', textAlign: 'center', width: '150px' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={3} style={{ padding: '32px', textAlign: 'center' }}>Cargando tipos de trámite...</td></tr>
              ) : tipos.map((tipo) => (
                <tr key={tipo.tipoTramiteId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                  <td style={{ padding: '14px 16px', color: '#6b7280' }}>{tipo.tipoTramiteId}</td>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: '#1f2937' }}>{tipo.nombre}</td>
                  <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '6px' }}>
                      <button onClick={() => abrirModalEditar(tipo)} title="Editar" style={{ padding: '6px', backgroundColor: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer' }}><Edit size={15} /></button>
                      <button onClick={() => handleEliminarClick(tipo.tipoTramiteId, tipo.nombre)} title="Eliminar" style={{ padding: '6px', backgroundColor: '#fee2e2', border: '1px solid #fecaca', borderRadius: '4px', cursor: 'pointer' }}><Trash2 size={15} color="#991b1b" /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {tipos.length === 0 && !cargando && (
                <tr><td colSpan={3} style={{ padding: '32px', textAlign: 'center' }}>No hay tipos de trámite configurados.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* MODAL CREAR / EDITAR */}
      {modalFormAbierto && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '400px' }}>
            <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px', display: 'flex', justifyContent: 'space-between' }}>
              <h3 style={{ margin: 0 }}>{modoEdicion ? 'Editar Tipo de Trámite' : 'Crear Nuevo Tipo'}</h3>
              <button onClick={() => setModalFormAbierto(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <form onSubmit={handleSubmitForm} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Nombre *</label>
                <input type="text" required value={formNombre} onChange={(e) => setFormNombre(e.target.value)} placeholder="Ej. Reembolso" style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" onClick={() => setModalFormAbierto(false)} style={{ padding: '10px 16px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}>Cancelar</button>
                <button type="button" onClick={(e) => {
                  const form = e.currentTarget.closest('form');
                  if (form && form.checkValidity()) { handleSubmitForm(e as any); } else if (form) { form.reportValidity(); }
                }} style={{ padding: '10px 16px', border: 'none', backgroundColor: '#8C1D40', color: 'white', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}>Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL CONFIRMACIÓN */}
      {accionPendiente && (
        <ModalConfirmacion
          titulo={
            accionPendiente.tipo === 'crear' ? "Confirmar Creación" :
              accionPendiente.tipo === 'editar' ? "Confirmar Edición" : "Eliminar Tipo de Trámite"
          }
          mensaje={
            accionPendiente.tipo === 'crear' ? `¿Está seguro de crear el tipo de trámite "${accionPendiente.nombre}"?` :
              accionPendiente.tipo === 'editar' ? `¿Está seguro de guardar los cambios en "${accionPendiente.nombre}"?` :
                `¿Está seguro de eliminar el tipo de trámite "${accionPendiente.nombre}"? Esta acción no se puede deshacer.`
          }
          onConfirmar={
            accionPendiente.tipo === 'eliminar' ? ejecutarEliminar : ejecutarGuardado
          }
          onCancelar={() => setAccionPendiente(null)}
        />
      )}

    </div>
  );
};