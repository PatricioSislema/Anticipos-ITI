import React from 'react';
import { Mail, Phone, MessageCircle, FileText } from 'lucide-react';

export const Soporte = () => {
    return (
        <div style={{ padding: '40px', maxWidth: '800px', margin: '0 auto', fontFamily: 'sans-serif' }}>
            <h1 style={{ fontSize: '28px', fontWeight: 'bold', color: '#1f2937', marginBottom: '8px' }}>Soporte Técnico</h1>
            <p style={{ color: '#6b7280', marginBottom: '32px' }}>¿Tienes problemas con el sistema? Estamos para ayudarte.</p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>

                <div style={{ backgroundColor: 'white', padding: '24px', borderRadius: '12px', border: '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                    <Mail size={32} color="#8C1D40" style={{ marginBottom: '16px' }} />
                    <h3 style={{ margin: '0 0 8px 0', color: '#1f2937' }}>Correo Electrónico</h3>
                    <p style={{ color: '#6b7280', fontSize: '14px' }}>soporte@iti.edu.ec</p>
                </div>

                <div style={{ backgroundColor: 'white', padding: '24px', borderRadius: '12px', border: '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                    <Phone size={32} color="#8C1D40" style={{ marginBottom: '16px' }} />
                    <h3 style={{ margin: '0 0 8px 0', color: '#1f2937' }}>Teléfono</h3>
                    <p style={{ color: '#6b7280', fontSize: '14px' }}>(02) 123-4567</p>
                </div>

                <div style={{ backgroundColor: 'white', padding: '24px', borderRadius: '12px', border: '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                    <MessageCircle size={32} color="#8C1D40" style={{ marginBottom: '16px' }} />
                    <h3 style={{ margin: '0 0 8px 0', color: '#1f2937' }}>Chatbot de Telegram</h3>
                    <p style={{ color: '#6b7280', fontSize: '14px' }}>Busca al bot oficial en Telegram para ayuda inmediata.</p>
                </div>

                <div style={{ backgroundColor: 'white', padding: '24px', borderRadius: '12px', border: '1px solid #e5e7eb', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
                    <FileText size={32} color="#8C1D40" style={{ marginBottom: '16px' }} />
                    <h3 style={{ margin: '0 0 8px 0', color: '#1f2937' }}>Manual de Usuario</h3>
                    <p style={{ color: '#6b7280', fontSize: '14px' }}>Consulta la documentación para resolver dudas frecuentes.</p>
                </div>

            </div>
        </div>
    );
};