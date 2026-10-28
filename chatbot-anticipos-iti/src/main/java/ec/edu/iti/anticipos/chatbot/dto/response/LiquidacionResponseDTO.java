package ec.edu.iti.anticipos.chatbot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LiquidacionResponseDTO {
    private Long liquidacionId;
    private Long tramiteId;

    // ✅ NUEVO CAMPO
    private Long tramiteOriginalId;

    private BigDecimal totalGastado;
    private BigDecimal saldo;
    private LocalDate fechaPresentacion;
    private String tipoCaso;
}