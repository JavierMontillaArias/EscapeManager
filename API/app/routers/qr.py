from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.dependencies import require_gamemaster
from app.limiter import limiter
from app.models.booking import Booking
from app.models.game import Game
from app.models.user import User
from app.schemas.booking import QRValidateRequest, QRValidateResponse
from app.services import booking_service

router = APIRouter(prefix="/qr", tags=["Validación QR"])


# S-01: Rate limit para evitar adivinanza masiva de tokens QR.
# I-1: require_gamemaster — solo un Game Master puede iniciar una partida.
# B-01: Recargamos game con joinedload para garantizar que game.reserva.sala
#       está disponible sin DetachedInstanceError al serializar la respuesta.
@router.post("/validate", response_model=QRValidateResponse, status_code=status.HTTP_201_CREATED)
@limiter.limit("20/minute")
def validate_qr(
    request: Request,
    data: QRValidateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_gamemaster),
):
    """
    Valida el QR y crea la partida si todo es correcto.
    El token es un string UUID (MySQL no tiene tipo UUID nativo).
    Solo accesible para usuarios con rol Game Master.
    """
    try:
        game = booking_service.validate_qr_and_start_game(db, data.token, current_user)
    except LookupError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
    except PermissionError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(e))

    # B-01: Recargar game con relaciones necesarias para evitar lazy load fuera de sesión.
    game = (
        db.query(Game)
        .options(joinedload(Game.reserva).joinedload(Booking.sala))
        .filter(Game.id == game.id)
        .first()
    )

    return QRValidateResponse(
        message="Partida iniciada correctamente",
        game_id=game.id,
        reserva_id=game.reserva_id,
        nombre_grupo=game.reserva.nombre_grupo,
        sala=game.reserva.sala.nombre,
        # I-5: sufijo Z para indicar UTC explícitamente y evitar ambigüedad en el cliente
        hora_inicio_real=game.hora_inicio_real.strftime("%Y-%m-%dT%H:%M:%SZ"),
    )
