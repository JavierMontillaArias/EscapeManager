from datetime import datetime, timezone
from typing import Optional

from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session, joinedload

from app.models.game import Game
from app.models.booking import Booking, EstadoReserva
from app.models.user import User
from app.schemas.game import GameCloseRequest


def get_games(db: Session, current_user: User, skip: int = 0, limit: int = 50) -> list[Game]:
    """
    Manager: todas las partidas (con paginación para evitar carga masiva).
    Game Master: solo las suyas.
    P-02: joinedload(Game.gamemaster) evita N+1 queries al serializar GameResponse.
    B-08: skip/limit para paginación.
    """
    query = db.query(Game).options(joinedload(Game.gamemaster))

    if current_user.rol.nombre == "Game Master":
        query = query.filter(Game.gamemaster_id == current_user.id)

    return query.order_by(Game.hora_inicio_real.desc()).offset(skip).limit(limit).all()


def get_game_by_id(db: Session, game_id: int, current_user: User) -> Optional[Game]:
    # P-02: joinedload para evitar lazy load de gamemaster al serializar
    game = (
        db.query(Game)
        .options(joinedload(Game.gamemaster))
        .filter(Game.id == game_id)
        .first()
    )
    if game is None:
        return None
    if current_user.rol.nombre == "Game Master" and game.gamemaster_id != current_user.id:
        return None
    return game


def add_hint(db: Session, game_id: int, current_user: User) -> Optional[Game]:
    """
    Incrementa pistas_usadas en +1.
    Solo para partidas en curso.
    """
    game = db.query(Game).options(joinedload(Game.reserva)).filter(Game.id == game_id).first()
    if game is None:
        return None

    if current_user.rol.nombre == "Game Master" and game.gamemaster_id != current_user.id:
        return None

    if game.reserva.estado != EstadoReserva.en_curso:
        raise ValueError("No se pueden añadir pistas a una partida que no está en curso")

    if game.hora_fin_real is not None:
        raise ValueError("La partida ya está cerrada")

    game.pistas_usadas += 1
    try:
        db.commit()
        db.refresh(game)
    except SQLAlchemyError:
        db.rollback()
        raise

    return game


def close_game(db: Session, game_id: int, data: GameCloseRequest, current_user: User) -> Optional[Game]:
    """
    Cierra la partida registrando resultado y hora de fin.
    Cambia la reserva a estado 'completada'.
    """
    game = (
        db.query(Game)
        .options(joinedload(Game.reserva), joinedload(Game.gamemaster))
        .filter(Game.id == game_id)
        .first()
    )
    if game is None:
        return None

    if current_user.rol.nombre == "Game Master" and game.gamemaster_id != current_user.id:
        return None

    if game.hora_fin_real is not None:
        raise ValueError("Esta partida ya fue cerrada anteriormente")

    if game.reserva.estado != EstadoReserva.en_curso:
        raise ValueError("Solo se pueden cerrar partidas cuya reserva esté en curso")

    game.hora_fin_real = datetime.now(timezone.utc).replace(tzinfo=None)
    game.escaparon = data.escaparon
    game.observaciones = data.observaciones
    game.reserva.estado = EstadoReserva.completada

    try:
        db.commit()
        db.refresh(game)
    except SQLAlchemyError:
        db.rollback()
        raise

    return game
