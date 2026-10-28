import React from 'react';
import { AlertCircle } from 'lucide-react';

interface Props {
    titulo: string;
    mensaje: string;
    onConfirmar: () => void;
    onCancelar: () => void;
}

export const ModalConfirmacion = ({ titulo, mensaje, onConfirmar, onCancelar }: Props) => {
    return (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
            <div style={{ backgroundColor: 'white', borderRadius: '12px', width: '100%', maxWidth: '400px', padding: '24px', boxShadow: '0 10px 25px rgba(0,0,0,0.2)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px', color: '#8C1D40' }}>
                    <AlertCircle size={28} />
                    <h3 style={{ margin: 0, fontSize: '20px' }}>{titulo}</h3>
                </div>
                <p style={{ color: '#4b5563', fontSize: '15px', marginBottom: '24px', lineHeight: '1.5' }}>{mensaje}</p>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                    <button onClick={onCancelar} style={{ padding: '10px 16px', borderRadius: '6px', border: '1px solid #d1d5db', backgroundColor: '#f3f4f6', cursor: 'pointer', fontWeight: 600, color: '#374151' }}>
                        Cancelar
                    </button>
                    <button onClick={onConfirmar} style={{ padding: '10px 16px', borderRadius: '6px', border: 'none', backgroundColor: '#8C1D40', color: 'white', cursor: 'pointer', fontWeight: 600 }}>
                        Sí, continuar
                    </button>
                </div>
            </div>
        </div>
    );
};