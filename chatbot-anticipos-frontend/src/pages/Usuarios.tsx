import React, { useState, useEffect } from 'react';
import { Search, UserPlus, Edit, Key, X, ChevronLeft, ChevronRight, CheckCircle, UserX, RotateCcw, Users, UserMinus, Shield, Eye, Unlock, Lock } from 'lucide-react';
import api from '../services/api';
import { ModalConfirmacion } from '../components/ModalConfirmacion';

interface Usuario {
  usuarioId: number;
  cedula: string;
  nombre: string;
  correo: string;
  rolNombre: string;
  bloqueado: boolean;
  telegramChatId?: string;
  jefeInmediatoId?: number;
  jefeNombre?: string;
  telefono?: string;
  cargo?: string;
}

export const Usuarios = () => {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [cargando, setCargando] = useState(true);
  const [busqueda, setBusqueda] = useState('');
  const [filtroRol, setFiltroRol] = useState('Todos');
  const [paginaActual, setPaginaActual] = useState(1);
  const [filtroTarjeta, setFiltroTarjeta] = useState<'todos' | 'activos' | 'bloqueados'>('todos');

  // Modales
  const [modalUsuarioAbierto, setModalUsuarioAbierto] = useState(false);
  const [modoEdicion, setModoEdicion] = useState(false);
  const [usuarioActualId, setUsuarioActualId] = useState<number | null>(null);
  const [usuarioDetalle, setUsuarioDetalle] = useState<Usuario | null>(null);
  const [alertaExito, setAlertaExito] = useState('');
  const [modalConfig, setModalConfig] = useState<{ abierto: boolean, id: number, nombre: string, bloqueado: boolean } | null>(null);
  const [accionPendiente, setAccionPendiente] = useState<{ tipo: 'crear' | 'editar' | 'eliminar', id?: number, nombre?: string } | null>(null);

  // Formulario
  const [formCedula, setFormCedula] = useState('');
  const [formNombre, setFormNombre] = useState('');
  const [formCorreo, setFormCorreo] = useState('');
  const [formTelefono, setFormTelefono] = useState('');
  const [formRol, setFormRol] = useState('Funcionario');
  const [formCargo, setFormCargo] = useState('DOCENTE');
  const [formContrasena, setFormContrasena] = useState('');
  const [formJefeInmediatoId, setFormJefeInmediatoId] = useState<string>('');

  const elementosPorPagina = 5;

  useEffect(() => {
    cargarUsuarios();
  }, []);

  const cargarUsuarios = async () => {
    try {
      setCargando(true);
      const response = await api.get('/usuarios');
      setUsuarios(response.data);
    } catch (error) {
      console.error("Error al cargar usuarios:", error);
      alert("No se pudieron cargar los usuarios. Verifica tu sesión.");
    } finally {
      setCargando(false);
    }
  };

  const handleBusqueda = (e: React.ChangeEvent<HTMLInputElement>) => {
    setBusqueda(e.target.value);
    setPaginaActual(1);
  };

  // FILTRADO CORREGIDO: Oculta a los bloqueados si estás en "todos" o "activos"
  const usuariosFiltrados = usuarios.filter((u) => {
    // Filtro por tarjeta
    if (filtroTarjeta === 'bloqueados' && !u.bloqueado) return false;
    if ((filtroTarjeta === 'todos' || filtroTarjeta === 'activos') && u.bloqueado) return false;

    // Filtro por texto y rol
    const coincideTexto = u.nombre.toLowerCase().includes(busqueda.toLowerCase()) ||
      u.correo.toLowerCase().includes(busqueda.toLowerCase()) ||
      u.cedula.includes(busqueda);
    const coincideRol = filtroRol === 'Todos' || u.rolNombre === filtroRol;
    return coincideTexto && coincideRol;
  });

  // Paginación
  const indiceUltimo = paginaActual * elementosPorPagina;
  const indicePrimero = indiceUltimo - elementosPorPagina;
  const usuariosPaginados = usuariosFiltrados.slice(indicePrimero, indiceUltimo);
  const totalPaginas = Math.ceil(usuariosFiltrados.length / elementosPorPagina) || 1;

  const posiblesJefes = usuarios.filter(u => u.rolNombre === 'Jefe Inmediato');

  // --- Lógica de Formulario ---
  const abrirModalCrear = () => {
    setModoEdicion(false);
    setFormCedula('');
    setFormNombre('');
    setFormCorreo('');
    setFormTelefono('');
    setFormRol('Funcionario');
    setFormCargo('DOCENTE');
    setFormContrasena('');
    setFormJefeInmediatoId('');
    setModalUsuarioAbierto(true);
  };

  const abrirModalEditar = (u: Usuario) => {
    setModoEdicion(true);
    setUsuarioActualId(u.usuarioId);
    setFormCedula(u.cedula);
    setFormNombre(u.nombre);
    setFormCorreo(u.correo);
    setFormTelefono(u.telefono || '');
    setFormRol(u.rolNombre);
    setFormCargo(u.cargo || 'DOCENTE');
    setFormJefeInmediatoId(u.jefeInmediatoId ? u.jefeInmediatoId.toString() : '');
    setModalUsuarioAbierto(true);
  };

  const ejecutarGuardado = async () => {
    let rolId = 1;
    if (formRol === 'Jefe Inmediato') rolId = 2;
    if (formRol === 'Director Financiero') rolId = 3;
    if (formRol === 'Contadora') rolId = 4;
    if (formRol === 'Colectora') rolId = 5;
    if (formRol === 'Asistente Contable') rolId = 6;
    if (formRol === 'Administrador') rolId = 7;

    try {
      if (modoEdicion && usuarioActualId !== null) {
        await api.put(`/usuarios/${usuarioActualId}`, {
          cedula: formCedula, nombre: formNombre, correo: formCorreo,
          rolId: rolId, telefono: formTelefono || null,
          cargo: formRol === 'Funcionario' ? formCargo : null,
          jefeInmediatoId: formJefeInmediatoId ? Number(formJefeInmediatoId) : null
        });
        mostrarAlerta('Usuario actualizado correctamente');
      } else {
        await api.post('/usuarios', {
          cedula: formCedula, nombre: formNombre, correo: formCorreo,
          contrasena: formContrasena || '123456', rolId: rolId,
          telefono: formTelefono || null,
          cargo: formRol === 'Funcionario' ? formCargo : null,
          jefeInmediatoId: formJefeInmediatoId ? Number(formJefeInmediatoId) : null
        });
        mostrarAlerta('Usuario creado correctamente');
      }
      setModalUsuarioAbierto(false);
      cargarUsuarios();
    } catch (error: any) {
      alert("Error: " + (error.response?.data?.message || error.message));
    } finally {
      setAccionPendiente(null);
    }
  };

  // --- Lógica de Eliminar y Bloquear ---
  const handleEliminarClick = (id: number, nombre: string) => {
    setAccionPendiente({ tipo: 'eliminar', id, nombre });
  };

  const ejecutarEliminar = async () => {
    if (!accionPendiente || !accionPendiente.id) return;
    try {
      await api.delete(`/usuarios/${accionPendiente.id}`);
      mostrarAlerta('Usuario desactivado correctamente');
      cargarUsuarios();
    } catch (error) {
      alert("Error al desactivar usuario");
    } finally {
      setAccionPendiente(null);
    }
  };

  const confirmarToggleBloqueo = async () => {
    if (!modalConfig) return;
    const { id, bloqueado, nombre } = modalConfig;

    try {
      // Solo enviamos el PATCH con el nuevo estado
      await api.patch(`/usuarios/${id}/bloqueo`, { bloqueado: !bloqueado });

      // Recargamos la lista para ver el cambio reflejado
      await cargarUsuarios();

      mostrarAlerta(`Usuario ${nombre} ${!bloqueado ? 'bloqueado' : 'desbloqueado'} correctamente.`);
    } catch (error: any) {
      alert(`Error al cambiar estado: ` + (error.response?.data?.message || error.message));
    } finally {
      setModalConfig(null);
    }
  };

  const resetearPassword = async (id: number, nombre: string) => {
    const nueva = prompt(`Ingrese la nueva contraseña temporal para ${nombre}:`);
    if (nueva) {
      try {
        await api.patch(`/usuarios/${id}/reset-password`, { nuevaContrasena: nueva });
        mostrarAlerta(`Contraseña restablecida con éxito para ${nombre}.`);
      } catch (error) {
        alert("Error al restablecer contraseña");
      }
    }
  };

  const mostrarAlerta = (msg: string) => {
    setAlertaExito(msg);
    setTimeout(() => setAlertaExito(''), 3000);
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f4f6f9', fontFamily: 'sans-serif', display: 'flex', flexDirection: 'column' }}>

      {alertaExito && (
        <div style={{ position: 'fixed', top: '20px', right: '20px', backgroundColor: '#d1fae5', color: '#065f46', padding: '16px 24px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', zIndex: 10000, fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <CheckCircle size={20} /> {alertaExito}
        </div>
      )}

      <main style={{ padding: '32px 24px', maxWidth: '1400px', width: '100%', margin: '0 auto', boxSizing: 'border-box', flex: 1 }}>

        <div style={{ marginBottom: '24px' }}>
          <h1 style={{ fontSize: '26px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>Administración</h1>
          <p style={{ color: '#6b7280', fontSize: '14px', marginTop: '4px' }}>Bienvenido(a), {localStorage.getItem('nombre')}</p>
        </div>

        {/* TARJETAS DE RESUMEN (Lógica corregida) */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}>

          {/* TARJETA 1: USUARIOS TOTALES (Cuenta a todos, activos y bloqueados) */}
          <div
            onClick={() => { setFiltroTarjeta('todos'); setPaginaActual(1); }}
            style={{ cursor: 'pointer', backgroundColor: filtroTarjeta === 'todos' ? '#f3e8ff' : 'white', padding: '24px', borderRadius: '12px', borderLeft: '6px solid #8C1D40', border: filtroTarjeta === 'todos' ? '1px solid #8C1D40' : '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6b7280', fontSize: '14px', fontWeight: 600 }}>USUARIOS TOTALES <Users size={20} /></div>
            <h2 style={{ fontSize: '36px', margin: '10px 0 0 0', color: '#1f2937' }}>
              {usuarios.length} {/* Aquí contamos absolutamente a todos */}
            </h2>
          </div>

          {/* TARJETA 2: USUARIOS BLOQUEADOS (Cuenta a todos los que tienen bloqueado = true) */}
          <div
            onClick={() => { setFiltroTarjeta('bloqueados'); setPaginaActual(1); }}
            style={{ cursor: 'pointer', backgroundColor: filtroTarjeta === 'bloqueados' ? '#fef2f2' : 'white', padding: '24px', borderRadius: '12px', borderLeft: '6px solid #dc2626', border: filtroTarjeta === 'bloqueados' ? '1px solid #dc2626' : '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6b7280', fontSize: '14px', fontWeight: 600 }}>USUARIOS BLOQUEADOS <UserMinus size={20} color="#dc2626" /></div>
            <h2 style={{ fontSize: '36px', margin: '10px 0 0 0', color: '#dc2626' }}>
              {usuarios.filter(u => u.bloqueado === true).length} {/* Aquí contamos a todos los bloqueados */}
            </h2>
          </div>

          {/* TARJETA 3: USUARIOS ACTIVOS (Cuenta a los no bloqueados) */}
          <div
            onClick={() => { setFiltroTarjeta('activos'); setPaginaActual(1); }}
            style={{ cursor: 'pointer', backgroundColor: filtroTarjeta === 'activos' ? '#ecfdf5' : 'white', padding: '24px', borderRadius: '12px', borderLeft: '6px solid #059669', border: filtroTarjeta === 'activos' ? '1px solid #059669' : '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', color: '#6b7280', fontSize: '14px', fontWeight: 600 }}>USUARIOS ACTIVOS <Shield size={20} color="#059669" /></div>
            <h2 style={{ fontSize: '36px', margin: '10px 0 0 0', color: '#059669' }}>
              {usuarios.filter(u => u.bloqueado === false).length} {/* Aquí contamos a los no bloqueados */}
            </h2>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>
              {filtroTarjeta === 'bloqueados' ? 'Usuarios Bloqueados' : 'Usuarios Activos'}
            </h2>
          </div>
          <button onClick={abrirModalCrear} style={{ backgroundColor: '#8C1D40', color: 'white', border: 'none', padding: '10px 16px', borderRadius: '6px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
            <UserPlus style={{ width: '18px', height: '18px' }} /> Añadir Usuario
          </button>
        </div>

        <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', backgroundColor: 'white', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <span style={{ position: 'absolute', top: '10px', left: '12px', color: '#9ca3af' }}><Search style={{ width: '18px', height: '18px' }} /></span>
            <input type="text" placeholder="Buscar por nombre, correo o cédula..." value={busqueda} onChange={handleBusqueda} style={{ width: '100%', padding: '10px 12px 10px 38px', borderRadius: '6px', border: '1px solid #d1d5db', boxSizing: 'border-box', outline: 'none' }} />
          </div>
          <select value={filtroRol} onChange={(e) => { setFiltroRol(e.target.value); setPaginaActual(1); }} style={{ padding: '10px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none' }}>
            <option value="Todos">Todos los Roles</option>
            <option value="Funcionario">Funcionario</option>
            <option value="Jefe Inmediato">Jefe Inmediato</option>
            <option value="Director Financiero">Director Financiero</option>
            <option value="Contadora">Contadora</option>
            <option value="Colectora">Colectora</option>
            <option value="Asistente Contable">Asistente Contable</option>
            <option value="Administrador">Administrador</option>
          </select>
        </div>

        <div style={{ backgroundColor: 'white', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead style={{ backgroundColor: '#8C1D40', color: 'white' }}>
              <tr>
                <th style={{ padding: '14px 16px' }}>Nombre</th>
                <th style={{ padding: '14px 16px' }}>Correo</th>
                <th style={{ padding: '14px 16px' }}>Teléfono</th>
                <th style={{ padding: '14px 16px' }}>Rol</th>
                <th style={{ padding: '14px 16px' }}>Jefe Inmediato</th>
                <th style={{ padding: '14px 16px' }}>Cargo</th>
                <th style={{ padding: '14px 16px' }}>Estado</th>
                <th style={{ padding: '14px 16px', textAlign: 'center' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={8} style={{ padding: '32px', textAlign: 'center' }}>Cargando usuarios...</td></tr>
              ) : usuariosPaginados.length > 0 ? (
                usuariosPaginados.map((u) => (
                  <tr key={u.usuarioId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td onClick={() => setUsuarioDetalle(u)} style={{ padding: '14px 16px', fontWeight: 500, color: '#8C1D40', cursor: 'pointer', textDecoration: 'underline' }}>{u.nombre}</td>
                    <td style={{ padding: '14px 16px', color: '#6b7280' }}>{u.correo}</td>
                    <td style={{ padding: '14px 16px', color: '#6b7280' }}>{u.telefono || '—'}</td>
                    <td style={{ padding: '14px 16px' }}><span style={{ padding: '4px 10px', borderRadius: '12px', backgroundColor: '#e0e7ff', color: '#3730a3', fontSize: '11px' }}>{u.rolNombre}</span></td>
                    <td style={{ padding: '14px 16px', color: '#6b7280' }}>{u.jefeNombre || '—'}</td>
                    <td style={{ padding: '14px 16px', color: '#6b7280' }}>{u.cargo || '—'}</td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{ display: 'inline-flex', gap: '6px', padding: '4px 10px', borderRadius: '12px', backgroundColor: u.bloqueado ? '#fee2e2' : '#d1fae5', color: u.bloqueado ? '#991b1b' : '#065f46', fontSize: '11px' }}>
                        {u.bloqueado ? 'Bloqueado' : 'Activo'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                      <div style={{ display: 'flex', justifyContent: 'center', gap: '6px' }}>
                        <button onClick={() => setUsuarioDetalle(u)} title="Ver Detalle" style={{ padding: '6px', backgroundColor: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer' }}><Eye size={15} /></button>
                        <button onClick={() => abrirModalEditar(u)} title="Editar" style={{ padding: '6px', backgroundColor: '#f3f4f6', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer' }}><Edit size={15} /></button>
                        <button onClick={() => resetearPassword(u.usuarioId, u.nombre)} title="Resetear Contraseña" style={{ padding: '6px', backgroundColor: '#fef3c7', border: '1px solid #fcd34d', borderRadius: '4px', cursor: 'pointer' }}><Key size={15} /></button>
                        <button onClick={() => setModalConfig({ abierto: true, id: u.usuarioId, nombre: u.nombre, bloqueado: u.bloqueado })} title={u.bloqueado ? "Desbloquear" : "Bloquear"} style={{ padding: '6px', backgroundColor: u.bloqueado ? '#d1fae5' : '#fef3c7', border: u.bloqueado ? '1px solid #a7f3d0' : '1px solid #fcd34d', borderRadius: '4px', cursor: 'pointer' }}>
                          {u.bloqueado ? <Unlock size={15} /> : <Lock size={15} />}
                        </button>
                        <button onClick={() => handleEliminarClick(u.usuarioId, u.nombre)} title="Desactivar" style={{ padding: '6px', backgroundColor: '#fee2e2', border: '1px solid #fecaca', borderRadius: '4px', cursor: 'pointer' }}><UserX size={15} /></button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan={8} style={{ padding: '32px', textAlign: 'center' }}>No se encontraron usuarios.</td></tr>
              )}
            </tbody>
          </table>

          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '16px 24px', borderTop: '1px solid #e5e7eb', backgroundColor: '#f9fafb' }}>
            <span style={{ fontSize: '13px', color: '#6b7280' }}>Mostrando {usuariosPaginados.length > 0 ? indicePrimero + 1 : 0} a {Math.min(indiceUltimo, usuariosFiltrados.length)} de {usuariosFiltrados.length} registros</span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button onClick={() => setPaginaActual(p => Math.max(p - 1, 1))} disabled={paginaActual === 1} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: 'white', cursor: paginaActual === 1 ? 'not-allowed' : 'pointer', opacity: paginaActual === 1 ? 0.5 : 1 }}><ChevronLeft size={16} /></button>
              <button onClick={() => setPaginaActual(p => Math.min(p + 1, totalPaginas))} disabled={paginaActual === totalPaginas} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: 'white', cursor: paginaActual === totalPaginas ? 'not-allowed' : 'pointer', opacity: paginaActual === totalPaginas ? 0.5 : 1 }}><ChevronRight size={16} /></button>
            </div>
          </div>
        </div>
      </main>

      {/* MODAL CREAR / EDITAR USUARIO */}
      {modalUsuarioAbierto && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '450px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px', display: 'flex', justifyContent: 'space-between' }}>
              <h3 style={{ margin: 0 }}>{modoEdicion ? 'Editar Usuario' : 'Nuevo Usuario'}</h3>
              <button onClick={() => setModalUsuarioAbierto(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <form autoComplete="off" onSubmit={(e) => e.preventDefault()} style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Cédula / Pasaporte *</label>
                <input type="text" required minLength={8} maxLength={15} pattern="[a-zA-Z0-9]+" title="Ingrese entre 8 y 15 caracteres alfanuméricos (sin espacios)" placeholder="Ej. 1712345678 o A12345678" value={formCedula} onChange={(e) => setFormCedula(e.target.value.toUpperCase().replace(/\s/g, ''))} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Nombre Completo *</label>
                <input type="text" required pattern="^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$" title="Solo letras y espacios" value={formNombre} onChange={(e) => setFormNombre(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Correo Institucional *</label>
                <input type="email" required pattern=".+@iti\.edu\.ec" title="Debe terminar en @iti.edu.ec" placeholder="usuario@iti.edu.ec" value={formCorreo} onChange={(e) => setFormCorreo(e.target.value.toLowerCase())} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Teléfono Celular *</label>
                <input type="tel" required pattern="09[0-9]{8}" title="10 dígitos, empieza con 09" placeholder="0991234567" maxLength={10} value={formTelefono} onChange={(e) => { const soloNumeros = e.target.value.replace(/\D/g, ''); if (soloNumeros.length <= 10) setFormTelefono(soloNumeros); }} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Rol en el Sistema *</label>
                <select value={formRol} onChange={(e) => setFormRol(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }}>
                  <option value="Funcionario">Funcionario</option>
                  <option value="Jefe Inmediato">Jefe Inmediato</option>
                  <option value="Director Financiero">Director Financiero</option>
                  <option value="Contadora">Contadora</option>
                  <option value="Colectora">Colectora</option>
                  <option value="Asistente Contable">Asistente Contable</option>
                  <option value="Administrador">Administrador</option>
                </select>
              </div>
              {formRol === 'Funcionario' && (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Cargo / Función</label>
                    <select value={formCargo} onChange={(e) => setFormCargo(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }}>
                      <option value="DOCENTE">Docente</option>
                      <option value="COORDINADOR DE CARRERA">Coordinador de Carrera</option>
                      <option value="ASISTENTE ADMINISTRATIVO">Asistente Administrativo</option>
                      <option value="CONTADOR">Contador</option>
                      <option value="FUNCIONARIO">Funcionario (Genérico)</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Jefe Inmediato</label>
                    <select value={formJefeInmediatoId} onChange={(e) => setFormJefeInmediatoId(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }}>
                      <option value="">-- Seleccione un Jefe --</option>
                      {posiblesJefes.map(j => <option key={j.usuarioId} value={j.usuarioId}>{j.nombre}</option>)}
                    </select>
                  </div>
                </>
              )}
              {!modoEdicion && (
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Contraseña Inicial *</label>
                  <input type="password" required autoComplete="new-password" value={formContrasena} onChange={(e) => setFormContrasena(e.target.value)} style={{ width: '100%', padding: '10px', border: '1px solid #ccc', borderRadius: '4px', boxSizing: 'border-box' }} />
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button type="button" onClick={() => setModalUsuarioAbierto(false)} style={{ padding: '10px 16px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}>Cancelar</button>
                <button type="button" onClick={(e) => {
                  const form = e.currentTarget.closest('form');
                  if (form && form.checkValidity()) {
                    setAccionPendiente({ tipo: modoEdicion ? 'editar' : 'crear', nombre: formNombre });
                  } else if (form) { form.reportValidity(); }
                }} style={{ padding: '10px 16px', border: 'none', backgroundColor: '#8C1D40', color: 'white', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}>Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL DE DETALLE DE USUARIO */}
      {usuarioDetalle && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: 'white', borderRadius: '8px', width: '100%', maxWidth: '500px' }}>
            <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px', display: 'flex', justifyContent: 'space-between' }}>
              <h3 style={{ margin: 0 }}>Información del Usuario</h3>
              <button onClick={() => setUsuarioDetalle(null)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
            </div>
            <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <p><strong>Nombre:</strong> {usuarioDetalle.nombre}</p>
              <p><strong>Cédula:</strong> {usuarioDetalle.cedula}</p>
              <p><strong>Correo:</strong> {usuarioDetalle.correo}</p>
              <p><strong>Teléfono:</strong> {usuarioDetalle.telefono}</p>
              <p><strong>Rol:</strong> {usuarioDetalle.rolNombre}</p>
              <p><strong>Cargo:</strong> {usuarioDetalle.cargo || 'No asignado'}</p>
              <p><strong>Jefe:</strong> {usuarioDetalle.jefeNombre || 'No asignado'}</p>
              <p><strong>Estado:</strong> {usuarioDetalle.bloqueado ? 'Bloqueado' : 'Activo'}</p>
              <p><strong>Telegram:</strong> {usuarioDetalle.telegramChatId ? 'Vinculado' : 'No vinculado'}</p>
            </div>
          </div>
        </div>
      )}

      {/* MODALES DE CONFIRMACIÓN */}
      {modalConfig && modalConfig.abierto && (
        <ModalConfirmacion
          titulo={modalConfig.bloqueado ? "Desbloquear Usuario" : "Bloquear Usuario"}
          mensaje={`¿Está seguro que desea ${modalConfig.bloqueado ? 'desbloquear' : 'bloquear'} al usuario ${modalConfig.nombre}?`}
          onConfirmar={confirmarToggleBloqueo}
          onCancelar={() => setModalConfig(null)}
        />
      )}

      {accionPendiente && (
        <ModalConfirmacion
          titulo={accionPendiente.tipo === 'crear' ? "Confirmar Creación" : accionPendiente.tipo === 'editar' ? "Confirmar Edición" : "Desactivar Usuario"}
          mensaje={accionPendiente.tipo === 'crear' ? `¿Está seguro de crear al usuario ${accionPendiente.nombre}? Verifique los datos.` : accionPendiente.tipo === 'editar' ? `¿Está seguro de guardar los cambios realizados a ${accionPendiente.nombre}?` : `¿Está seguro de desactivar a ${accionPendiente.nombre}? Su historial se conservará, pero no podrá iniciar sesión.`}
          onConfirmar={accionPendiente.tipo === 'eliminar' ? ejecutarEliminar : ejecutarGuardado}
          onCancelar={() => setAccionPendiente(null)}
        />
      )}
    </div>
  );
};