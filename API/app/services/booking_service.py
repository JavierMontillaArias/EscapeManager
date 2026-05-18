import uuid
import logging
from datetime import datetime, timezone, date, time, timedelta
from typing import Optional
from zoneinfo import ZoneInfo

from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session, joinedload

from app.config import settings
from app.models.booking import Booking, EstadoReserva
from app.models.room import Room
from app.models.game import Game
from app.models.user import User
from app.schemas.booking import BookingCreate, BookingUpdate
from app.utils.qr_generator import generate_qr_image
from app.utils.email_sender import send_booking_confirmation

logger = logging.getLogger(__name__)


def get_all_bookings(
    db: Session,
    fecha: Optional[date] = None,
    sala_id: Optional[int] = None,
    estado: Optional[EstadoReserva] = None,
    skip: int = 0,
    limit: int = 50,
) -> list[Booking]:
    # P-01: joinedload evita N+1 queries al acceder a booking.sala en BookingResponse
    # C-2: skip/limit para paginación y evitar respuestas ilimitadas
    query = db.query(Booking).options(joinedload(Booking.sala))
    if fecha is not None:
        query = query.filter(Booking.fecha == fecha)
    if sala_id is not None:
        query = query.filter(Booking.sala_id == sala_id)
    if estado is not None:
        query = query.filter(Booking.estado == estado)
    return query.order_by(Booking.fecha.desc(), Booking.hora_inicio).offset(skip).limit(limit).all()


def get_booking_by_id(db: Session, booking_id: int) -> Optional[Booking]:
    # P-01: joinedload para evitar lazy load de sala al serializar BookingResponse
    return (
        db.query(Booking)
        .options(joinedload(Booking.sala))
        .filter(Booking.id == booking_id)
        .first()
    )


