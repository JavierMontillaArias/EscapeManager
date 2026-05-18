from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_gamemaster
from app.models.user import User
from app.schemas.game import GameResponse, GameCloseRequest, GameHintResponse
from app.services import game_service

router = APIRouter(prefix="/games", tags=["Partidas"])


# B-08: Paginación para evitar carga masiva de todas las partidas.
@router.get("", response_model=list[GameResponse])
def list_games(
    skip: int = Query(default=0, ge=0, description="Número de registros a omitir"),
    limit: int = Query(default=50, ge=1, le=200, description="Máximo de registros a devolver"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return game_service.get_games(db, current_user, skip=skip, limit=limit)


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
    return GameHintResponse(id=game.id, pistas_usadas=game.pistas_usadas, message="Pista registrada correctamente")


@router.post("/{game_id}/close", response_model=GameResponse)
def close_game(
    game_id: int,
    data: GameCloseRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_gamemaster),
):
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
