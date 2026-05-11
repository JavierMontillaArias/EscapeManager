from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.booking import QRValidateRequest, QRValidateResponse
from app.services import booking_service

router = APIRouter(prefix="/qr", tags=["Validación QR"])


@router.post("/validate", response_model=QRValidateResponse, status_code=status.HTTP_201_CREATED)
def validate_qr(data: QRValidateRequest, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    """
    Valida el QR y crea la partida si todo es correcto.
    El token es un string UUID (MySQL no tiene tipo UUID nativo).
    """
    try:
        game = booking_service.validate_qr_and_start_game(db, data.token, current_user)
    except LookupError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
    except PermissionError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(e))

    return QRValidateResponse(
        message="Partida iniciada correctamente",
        partida_id=game.id,
        reserva_id=game.reserva_id,
        sala=game.reserva.sala.nombre,
        hora_inicio_real=game.hora_inicio_real.strftime("%H:%M:%S"),
    )