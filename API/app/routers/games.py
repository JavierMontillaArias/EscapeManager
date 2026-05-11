from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_gamemaster
from app.models.user import User
from app.schemas.game import GameResponse, GameCloseRequest, GameHintResponse
from app.services import game_service

router = APIRouter(prefix="/games", tags=["Partidas"])


@router.get("", response_model=list[GameResponse])
def list_games(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    return game_service.get_games(db, current_user)


@router.get("/{game_id}", response_model=GameResponse)
def get_game(game_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    game = game_service.get_game_by_id(db, game_id, current_user)
    if game is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Partida no encontrada o sin acceso",
        )
    return game


@router.patch("/{game_id}/hints", response_model=GameHintResponse)
def add_hint(game_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_gamemaster)):
    try:
        game = game_service.add_hint(db, game_id, current_user)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if game is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Partida no encontrada o sin acceso",
        )
    return GameHintResponse(id=game.id, pistas_usadas=game.pistas_usadas)


@router.post("/{game_id}/close", response_model=GameResponse)
def close_game(game_id: int, data: GameCloseRequest, db: Session = Depends(get_db), current_user: User = Depends(require_gamemaster)):
    try:
        game = game_service.close_game(db, game_id, data, current_user)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if game is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Partida no encontrada o sin acceso",
        )
    return game