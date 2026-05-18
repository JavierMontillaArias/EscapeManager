"""
PR-02: Tests para el filtrado por fecha en get_all_bookings.
Usa SQLite en memoria para no requerir MySQL ni fixtures de red.
"""
from datetime import date, time

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.database import Base
from app.models.booking import Booking, EstadoReserva
from app.models.room import Room, Dificultad
from app.services.booking_service import get_all_bookings


@pytest.fixture
def db():
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
    )
    Base.metadata.create_all(engine)
    Session = sessionmaker(bind=engine)
    session = Session()
    yield session
    session.close()
    Base.metadata.drop_all(engine)


@pytest.fixture
def room(db):
    r = Room(nombre="Sala Test", tematica="Terror", capacidad_max=6, dificultad=Dificultad.media, activa=True)
    db.add(r)
    db.commit()
    db.refresh(r)
    return r


def _make_booking(db, room, fecha, nombre="Grupo", estado=EstadoReserva.confirmada):
    import uuid
    b = Booking(
        sala_id=room.id,
        nombre_grupo=nombre,
        num_personas=2,
        email_cliente="test@test.com",
        fecha=fecha,
        hora_inicio=time(10, 0),
        hora_fin=time(11, 0),
        estado=estado,
        qr_token=str(uuid.uuid4()),
        qr_usado=False,
    )
    db.add(b)
    db.commit()
    return b


def test_get_all_bookings_no_filter(db, room):
    _make_booking(db, room, date(2026, 5, 1))
    _make_booking(db, room, date(2026, 5, 2))
    result = get_all_bookings(db)
    assert len(result) == 2


def test_get_all_bookings_filter_by_date(db, room):
    _make_booking(db, room, date(2026, 5, 1), nombre="Grupo A")
    _make_booking(db, room, date(2026, 5, 2), nombre="Grupo B")
    result = get_all_bookings(db, fecha=date(2026, 5, 1))
    assert len(result) == 1
    assert result[0].nombre_grupo == "Grupo A"


def test_get_all_bookings_filter_by_date_no_match(db, room):
    _make_booking(db, room, date(2026, 5, 1))
    result = get_all_bookings(db, fecha=date(2026, 6, 1))
    assert result == []


def test_get_all_bookings_filter_by_sala(db, room):
    _make_booking(db, room, date(2026, 5, 1))
    result = get_all_bookings(db, sala_id=room.id)
    assert len(result) == 1
    result_other = get_all_bookings(db, sala_id=room.id + 999)
    assert result_other == []


def test_get_all_bookings_filter_by_estado(db, room):
    _make_booking(db, room, date(2026, 5, 1), estado=EstadoReserva.confirmada)
    _make_booking(db, room, date(2026, 5, 2), estado=EstadoReserva.cancelada)
    confirmed = get_all_bookings(db, estado=EstadoReserva.confirmada)
    assert len(confirmed) == 1
    assert confirmed[0].estado == EstadoReserva.confirmada
