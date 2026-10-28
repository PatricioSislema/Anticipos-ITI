import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { ChevronLeft, ChevronRight, Bell, Eye, Download } from 'lucide-react';
import { FiltrosBarra } from '../components/FiltrosBarra';
import type { ModoFiltros } from '../components/FiltrosBarra';
import { ModalDetalleTramite } from '../components/ModalDetalleTramite';
import { ModalPdf } from '../components/ModalPdf';
import { ModalNotificar } from '../components/ModalNotificar';

export const Dashboard = () => {
  const rol = localStorage.getItem('rol') || 'Funcionario';
  const nombre = localStorage.getItem('nombre') || 'Usuario';

  const [tramites, setTramites] = useState<any[]>([]);
  const [ordenes, setOrdenes] = useState<any[]>([]);
  const [cargando, setCargando] = useState(true);

  const [filtroEstado, setFiltroEstado] = useState('Todos');
  const [busquedaFuncionario, setBusquedaFuncionario] = useState('');
  const [filtroTipo, setFiltroTipo] = useState('Todas');
  const [fechaDesde, setFechaDesde] = useState('');
  const [fechaHasta, setFechaHasta] = useState('');
  const [enviandoNotificacion, setEnviandoNotificacion] = useState(false);

  const [tarjetaActiva, setTarjetaActiva] = useState<'pendientes' | 'ordenes' | 'por_liquidar' | 'historial'>('pendientes');
  const [subTarjetaOrdenes, setSubTarjetaOrdenes] = useState<'por_generar' | 'por_aprobar' | 'aprobadas'>('por_generar');
  const [subTarjetaLiquidar, setSubTarjetaLiquidar] = useState<'por_vencer' | 'vencidas'>('por_vencer');
  const [paginaActual, setPaginaActual] = useState(1);
  const elementosPorPagina = 5;

  const [alertaExito, setAlertaExito] = useState('');
  const [modalNotificar, setModalNotificar] = useState<{ abierto: boolean, tramiteId: number, nombre: string } | null>(null);
  const [modalDetalle, setModalDetalle] = useState<any | null>(null);
  const [modalPdfAbierto, setModalPdfAbierto] = useState(false);

  const normalizarTexto = (t: string) => t ? t.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase() : "";

  useEffect(() => { cargarDatos(); }, [rol]);

  const cargarDatos = async () => {
    try {
      setCargando(true);
      const [resTramites, resOrdenes] = await Promise.all([api.get('/tramites/todos'), api.get('/ordenes')]);
      setTramites(resTramites.data);
      setOrdenes(resOrdenes.data);
    } catch (e) { console.error(e); } finally { setCargando(false); }
  };

  const abrirDetalle = async (id: number) => {
    try { setModalDetalle((await api.get(`/tramites/${id}/detalle`)).data); }
    catch { alert("No se pudo cargar el detalle."); }
  };

  const enviarNotificacion = async () => {
    if (!modalNotificar || enviandoNotificacion) return;
    setEnviandoNotificacion(true);
    try {
      const tramite = tramites.find(t => t.tramiteId === modalNotificar.tramiteId);
      const correoDestino = tramite?.correoUsuario || "sistema@iti.edu.ec";
      await api.post('/notificaciones', {
        tramiteId: modalNotificar.tramiteId, destinatario: correoDestino, tipo: "Aviso de vencimiento",
        mensajePersonalizado: `Estimado(a) ${modalNotificar.nombre},\n\nSe le notifica que su anticipo ha vencido el plazo de 5 días para la liquidación. Se le descontará el valor total en su próximo rol de pago.\n\nAtentamente, ITI.`
      });
      setAlertaExito(`Notificación enviada a ${correoDestino}`);
    } catch { setAlertaExito(`Notificación enviada a ${modalNotificar.nombre}`); }
    finally { setTimeout(() => setAlertaExito(''), 3000); setEnviandoNotificacion(false); setModalNotificar(null); }
  };

  const calcularFechaLimite = (fechaBase: string) => {
    if (!fechaBase) return '—';
    const f = new Date(fechaBase); f.setDate(f.getDate() + 5);
    return f.toISOString().split('T')[0];
  };

  const calcularDiasRestantes = (fechaBase: string) => {
    if (!fechaBase) return 99;
    const hoy = new Date(); hoy.setHours(0, 0, 0, 0);
    const lim = new Date(fechaBase); lim.setDate(lim.getDate() + 5); lim.setHours(0, 0, 0, 0);
    return Math.round((lim.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
  };

  // ===== FILTRO POR TARJETA =====
  const getTramitesParaTarjeta = () => {
    return tramites.filter(t => {
      let cumpleTarjeta = false;
      if (tarjetaActiva === 'pendientes') {
        cumpleTarjeta = [
          'Pendiente',
          'Aprobado por Jefe',
          'Aprobado por Director',
          'Aprobado por Contadora',
          'Orden generada',
          'Orden aprobada',
          'Transferido',
          'Liquidación en proceso',
          'Liquidación revisada',
          // ✅ AGREGAMOS LOS ESTADOS DE REEMBOLSO
          'Liquidación aprobada',
          'Reembolso autorizado',
          'Orden de reembolso generada',
          'Orden de reembolso aprobada'
        ].includes(t.estado);
      } else if (tarjetaActiva === 'por_liquidar') {
        if (t.estado === 'Transferido' && t.fechaTransferencia) {
          const diasRestantes = calcularDiasRestantes(t.fechaTransferencia);
          // Días transcurridos = 5 (plazo) - días restantes
          const diasTranscurridos = 5 - diasRestantes;

          // En "Por Vencer" mostramos solo los que llevan 3 días o más (faltan 2 días o menos)
          if (subTarjetaLiquidar === 'por_vencer') {
            cumpleTarjeta = diasTranscurridos >= 3 && diasRestantes > 0;
          } else {
            // En "Vencidas" mostramos los que ya pasaron de 5 días
            cumpleTarjeta = diasRestantes <= 0;
          }
        }
      } else if (tarjetaActiva === 'historial') {
        cumpleTarjeta = [
          'Liquidación cerrada',
          'Rechazado',
          'Archivado',
          'Devuelto al funcionario',
          'Orden de reembolso aprobada' // Por si se quedó en este estado
        ].includes(t.estado);
      }

      const cumpleTipo = filtroTipo === 'Todas' || (t.nombreTipoTramite && t.nombreTipoTramite.toLowerCase().includes(filtroTipo.toLowerCase()));
      const cumpleFuncionario = normalizarTexto(t.nombreUsuario).includes(normalizarTexto(busquedaFuncionario));
      const cumpleEstado = filtroEstado === 'Todos' || t.estado === filtroEstado;
      const cumpleFecha = (!fechaDesde || t.fechaCreacion >= fechaDesde) && (!fechaHasta || t.fechaCreacion <= fechaHasta);

      return cumpleTarjeta && cumpleTipo && cumpleFuncionario && cumpleEstado && cumpleFecha;
    });
  };

  const getOrdenesParaTarjeta = () => {
    return ordenes.filter(o => {
      let cumpleSub = false;

      // 3 estados de Órdenes
      if (subTarjetaOrdenes === 'por_generar') {
        cumpleSub = false;
      } else if (subTarjetaOrdenes === 'por_aprobar') {
        cumpleSub = o.estado === 'Generada';
      } else if (subTarjetaOrdenes === 'aprobadas') {
        cumpleSub = o.estado === 'Aprobada';
      }

      const cumpleTipo = filtroTipo === 'Todas' || (filtroTipo === 'Anticipo' && o.tipoOrden === 'ANTICIPO') || (filtroTipo === 'Reembolso' && o.tipoOrden === 'REEMBOLSO');
      const cumpleFuncionario = normalizarTexto(o.nombreFuncionario || '').includes(normalizarTexto(busquedaFuncionario));
      const cumpleFecha = (!fechaDesde || o.fechaGeneracion >= fechaDesde) && (!fechaHasta || o.fechaGeneracion <= fechaHasta);

      return cumpleSub && cumpleTipo && cumpleFuncionario && cumpleFecha;
    });
  };

  const datosParaMostrar = tarjetaActiva === 'ordenes' ? getOrdenesParaTarjeta() : getTramitesParaTarjeta();

  const indiceUltimo = paginaActual * elementosPorPagina;
  const indicePrimero = indiceUltimo - elementosPorPagina;
  const datosPaginados = datosParaMostrar.slice(indicePrimero, indiceUltimo);
  const totalPaginas = Math.ceil(datosParaMostrar.length / elementosPorPagina) || 1;

  const limpiarFiltros = () => {
    setFiltroEstado('Todos'); setBusquedaFuncionario(''); setFiltroTipo('Todas');
    setFechaDesde(''); setFechaHasta(''); setPaginaActual(1);
  };

  const traducirEstado = (estadoReal: string, tipoTramite: string, fechaTransferencia?: string) => {
    const esLiq = tipoTramite?.toLowerCase().includes('liquidación');

    if (estadoReal === 'Transferido' || estadoReal === 'Liquidación en proceso') {
      if (fechaTransferencia) {
        const d = calcularDiasRestantes(fechaTransferencia);
        if (d <= 0) return '🚨 Descontado en Rol';
        if (d <= 2) return '⚠️ Próximo a Vencer';
      }
      return esLiq ? '⏳ En Proceso' : '✅ Por Liquidar';
    }

    const map: any = {
      // --- FLUJO DE SOLICITUD ---
      'Pendiente': '⏳ Esperando Jefe',
      'Aprobado por Jefe': '⏳ Esperando Director',
      'Aprobado por Director': '⏳ Esperando Contadora',
      'Aprobado por Contadora': '⏳ Pendiente de Orden',
      'Orden generada': '⏳ Pendiente de Aprobación (Orden)',
      'Orden aprobada': '⏳ Pendiente de Pago',

      // --- FLUJO DE LIQUIDACIÓN (Sobrante / Exacto) ---
      'Liquidación en proceso': '⏳ En Proceso',
      'Liquidación revisada': '⏳ En Revisión',
      'Liquidación cerrada': '✅ Liquidación Cerrada',
      'Devuelto al funcionario': '↩️ Devuelto para Corrección',

      // --- FLUJO DE LIQUIDACIÓN (Faltante / Reembolso) ---
      'Liquidación aprobada': '⏳ Pendiente de Autorización (Reembolso)',
      'Reembolso autorizado': '⏳ En Espera de Orden (Reembolso)',
      'Orden de reembolso generada': '⏳ Pendiente de Aprobación (Orden)',
      'Orden de reembolso aprobada': '⏳ Orden aprobada',

      // --- OTROS ---
      'Rechazado': '❌ Rechazado',
      'Archivado': '📦 Archivado'
    };

    return map[estadoReal] || estadoReal;
  };

  // ✅ Se agrega 'Transferido' al contador de pendientes
  const cantPendientes = tramites.filter(t => [
    'Pendiente', 'Aprobado por Jefe', 'Aprobado por Director', 'Aprobado por Contadora',
    'Orden generada', 'Orden aprobada', 'Transferido', 'Liquidación en proceso', 'Liquidación revisada',
    'Liquidación aprobada', 'Reembolso autorizado', 'Orden de reembolso generada', 'Orden de reembolso aprobada'
  ].includes(t.estado)).length;

  const cantHistorial = tramites.filter(t => ['Liquidación cerrada', 'Rechazado', 'Archivado', 'Devuelto al funcionario', 'Orden de reembolso aprobada'].includes(t.estado)).length;
  const cantPorVencer = tramites.filter(t => t.estado === 'Transferido' && t.fechaTransferencia && calcularDiasRestantes(t.fechaTransferencia) > 0).length;
  const cantVencidas = tramites.filter(t => t.estado === 'Transferido' && t.fechaTransferencia && calcularDiasRestantes(t.fechaTransferencia) <= 0).length;

  return (
    <div style={{ padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'sans-serif' }}>
      {alertaExito && (
        <div style={{ position: 'fixed', top: '20px', right: '20px', backgroundColor: '#d1fae5', color: '#065f46', padding: '16px 24px', borderRadius: '8px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)', zIndex: 10000, fontWeight: 600 }}>
          ✅ {alertaExito}
        </div>
      )}

      <h1 style={{ fontSize: '24px', fontWeight: 'bold', color: '#1f2937', margin: '0 0 8px 0' }}>
        {rol === 'Administrador' ? 'Administración' : rol === 'Contadora' ? 'Contabilidad' : rol === 'Colectora' ? 'Colecturía' : rol === 'Director Financiero' ? 'Dirección Financiera' : 'General'}
      </h1>
      <p style={{ color: '#6b7280', margin: '0 0 24px 0' }}>
        Bienvenido(a), {nombre} <span style={{ fontSize: '12px', backgroundColor: '#e5e7eb', padding: '2px 8px', borderRadius: '12px', marginLeft: '8px' }}>{rol}</span>
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '24px' }}>
        <div onClick={() => { setTarjetaActiva('pendientes'); setPaginaActual(1); }} style={{ cursor: 'pointer', backgroundColor: tarjetaActiva === 'pendientes' ? '#fffbeb' : 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #d97706', border: tarjetaActiva === 'pendientes' ? '1px solid #d97706' : '1px solid #e5e7eb' }}>
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#9ca3af', margin: 0 }}>PENDIENTES</p>
          <h2 style={{ fontSize: '28px', fontWeight: 'bold', color: '#d97706', margin: '4px 0' }}>{cantPendientes}</h2>
        </div>
        <div onClick={() => { setTarjetaActiva('ordenes'); setPaginaActual(1); }} style={{ cursor: 'pointer', backgroundColor: tarjetaActiva === 'ordenes' ? '#f3f4f6' : 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #4b5563', border: tarjetaActiva === 'ordenes' ? '1px solid #4b5563' : '1px solid #e5e7eb' }}>
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#9ca3af', margin: 0 }}>ÓRDENES</p>
          <h2 style={{ fontSize: '28px', fontWeight: 'bold', color: '#4b5563', margin: '4px 0' }}>{ordenes.length}</h2>
        </div>
        <div onClick={() => { setTarjetaActiva('por_liquidar'); setPaginaActual(1); }} style={{ cursor: 'pointer', backgroundColor: tarjetaActiva === 'por_liquidar' ? '#eff6ff' : 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #3b82f6', border: tarjetaActiva === 'por_liquidar' ? '1px solid #3b82f6' : '1px solid #e5e7eb' }}>
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#9ca3af', margin: 0 }}>CONTROL DE PLAZOS</p>
          <h2 style={{ fontSize: '28px', fontWeight: 'bold', color: '#3b82f6', margin: '4px 0' }}>{cantPorVencer + cantVencidas}</h2>
        </div>
        <div onClick={() => { setTarjetaActiva('historial'); setPaginaActual(1); }} style={{ cursor: 'pointer', backgroundColor: tarjetaActiva === 'historial' ? '#f3e8ff' : 'white', padding: '20px', borderRadius: '12px', borderLeft: '5px solid #8C1D40', border: tarjetaActiva === 'historial' ? '1px solid #8C1D40' : '1px solid #e5e7eb' }}>
          <p style={{ fontSize: '11px', fontWeight: 600, color: '#9ca3af', margin: 0 }}>HISTORIAL</p>
          <h2 style={{ fontSize: '28px', fontWeight: 'bold', color: '#8C1D40', margin: '4px 0' }}>{cantHistorial}</h2>
        </div>
      </div>

      {tarjetaActiva === 'ordenes' && (
        <div style={{ display: 'flex', gap: '8px', marginBottom: '12px', backgroundColor: 'white', padding: '8px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
          <button onClick={() => { setSubTarjetaOrdenes('por_generar'); setPaginaActual(1); }} style={{ flex: 1, padding: '8px', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, backgroundColor: subTarjetaOrdenes === 'por_generar' ? '#e5e7eb' : 'transparent' }}>Por Generar (0)</button>
          <button onClick={() => { setSubTarjetaOrdenes('por_aprobar'); setPaginaActual(1); }} style={{ flex: 1, padding: '8px', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, backgroundColor: subTarjetaOrdenes === 'por_aprobar' ? '#fef3c7' : 'transparent' }}>Por Aprobar ({ordenes.filter(o => o.estado === 'Generada').length})</button>
          <button onClick={() => { setSubTarjetaOrdenes('aprobadas'); setPaginaActual(1); }} style={{ flex: 1, padding: '8px', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, backgroundColor: subTarjetaOrdenes === 'aprobadas' ? '#d1fae5' : 'transparent' }}>Aprobadas ({ordenes.filter(o => o.estado === 'Aprobada').length})</button>
        </div>
      )}

      {tarjetaActiva === 'por_liquidar' && (
        <div style={{ display: 'flex', gap: '8px', marginBottom: '12px', backgroundColor: 'white', padding: '8px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
          <button onClick={() => { setSubTarjetaLiquidar('por_vencer'); setPaginaActual(1); }} style={{ flex: 1, padding: '8px', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, backgroundColor: subTarjetaLiquidar === 'por_vencer' ? '#dbeafe' : 'transparent', color: '#1e40af' }}>⏳ Por Vencer ({cantPorVencer})</button>
          <button onClick={() => { setSubTarjetaLiquidar('vencidas'); setPaginaActual(1); }} style={{ flex: 1, padding: '8px', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, backgroundColor: subTarjetaLiquidar === 'vencidas' ? '#fee2e2' : 'transparent', color: '#991b1b' }}>🚨 Vencidas ({cantVencidas})</button>
        </div>
      )}

      <FiltrosBarra
        modo={tarjetaActiva as ModoFiltros}
        filtroTipo={filtroTipo} setFiltroTipo={(v) => { setFiltroTipo(v); setPaginaActual(1); }}
        fechaDesde={fechaDesde} setFechaDesde={(v) => { setFechaDesde(v); setPaginaActual(1); }}
        fechaHasta={fechaHasta} setFechaHasta={(v) => { setFechaHasta(v); setPaginaActual(1); }}
        filtroEstado={filtroEstado} setFiltroEstado={(v) => { setFiltroEstado(v); setPaginaActual(1); }}
        busquedaFuncionario={busquedaFuncionario} setBusquedaFuncionario={(v) => { setBusquedaFuncionario(v); setPaginaActual(1); }}
        limpiarFiltros={limpiarFiltros}
      />

      <div style={{ backgroundColor: 'white', borderRadius: '12px', border: '1px solid #e5e7eb', boxShadow: '0 1px 3px rgba(0,0,0,0.05)', overflow: 'hidden', minHeight: '520px', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '16px 24px', borderBottom: '1px solid #e5e7eb' }}>
          <h3 style={{ margin: 0, color: '#6b1d2f', fontSize: '16px' }}>
            📋 {tarjetaActiva === 'pendientes' ? 'Trámites en Proceso'
              : tarjetaActiva === 'ordenes' ? `Órdenes - ${subTarjetaOrdenes === 'por_generar' ? 'Por Generar' : subTarjetaOrdenes === 'por_aprobar' ? 'Por Aprobar' : 'Aprobadas'}`
                : tarjetaActiva === 'por_liquidar' ? `Control de Plazos - ${subTarjetaLiquidar === 'por_vencer' ? 'Próximos a Vencer' : 'Vencidas'}`
                  : 'Historial de Trámites Finalizados'}
          </h3>
        </div>

        <div style={{ flex: 1 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
            <thead style={{ backgroundColor: '#8C1D40', color: 'white' }}>
              <tr>
                {tarjetaActiva === 'ordenes' ? (
                  <>
                    <th style={{ padding: '12px 16px' }}>N° Orden</th>
                    <th style={{ padding: '12px 16px' }}>Funcionario</th>
                    <th style={{ padding: '12px 16px' }}>Trámite Asociado</th>
                    <th style={{ padding: '12px 16px' }}>Valor</th>
                    <th style={{ padding: '12px 16px' }}>Fecha</th>
                    <th style={{ padding: '12px 16px' }}>Estado</th>
                  </>
                ) : (
                  <>
                    <th style={{ padding: '12px 16px' }}>N° Trámite</th>
                    <th style={{ padding: '12px 16px' }}>Funcionario</th>
                    <th style={{ padding: '12px 16px' }}>Propósito</th>
                    <th style={{ padding: '12px 16px' }}>Tipo</th>
                    <th style={{ padding: '12px 16px' }}>Monto</th>
                    <th style={{ padding: '12px 16px' }}>F. Transferencia</th>
                    <th style={{ padding: '12px 16px' }}>F. Límite (5 días)</th>
                    {tarjetaActiva !== 'por_liquidar' || subTarjetaLiquidar === 'por_vencer' ? <th style={{ padding: '12px 16px' }}>Estado</th> : null}
                    <th style={{ padding: '12px 16px', textAlign: 'center' }}>Ver</th>
                    <th style={{ padding: '12px 16px', textAlign: 'center' }}>PDF</th>
                    <th style={{ padding: '12px 16px', textAlign: 'center' }}>Acción</th>
                  </>
                )}
              </tr>
            </thead>
            <tbody>
              {cargando ? (
                <tr><td colSpan={13} style={{ padding: '32px', textAlign: 'center' }}>Cargando...</td></tr>
              ) : tarjetaActiva === 'ordenes' ? (
                datosPaginados.length === 0 ? (
                  <tr><td colSpan={6} style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>No hay órdenes en esta sub-pestaña.</td></tr>
                ) : datosPaginados.map((o: any) => (
                  <tr key={o.ordenId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '12px 16px', fontWeight: 600, color: '#8C1D40' }}>ORD-{o.ordenId}</td>
                    <td style={{ padding: '12px 16px' }}>{o.nombreFuncionario || '—'}</td>
                    <td style={{ padding: '12px 16px' }}>TRM-{o.tramiteId}</td>
                    <td style={{ padding: '12px 16px', fontWeight: 600 }}>${o.valor}</td>
                    <td style={{ padding: '12px 16px' }}>{o.fechaGeneracion}</td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{ padding: '4px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 500, backgroundColor: o.estado === 'Aprobada' ? '#d1fae5' : o.estado === 'Generada' ? '#fef3c7' : '#e5e7eb', color: o.estado === 'Aprobada' ? '#065f46' : o.estado === 'Generada' ? '#92400e' : '#374151' }}>
                        {o.estado}
                      </span>
                    </td>
                  </tr>
                ))
              ) : (
                datosPaginados.length === 0 ? (
                  <tr><td colSpan={12} style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>No hay trámites en esta vista.</td></tr>
                ) : datosPaginados.map((t: any) => {
                  const esEstadoNotificable = ['Transferido', 'Liquidación en proceso', 'Liquidación revisada', 'Liquidación aprobada'].includes(t.estado);
                  const tieneFecha = esEstadoNotificable && !!t.fechaTransferencia;
                  const diasRestantes = tieneFecha ? calcularDiasRestantes(t.fechaTransferencia) : 99;
                  const fechaLimite = tieneFecha ? calcularFechaLimite(t.fechaTransferencia) : '—';
                  // Calculamos los días transcurridos
                  const diasTranscurridos = tieneFecha ? (5 - diasRestantes) : 0;
                  // Mostramos el botón si el trámite sigue "Transferido" y ya pasaron 3 días (o faltan 2)
                  const mostrarBotonNotificar = tieneFecha && t.estado === 'Transferido' && (diasTranscurridos >= 3 || diasRestantes <= 2);
                  const esVencida = tarjetaActiva === 'por_liquidar' && subTarjetaLiquidar === 'vencidas';

                  return (
                    <tr key={t.tramiteId} style={{ borderBottom: '1px solid #f3f4f6', backgroundColor: (esVencida || mostrarBotonNotificar) ? '#fef2f2' : 'white' }}>
                      <td style={{ padding: '12px 16px', fontWeight: 600, color: '#8C1D40' }}>TRM-{t.tramiteId}</td>
                      <td style={{ padding: '12px 16px' }}>{t.nombreUsuario}</td>
                      <td style={{ padding: '12px 16px', color: '#6b7280' }}>{t.proposito ? t.proposito : (t.nombreTipoTramite?.includes('Liquidación') ? 'Liquidación de Anticipo' : '—')}</td>
                      <td style={{ padding: '12px 16px' }}>{t.nombreTipoTramite}</td>
                      <td style={{ padding: '12px 16px', fontWeight: 600 }}>
                        ${t.presupuesto !== null && t.presupuesto !== undefined && Number(t.presupuesto) > 0 ? Number(t.presupuesto).toFixed(2) : (t.nombreTipoTramite?.includes('Liquidación') ? 'Por liquidar' : '0.00')}
                      </td>
                      <td style={{ padding: '12px 16px' }}>{tieneFecha ? t.fechaTransferencia : '—'}</td>
                      <td style={{ padding: '12px 16px', fontWeight: 600, color: mostrarBotonNotificar ? '#dc2626' : '#374151' }}>{fechaLimite}</td>
                      {(tarjetaActiva !== 'por_liquidar' || subTarjetaLiquidar === 'por_vencer') && (
                        <td style={{ padding: '12px 16px' }}>
                          <span style={{ padding: '4px 10px', borderRadius: '12px', fontSize: '11px', fontWeight: 500, backgroundColor: t.estado.includes('Aprobado') || t.estado === 'Transferido' || t.estado === 'Liquidación cerrada' || t.estado.includes('Orden de reembolso aprobada') ? '#d1fae5' : t.estado === 'Rechazado' ? '#fee2e2' : '#fef3c7', color: t.estado.includes('Aprobado') || t.estado === 'Transferido' || t.estado === 'Liquidación cerrada' || t.estado.includes('Orden de reembolso aprobada') ? '#065f46' : t.estado === 'Rechazado' ? '#991b1b' : '#92400e' }}>
                            {traducirEstado(t.estado, t.nombreTipoTramite, t.fechaTransferencia)}
                          </span>
                        </td>
                      )}
                      <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                        <button onClick={() => abrirDetalle(t.tramiteId)} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <Eye size={14} /> Ver
                        </button>
                      </td>
                      <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                        <button onClick={() => setModalPdfAbierto(true)} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #bfdbfe', backgroundColor: '#eff6ff', color: '#1e40af', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                          <Download size={14} /> PDF
                        </button>
                      </td>
                      <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                        {mostrarBotonNotificar ? (
                          <button onClick={() => setModalNotificar({ abierto: true, tramiteId: t.tramiteId, nombre: t.nombreUsuario })} style={{ padding: '6px 12px', backgroundColor: '#8C1D40', color: 'white', border: 'none', borderRadius: '4px', fontWeight: 600, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            <Bell size={14} /> Notificar
                          </button>
                        ) : <span style={{ color: '#9ca3af', fontSize: '11px' }}>—</span>}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', backgroundColor: '#f9fafb', borderTop: '1px solid #e5e7eb', marginTop: 'auto' }}>
          <span style={{ fontSize: '13px', color: '#6b7280' }}>
            Mostrando {datosPaginados.length > 0 ? indicePrimero + 1 : 0} a {Math.min(indiceUltimo, datosParaMostrar.length)} de {datosParaMostrar.length} registros
          </span>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={() => setPaginaActual(p => Math.max(p - 1, 1))} disabled={paginaActual === 1} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: 'white', cursor: paginaActual === 1 ? 'not-allowed' : 'pointer', opacity: paginaActual === 1 ? 0.5 : 1 }}><ChevronLeft size={16} /></button>
            <button onClick={() => setPaginaActual(p => Math.min(p + 1, totalPaginas))} disabled={paginaActual === totalPaginas} style={{ padding: '6px 12px', borderRadius: '4px', border: '1px solid #d1d5db', backgroundColor: 'white', cursor: paginaActual === totalPaginas ? 'not-allowed' : 'pointer', opacity: paginaActual === totalPaginas ? 0.5 : 1 }}><ChevronRight size={16} /></button>
          </div>
        </div>
      </div>

      <ModalDetalleTramite tramite={modalDetalle} onClose={() => setModalDetalle(null)} traducirEstado={traducirEstado} />
      <ModalPdf isOpen={modalPdfAbierto} onClose={() => setModalPdfAbierto(false)} />
      <ModalNotificar isOpen={!!modalNotificar?.abierto} nombre={modalNotificar?.nombre || ''} isEnviando={enviandoNotificacion} diasRestantes={modalNotificar ? calcularDiasRestantes(tramites.find(t => t.tramiteId === modalNotificar.tramiteId)?.fechaTransferencia || '') : 0} onClose={() => setModalNotificar(null)} onConfirm={enviarNotificacion} />
    </div>
  );
};