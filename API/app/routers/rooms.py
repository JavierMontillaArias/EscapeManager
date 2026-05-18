from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_manager
from app.limiter import limiter
from app.models.user import User
from app.schemas.room import RoomCreate, RoomUpdate, RoomResponse
from app.services import room_service

router = APIRouter(prefix="/rooms", tags=["Salas"])


@router.get("", response_model=list[RoomResponse])
@limiter.limit("60/minute")
def list_rooms(request: Request, activa: bool | None = Query(default=None), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return room_service.get_all_rooms(db, only_active=activa is True)


@router.get("/active", response_model=list[RoomResponse])
@limiter.limit("60/minute")
def list_active_rooms(request: Request, db: Session = Depends(get_db), _: User = Depends(get_current_user)):
    """Devuelve solo las salas activas. Accesible a cualquier usuario autenticado (Manager y Game Master)."""
    return room_service.get_all_rooms(db, only_active=True)


@router.get("/{room_id}", response_model=RoomResponse)
@limiter.limit("60/minute")
def get_room(request: Request, room_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    room = room_service.get_room_by_id(db, room_id)
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sala no encontrada")
    return room


@router.post("", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
@limiter.limit("30/minute")
def create_room(request: Request, data: RoomCreate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return room_service.create_room(db, data)


@router.put("/{room_id}", response_model=RoomResponse)
@limiter.limit("30/minute")
def update_room(request: Request, room_id: int, data: RoomUpdate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        room = room_service.update_room(db, room_id, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sala no encontrada")
    return room


@router.delete("/{room_id}", response_model=RoomResponse)
@limiter.limit("30/minute")
def deactivate_room(request: Request, room_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        room = room_service.deactivate_room(db, room_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sala no encontrada")
    return room
