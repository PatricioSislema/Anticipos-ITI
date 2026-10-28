import React, { useState, useEffect } from 'react';
import { Search, Eye, X } from 'lucide-react';
import api from '../services/api';

interface Orden {
    ordenId: number;
    tramiteId: number;
    numeroCuenta: string;
    valor: number;
    concepto: string;
    fechaGeneracion: string;
    estado: string;
}

export const Ordenes = () => {
    const [ordenes, setOrdenes] = useState<Orden[]>([]);
    const [cargando, setCargando] = useState(true);
    const [busqueda, setBusqueda] = useState('');

    // Estado para el Modal de Detalle
    const [ordenSeleccionada, setOrdenSeleccionada] = useState<Orden | null>(null);

    useEffect(() => {
        const cargarOrdenes = async () => {
            try {
                const res = await api.get('/ordenes');
                setOrdenes(res.data);
            } catch (error) {
                console.error("Error al cargar órdenes:", error);
            } finally {
                setCargando(false);
            }
        };
        cargarOrdenes();
    }, []);

    const ordenesFiltradas = ordenes.filter((o) =>
        o.ordenId.toString().includes(busqueda) ||
        (o.concepto && o.concepto.toLowerCase().includes(busqueda.toLowerCase()))
    );

    return (
        <div style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto', fontFamily: 'sans-serif' }}>

            {/* ENCABEZADO CORREGIDO */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
                <div>
                    <h1 style={{ fontSize: '26px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>Revisión de Órdenes</h1>
                    <p style={{ color: '#6b7280', fontSize: '14px', marginTop: '4px' }}>Listado de órdenes de transferencia generadas en el sistema.</p>
                </div>
            </div>

            {/* BARRA DE BÚSQUEDA */}
            <div style={{ display: 'flex', gap: '16px', marginBottom: '24px', backgroundColor: 'white', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
                <div style={{ position: 'relative', flex: 1 }}>
                    <span style={{ position: 'absolute', top: '10px', left: '12px', color: '#9ca3af' }}><Search size={18} /></span>
                    <input
                        type="text"
                        placeholder="Buscar por ID de orden o concepto..."
                        value={busqueda}
                        onChange={(e) => setBusqueda(e.target.value)}
                        style={{ width: '100%', padding: '10px 12px 10px 38px', borderRadius: '6px', border: '1px solid #d1d5db', outline: 'none', boxSizing: 'border-box' }}
                    />
                </div>
            </div>

            {/* TABLA DE ÓRDENES */}
            <div style={{ backgroundColor: 'white', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                    <thead style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                        <tr>
                            <th style={{ padding: '14px 16px' }}>N° Orden</th>
                            <th style={{ padding: '14px 16px' }}>Trámite Asociado</th>
                            <th style={{ padding: '14px 16px' }}>Cuenta Destino</th>
                            <th style={{ padding: '14px 16px' }}>Valor</th>
                            <th style={{ padding: '14px 16px' }}>Fecha</th>
                            <th style={{ padding: '14px 16px' }}>Estado</th>
                            <th style={{ padding: '14px 16px', textAlign: 'center' }}>Acción</th>
                        </tr>
                    </thead>
                    <tbody>
                        {cargando ? (
                            <tr><td colSpan={7} style={{ padding: '32px', textAlign: 'center' }}>Cargando órdenes...</td></tr>
                        ) : ordenesFiltradas.length > 0 ? (
                            ordenesFiltradas.map((o) => (
                                <tr key={o.ordenId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                                    <td style={{ padding: '14px 16px', fontWeight: 600, color: '#8C1D40' }}>ORD-{o.ordenId}</td>
                                    <td style={{ padding: '14px 16px' }}>TRM-{o.tramiteId}</td>
                                    <td style={{ padding: '14px 16px' }}>{o.numeroCuenta || 'N/A'}</td>
                                    <td style={{ padding: '14px 16px', fontWeight: 600 }}>${o.valor}</td>
                                    <td style={{ padding: '14px 16px' }}>{o.fechaGeneracion}</td>
                                    <td style={{ padding: '14px 16px' }}>
                                        <span style={{ padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 500, backgroundColor: o.estado === 'Aprobada' || o.estado === 'Transferido' ? '#d1fae5' : '#fef3c7', color: o.estado === 'Aprobada' || o.estado === 'Transferido' ? '#065f46' : '#92400e' }}>
                                            {o.estado}
                                        </span>
                                    </td>
                                    <td style={{ padding: '14px 16px', textAlign: 'center' }}>
                                        <button
                                            onClick={() => setOrdenSeleccionada(o)}
                                            style={{ padding: '6px 12px', borderRadius: '6px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px', fontWeight: 500 }}
                                        >
                                            <Eye size={14} /> Ver Detalle
                                        </button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr><td colSpan={7} style={{ padding: '32px', textAlign: 'center' }}>No hay órdenes registradas.</td></tr>
                        )}
                    </tbody>
                </table>
            </div>

            {/* MODAL DE DETALLE DE LA ORDEN */}
            {ordenSeleccionada && (
                <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
                    <div style={{ backgroundColor: 'white', borderRadius: '12px', maxWidth: '500px', width: '100%', overflow: 'hidden', boxShadow: '0 10px 25px rgba(0,0,0,0.2)' }}>

                        <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>Detalle de la Orden ORD-{ordenSeleccionada.ordenId}</h3>
                            <button onClick={() => setOrdenSeleccionada(null)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
                        </div>

                        <div style={{ padding: '24px', fontSize: '14px', color: '#374151', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                            <div style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
                                <p style={{ margin: '0 0 8px 0' }}><strong>Trámite Asociado:</strong> TRM-{ordenSeleccionada.tramiteId}</p>
                                <p style={{ margin: '0 0 8px 0' }}><strong>Cuenta Destino:</strong> {ordenSeleccionada.numeroCuenta || 'No registrada'}</p>
                                <p style={{ margin: '0 0 8px 0' }}><strong>Valor:</strong> <span style={{ fontWeight: 600, color: '#8C1D40' }}>${ordenSeleccionada.valor}</span></p>
                                <p style={{ margin: '0 0 8px 0' }}><strong>Fecha Generación:</strong> {ordenSeleccionada.fechaGeneracion}</p>
                                <p style={{ margin: 0 }}><strong>Estado Actual:</strong> {ordenSeleccionada.estado}</p>
                            </div>
                            {ordenSeleccionada.concepto && (
                                <div style={{ backgroundColor: '#eff6ff', padding: '16px', borderRadius: '8px', border: '1px solid #bfdbfe' }}>
                                    <p style={{ margin: 0 }}><strong>Concepto:</strong> {ordenSeleccionada.concepto}</p>
                                </div>
                            )}
                        </div>

                        <div style={{ padding: '16px 24px', backgroundColor: '#f9fafb', borderTop: '1px solid #e5e7eb', display: 'flex', justifyContent: 'flex-end' }}>
                            <button onClick={() => setOrdenSeleccionada(null)} style={{ padding: '8px 16px', backgroundColor: '#e5e7eb', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 500 }}>Cerrar</button>
                        </div>
                    </div>
                </div>
            )}

        </div>
    );
};