package ec.edu.iti.anticipos.chatbot.service;

import ec.edu.iti.anticipos.chatbot.dto.request.LoginRequestDTO;
import ec.edu.iti.anticipos.chatbot.dto.response.LoginResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el servicio de Usuario.
 * Verifica el login con credenciales correctas e incorrectas.
 */
@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void loginConCredencialesCorrectas() {
        // Preparar
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo("kurtsislema@hotmail.com");
        dto.setContrasena("87654321");

        // Ejecutar
        LoginResponseDTO response = usuarioService.login(dto);

        // Verificar
        assertTrue(response.isAutenticado());
        assertEquals("Javier Sandoval", response.getNombre());
        assertEquals("Funcionario", response.getRolNombre());
    }

    @Test
    void loginConCredencialesIncorrectas() {
        // Preparar: usuario que NO existe en BD
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo("noexiste@iti.edu.ec");
        dto.setContrasena("claveincorrecta");

        // Ejecutar
        LoginResponseDTO response = usuarioService.login(dto);

        // Verificar
        assertFalse(response.isAutenticado());
    }
}