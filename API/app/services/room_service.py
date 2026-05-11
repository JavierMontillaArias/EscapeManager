from typing import Optional
from sqlalchemy.orm import Session

from app.models.room import Room
from app.schemas.room import RoomCreate, RoomUpdate


def get_all_rooms(db: Session, only_active: bool = False) -> list[Room]:
    query = db.query(Room)
    if only_active:
        query = query.filter(Room.activa == True)
    return query.order_by(Room.id).all()


def get_room_by_id(db: Session, room_id: int) -> Optional[Room]:
    return db.query(Room).filter(Room.id == room_id).first()


def create_room(db: Session, data: RoomCreate) -> Room:
    room = Room(
        nombre=data.nombre,
        tematica=data.tematica,
        capacidad_max=data.capacidad_max,
        dificultad=data.dificultad,
        activa=True,
    )
    db.add(room)
    db.commit()
    db.refresh(room)
    return room


def update_room(db: Session, room_id: int, data: RoomUpdate) -> Optional[Room]:
    room = db.query(Room).filter(Room.id == room_id).first()
    if room is None:
        return None

    update_data = data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(room, field, value)

    db.commit()
    db.refresh(room)
    return room


def deactivate_room(db: Session, room_id: int) -> Optional[Room]:
    room = db.query(Room).filter(Room.id == room_id).first()
    if room is None:
        return None

    from app.models.booking import Booking, EstadoReserva
    active_bookings = (
        db.query(Booking)
        .filter(
            Booking.sala_id == room_id,
            Booking.estado == EstadoReserva.en_curso,
        )
        .count()
    )
    if active_bookings > 0:
        raise ValueError("No se puede desactivar una sala con partidas en curso")

    room.activa = False
    db.commit()
    db.refresh(room)
    return room