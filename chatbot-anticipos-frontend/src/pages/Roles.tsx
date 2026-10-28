import React, { useState, useEffect } from 'react';
import { Plus, Trash2, X, CheckCircle, Edit, RotateCcw } from 'lucide-react';
import api from '../services/api';
import { ModalConfirmacion } from '../components/ModalConfirmacion';

interface Rol {
  id: number; // <-- CORREGIDO AQUÍ
  nombre: string;
  descripcion: string;
  activo: boolean;
}

export const Roles = () => {
  const [roles, setRoles] = useState<Rol[]>([]);
  const [cargando, setCargando] = useState(true);

  // Alertas
  const [alertaExito, setAlertaExito] = useState('');
  const [alertaError, setAlertaError] = useState('');

  // Pestañas
  const [verDesactivados, setVerDesactivados] = useState(false);

  // Modales
  const [modalFormAbierto, setModalFormAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [rolActualId, setRolActualId] = useState<number | null>(null);

  // Modal de Confirmación
  const [accionPendiente, setAccionPendiente] = useState<{ tipo: 'crear' | 'editar' | 'desactivar', id?: number, nombre?: string } | null>(null);

  // Formulario
  const [formNombre, setFormNombre] = useState('');
  const [formDescripcion, setFormDescripcion] = useState('');

  useEffect(() => {
    cargarRoles();
  }, []);

  const cargarRoles = async () => {
    try {
      setCargando(true);
      const res = await api.get('/roles');
      setRoles(res.data);
    } catch (error) {
      console.error("Error al cargar roles:", error);
      mostrarError("No se pudieron cargar los roles.");
    } finally {
      setCargando(false);
    }
  };

  // --- FILTRADO Y PESTAÑAS ---
  const rolesFiltrados = roles.filter((r) => {
    const esDesactivado = r.activo === false;
    if (verDesactivados !== esDesactivado) return false;
    return true;
  });

  const abrirModalCrear = () => {
    setModoEdicion(false);
    setFormNombre('');
    setFormDescripcion('');
    setModalFormAbierto(true);
  };

  const abrirModalEditar = (rol: Rol) => {
    setModoEdicion(true);
    setRolActualId(rol.id); // <-- CORREGIDO AQUÍ
    setFormNombre(rol.nombre);
    setFormDescripcion(rol.descripcion || '');
    setModalFormAbierto(true);
  };

  // --- VALIDACIÓN Y CONFIRMACIÓN ---
  const handleSubmitForm = (e: React.FormEvent) => {
    e.preventDefault();
    setAccionPendiente({
      tipo: modoEdicion ? 'editar' : 'crear',
      nombre: formNombre
    });
  };

  const ejecutarGuardado = async () => {
    try {
      if (modoEdicion && rolActualId !== null) {
        await api.put(`/roles/${rolActualId}`, {
          nombre: formNombre,
          descripcion: formDescripcion,
          activo: true
        });
        mostrarAlerta('Rol actualizado correctamente');
      } else {
        await api.post('/roles', {
          nombre: formNombre,
          descripcion: formDescripcion
        });
        mostrarAlerta('Rol creado correctamente');
      }
      setModalFormAbierto(false);
      cargarRoles();
    } catch (error: any) {
      mostrarError("Error: " + (error.response?.data?.message || error.response?.data || error.message));
    } finally {
      setAccionPendiente(null);
    }
  };

  // --- DESACTIVACIÓN LÓGICA ---
  const handleDesactivarClick = (id: number, nombre: string) => {
    setAccionPendiente({ tipo: 'desactivar', id, nombre });
  };

  const ejecutarDesactivar = async () => {
    if (!accionPendiente || !accionPendiente.id) return;

    try {
      await api.delete(`/roles/${accionPendiente.id}`);
      mostrarAlerta('Rol desactivado correctamente');
      cargarRoles();
    } catch (error: any) {
      console.error("ERROR COMPLETO:", error);
      const mensajeError = typeof error.response?.data === 'string'
        ? error.response.data
        : (error.response?.data?.message || "Error al desactivar rol");

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

      {/* ALERTA FLOTANTE DE ÉXITO */}
      {alertaExito && (
        <div style={{ position: 'fixed', top: '20px', right: '20px', backgroundColor: '#d1fae5', color: '#065f46', padding: '16px 24px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', zIndex: 10000, fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle size={20} /> {alertaExito}
        </div>
      )}

      {/* ALERTA FLOTANTE DE ERROR */}
      {alertaError && (
        <div style={{ position: 'fixed', top: '20px', right: '20px', backgroundColor: '#fee2e2', color: '#991b1b', padding: '16px 24px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', zIndex: 10000, fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <X size={20} /> {alertaError}
        </div>
      )}

      <div style={{ maxWidth: '1000px', margin: '0 auto' }}>

        {/* ENCABEZADO Y BOTÓN CREAR */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h1 style={{ fontSize: '26px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>Gestión de Roles</h1>
            <p style={{ color: '#6b7280', fontSize: '14px', marginTop: '4px' }}>Administre los accesos y permisos del sistema.</p>
          </div>
          {!verDesactivados && (
            <button onClick={abrirModalCrear} style={{ backgroundColor: '#8C1D40', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '6px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
              <Plus size={18} /> Nuevo Rol
            </button>
          )}
        </div>

        {/* PESTAÑAS DE ACTIVOS / PAPELERA */}
        <div style={{ display: 'flex', gap: '24px', borderBottom: '1px solid #e5e7eb', marginBottom: '16px' }}>
          <button onClick={() => setVerDesactivados(false)} style={{ background: 'none', border: 'none', paddingBottom: '8px', cursor: 'pointer', fontSize: '14px', fontWeight: !verDesactivados ? 600 : 'normal', color: !verDesactivados ? '#8C1D40' : '#6b7280', borderBottom: !verDesactivados ? '3px solid #8C1D40' : 'none' }}>
            🛡️ Roles Activos
          </button>
          <button onClick={() => setVerDesactivados(true)} style={{ background: 'none', border: 'none', paddingBottom: '8px', cursor: 'pointer', fontSize: '14px', fontWeight: verDesactivados ? 600 : 'normal', color: verDesactivados ? '#8C1D40' : '#6b7280', borderBottom: verDesactivados ? '3px solid #8C1D40' : 'none' }}>
            🗑️ Papelera (Desactivados)
          </button>
        </div>

        {/* TABLA DE ROLES */}
        <div style={{ backgroundColor: 'white', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                <th style={{ padding: '14px 16px', width: '80px' }}>ID</th>
                <th style={{ padding: '14px 16px' }}>Nombre del Rol</th>
                <th style={{ padding: '14px 16px' }}>Descripción</th>
                <th style={{ padding: '14px 16px', textAlign: 'center', width: '150px' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={4} style={{ padding: '32px', textAlign: 'center' }}>Cargando roles...</td></tr>
              ) : rolesFiltrados.map((rol) => (
                <tr key={rol.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                  <td style={{ padding: '14px 16px', color: '#6b7280' }}>{rol.id}</td>
                  <td style={{ padding: '14px 16px', fontWeight: 600, color: '#1f2937' }}>{rol.nombre}</td>
                  <td style={{ padding: '14px 16px', color: '#4b5563' }}>{rol.descripcion || 'Sin descripción'}</td>
                  <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '6px' }}>
                      <button onClick={() => abrirModalEditar(rol)} title="Editar Rol" style={{ padding: '6px', backgroundColor: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer' }}><Edit size={15} /></button>
                      {verDesactivados ? (
                        <button
                          onClick={async () => {
                            try {
                              // Enviamos un PUT para reactivar el rol
                              await api.put(`/roles/${rol.id}`, {
                                nombre: rol.nombre,
                                descripcion: rol.descripcion,
                                activo: true
                              });
                              mostrarAlerta('Rol restaurado correctamente');
                              cargarRoles(); // Recargamos la lista
                            } catch (error) {
                              mostrarError('Error al restaurar el rol');
                            }
                          }}
                          title="Restaurar"
                          style={{ padding: '6px', backgroundColor: '#d1fae5', border: '1px solid #a7f3d0', borderRadius: '4px', cursor: 'pointer', color: '#065f46' }}
                        >
                          <RotateCcw size={15} />
                        </button>
                      ) : (
                        <button onClick={() => handleDesactivarClick(rol.id, rol.nombre)} title="Desactivar Rol" style={{ padding: '6px', backgroundColor: '#fee2e2', border: '1px solid #fecaca', borderRadius: '4px', cursor: 'pointer' }}><Trash2 size={15} color="#991b1b" /></button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {rolesFiltrados.length === 0 && !cargando && (
                <tr><td colSpan={4} style={{ padding: '32px', textAlign: 'center' }}>No hay roles en esta sección.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* MODAL CREAR / EDITAR ROL */}
      {modalFormAbierto && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '400px' }}>
            <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px', display: 'flex', justifyContent: 'space-between' }}>
              <h3 style={{ margin: 0 }}>{modoEdicion ? 'Editar Rol' : 'Crear Nuevo Rol'}</h3>
              <button onClick={() => setModalFormAbierto(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
            </div>

            <form onSubmit={handleSubmitForm} autoComplete="off" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Nombre del Rol *</label>
                <input type="text" required value={formNombre} onChange={(e) => setFormNombre(e.target.value)} placeholder="Ej. Auditor" style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Descripción *</label>
                <textarea required value={formDescripcion} onChange={(e) => setFormDescripcion(e.target.value)} placeholder="¿Qué puede hacer este rol?" rows={3} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box', resize: 'vertical' }} />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button type="button" onClick={() => setModalFormAbierto(false)} style={{ padding: '10px 16px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}>Cancelar</button>
                <button type="button" onClick={(e) => {
                  const form = e.currentTarget.closest('form');
                  if (form && form.checkValidity()) {
                    handleSubmitForm(e as any);
                  } else if (form) { form.reportValidity(); }
                }} style={{ padding: '10px 16px', border: 'none', backgroundColor: '#8C1D40', color: 'white', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}>Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL CONFIRMACIÓN CREAR / EDITAR / DESACTIVAR */}
      {accionPendiente && (
        <ModalConfirmacion
          titulo={accionPendiente.tipo === 'crear' ? "Confirmar Creación" : accionPendiente.tipo === 'editar' ? "Confirmar Edición" : "Desactivar Rol"}
          mensaje={accionPendiente.tipo === 'crear' ? `¿Está seguro de crear el rol "${accionPendiente.nombre}"?` : accionPendiente.tipo === 'editar' ? `¿Está seguro de guardar los cambios realizados al rol "${accionPendiente.nombre}"?` : `¿Está seguro de desactivar el rol "${accionPendiente.nombre}"? Los usuarios con este rol podrían perder acceso.`}
          onConfirmar={accionPendiente.tipo === 'desactivar' ? ejecutarDesactivar : ejecutarGuardado}
          onCancelar={() => setAccionPendiente(null)}
        />
      )}

    </div>
  );
};