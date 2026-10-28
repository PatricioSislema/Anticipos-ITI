import React from 'react';
import { AlertTriangle, X } from 'lucide-react';

interface ModalNotificarProps {
    isOpen: boolean;
    nombre: string;
    isEnviando: boolean;
    diasRestantes: number; // <-- NUEVO
    onClose: () => void;
    onConfirm: () => void;
}

export const ModalNotificar = ({ isOpen, nombre, isEnviando, diasRestantes, onClose, onConfirm }: ModalNotificarProps) => {
    if (!isOpen) return null;

    const esVencido = diasRestantes <= 0;

    return (
        <div style={{ /* ... estilos del fondo ... */ }}>
            <div style={{ /* ... estilos del contenedor ... */ }}>

                {/* Cabecera */}
                <div style={{ backgroundColor: esVencido ? '#fef2f2' : '#fffbeb', /* ... resto de estilos ... */ }}>
                    {/* ... icono ... */}
                    <h3 style={{ color: esVencido ? '#991b1b' : '#92400e' }}>
                        {esVencido ? 'Notificación de Vencimiento' : 'Recordatorio de Liquidación'}
                    </h3>
                </div>

                {/* Cuerpo */}
                <div style={{ /* ... */ }}>
                    {/* ... nombre del funcionario ... */}

                    <div style={{ backgroundColor: esVencido ? '#fff1f2' : '#fffbeb', border: `1px solid ${esVencido ? '#fecdd3' : '#fde68a'}`, padding: '16px', borderRadius: '8px', color: esVencido ? '#881337' : '#92400e' }}>
                        {esVencido ? (
                            <>
                                <strong>🚨 Plazo Vencido:</strong> Han pasado más de 5 días desde la acreditación del anticipo.
                                <br /><br />
                                Según las reglas del negocio, <strong>se le descontará el valor total en su próximo rol de pago</strong>.
                            </>
                        ) : (
                            <>
                                <strong>⚠️ Plazo Próximo a Vencer:</strong> Quedan <strong>{diasRestantes} días</strong> para que se cumpla el plazo de 5 días para liquidar el anticipo.
                                <br /><br />
                                Se le recuerda que, de no hacerlo, <strong>se le descontará el valor total en su próximo rol de pago</strong>.
                            </>
                        )}
                    </div>
                </div>

                {/* Footer con botón */}
                <div style={{ /* ... */ }}>
                    {/* ... botones ... */}
                    <button onClick={onConfirm} disabled={isEnviando} style={{ backgroundColor: esVencido ? '#dc2626' : '#d97706', /* ... */ }}>
                        {isEnviando ? 'Enviando...' : 'Enviar Notificación'}
                    </button>
                </div>
            </div>
        </div>
    );
};