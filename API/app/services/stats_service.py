from datetime import date, datetime, timezone
from typing import Optional

from sqlalchemy.orm import Session
from sqlalchemy import func, case, cast, Float

from app.models.game import Game
from app.models.booking import Booking, EstadoReserva
from app.models.room import Room
from app.schemas.stats import EscapeRateItem, HintsAvgItem, OccupancyItem, RankingItem, SummaryResponse


def _apply_date_filter(query, date_field, fecha_inicio, fecha_fin):
    if fecha_inicio:
        query = query.filter(date_field >= fecha_inicio)
    if fecha_fin:
        query = query.filter(date_field <= fecha_fin)
    return query


def get_escape_rate(db: Session, fecha_inicio: Optional[date] = None, fecha_fin: Optional[date] = None) -> list[EscapeRateItem]:
    """
    Tasa de escape por sala.
    Solo considera partidas cerradas (hora_fin_real IS NOT NULL).
    """
    escaparon_expr = func.sum(
        case((Game.escaparon == True, 1), else_=0)
    ).label("escapadas")

    total_expr = func.count(Game.id).label("total")

    query = (
        db.query(
            Room.id.label("sala_id"),
            Room.nombre.label("sala_nombre"),
            total_expr,
            escaparon_expr,
        )
        .join(Booking, Game.reserva_id == Booking.id)
        .join(Room, Booking.sala_id == Room.id)
        .filter(Game.hora_fin_real.isnot(None))
    )

    query = _apply_date_filter(query, Booking.fecha, fecha_inicio, fecha_fin)
    rows = query.group_by(Room.id, Room.nombre).all()

    result = []
    for row in rows:
        total = row.total or 0
        escapadas = row.escapadas or 0
        tasa = round(escapadas / total * 100, 2) if total > 0 else 0.0
        result.append(
            EscapeRateItem(
                sala_id=row.sala_id,
                sala_nombre=row.sala_nombre,
                total_partidas=total,
                partidas_escapadas=escapadas,
                tasa_escape=tasa,
            )
        )

    return sorted(result, key=lambda x: x.tasa_escape, reverse=True)


def get_hints_avg(
    db: Session,
    fecha_inicio: Optional[date] = None,
    fecha_fin: Optional[date] = None,
) -> list[HintsAvgItem]:
    avg_expr = func.avg(cast(Game.pistas_usadas, Float)).label("promedio")
    total_expr = func.count(Game.id).label("total")

    query = (
        db.query(
            Room.id.label("sala_id"),
            Room.nombre.label("sala_nombre"),
            total_expr,
            avg_expr,
        )
        .join(Booking, Game.reserva_id == Booking.id)
        .join(Room, Booking.sala_id == Room.id)
        .filter(Game.hora_fin_real.isnot(None))
    )

    query = _apply_date_filter(query, Booking.fecha, fecha_inicio, fecha_fin)
    rows = query.group_by(Room.id, Room.nombre).all()

    return [
        HintsAvgItem(
            sala_id=row.sala_id,
            sala_nombre=row.sala_nombre,
            total_partidas=row.total or 0,
            promedio_pistas=round(float(row.promedio or 0), 2),
        )
        for row in rows
    ]


