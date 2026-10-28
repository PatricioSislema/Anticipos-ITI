import React from 'react';
import { X } from 'lucide-react';

interface ModalPdfProps {
    isOpen: boolean;
    onClose: () => void;
}

export const ModalPdf = ({ isOpen, onClose }: ModalPdfProps) => {
    if (!isOpen) return null;

    return (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '16px' }}>
            <div style={{ backgroundColor: 'white', borderRadius: '12px', maxWidth: '450px', width: '100%', overflow: 'hidden' }}>
                <div style={{ backgroundColor: '#8C1D40', color: 'white', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>Documentos del Trámite</h3>
                    <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={20} /></button>
                </div>
                <div style={{ padding: '24px', fontSize: '14px', color: '#374151', lineHeight: '1.6' }}>
                    <p style={{ marginTop: 0 }}><strong>Documentos asociados:</strong></p>
                    <ul style={{ paddingLeft: '20px', marginBottom: '20px', color: '#4b5563' }}>
                        <li>Formulario de Solicitud / Liquidación</li>
                        <li>Comprobantes y Facturas (si aplica)</li>
                    </ul>
                    <div style={{ backgroundColor: '#fef3c7', borderLeft: '4px solid #d97706', padding: '12px 16px', borderRadius: '0 8px 8px 0', color: '#92400e' }}>
                        <strong>⚠️ Archivo no disponible para descarga</strong>
                        <p style={{ margin: '8px 0 0 0', fontSize: '13px' }}>
                            Para evitar costos elevados de infraestructura en este prototipo, los PDFs no se almacenan en la base de datos.
                            <br /><br />
                            <strong>Mejora futura:</strong> En producción se implementará <strong>Azure Blob Storage</strong> para permitir la descarga desde el Chatbot y la Web.
                        </p>
                    </div>
                </div>
                <div style={{ padding: '16px 24px', backgroundColor: '#f9fafb', borderTop: '1px solid #e5e7eb', display: 'flex', justifyContent: 'flex-end' }}>
                    <button onClick={onClose} style={{ padding: '8px 16px', backgroundColor: '#8C1D40', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600 }}>Entendido</button>
                </div>
            </div>
        </div>
    );
};