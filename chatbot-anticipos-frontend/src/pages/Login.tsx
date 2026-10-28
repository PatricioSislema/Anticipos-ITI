import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { Lock, User, CreditCard, ShieldAlert } from 'lucide-react';
// imagenes:
import llamaImg from "../image/iti_bot.jpeg";
import logoIti from "../image/logo_iti.png";


export const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = await api.post('/auth/login', {
        correo: email,
        contrasena: password
      });

      localStorage.setItem('token', response.data.token);
      localStorage.setItem('correo', response.data.correo || email.toLowerCase().trim());
      localStorage.setItem('nombre', response.data.nombre || '');

      const userRol = response.data.rol || 'Funcionario';
      localStorage.setItem('rol', userRol);

      if (userRol === 'Administrador') {
        navigate('/usuarios');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      console.error(err);
      setError('Credenciales inválidas o error de conexión con el servidor.');
    }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f4f6f9', display: 'flex', flexDirection: 'column', fontFamily: 'sans-serif' }}>

      <main style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px' }}>

        {/* CONTENEDOR PRINCIPAL (Tarjeta grande) */}
        <div style={{
          display: 'flex',
          flexDirection: 'row',
          flexWrap: 'wrap',
          maxWidth: '900px',
          width: '100%',
          backgroundColor: 'white',
          borderRadius: '20px',
          boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
          overflow: 'hidden'
        }}>

          {/* COLUMNA IZQUIERDA: Imagen de la Llama y Logo */}
          <div style={{
            flex: '1 1 400px',
            backgroundColor: '#e5e7eb',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '40px',
            position: 'relative'
          }}>
            {/* Logo del ITI arriba a la izquierda */}
            <img src={logoIti} alt="Logo ITI" style={{ position: 'absolute', top: '20px', left: '20px', height: '40px' }} />

            {/* Imagen de la Llama */}
            <img src={llamaImg} alt="Mascota ITI" style={{ width: '100%', maxWidth: '300px', objectFit: 'contain' }} />
          </div>

          {/* COLUMNA DERECHA: Formulario de Login */}
          <div style={{ flex: '1 1 400px', padding: '40px' }}>
            <h2 style={{ margin: '0 0 8px 0', fontSize: '26px', color: '#8C1D40', fontWeight: 'bold' }}>PORTAL ITI</h2>
            <p style={{ margin: '0 0 24px 0', fontSize: '14px', color: '#6b7280' }}>Ingrese sus credenciales institucionales</p>

            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

              {error && (
                <div style={{ backgroundColor: '#fef2f2', borderLeft: '4px solid #ef4444', padding: '12px', borderRadius: '4px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <ShieldAlert style={{ width: '20px', height: '20px', color: '#ef4444', flexShrink: 0 }} />
                  <p style={{ margin: 0, fontSize: '13px', color: '#b91c1c' }}>{error}</p>
                </div>
              )}

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Usuario / Correo</label>
                <div style={{ position: 'relative' }}>
                  <div style={{ position: 'absolute', top: '10px', left: '12px', color: '#9ca3af' }}>
                    <User style={{ width: '18px', height: '18px' }} />
                  </div>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="usuario@iti.edu.ec"
                    style={{ width: '100%', padding: '12px 12px 12px 38px', border: '1px solid #d1d5db', borderRadius: '8px', fontSize: '14px', outline: 'none', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' }}>Contraseña</label>
                <div style={{ position: 'relative' }}>
                  <div style={{ position: 'absolute', top: '10px', left: '12px', color: '#9ca3af' }}>
                    <Lock style={{ width: '18px', height: '18px' }} />
                  </div>
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    style={{ width: '100%', padding: '12px 12px 12px 38px', border: '1px solid #d1d5db', borderRadius: '8px', fontSize: '14px', outline: 'none', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div style={{ marginTop: '10px' }}>
                <button
                  type="submit"
                  style={{ width: '100%', padding: '14px', backgroundColor: '#8C1D40', color: 'white', border: 'none', borderRadius: '8px', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer', transition: 'background 0.2s' }}
                >
                  INGRESAR
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer style={{ backgroundColor: 'white', borderTop: '1px solid #e5e7eb', padding: '16px', textAlign: 'center', fontSize: '12px', color: '#6b7280' }}>
        Instituto Superior Tecnológico Internacional ITI &copy; 2026 — Módulo de Gestión de Anticipos
      </footer>
    </div>
  );
};