def get_occupancy(db: Session, fecha_inicio: Optional[date] = None, fecha_fin: Optional[date] = None) -> list[OccupancyItem]:
    """
    Ocupación por franja horaria.

    MySQL: usamos func.hour() en lugar de extract("hour", ...)
    - Mañana:  06:00 – 13:59
    - Tarde:   14:00 – 20:59
    - Noche:   21:00 – 05:59
    """
    estados_validos = [
        EstadoReserva.confirmada,
        EstadoReserva.en_curso,
        EstadoReserva.completada,
    ]

    hora = func.hour(Booking.hora_inicio)

    franja_expr = case(
        (
            (hora >= 6) & (hora < 14),
            "mañana",
        ),
        (
            (hora >= 14) & (hora < 21),
            "tarde",
        ),
        else_="noche",
    ).label("franja")

    query = (
        db.query(
            franja_expr,
            func.count(Booking.id).label("total"),
        )
        .filter(Booking.estado.in_(estados_validos))
    )

    query = _apply_date_filter(query, Booking.fecha, fecha_inicio, fecha_fin)
    rows = query.group_by("franja").all()

    total_global = sum(row.total for row in rows) or 1
    franja_map = {row.franja: row.total for row in rows}
    franjas = ["mañana", "tarde", "noche"]

    return [
        OccupancyItem(
            franja=franja,
            total_reservas=franja_map.get(franja, 0),
            porcentaje=round(franja_map.get(franja, 0) / total_global * 100, 2),
        )
        for franja in franjas
    ]


def get_ranking(db: Session, fecha_inicio: Optional[date] = None, fecha_fin: Optional[date] = None,) -> list[RankingItem]:
    """
    Ranking combinado: 60% tasa escape + 40% eficiencia de pistas.
    """
    escape_rates = get_escape_rate(db, fecha_inicio, fecha_fin)
    hints_avgs = get_hints_avg(db, fecha_inicio, fecha_fin)

    hints_by_sala = {item.sala_id: item.promedio_pistas for item in hints_avgs}
    max_pistas = max(hints_by_sala.values(), default=1) or 1

    ranked = []
    for i, item in enumerate(escape_rates):
        promedio = hints_by_sala.get(item.sala_id, 0.0)
        hints_factor = (1 - promedio / max_pistas) * 40
        score = round((item.tasa_escape * 0.6) + hints_factor, 2)

        ranked.append(
            RankingItem(
                posicion=i + 1,
                sala_id=item.sala_id,
                sala_nombre=item.sala_nombre,
                tasa_escape=item.tasa_escape,
                promedio_pistas=promedio,
                total_partidas=item.total_partidas,
                score=score,
            )
        )

    ranked.sort(key=lambda x: x.score, reverse=True)
    for i, item in enumerate(ranked):
        item.posicion = i + 1

    return ranked


def get_summary(db: Session) -> SummaryResponse:
    today = datetime.now(timezone.utc).date()

    total_reservas = db.query(func.count(Booking.id)).scalar() or 0
    total_partidas = db.query(func.count(Game.id)).scalar() or 0

    partidas_con_resultado = (
        db.query(func.count(Game.id))
        .filter(Game.escaparon.isnot(None))
        .scalar() or 0
    )
    escapadas = (
        db.query(func.count(Game.id))
        .filter(Game.escaparon == True)
        .scalar() or 0
    )
    porcentaje_escape = (
        round(escapadas / partidas_con_resultado * 100, 2)
        if partidas_con_resultado > 0
        else 0.0
    )

    sala_popular_row = (
        db.query(Room.nombre, func.count(Booking.id).label("total"))
        .join(Booking, Room.id == Booking.sala_id)
        .filter(Booking.estado != EstadoReserva.cancelada)
        .group_by(Room.id, Room.nombre)
        .order_by(func.count(Booking.id).desc())
        .first()
    )
    sala_mas_popular = sala_popular_row.nombre if sala_popular_row else None

    reservas_hoy = (
        db.query(func.count(Booking.id))
        .filter(Booking.fecha == today)
        .scalar() or 0
    )

    partidas_en_curso = (
        db.query(func.count(Booking.id))
        .filter(Booking.estado == EstadoReserva.en_curso)
        .scalar() or 0
    )

    return SummaryResponse(
        total_reservas=total_reservas,
        total_partidas=total_partidas,
        porcentaje_escape_global=porcentaje_escape,
        sala_mas_popular=sala_mas_popular,
        reservas_hoy=reservas_hoy,
        partidas_en_curso=partidas_en_curso,
    )