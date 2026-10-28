package ec.edu.iti.anticipos.chatbot.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generarComprobante(Long tramiteId, String nombreSolicitante, String monto, String estado) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();
            document.add(new Paragraph("========================================="));
            document.add(new Paragraph("       INSTITUTO TECNOLÓGICO ITI"));
            document.add(new Paragraph("       COMPROBANTE DE TRÁMITE"));
            document.add(new Paragraph("========================================="));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Trámite ID: " + tramiteId));
            document.add(new Paragraph("Solicitante: " + nombreSolicitante));
            document.add(new Paragraph("Monto: " + monto));
            document.add(new Paragraph("Estado: " + estado));
            document.add(new Paragraph("Fecha: " + java.time.LocalDateTime.now()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Este documento es un comprobante digital generado automáticamente."));
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}