def _check_time_overlap(
    db: Session,
    sala_id: int,
    fecha: date,
    hora_inicio: time,
    hora_fin: time,
    exclude_booking_id: Optional[int] = None,
) -> bool:
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
    5. Genera imagen QR y envía email; actualiza qr_enviado si tiene éxito
    """
    # BUG-01: validar también que hora_inicio no haya pasado cuando la fecha es hoy
    tz = ZoneInfo(settings.TIMEZONE)
    now_local = datetime.now(tz)
    today = now_local.date()
    if data.fecha < today:
        raise ValueError("No se pueden crear reservas con fecha pasada")
    if data.fecha == today and data.hora_inicio <= now_local.time():
        raise ValueError("La hora de inicio ya ha pasado para el día de hoy")

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
        qr_enviado=False,
    )
    try:
        db.add(booking)
        db.commit()
        db.refresh(booking)
    except SQLAlchemyError:
        db.rollback()
        raise

    # PR-02: Enviar email y marcar qr_enviado si tiene éxito
    try:
        qr_bytes = generate_qr_image(booking.qr_token)
        enviado = send_booking_confirmation(
            email=booking.email_cliente,
            nombre_grupo=booking.nombre_grupo,
            fecha=booking.fecha.strftime("%d/%m/%Y"),
            hora_inicio=booking.hora_inicio.strftime("%H:%M"),
            hora_fin=booking.hora_fin.strftime("%H:%M"),
            sala=room.nombre,
            qr_bytes=qr_bytes,
        )
        if enviado:
            booking.qr_enviado = True
            db.commit()
    except Exception:
        logger.exception("Error al generar QR o enviar email para reserva %d", booking.id)

    return booking


def update_booking(db: Session, booking_id: int, data: BookingUpdate) -> Optional[Booking]:
    booking = (
        db.query(Booking)
        .options(joinedload(Booking.sala))
        .filter(Booking.id == booking_id)
        .first()
    )
    if booking is None:
        return None

    if booking.estado in (EstadoReserva.cancelada, EstadoReserva.completada):
        raise ValueError(f"No se puede modificar una reserva en estado '{booking.estado}'")

    update_data = data.model_dump(exclude_unset=True)
    new_estado = update_data.get("estado")

    if new_estado == EstadoReserva.en_curso:
        raise ValueError(
            "No se puede marcar una reserva como 'en_curso' directamente. "
            "El estado se asigna automáticamente al escanear el código QR."
        )

    if new_estado == EstadoReserva.cancelada and booking.estado == EstadoReserva.en_curso:
        raise ValueError("No se puede cancelar una reserva en_curso. Cierra la partida primero.")

    if new_estado == EstadoReserva.completada:
        game = db.query(Game).filter(Game.reserva_id == booking.id, Game.hora_fin_real.isnot(None)).first()
        if game is None:
            raise ValueError("No se puede marcar como completada: no existe partida cerrada asociada")

    new_sala_id = update_data.get("sala_id", booking.sala_id)
    new_fecha = update_data.get("fecha", booking.fecha)
    new_hora_inicio = update_data.get("hora_inicio", booking.hora_inicio)
    new_hora_fin = update_data.get("hora_fin", booking.hora_fin)

    horario_changed = any(
        key in update_data for key in ("sala_id", "fecha", "hora_inicio", "hora_fin")
    )

    if horario_changed:
        # BUG-01: validar también que hora_inicio no haya pasado cuando la fecha es hoy
        tz = ZoneInfo(settings.TIMEZONE)
        now_local = datetime.now(tz)
        today = now_local.date()
        if new_fecha < today:
            raise ValueError("No se puede mover una reserva a una fecha pasada")
        if new_fecha == today and new_hora_inicio <= now_local.time():
            raise ValueError("La hora de inicio ya ha pasado para el día de hoy")
        if new_hora_fin <= new_hora_inicio:
            raise ValueError("hora_fin debe ser posterior a hora_inicio")
        if _check_time_overlap(
            db, new_sala_id, new_fecha, new_hora_inicio, new_hora_fin,
            exclude_booking_id=booking_id,
        ):
            raise ValueError("El nuevo horario genera conflicto con otra reserva")

    # PR-06: Validar capacidad si se cambia num_personas o sala_id
    if "num_personas" in update_data or "sala_id" in update_data:
        new_num = update_data.get("num_personas", booking.num_personas)
        room = db.query(Room).filter(Room.id == new_sala_id).first()
        if room and new_num > room.capacidad_max:
            raise ValueError(
                f"El número de personas ({new_num}) supera la "
                f"capacidad máxima ({room.capacidad_max})"
            )

    for field, value in update_data.items():
        setattr(booking, field, value)

    try:
        db.commit()
        db.refresh(booking)
    except SQLAlchemyError:
        db.rollback()
        raise

    return booking


def cancel_booking(db: Session, booking_id: int) -> Optional[Booking]:
    booking = (
        db.query(Booking)
        .filter(Booking.id == booking_id)
        .first()
    )
    if booking is None:
        return None

    if booking.estado == EstadoReserva.cancelada:
        raise ValueError("La reserva ya está cancelada")

    if booking.estado in (EstadoReserva.en_curso, EstadoReserva.completada):
        raise ValueError(f"No se puede cancelar una reserva en estado '{booking.estado}'")

    booking.estado = EstadoReserva.cancelada
    try:
        db.commit()
        db.refresh(booking)
    except SQLAlchemyError:
        db.rollback()
        raise

    return booking


def resend_qr(db: Session, booking_id: int) -> bool:
    """
    PR-02: Reenvía el email con el QR para una reserva existente.
    Returns True si el envío fue exitoso, False si falló.
    """
    booking = (
        db.query(Booking)
        .options(joinedload(Booking.sala))
        .filter(Booking.id == booking_id)
        .first()
    )
    if booking is None:
        raise LookupError(f"Reserva {booking_id} no encontrada")

    if booking.estado == EstadoReserva.cancelada:
        raise ValueError("No se puede reenviar el QR de una reserva cancelada")

    # SEC-02: no reenviar QR ya utilizado — el cliente recibiría un código que el sistema rechazará
    if booking.qr_usado:
        raise ValueError("El código QR de esta reserva ya fue utilizado y no puede reenviarse")

    # PROD-01: verificar SMTP antes de generar la imagen (evita coste de CPU innecesario)
    if not settings.SMTP_USER or not settings.SMTP_PASSWORD:
        raise RuntimeError("El servidor de email no está configurado")

    try:
        qr_bytes = generate_qr_image(booking.qr_token)
        enviado = send_booking_confirmation(
            email=booking.email_cliente,
            nombre_grupo=booking.nombre_grupo,
            fecha=booking.fecha.strftime("%d/%m/%Y"),
            hora_inicio=booking.hora_inicio.strftime("%H:%M"),
            hora_fin=booking.hora_fin.strftime("%H:%M"),
            sala=booking.sala.nombre,
            qr_bytes=qr_bytes,
        )
        if enviado:
            booking.qr_enviado = True
            db.commit()
        return enviado
    except Exception:
        logger.exception("Error al reenviar QR para reserva %d", booking_id)
        return False


def validate_qr_and_start_game(db: Session, token: str, gamemaster: User) -> Game:
    """
    Validación QR en 6 pasos y creación de partida.
    La comparación es directa: Booking.qr_token == token.
    """
    # Paso 1: Existe la reserva — B-01: joinedload para evitar lazy load posterior
    booking = (
        db.query(Booking)
        .options(joinedload(Booking.sala))
        .filter(Booking.qr_token == token)
        .first()
    )
    if booking is None:
        raise LookupError("No existe ninguna reserva con ese código QR")

    # Paso 2: INC-02 — solo reservas confirmadas pueden iniciar partida
    if booking.estado != EstadoReserva.confirmada:
        raise PermissionError(
            f"El QR solo es válido para reservas confirmadas (estado actual: {booking.estado})"
        )

    # Paso 3: QR no usado
    if booking.qr_usado:
        raise PermissionError("Este código QR ya ha sido utilizado anteriormente")

    # B-05: Usar zona horaria configurable para comparar con horas de reserva (que son locales).
    tz = ZoneInfo(settings.TIMEZONE)

    # Paso 4: Fecha de hoy en la zona horaria del negocio
    today = datetime.now(tz).date()
    if booking.fecha != today:
        raise PermissionError(
            f"Este QR es válido el {booking.fecha.strftime('%d/%m/%Y')}, "
            f"hoy es {today.strftime('%d/%m/%Y')}"
        )

    # Paso 5: Ventana horaria ±15 minutos — comparación en datetime para manejar cruces de medianoche
    now_time = datetime.now(tz).time()
    now_dt = datetime.combine(today, now_time)
    inicio_dt = datetime.combine(today, booking.hora_inicio)
    margin = timedelta(minutes=15)
    ventana_inicio_dt = inicio_dt - margin
    ventana_fin_dt = inicio_dt + margin

    if not (ventana_inicio_dt <= now_dt <= ventana_fin_dt):
        raise PermissionError(
            f"Solo se puede escanear entre {ventana_inicio_dt.strftime('%H:%M')} "
            f"y {ventana_fin_dt.strftime('%H:%M')}"
        )

    # Paso 6: Sala activa y sin partida en curso (booking.sala ya está cargada)
    room = booking.sala
    if not room.activa:
        raise RuntimeError("La sala asociada a esta reserva está inactiva")

    # BUG-06: usar Game (fuente de verdad) en lugar de Booking.estado para detectar
    # partidas activas — hora_fin_real IS NULL significa partida abierta.
    partida_en_curso = (
        db.query(Game)
        .join(Booking, Game.reserva_id == Booking.id)
        .filter(
            Booking.sala_id == room.id,
            Game.hora_fin_real.is_(None),
        )
        .first()
    )
    if partida_en_curso is not None:
        raise RuntimeError(f"La sala '{room.nombre}' ya tiene una partida en curso")

    # Todo OK: iniciar partida
    booking.qr_usado = True
    booking.estado = EstadoReserva.en_curso

    # Almacenar UTC naive en MySQL DateTime
    game = Game(
        reserva_id=booking.id,
        gamemaster_id=gamemaster.id,
        hora_inicio_real=datetime.now(timezone.utc).replace(tzinfo=None),
        pistas_usadas=0,
        escaparon=None,
        hora_fin_real=None,
        observaciones=None,
    )
    try:
        db.add(game)
        db.commit()
        db.refresh(game)
        db.refresh(booking)
    except SQLAlchemyError:
        db.rollback()
        raise

    return game
