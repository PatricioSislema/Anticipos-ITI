package ec.edu.iti.anticipos.chatbot.service.parser;

import ec.edu.iti.anticipos.chatbot.dto.response.FormularioLiquidacionDTO;
import ec.edu.iti.anticipos.chatbot.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Parser de Liquidación que utiliza Inteligencia Artificial (Ollama)
 * para leer el texto crudo de Azure DI y extraer los datos sin importar
 * cómo esté maquetado el PDF.
 */
@Component
@RequiredArgsConstructor
public class LiquidacionParser {

    private final OpenAIService openAIService;

    public FormularioLiquidacionDTO parsear(String texto) {
        System.out.println("=== PARSEANDO LIQUIDACIÓN CON IA ===");

        if (texto == null || texto.isEmpty()) {
            FormularioLiquidacionDTO dto = new FormularioLiquidacionDTO();
            dto.setExtraccionExitosa(false);
            return dto;
        }

        // Le pasamos el texto sucio directamente a la IA
        return openAIService.extraerDatosLiquidacionConIA(texto);
    }
}