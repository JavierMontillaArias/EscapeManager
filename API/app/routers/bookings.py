from datetime import date
from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import require_manager
from app.limiter import limiter
from app.models.booking import EstadoReserva
from app.models.user import User
from app.schemas.booking import BookingCreate, BookingUpdate, BookingResponse, BookingStatusUpdate, MessageResponse
from app.services import booking_service

router = APIRouter(prefix="/bookings", tags=["Reservas"])


@router.get("", response_model=list[BookingResponse])
def list_bookings(
    fecha: date | None = Query(default=None, alias="date"),
    sala_id: int | None = Query(default=None),
    estado: EstadoReserva | None = Query(default=None),
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=50, ge=1, le=200),
    db: Session = Depends(get_db),
    _: User = Depends(require_manager),
):
    return booking_service.get_all_bookings(db, fecha=fecha, sala_id=sala_id, estado=estado, skip=skip, limit=limit)


@router.get("/{booking_id}", response_model=BookingResponse)
def get_booking(booking_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    booking = booking_service.get_booking_by_id(db, booking_id)
    if booking is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reserva no encontrada")
    return booking


@router.post("", response_model=BookingResponse, status_code=status.HTTP_201_CREATED)
def create_booking(data: BookingCreate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        return booking_service.create_booking(db, data)
    except ValueError as e:
        error_msg = str(e)
        if "ya tiene una reserva" in error_msg:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=error_msg)
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=error_msg)


@router.put("/{booking_id}", response_model=BookingResponse)
def update_booking(booking_id: int, data: BookingUpdate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        booking = booking_service.update_booking(db, booking_id, data)
    except ValueError as e:
        error_msg = str(e)
        if "conflicto" in error_msg.lower():
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=error_msg)
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=error_msg)
    if booking is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reserva no encontrada")
    return booking


@router.delete("/{booking_id}", status_code=status.HTTP_204_NO_CONTENT)
def cancel_booking(booking_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        booking = booking_service.cancel_booking(db, booking_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if booking is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reserva no encontrada")


@router.patch("/{booking_id}/status", response_model=BookingResponse)
def update_booking_status(booking_id: int, data: BookingStatusUpdate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        booking = booking_service.update_booking(db, booking_id, BookingUpdate(estado=data.estado))
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if booking is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Reserva no encontrada")
    return booking


# PR-02: Reenvío manual del email con QR para reservas con qr_enviado=False
@router.post("/{booking_id}/resend-qr", response_model=MessageResponse)
@limiter.limit("5/minute")
def resend_qr(request: Request, booking_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        sent = booking_service.resend_qr(db, booking_id)
    except LookupError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if not sent:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="El email no pudo ser enviado. Inténtalo más tarde.",
        )
    return MessageResponse(message="Email con QR reenviado correctamente")