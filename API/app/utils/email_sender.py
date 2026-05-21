import html
import logging
import base64
from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import (
    Mail, Attachment, FileContent, FileName,
    FileType, Disposition, ContentId
)

from app.config import settings

logger = logging.getLogger(__name__)


def _mask_email(email: str) -> str:
    parts = email.split("@")
    return f"{parts[0][:2]}***@{parts[1]}" if len(parts) == 2 else "***"


def _build_html_body(nombre_grupo: str, sala: str, fecha: str, hora_inicio: str, hora_fin: str) -> str:
    nombre_grupo_safe = html.escape(nombre_grupo)
    sala_safe = html.escape(sala)
    fecha_safe = html.escape(fecha)
    hora_inicio_safe = html.escape(hora_inicio)
    hora_fin_safe = html.escape(hora_fin)

    return f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <style>
            body {{ font-family: Arial, sans-serif; color: #333; }}
            .container {{ max-width: 600px; margin: auto; padding: 20px; }}
            .header {{ background: #1a1a2e; color: white; padding: 20px;
                       text-align: center; border-radius: 8px 8px 0 0; }}
            .content {{ background: #f9f9f9; padding: 20px;
                        border-radius: 0 0 8px 8px; }}
            .info-box {{ background: white; border-left: 4px solid #e94560;
                         padding: 15px; margin: 15px 0; border-radius: 4px; }}
            .qr-section {{ text-align: center; padding: 20px; background: white;
                           border-radius: 8px; margin-top: 15px; }}
            .footer {{ color: #888; font-size: 12px; text-align: center; margin-top: 20px; }}
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h1>EscapeManager</h1>
                <p>Confirmación de Reserva</p>
            </div>
            <div class="content">
                <p>¡Hola, <strong>{nombre_grupo_safe}</strong>!</p>
                <p>Tu reserva ha sido confirmada:</p>
                <div class="info-box">
                    <p><strong>Sala:</strong> {sala_safe}</p>
                    <p><strong>Fecha:</strong> {fecha_safe}</p>
                    <p><strong>Horario:</strong> {hora_inicio_safe} – {hora_fin_safe}</p>
                </div>
                <div class="qr-section">
                    <p><strong>Tu código QR de acceso:</strong></p>
                    <p style="font-size: 16px;">📎 El código QR está adjunto en este email como archivo PNG.</p>
                    <p style="color: #888; font-size: 12px;">
                        Descarga el adjunto y preséntalo al Game Master al llegar.<br>
                        El código es de un solo uso.
                    </p>
                </div>
                <div class="footer">
                    <p>EscapeManager © {__import__('datetime').datetime.now().year}</p>
                </div>
            </div>
        </div>
    </body>
    </html>
    """


def send_booking_confirmation(
    email: str,
    nombre_grupo: str,
    fecha: str,
    hora_inicio: str,
    hora_fin: str,
    sala: str,
    qr_bytes: bytes,
) -> bool:
    if not settings.SENDGRID_API_KEY:
        logger.warning("SendGrid no configurado. Email no enviado para %s", _mask_email(email))
        return False

    try:
        message = Mail(
            from_email=settings.SMTP_FROM or settings.SMTP_USER,
            to_emails=email,
            subject=f"Confirmación de reserva — {sala} — {fecha}",
            html_content=_build_html_body(nombre_grupo, sala, fecha, hora_inicio, hora_fin),
        )

        # QR como adjunto descargable
        encoded_qr = base64.b64encode(qr_bytes).decode()
        attachment = Attachment(
            FileContent(encoded_qr),
            FileName("qr_acceso.png"),
            FileType("image/png"),
            Disposition("attachment"),
            ContentId("qr_code")
        )
        message.attachment = attachment

        sg = SendGridAPIClient(settings.SENDGRID_API_KEY)
        response = sg.send(message)

        if response.status_code in (200, 202):
            logger.info("Email enviado a %s", _mask_email(email))
            return True
        else:
            logger.error("SendGrid respondió con status %d", response.status_code)
            return False

    except Exception as e:
        logger.error("Error inesperado al enviar email: %s", str(e))
        return False