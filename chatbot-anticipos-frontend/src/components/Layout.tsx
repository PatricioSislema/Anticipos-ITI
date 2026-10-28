import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LayoutDashboard, Users, FileText, Settings, LogOut, ShieldAlert, Menu, HelpCircle, ShoppingCart, RotateCcw } from 'lucide-react';
import { MenuDesplegable } from './MenuDesplegable';

export const Layout = ({ children }: { children: React.ReactNode }) => {
    const navigate = useNavigate();
    const rol = localStorage.getItem('rol') || 'Funcionario';
    const nombre = localStorage.getItem('nombre') || 'Usuario';
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    const handleLogout = () => {
        localStorage.clear();
        navigate('/login');
    };

    // MENÚ ESTRUCTURADO POR ROLES
    const menuItems = [
        // --- COMUNES (El Administrador ya no tiene Dashboard) ---
        { path: '/dashboard', label: 'Pagina principal', icon: <LayoutDashboard size={20} />, roles: ['Administrador', 'Contadora', 'Colectora', 'Director Financiero', 'Asistente Contable', 'Jefe Inmediato'] },

        // --- ADMINISTRADOR (Su panel principal es Usuarios) ---
        { path: '/usuarios', label: 'Usuarios (Panel Principal)', icon: <Users size={20} />, roles: ['Administrador'] },
        { path: '/roles', label: 'Roles', icon: <ShieldAlert size={20} />, roles: ['Administrador'] },
        { path: '/tipos-tramite', label: 'Tipos Trámite', icon: <Settings size={20} />, roles: ['Administrador'] },

        // --- CONTADORA ---
        {
            label: 'Trámites', icon: <FileText size={20} />, roles: ['Contadora'],
            subItems: [
                { label: 'Historial Completo', path: '/tramites' },
                { label: 'Pendientes de mi Acción', path: '/tramites?filtro=contadora' }
            ]
        },
        {
            label: 'Órdenes y Pagos', icon: <ShoppingCart size={20} />, roles: ['Contadora'],
            subItems: [
                { label: 'Órdenes por Aprobar', path: '/ordenes' },
                { label: 'Registrar Comprobante', path: '/ordenes?filtro=comprobantes' }
            ]
        },
        { path: '/reembolsos', label: 'Reembolsos', icon: <RotateCcw size={20} />, roles: ['Contadora'] },

        // --- DIRECTOR FINANCIERO ---
        {
            label: 'Aprobaciones Pendientes', icon: <FileText size={20} />, roles: ['Director Financiero'],
            subItems: [
                { label: 'Solicitudes de Anticipo', path: '/tramites?filtro=director' },
                { label: 'Órdenes de Transferencia', path: '/ordenes?filtro=director' }
            ]
        },
        { path: '/reembolsos', label: 'Reembolsos', icon: <RotateCcw size={20} />, roles: ['Director Financiero'] },

        // --- COLECTORA ---
        { path: '/ordenes', label: 'Generar Órdenes', icon: <ShoppingCart size={20} />, roles: ['Colectora'] },

        // --- ASISTENTE CONTABLE ---
        { path: '/tramites', label: 'Liquidaciones por Revisar', icon: <FileText size={20} />, roles: ['Asistente Contable'] },

        // --- SOPORTE (TODOS) ---
        { path: '/soporte', label: 'Soporte Técnico', icon: <HelpCircle size={20} />, roles: ['Administrador', 'Contadora', 'Colectora', 'Director Financiero', 'Asistente Contable', 'Jefe Inmediato'] },
    ];

    return (
        <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#f4f6f9', fontFamily: 'sans-serif' }}>

            {/* SIDEBAR IZQUIERDO */}
            <aside style={{ width: isSidebarOpen ? '260px' : '0px', backgroundColor: '#8C1D40', color: 'white', display: 'flex', flexDirection: 'column', transition: 'width 0.3s ease', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                <div style={{ padding: '24px', display: 'flex', alignItems: 'center', gap: '12px', borderBottom: '1px solid #a32a42', minWidth: '260px' }}>
                    <img src="/src/image/logo_iti.png" alt="ITI" style={{ height: '40px', backgroundColor: 'white', padding: '4px', borderRadius: '6px' }} />
                    <span style={{ fontSize: '18px', fontWeight: 'bold' }}>Anticipos ITI</span>
                </div>

                <nav style={{ flex: 1, padding: '16px 0', minWidth: '260px' }}>
                    {menuItems.map((item, index) => (
                        <MenuDesplegable key={index} item={item} rolActual={rol} />
                    ))}
                </nav>

                <div style={{ padding: '20px', borderTop: '1px solid #a32a42', minWidth: '260px' }}>
                    <div style={{ fontSize: '12px', marginBottom: '12px' }}>👤 {nombre} ({rol})</div>
                    <button onClick={handleLogout} style={{ width: '100%', padding: '10px', backgroundColor: '#a32a42', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', display: 'flex', justifyContent: 'center', gap: '8px' }}>
                        <LogOut size={16} /> Cerrar Sesión
                    </button>
                </div>
            </aside>

            {/* CONTENIDO PRINCIPAL */}
            <main style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
                <div style={{ padding: '16px 24px', backgroundColor: 'white', borderBottom: '1px solid #e5e7eb' }}>
                    <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} style={{ background: 'none', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', color: '#4b5563' }}>
                        <Menu size={24} />
                    </button>
                </div>
                {children}
            </main>
        </div>
    );
};