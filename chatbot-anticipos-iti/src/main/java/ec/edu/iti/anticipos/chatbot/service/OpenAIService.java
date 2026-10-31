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

        String mensajeLower = mensaje.toLowerCase().trim();

        // ✅ DETECCIÓN DIRECTA DE PREGUNTAS GENERALES (sin IA)
        if (mensajeLower.contains("capital")
                || mensajeLower.contains("quién")
                || mensajeLower.contains("quien")
                || mensajeLower.contains("qué es")
                || mensajeLower.contains("que es")
                || mensajeLower.contains("cómo ")
                || mensajeLower.contains("como ")
                || mensajeLower.contains("cuál")
                || mensajeLower.contains("cual ")
                || mensajeLower.contains("dónde")
                || mensajeLower.contains("donde")
                || mensajeLower.contains("cuándo")
                || mensajeLower.contains("cuando")
                || mensajeLower.contains("por qué")
                || mensajeLower.contains("porque")
                || mensajeLower.contains("hora")
                || mensajeLower.contains("fecha")
                || mensajeLower.contains("clima")
                || mensajeLower.endsWith("?")) {
            System.out.println("[IA] Detectada pregunta general (sin IA): " + mensaje);
            return "CONSULTA_GENERAL";
        }

        // ✅ DETECCIÓN DIRECTA DE COMANDOS DE ANTICIPOS/LIQUIDACIONES
        if (mensajeLower.contains("solicitar") || mensajeLower.contains("pedir")
                || mensajeLower.contains("anticipo") || mensajeLower.contains("adelanto")
                || mensajeLower.contains("quiero plata") || mensajeLower.contains("necesito plata")) {
            return "SOLICITAR";
        }

        if (mensajeLower.contains("liquidar") || mensajeLower.contains("rendir")
                || mensajeLower.contains("justificar") || mensajeLower.contains("cuentas")) {
            return "LIQUIDAR";
        }

        // ✅ SI NO ES NINGUNA DE LAS ANTERIORES, USAR LA IA
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
    // 2. RESPUESTA A CONSULTAS GENERALES (CON IA)
    // ==========================================
    public String responderConsultaGeneral(String mensaje) {
        String prompt = """
            Eres ITI, el asistente virtual del Instituto Tecnológico Internacional (ITI).
            Tu especialidad es gestionar anticipos y liquidaciones de fondos del ITI.
            
            El usuario te ha hecho esta pregunta: "%s"
            
            REGLAS:
            1. Si la pregunta es sobre anticipos, liquidaciones, trámites o el ITI, responde de forma útil y breve.
            2. Si la pregunta es GENERAL (ej: capital de Ecuador, hora, quién eres, etc.), responde con la respuesta correcta de forma breve (máximo 2 líneas).
            3. Si la pregunta no tiene sentido, responde amablemente.
            4. NO uses Markdown (solo texto plano con emojis).
            5. NUNCA digas "no soy programado para responder preguntas generales". Siempre responde algo útil.
            6. NO agregues recordatorios del ITI en la respuesta (el sistema lo hará automáticamente cada 5 mensajes).
            
            Respuesta:
            """.formatted(mensaje);

        try {
            String respuesta = chatClient.prompt().user(prompt).call().content().trim();
            System.out.println("[IA] Respuesta general: " + respuesta);
            return respuesta;
        } catch (Exception e) {
            System.err.println("[IA] Error al responder consulta general: " + e.getMessage());
            return "🤔 Esa pregunta está fuera de mi alcance.\n\n" +
                    "📌 Soy ITI, tu asistente para gestionar anticipos y liquidaciones de fondos.\n\n" +
                    "👉 Usa los botones para continuar.";
        }
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