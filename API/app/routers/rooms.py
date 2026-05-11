from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import require_manager
from app.models.user import User
from app.schemas.room import RoomCreate, RoomUpdate, RoomResponse
from app.services import room_service

router = APIRouter(prefix="/rooms", tags=["Salas"])


@router.get("", response_model=list[RoomResponse])
def list_rooms(activa: bool | None = Query(default=None), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return room_service.get_all_rooms(db, only_active=activa is True)


@router.get("/{room_id}", response_model=RoomResponse)
def get_room(room_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    room = room_service.get_room_by_id(db, room_id)
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sala no encontrada")
    return room


@router.post("", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
def create_room(data: RoomCreate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return room_service.create_room(db, data)


@router.put("/{room_id}", response_model=RoomResponse)
def update_room(room_id: int, data: RoomUpdate, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        room = room_service.update_room(db, room_id, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sala no encontrada")
    return room


@router.delete("/{room_id}", response_model=RoomResponse)
def deactivate_room(room_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        room = room_service.deactivate_room(db, room_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if room is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Sala no encontrada")
    return room