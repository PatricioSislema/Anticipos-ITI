import React from 'react';
import { X } from 'lucide-react';

interface ModalDetalleProps {
    tramite: any | null;
    onClose: () => void;
    traducirEstado: (estado: string, tipo: string) => string;
}

export const ModalDetalleTramite = ({ tramite, onClose, traducirEstado }: ModalDetalleProps) => {
    if (!tramite) return null;

    return (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
            <div style={{ backgroundColor: 'white', borderRadius: '12px', maxWidth: '650px', width: '100%', maxHeight: '90vh', overflowY: 'auto', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>

                {/* Cabecera del Modal */}
                <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTopLeftRadius: '12px', borderTopRightRadius: '12px' }}>
                    <div>
                        <h3 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>Detalle del Trámite</h3>
                        <p style={{ margin: '4px 0 0 0', fontSize: '13px', opacity: 0.9 }}>ID: TRM-{tramite.tramiteId}</p>
                    </div>
                    <button onClick={onClose} style={{ background: 'rgba(255,255,255,0.2)', border: 'none', color: 'white', cursor: 'pointer', borderRadius: '50%', width: '32px', height: '32px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'background 0.2s' }} onMouseOver={(e) => e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.3)'} onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.2)'}>
                        <X size={18} />
                    </button>
                </div>

                <div style={{ padding: '24px' }}>
                    {/* Sección 1: Datos del Funcionario */}
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '24px' }}>
                        <div style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
                            <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: 700, color: '#6b7280', textTransform: 'uppercase' }}>Funcionario</p>
                            <p style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#1f2937' }}>{tramite.nombreUsuario}</p>
                            <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#4b5563' }}>{tramite.correoUsuario}</p>
                        </div>
                        <div style={{ backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
                            <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: 700, color: '#6b7280', textTransform: 'uppercase' }}>Información del Trámite</p>
                            <p style={{ margin: 0, fontSize: '14px', color: '#374151' }}><strong style={{ color: '#1f2937' }}>Tipo:</strong> {tramite.nombreTipoTramite}</p>
                            <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: '#374151' }}><strong style={{ color: '#1f2937' }}>Fecha:</strong> {tramite.fechaCreacion}</p>
                        </div>
                    </div>

                    {/* Sección 2: Datos Financieros (Dinámico) */}
                    <h4 style={{ margin: '0 0 16px 0', fontSize: '16px', color: '#1f2937', borderBottom: '2px solid #f3f4f6', paddingBottom: '8px' }}>Resumen Financiero</h4>

                    {tramite.totalGastado !== null && tramite.totalGastado !== undefined ? (
                        // Es una LIQUIDACIÓN
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                            <div style={{ backgroundColor: '#eff6ff', padding: '16px', borderRadius: '8px', border: '1px solid #bfdbfe', textAlign: 'center' }}>
                                <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: 700, color: '#1e40af', textTransform: 'uppercase' }}>Presupuesto (Anticipo)</p>
                                <p style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', color: '#1e40af' }}>${tramite.presupuesto || '0.00'}</p>
                            </div>
                            <div style={{ backgroundColor: '#fef3c7', padding: '16px', borderRadius: '8px', border: '1px solid #fde68a', textAlign: 'center' }}>
                                <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: 700, color: '#92400e', textTransform: 'uppercase' }}>Total Gastado</p>
                                <p style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', color: '#92400e' }}>${tramite.totalGastado}</p>
                            </div>
                            <div style={{ backgroundColor: Number(tramite.saldo) < 0 ? '#fee2e2' : Number(tramite.saldo) > 0 ? '#d1fae5' : '#f3f4f6', padding: '16px', borderRadius: '8px', border: `1px solid ${Number(tramite.saldo) < 0 ? '#fecaca' : Number(tramite.saldo) > 0 ? '#a7f3d0' : '#e5e7eb'}`, textAlign: 'center' }}>
                                <p style={{ margin: '0 0 4px 0', fontSize: '11px', fontWeight: 700, color: Number(tramite.saldo) < 0 ? '#991b1b' : Number(tramite.saldo) > 0 ? '#065f46' : '#374151', textTransform: 'uppercase' }}>
                                    {Number(tramite.saldo) < 0 ? 'Faltante (Reembolso)' : Number(tramite.saldo) > 0 ? 'Sobrante (Devolución)' : 'Saldo Exacto'}
                                </p>
                                <p style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', color: Number(tramite.saldo) < 0 ? '#991b1b' : Number(tramite.saldo) > 0 ? '#065f46' : '#374151' }}>
                                    ${Math.abs(Number(tramite.saldo))}
                                </p>
                            </div>
                        </div>
                    ) : (
                        // Es una SOLICITUD
                        <div style={{ backgroundColor: '#eff6ff', padding: '20px', borderRadius: '8px', border: '1px solid #bfdbfe', textAlign: 'center' }}>
                            <p style={{ margin: '0 0 4px 0', fontSize: '12px', fontWeight: 700, color: '#1e40af', textTransform: 'uppercase' }}>Presupuesto Solicitado</p>
                            <p style={{ margin: 0, fontSize: '28px', fontWeight: 'bold', color: '#1e40af' }}>${tramite.presupuesto || '0.00'}</p>
                            {tramite.proposito && <p style={{ margin: '8px 0 0 0', fontSize: '13px', color: '#1e40af' }}><strong>Propósito:</strong> {tramite.proposito}</p>}
                        </div>
                    )}

                    {/* Sección 3: Estado Actual */}
                    <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#f9fafb', padding: '16px', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
                        <span style={{ fontSize: '13px', fontWeight: 600, color: '#6b7280', textTransform: 'uppercase' }}>Estado Actual del Trámite:</span>
                        <span style={{ padding: '6px 12px', borderRadius: '16px', fontSize: '13px', fontWeight: 'bold', backgroundColor: tramite.estado.includes('Aprobado') || tramite.estado.includes('cerrada') || tramite.estado === 'Transferido' ? '#d1fae5' : tramite.estado === 'Rechazado' ? '#fee2e2' : '#fef3c7', color: tramite.estado.includes('Aprobado') || tramite.estado.includes('cerrada') || tramite.estado === 'Transferido' ? '#065f46' : tramite.estado === 'Rechazado' ? '#991b1b' : '#92400e' }}>
                            {traducirEstado(tramite.estado, tramite.nombreTipoTramite)}
                        </span>
                    </div>
                </div>

                {/* Footer del Modal */}
                <div style={{ padding: '16px 24px', backgroundColor: '#f9fafb', borderTop: '1px solid #e5e7eb', display: 'flex', justifyContent: 'flex-end', borderBottomLeftRadius: '12px', borderBottomRightRadius: '12px' }}>
                    <button onClick={onClose} style={{ padding: '10px 20px', backgroundColor: '#8C1D40', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600, transition: 'background 0.2s' }} onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#6b1d2f'} onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#8C1D40'}>
                        Cerrar Detalle
                    </button>
                </div>
            </div>
        </div>
    );
};