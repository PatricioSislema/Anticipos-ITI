import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Usuarios } from './pages/Usuarios';
import { Roles } from './pages/Roles';
import { TiposTramite } from './pages/TiposTramite';
import { Soporte } from './pages/Soporte';
import { Ordenes } from './pages/Ordenes';
import { Reembolsos } from './pages/Reembolsos';
import { Tramites } from './pages/Tramites';
import { Layout } from './components/Layout';

const RutaProtegida = ({ children, rolesPermitidos }: { children: React.ReactNode, rolesPermitidos: string[] }) => {
  const token = localStorage.getItem('token');
  const rol = localStorage.getItem('rol') || 'Funcionario';
  if (!token) return <Navigate to="/login" replace />;
  if (rolesPermitidos.length > 0 && !rolesPermitidos.includes(rol)) {
    return <div style={{ padding: '50px', textAlign: 'center' }}><h1 style={{ color: '#8C1D40' }}>🚫 Acceso Denegado</h1></div>;
  }
  return <Layout>{children}</Layout>;
};

function App() {
  // Nota: El Administrador ya no está en rolesOficina para que no pueda entrar al Dashboard de trámites
  const rolesOficina = ['Contadora', 'Colectora', 'Director Financiero', 'Asistente Contable', 'Jefe Inmediato'];

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />

        {/* Al Administrador lo sacamos del Dashboard, su panel principal es Usuarios */}
        <Route path="/dashboard" element={
          <RutaProtegida rolesPermitidos={['Administrador', 'Contadora', 'Colectora', 'Director Financiero', 'Asistente Contable', 'Jefe Inmediato']}>
            <Dashboard />
          </RutaProtegida>
        } />

        <Route path="/tramites" element={<RutaProtegida rolesPermitidos={rolesOficina}><Tramites /></RutaProtegida>} />
        <Route path="/ordenes" element={<RutaProtegida rolesPermitidos={rolesOficina}><Ordenes /></RutaProtegida>} />
        <Route path="/reembolsos" element={<RutaProtegida rolesPermitidos={rolesOficina}><Reembolsos /></RutaProtegida>} />

        <Route path="/usuarios" element={<RutaProtegida rolesPermitidos={['Administrador']}><Usuarios /></RutaProtegida>} />
        <Route path="/roles" element={<RutaProtegida rolesPermitidos={['Administrador']}><Roles /></RutaProtegida>} />
        <Route path="/tipos-tramite" element={<RutaProtegida rolesPermitidos={['Administrador']}><TiposTramite /></RutaProtegida>} />

        <Route path="/soporte" element={<RutaProtegida rolesPermitidos={['Administrador', ...rolesOficina]}><Soporte /></RutaProtegida>} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </Router>
  );
}

export default App;