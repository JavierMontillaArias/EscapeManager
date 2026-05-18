import time
from datetime import date, datetime, timezone
from typing import Optional
from zoneinfo import ZoneInfo

from sqlalchemy.orm import Session
from sqlalchemy import func, case, cast, Float

from app.config import settings
from app.models.game import Game
from app.models.booking import Booking, EstadoReserva
from app.models.room import Room
from app.models.incident import Incident
from app.schemas.stats import EscapeRateItem, HintsAvgItem, OccupancyItem, RankingItem, SummaryResponse

# PERF-02: TTL reducido en desarrollo para evitar servir datos obsoletos durante pruebas
_SUMMARY_CACHE_TTL = 10.0 if settings.ENVIRONMENT == "development" else 60.0
# CAL-04: caché en memoria por proceso. Con múltiples workers (gunicorn) cada proceso
# tiene su propia copia — no hay estado compartido entre procesos. Para producción
# con varios workers se necesitaría Redis o un TTLCache con lock.
_summary_cache: dict = {"result": None, "ts": 0.0}


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
        .select_from(Game)
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
        .select_from(Game)
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
    rows = query.group_by(franja_expr).all()

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
    P-03: se calcula con una sola query consolidada en lugar de reutilizar
    get_escape_rate/get_hints_avg para evitar duplicar queries cuando el cliente
    llama a los tres endpoints en paralelo.
    """
    escaparon_expr = func.sum(case((Game.escaparon == True, 1), else_=0)).label("escapadas")
    total_expr = func.count(Game.id).label("total")
    avg_expr = func.avg(cast(Game.pistas_usadas, Float)).label("promedio")

    query = (
        db.query(
            Room.id.label("sala_id"),
            Room.nombre.label("sala_nombre"),
            total_expr,
            escaparon_expr,
            avg_expr,
        )
        .select_from(Game)
        .join(Booking, Game.reserva_id == Booking.id)
        .join(Room, Booking.sala_id == Room.id)
        .filter(Game.hora_fin_real.isnot(None))
    )
    query = _apply_date_filter(query, Booking.fecha, fecha_inicio, fecha_fin)
    rows = query.group_by(Room.id, Room.nombre).all()

    max_pistas = max((float(row.promedio or 0) for row in rows), default=1) or 1

    ranked = []
    for row in rows:
        total = row.total or 0
        escapadas = row.escapadas or 0
        tasa = round(escapadas / total * 100, 2) if total > 0 else 0.0
        promedio = round(float(row.promedio or 0), 2)
        hints_factor = (1 - promedio / max_pistas) * 40
        score = round((tasa * 0.6) + hints_factor, 2)

        ranked.append(
            RankingItem(
                posicion=0,
                sala_id=row.sala_id,
                sala_nombre=row.sala_nombre,
                tasa_escape=tasa,
                promedio_pistas=promedio,
                total_partidas=total,
                score=score,
            )
        )

    ranked.sort(key=lambda x: x.score, reverse=True)
    for i, item in enumerate(ranked):
        item.posicion = i + 1

    return ranked


def get_summary(db: Session) -> SummaryResponse:
    # Caché con TTL dinámico según ENVIRONMENT (PERF-02)
    now = time.monotonic()
    if _summary_cache["result"] is not None and now - _summary_cache["ts"] < _SUMMARY_CACHE_TTL:
        return _summary_cache["result"]

    # BUG-04: usar la zona horaria del negocio para que "hoy" coincida con la
    # fecha local de las reservas, no con la UTC del servidor.
    tz = ZoneInfo(settings.TIMEZONE)
    today = datetime.now(tz).date()

    booking_stats = db.query(
        func.count(Booking.id).label("total"),
        func.sum(case((Booking.fecha == today, 1), else_=0)).label("hoy"),
        func.sum(case((Booking.estado == EstadoReserva.en_curso, 1), else_=0)).label("en_curso"),
    ).one()
    total_reservas = booking_stats.total or 0
    reservas_hoy = booking_stats.hoy or 0
    partidas_en_curso = booking_stats.en_curso or 0

    game_stats = db.query(
        func.count(Game.id).label("total"),
        func.sum(case((Game.escaparon.isnot(None), 1), else_=0)).label("con_resultado"),
        func.sum(case((Game.escaparon == True, 1), else_=0)).label("escapadas"),
    ).one()
    total_partidas = game_stats.total or 0
    partidas_con_resultado = game_stats.con_resultado or 0
    escapadas = game_stats.escapadas or 0
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

    total_salas = db.query(func.count(Room.id)).filter(Room.activa == True).scalar() or 0
    incidencias_pendientes = (
        db.query(func.count(Incident.id)).filter(Incident.resuelta == False).scalar() or 0
    )

    result = SummaryResponse(
        total_reservas=total_reservas,
        total_partidas=total_partidas,
        porcentaje_escape_global=porcentaje_escape,
        sala_mas_popular=sala_mas_popular,
        reservas_hoy=reservas_hoy,
        partidas_en_curso=partidas_en_curso,
        total_salas=total_salas,
        incidencias_pendientes=incidencias_pendientes,
    )
    _summary_cache["result"] = result
    _summary_cache["ts"] = now
    return result
