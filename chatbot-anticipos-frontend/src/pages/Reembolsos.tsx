import { useState, useEffect } from 'react';
import api from '../services/api';

interface Reembolso {
    reembolsoId: number;
    montoSolicitado: number;
    estado: string;
    fechaSolicitud: string;
    observacion: string;
}

export const Reembolsos = () => {
    const [reembolsos, setReembolsos] = useState<Reembolso[]>([]);
    const [cargando, setCargando] = useState(true);

    useEffect(() => {
        const cargarReembolsos = async () => {
            try {
                const res = await api.get('/reembolsos');
                setReembolsos(res.data);
            } catch (error) {
                console.error("Error al cargar reembolsos:", error);
            } finally {
                setCargando(false);
            }
        };
        cargarReembolsos();
    }, []);

    return (
        <div style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto', fontFamily: 'sans-serif' }}>
            <div style={{ marginBottom: '24px' }}>
                <h1 style={{ fontSize: '26px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>Gestión de Reembolsos</h1>
                <p style={{ color: '#6b7280', fontSize: '14px', marginTop: '4px' }}>Solicitudes de reembolso por gastos excedidos.</p>
            </div>

            <div style={{ backgroundColor: 'white', borderRadius: '8px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
                    <thead style={{ backgroundColor: '#f9fafb', borderBottom: '1px solid #e5e7eb', color: '#6b7280' }}>
                        <tr>
                            <th style={{ padding: '14px 16px' }}>ID Reembolso</th>
                            <th style={{ padding: '14px 16px' }}>Monto Solicitado</th>
                            <th style={{ padding: '14px 16px' }}>Fecha Solicitud</th>
                            <th style={{ padding: '14px 16px' }}>Observación</th>
                            <th style={{ padding: '14px 16px' }}>Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        {cargando ? (
                            <tr><td colSpan={5} style={{ padding: '32px', textAlign: 'center' }}>Cargando reembolsos...</td></tr>
                        ) : reembolsos.length > 0 ? (
                            reembolsos.map((r) => (
                                <tr key={r.reembolsoId} style={{ borderBottom: '1px solid #f3f4f6' }}>
                                    <td style={{ padding: '14px 16px', fontWeight: 600, color: '#8C1D40' }}>REB-{r.reembolsoId}</td>
                                    <td style={{ padding: '14px 16px', fontWeight: 600 }}>${r.montoSolicitado}</td>
                                    <td style={{ padding: '14px 16px' }}>{r.fechaSolicitud}</td>
                                    <td style={{ padding: '14px 16px' }}>{r.observacion || 'Sin observación'}</td>
                                    <td style={{ padding: '14px 16px' }}>
                                        <span style={{ padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 500, backgroundColor: r.estado === 'Autorizado' ? '#d1fae5' : '#fef3c7', color: r.estado === 'Autorizado' ? '#065f46' : '#92400e' }}>
                                            {r.estado}
                                        </span>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr><td colSpan={5} style={{ padding: '32px', textAlign: 'center' }}>No hay reembolsos registrados.</td></tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};