import io
import qrcode
from qrcode.image.styledpil import StyledPilImage
from qrcode.image.styles.moduledrawers.pil import RoundedModuleDrawer
from PIL import Image


def generate_qr_image(token: str) -> bytes:
    """
    Genera una imagen QR con el token y la devuelve como bytes PNG.

    En MySQL el token es un string UUID (String(36)).
    En PostgreSQL era un UUID nativo, pero el contenido del QR es el mismo: el string del UUID.
    Args:
        token: string del qr_token de la reserva.

    Returns:
        bytes: imagen PNG del QR lista para adjuntar al email.
    """

    qr = qrcode.QRCode(version=1, error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=10, border=4)
    qr.add_data(token)
    qr.make(fit=True)

    img: Image.Image = qr.make_image(
        image_factory=StyledPilImage,
        module_drawer=RoundedModuleDrawer(),
        fill_color="black",
        back_color="white"
    )

    buffer = io.BytesIO()
    img.save(buffer, format="PNG")
    buffer.seek(0)
    return buffer.read()

