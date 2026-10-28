import React from 'react';

export type ModoFiltros = 'pendientes' | 'ordenes' | 'por_liquidar' | 'historial';

interface FiltrosBarraProps {
    modo: ModoFiltros;
    filtroTipo: string;
    setFiltroTipo: (val: string) => void;
    fechaDesde: string;
    setFechaDesde: (val: string) => void;
    fechaHasta: string;
    setFechaHasta: (val: string) => void;
    filtroEstado: string;
    setFiltroEstado: (val: string) => void;
    busquedaFuncionario: string;
    setBusquedaFuncionario: (val: string) => void;
    limpiarFiltros: () => void;
}

export const FiltrosBarra = ({
    modo,
    filtroTipo, setFiltroTipo,
    fechaDesde, setFechaDesde,
    fechaHasta, setFechaHasta,
    filtroEstado, setFiltroEstado,
    busquedaFuncionario, setBusquedaFuncionario,
    limpiarFiltros,
}: FiltrosBarraProps) => {

    // ✅ FIX: En 'ordenes' solo queremos fechas y funcionario, así que ocultamos Tipo y Estado.
    // En 'por_liquidar' también se ocultan ambos (todas son liquidaciones y se filtran por sub-pestañas).
    const mostrarTipo = modo === 'pendientes' || modo === 'historial';
    const mostrarEstado = modo === 'pendientes' || modo === 'historial';

    return (
        <div style={{ backgroundColor: 'white', padding: '16px', borderRadius: '12px', border: '1px solid #e5e7eb', marginBottom: '16px', display: 'flex', flexWrap: 'wrap', gap: '16px', alignItems: 'flex-end' }}>

            {mostrarTipo && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <label style={{ fontSize: '10px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>Tipo</label>
                    <select value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value)} style={{ padding: '8px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none' }}>
                        <option value="Todas">Todas</option>
                        <option value="Solicitud">Solicitud</option>
                        <option value="Liquidación">Liquidación</option>
                    </select>
                </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '10px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>Desde</label>
                <input type="date" value={fechaDesde} onChange={(e) => setFechaDesde(e.target.value)} style={{ padding: '8px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none' }} />
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <label style={{ fontSize: '10px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>Hasta</label>
                <input type="date" value={fechaHasta} onChange={(e) => setFechaHasta(e.target.value)} style={{ padding: '8px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none' }} />
            </div>

            {mostrarEstado && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <label style={{ fontSize: '10px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>Estado / Etapa</label>
                    <select value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value)} style={{ padding: '8px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none', maxWidth: '280px' }}>
                        <option value="Todos">Todos los estados</option>
                        {modo === 'pendientes' ? (
                            <>
                                <option value="Pendiente">Pendiente</option>
                                <option value="Aprobado por Jefe">Aprobado por Jefe</option>
                                <option value="Aprobado por Director">Aprobado por Director</option>
                                <option value="Aprobado por Contadora">Aprobado por Contadora</option>
                                <option value="Orden generada">Orden generada</option>
                                <option value="Orden aprobada">Orden aprobada</option>
                                <option value="Liquidación en proceso">Liquidación en proceso</option>
                                <option value="Liquidación revisada">Liquidación revisada</option>
                            </>
                        ) : (
                            <>
                                <option value="Liquidación cerrada">Liquidación cerrada</option>
                                <option value="Rechazado">Rechazado</option>
                                <option value="Archivado">Archivado</option>
                                <option value="Devuelto al funcionario">Devuelto al funcionario</option>
                            </>
                        )}
                    </select>
                </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', flex: 1, minWidth: '200px' }}>
                <label style={{ fontSize: '10px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>Funcionario</label>
                <input type="text" placeholder="Nombre..." value={busquedaFuncionario} onChange={(e) => setBusquedaFuncionario(e.target.value)} style={{ padding: '8px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none' }} />
            </div>

            <button onClick={limpiarFiltros} style={{ padding: '8px 16px', backgroundColor: '#e5e7eb', color: '#374151', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600 }}>
                Limpiar
            </button>
        </div>
    );
};