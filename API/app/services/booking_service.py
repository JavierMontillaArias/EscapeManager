import uuid
import logging
from datetime import datetime, timezone, date, time, timedelta
from typing import Optional

from sqlalchemy.orm import Session

from app.models.booking import Booking, EstadoReserva
from app.models.room import Room
from app.models.game import Game
from app.models.user import User
from app.schemas.booking import BookingCreate, BookingUpdate
from app.utils.qr_generator import generate_qr_image
from app.utils.email_sender import send_booking_confirmation

logger = logging.getLogger(__name__)


def get_all_bookings(db: Session, fecha: Optional[date] = None, sala_id: Optional[int] = None, estado: Optional[EstadoReserva] = None,) -> list[Booking]:
    query = db.query(Booking)
    if fecha is not None:
        query = query.filter(Booking.fecha == fecha)
    if sala_id is not None:
        query = query.filter(Booking.sala_id == sala_id)
    if estado is not None:
        query = query.filter(Booking.estado == estado)
    return query.order_by(Booking.fecha.desc(), Booking.hora_inicio).all()


def get_booking_by_id(db: Session, booking_id: int) -> Optional[Booking]:
    return db.query(Booking).filter(Booking.id == booking_id).first()


def get_booking_by_qr_token(db: Session, token: str) -> Optional[Booking]:
    return db.query(Booking).filter(Booking.qr_token == token).first()


def _check_time_overlap(db: Session, sala_id: int, fecha: date, hora_inicio: time, hora_fin: time, exclude_booking_id: Optional[int] = None,) -> bool:
    """
    Verifica si existe solapamiento horario para una sala en una fecha.

    Condición de solapamiento:
        hora_inicio_existente < nueva_hora_fin
        AND hora_fin_existente > nueva_hora_inicio

    Returns True si HAY conflicto, False si el horario está libre.
    """
    estados_activos = [
        EstadoReserva.pendiente,
        EstadoReserva.confirmada,
        EstadoReserva.en_curso,
    ]

    query = db.query(Booking).filter(
        Booking.sala_id == sala_id,
        Booking.fecha == fecha,
        Booking.estado.in_(estados_activos),
        Booking.hora_inicio < hora_fin,
        Booking.hora_fin > hora_inicio,
    )

    if exclude_booking_id is not None:
        query = query.filter(Booking.id != exclude_booking_id)

    return query.first() is not None


def create_booking(db: Session, data: BookingCreate) -> Booking:
    """
    Crea una reserva con flujo completo:
    1. Verifica sala activa y capacidad
    2. Verifica disponibilidad horaria
    3. Genera qr_token como string UUID
    4. Persiste la reserva
    5. Genera imagen QR y envía email (no bloquea si falla)
    """
    room = db.query(Room).filter(Room.id == data.sala_id, Room.activa == True).first()
    if room is None:
        raise ValueError(f"La sala {data.sala_id} no existe o no está activa")

    if data.num_personas > room.capacidad_max:
        raise ValueError(
            f"El número de personas ({data.num_personas}) supera la "
            f"capacidad máxima ({room.capacidad_max})"
        )

    if _check_time_overlap(db, data.sala_id, data.fecha, data.hora_inicio, data.hora_fin):
        raise ValueError(f"La sala '{room.nombre}' ya tiene una reserva en ese horario")

    booking = Booking(
        sala_id=data.sala_id,
        nombre_grupo=data.nombre_grupo,
        num_personas=data.num_personas,
        email_cliente=data.email_cliente,
        fecha=data.fecha,
        hora_inicio=data.hora_inicio,
        hora_fin=data.hora_fin,
        estado=EstadoReserva.confirmada,
        qr_token=str(uuid.uuid4()),
        qr_usado=False,
    )
    db.add(booking)
    db.commit()
    db.refresh(booking)

    try:
        qr_bytes = generate_qr_image(booking.qr_token)
        send_booking_confirmation(
            email=booking.email_cliente,
            nombre_grupo=booking.nombre_grupo,
            fecha=booking.fecha.strftime("%d/%m/%Y"),
            hora_inicio=booking.hora_inicio.strftime("%H:%M"),
            hora_fin=booking.hora_fin.strftime("%H:%M"),
            sala=room.nombre,
            qr_bytes=qr_bytes,
        )
    except Exception:
        logger.exception("Error al generar QR o enviar email para reserva %d", booking.id)

    return booking


