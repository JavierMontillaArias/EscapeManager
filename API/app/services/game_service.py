from datetime import datetime, timezone
from typing import Optional

from sqlalchemy.orm import Session

from app.models.game import Game
from app.models.booking import Booking, EstadoReserva
from app.models.user import User
from app.schemas.game import GameCloseRequest


def get_games(db: Session, current_user: User) -> list[Game]:
    """
    Manager: todas las partidas.
    Game Master: solo las suyas.
    """

    query = db.query(Game)

    if current_user.rol.nombre == "Game Master":
        query = query.filter(Game.gamemaster_id == current_user.id)

    return query.order_by(Game.hora_inicio_real.desc()).all()

def get_game_by_id(db: Session, game_id: int, current_user: User) -> Optional[Game]:
    game = db.query(Game).filter(Game.id == game_id).first()
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
    game = db.query(Game).filter(Game.id == game_id).first()
    if game is None:
        return None

    if current_user.rol.nombre == "Game Master" and game.gamemaster_id != current_user.id:
        return None

    if game.reserva.estado != EstadoReserva.en_curso:
        raise ValueError("No se pueden añadir pistas a una partida que no está en curso")

    if game.hora_fin_real is not None:
        raise ValueError("La partida ya está cerrada")

    game.pistas_usadas += 1
    db.commit()
    db.refresh(game)
    return game


def close_game(db: Session, game_id: int, data: GameCloseRequest, current_user: User) -> Optional[Game]:
    """
    Cierra la partida registrando resultado y hora de fin.
    Cambia la reserva a estado 'completada'.
    """
    game = db.query(Game).filter(Game.id == game_id).first()
    if game is None:
        return None

    if current_user.rol.nombre == "Game Master" and game.gamemaster_id != current_user.id:
        return None

    if game.hora_fin_real is not None:
        raise ValueError("Esta partida ya fue cerrada anteriormente")

    if game.reserva.estado != EstadoReserva.en_curso:
        raise ValueError("Solo se pueden cerrar partidas cuya reserva esté en curso")

    game.hora_fin_real = datetime.now()
    game.escaparon = data.escaparon
    game.observaciones = data.observaciones
    game.reserva.estado = EstadoReserva.completada

    db.commit()
    db.refresh(game)
    return game