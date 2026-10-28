package ec.edu.iti.anticipos.chatbot.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final ChatClient chatClient;

    // ✅ Mapper configurado para leer fechas (LocalDate) y evitar errores
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    // ==========================================
    // 1. CLASIFICADOR DE INTENCIONES
    // ==========================================
    public String clasificarIntencion(String mensaje) {
        if (mensaje != null && mensaje.trim().matches("\\d+")) {
            return "DESCONOCIDA";
        }
        String prompt = """
            Eres un clasificador de intenciones para un chatbot del ITI.
            Analiza el mensaje del usuario y responde SOLO con UNA de estas palabras clave:
            SOLICITAR, LIQUIDAR, CONSULTAR, AYUDA, SALIR, MENU, CANCELAR, CONSULTA_GENERAL, DESCONOCIDA.
            Responde SOLO con la palabra clave en MAYÚSCULAS. Sin explicaciones, sin puntos, sin comillas.
            Mensaje: "%s"
            """.formatted(mensaje);

        try {
            String respuesta = chatClient.prompt().user(prompt).call().content()
                    .trim().toUpperCase().replaceAll("[^A-Z_]", "");
            System.out.println("[IA] Intención detectada: " + respuesta);
            return respuesta;
        } catch (Exception e) {
            System.err.println("[IA] Error al clasificar: " + e.getMessage());
            return "DESCONOCIDA";
        }
    }

    // ==========================================
    // 2. RESPUESTA A CONSULTAS GENERALES
    // ==========================================
    public String responderConsultaGeneral(String mensaje) {
        return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                "📌 Soy un asistente especializado en la gestión de anticipos del ITI.\n\n" +
                "Puedo ayudarte con:\n" +
                "📝 Solicitar un anticipo\n" +
                "💰 Liquidar un anticipo\n" +
                "📋 Consultar el estado de tus trámites\n\n" +
                "👉 Usa los botones para continuar.";
    }

    public String interpretarMensaje(String mensaje) {
        return responderConsultaGeneral(mensaje);
    }

    // ==========================================
    // 3. ✅ EXTRACCIÓN DE DATOS CON IA
    // ==========================================
    public FormularioLiquidacionDTO extraerDatosLiquidacionConIA(String textoPdf) {
        System.out.println(">>> IA Extrayendo datos de liquidación...");

        // ✅ PROMPT MEJORADO: Pide fechas en formato YYYY-MM-DD y arregla el anticipo
        String prompt = """
            Eres un asistente experto en extracción de datos de documentos del ITI.
            Te pasaré el texto de un 'FORMATO DE LIQUIDACIÓN DE FONDOS'. 
            Devuelve ÚNICAMENTE un JSON válido, sin markdown, sin explicaciones.

            REGLAS ESTRICTAS:
            1. Si el texto dice "UNIDAD/COORDINACIÓN: Académica", escribe siempre "unidad": "Coordinación Académica".
            2. Si el texto dice "CAMPUS: Norte", escribe siempre "campus": "Campus Norte".
            3. Si el "anticipo" no aparece explícito, usa el valor de "totalGastos".
            4. La fecha siempre en formato YYYY-MM-DD (ej: "2026-10-23").

            Extrae estos campos exactos:
            "solicitante" (string), 
            "ci" (string de 10 dígitos), 
            "unidad" (string), 
            "campus" (string), 
            "fecha" (string en formato yyyy-MM-dd), 
            "proposito" (string), 
            "lugar" (string), 
            "anticipo" (número decimal), 
            "totalGastos" (número decimal), 
            "diferencia" (número decimal).
            
            Texto del PDF:
            %s
            """.formatted(textoPdf);

        try {
            String jsonResponse = chatClient.prompt().user(prompt).call().content();

            // Limpiamos posible markdown (```json ... ```)
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();
            System.out.println(">>> RESPUESTA IA (JSON): " + jsonResponse);

            // Convertimos el JSON a tu DTO
            FormularioLiquidacionDTO dto = objectMapper.readValue(jsonResponse, FormularioLiquidacionDTO.class);

            // ✅ SOLUCIÓN DEFINITIVA: Si la IA no encontró el anticipo, lo igualamos al total de gastos
            if (dto.getAnticipo() == null && dto.getTotalGastos() != null) {
                dto.setAnticipo(dto.getTotalGastos());
            }

            dto.setExtraccionExitosa(true);
            dto.setFirma("firma_registrada");

            return dto;

        } catch (Exception e) {
            System.err.println("Error al extraer con IA: " + e.getMessage());
            FormularioLiquidacionDTO dtoError = new FormularioLiquidacionDTO();
            dtoError.setExtraccionExitosa(false);
            return dtoError;
        }
    }
}