def update_booking(db: Session, booking_id: int, data: BookingUpdate) -> Optional[Booking]:
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if booking is None:
        return None

    if booking.estado in (EstadoReserva.cancelada, EstadoReserva.completada):
        raise ValueError(f"No se puede modificar una reserva en estado '{booking.estado}'")

    update_data = data.model_dump(exclude_unset=True)

    new_sala_id = update_data.get("sala_id", booking.sala_id)
    new_fecha = update_data.get("fecha", booking.fecha)
    new_hora_inicio = update_data.get("hora_inicio", booking.hora_inicio)
    new_hora_fin = update_data.get("hora_fin", booking.hora_fin)

    horario_changed = any(
        key in update_data for key in ("sala_id", "fecha", "hora_inicio", "hora_fin")
    )

    if horario_changed:
        if new_hora_fin <= new_hora_inicio:
            raise ValueError("hora_fin debe ser posterior a hora_inicio")
        if _check_time_overlap(
            db, new_sala_id, new_fecha, new_hora_inicio, new_hora_fin,
            exclude_booking_id=booking_id,
        ):
            raise ValueError("El nuevo horario genera conflicto con otra reserva")

    for field, value in update_data.items():
        setattr(booking, field, value)

    db.commit()
    db.refresh(booking)
    return booking


def cancel_booking(db: Session, booking_id: int) -> Optional[Booking]:
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if booking is None:
        return None

    if booking.estado == EstadoReserva.cancelada:
        raise ValueError("La reserva ya está cancelada")

    if booking.estado in (EstadoReserva.en_curso, EstadoReserva.completada):
        raise ValueError(f"No se puede cancelar una reserva en estado '{booking.estado}'")

    booking.estado = EstadoReserva.cancelada
    db.commit()
    db.refresh(booking)
    return booking


def validate_qr_and_start_game(db: Session, token: str, gamemaster: User,) -> Game:
    """
    Validación QR en 6 pasos y creación de partida.
    La comparación es directa: Booking.qr_token == token.
    """
    # Paso 1: Existe la reserva
    booking = db.query(Booking).filter(Booking.qr_token == token).first()
    if booking is None:
        raise LookupError("No existe ninguna reserva con ese código QR")

    # Paso 2: No cancelada
    if booking.estado == EstadoReserva.cancelada:
        raise PermissionError("La reserva asociada a este QR está cancelada")

    # Paso 3: QR no usado
    if booking.qr_usado:
        raise PermissionError("Este código QR ya ha sido utilizado anteriormente")

    # Paso 4: Fecha de hoy
    today = datetime.now().date()
    if booking.fecha != today:
        raise PermissionError(
            f"Este QR es válido el {booking.fecha.strftime('%d/%m/%Y')}, "
            f"hoy es {today.strftime('%d/%m/%Y')}"
        )

    # Paso 5: Ventana horaria ±15 minutos
    now_time = datetime.now().time()
    now_dt = datetime.combine(today, now_time)
    inicio_dt = datetime.combine(today, booking.hora_inicio)
    margin = timedelta(minutes=15)
    ventana_inicio = (inicio_dt - margin).time()
    ventana_fin = (inicio_dt + margin).time()

    if not (ventana_inicio <= now_time <= ventana_fin):
        raise PermissionError(
            f"Solo se puede escanear entre {ventana_inicio.strftime('%H:%M')} "
            f"y {ventana_fin.strftime('%H:%M')}"
        )

    # Paso 6: Sala activa y sin partida en curso
    room = booking.sala
    if not room.activa:
        raise RuntimeError("La sala asociada a esta reserva está inactiva")

    partida_en_curso = (
        db.query(Booking)
        .filter(
            Booking.sala_id == room.id,
            Booking.estado == EstadoReserva.en_curso,
        )
        .first()
    )
    if partida_en_curso is not None:
        raise RuntimeError(f"La sala '{room.nombre}' ya tiene una partida en curso")

    # Todo OK: iniciar partida
    booking.qr_usado = True
    booking.estado = EstadoReserva.en_curso

    game = Game(
        reserva_id=booking.id,
        gamemaster_id=gamemaster.id,
        hora_inicio_real=datetime.now(),
        pistas_usadas=0,
        escaparon=None,
        hora_fin_real=None,
        observaciones=None,
    )
    db.add(game)
    db.commit()
    db.refresh(game)
    db.refresh(booking)

    return game