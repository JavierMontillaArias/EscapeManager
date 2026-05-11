import smtplib
import logging
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.image import MIMEImage
from email.mime.base import MIMEBase
from email import encoders

from app.config import settings

logger = logging.getLogger(__name__)


def _build_html_body(nombre_grupo: str,sala: str,fecha: str,hora_inicio: str,hora_fin: str,) -> str:
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
            .qr-section {{ text-align: center; padding: 20px; }}
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
                <p>¡Hola, <strong>{nombre_grupo}</strong>!</p>
                <p>Tu reserva ha sido confirmada:</p>
                <div class="info-box">
                    <p><strong>Sala:</strong> {sala}</p>
                    <p><strong>Fecha:</strong> {fecha}</p>
                    <p><strong>Horario:</strong> {hora_inicio} – {hora_fin}</p>
                </div>
                <div class="qr-section">
                    <p><strong>Tu código QR de acceso:</strong></p>
                    <img src="cid:qr_code" alt="Código QR" width="200" height="200"/>
                    <p style="color: #888; font-size: 12px;">
                        Presenta este código al Game Master al llegar.<br>
                        El código es de un solo uso.
                    </p>
                </div>
                <div class="footer">
                    <p>EscapeManager © 2025</p>
                </div>
            </div>
        </div>
    </body>
    </html>
    """


def send_booking_confirmation(email: str,nombre_grupo: str,fecha: str,hora_inicio: str,hora_fin: str,sala: str,qr_bytes: bytes,) -> bool:
    """
    Envía el email de confirmación con el QR adjunto e inline.

    Returns:
        True si se envió correctamente, False si hubo error.
        Los errores se loggean pero no se propagan para no bloquear
        la creación de la reserva.
    """
    if not settings.SMTP_USER or not settings.SMTP_PASSWORD:
        logger.warning("SMTP no configurado. Email no enviado para %s", email)
        return False

    try:
        msg = MIMEMultipart("related")
        msg["Subject"] = f"Confirmación de reserva — {sala} — {fecha}"
        msg["From"] = settings.SMTP_FROM or settings.SMTP_USER
        msg["To"] = email

        msg_alternative = MIMEMultipart("alternative")
        msg.attach(msg_alternative)

        text_plain = (
            f"Confirmación de reserva\n\n"
            f"Grupo: {nombre_grupo}\nSala: {sala}\n"
            f"Fecha: {fecha}\nHorario: {hora_inicio} – {hora_fin}\n\n"
            f"Adjunto encontrarás tu código QR de acceso."
        )
        msg_alternative.attach(MIMEText(text_plain, "plain", "utf-8"))
        msg_alternative.attach(
            MIMEText(
                _build_html_body(nombre_grupo, sala, fecha, hora_inicio, hora_fin),
                "html",
                "utf-8",
            )
        )

        # QR embebido inline en el HTML
        qr_image = MIMEImage(qr_bytes, _subtype="png")
        qr_image.add_header("Content-ID", "<qr_code>")
        qr_image.add_header("Content-Disposition", "inline", filename="qr_acceso.png")
        msg.attach(qr_image)

        # QR también como adjunto descargable
        qr_attachment = MIMEBase("image", "png")
        qr_attachment.set_payload(qr_bytes)
        encoders.encode_base64(qr_attachment)
        qr_attachment.add_header("Content-Disposition", "attachment", filename="qr_acceso.png")
        msg.attach(qr_attachment)

        with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT) as server:
            server.ehlo()
            server.starttls()
            server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            server.sendmail(
                from_addr=settings.SMTP_FROM or settings.SMTP_USER,
                to_addrs=[email],
                msg=msg.as_string(),
            )

        logger.info("Email enviado a %s", email)
        return True

    except smtplib.SMTPAuthenticationError:
        logger.error("Error de autenticación SMTP.")
        return False
    except smtplib.SMTPException as e:
        logger.error("Error SMTP: %s", str(e))
        return False
    except Exception as e:
        logger.error("Error inesperado al enviar email: %s", str(e))
        return False